package cn.sdu.radar.service.impl;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.ApplicationRecordMapper;
import cn.sdu.radar.pojo.ApplicationRecord;
import cn.sdu.radar.pojo.dto.ApplicationCreateDTO;
import cn.sdu.radar.pojo.dto.ApplicationUpdateDTO;
import cn.sdu.radar.pojo.vo.ApplicationVO;
import cn.sdu.radar.service.ApplicationRecordService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationRecordService {
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "PREPARING", "APPLIED", "INTERVIEW", "OFFER", "REJECTED"));

    private final ApplicationRecordMapper applicationMapper;
    private final JobClient jobClient;

    @Autowired
    public ApplicationServiceImpl(ApplicationRecordMapper applicationMapper, JobClient jobClient) {
        this.applicationMapper = applicationMapper;
        this.jobClient = jobClient;
    }

    @Override
    public ApplicationVO create(Long userId, ApplicationCreateDTO input) {
        if (input == null || input.getJobId() == null) {
            throw new BusinessException(400, "请选择岗位");
        }
        JobSummaryVO job = requireJob(input.getJobId());
        Integer count = applicationMapper.selectCount(new QueryWrapper<ApplicationRecord>()
                .eq("user_id", userId).eq("job_id", input.getJobId()));
        if (count != null && count > 0) {
            throw new BusinessException(409, "该岗位已有投递记录");
        }
        LocalDateTime now = LocalDateTime.now();
        ApplicationRecord record = new ApplicationRecord();
        record.setUserId(userId);
        record.setJobId(input.getJobId());
        record.setStatus("PREPARING");
        record.setProgressNote(valueOrEmpty(input.getProgressNote()));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        applicationMapper.insert(record);
        return toVO(record, job);
    }

    @Override
    public ApplicationVO update(Long userId, Long id, ApplicationUpdateDTO input) {
        ApplicationRecord record = id == null ? null : applicationMapper.selectById(id);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(404, "投递记录不存在");
        }
        String status = input == null ? null : input.getStatus();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException(400, "投递状态不正确");
        }
        record.setStatus(status);
        record.setProgressNote(valueOrEmpty(input.getProgressNote()));
        if ("APPLIED".equals(status) && record.getAppliedAt() == null) {
            record.setAppliedAt(LocalDateTime.now());
        }
        record.setUpdatedAt(LocalDateTime.now());
        applicationMapper.updateById(record);
        return toVO(record, requireJob(record.getJobId()));
    }

    @Override
    public List<ApplicationVO> list(Long userId) {
        return applicationMapper.selectList(new QueryWrapper<ApplicationRecord>()
                        .eq("user_id", userId).orderByDesc("updated_at"))
                .stream()
                .map(record -> toVO(record, requireJob(record.getJobId())))
                .collect(Collectors.toList());
    }

    private JobSummaryVO requireJob(Long jobId) {
        CommonResult<JobSummaryVO> result = jobClient.getJob(jobId);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(503, "岗位服务暂不可用");
        }
        return result.getData();
    }

    private ApplicationVO toVO(ApplicationRecord record, JobSummaryVO job) {
        ApplicationVO vo = new ApplicationVO();
        vo.setId(record.getId());
        vo.setJob(job);
        vo.setStatus(record.getStatus());
        vo.setProgressNote(record.getProgressNote());
        vo.setAppliedAt(record.getAppliedAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

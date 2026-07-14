package cn.sdu.radar.service.impl;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.event.ApplicationStatusEvent;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.ApplicationRecordMapper;
import cn.sdu.radar.mq.ApplicationEventPublisher;
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
import java.util.UUID;

@Service
public class ApplicationServiceImpl implements ApplicationRecordService {
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "PREPARING", "APPLIED", "INTERVIEW", "OFFER", "REJECTED"));

    private final ApplicationRecordMapper applicationMapper;
    private final JobClient jobClient;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ApplicationServiceImpl(ApplicationRecordMapper applicationMapper, JobClient jobClient,
                                  ApplicationEventPublisher eventPublisher) {
        this.applicationMapper = applicationMapper;
        this.jobClient = jobClient;
        this.eventPublisher = eventPublisher;
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
        record.setStatus("APPLIED");
        record.setProgressNote(valueOrEmpty(input.getProgressNote()));
        record.setAppliedAt(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        applicationMapper.insert(record);
        publishEvent(record, job, "已成功投递「" + job.getTitle() + "」");
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
        JobSummaryVO job = requireJob(record.getJobId());
        publishEvent(record, job, "投递状态已更新为 " + status);
        return toVO(record, job);
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

    private void publishEvent(ApplicationRecord record, JobSummaryVO job, String message) {
        eventPublisher.publish(new ApplicationStatusEvent(
                UUID.randomUUID().toString(), record.getUserId(), record.getJobId(),
                job.getTitle(), job.getCompany(), record.getStatus(), message, LocalDateTime.now()));
    }
}

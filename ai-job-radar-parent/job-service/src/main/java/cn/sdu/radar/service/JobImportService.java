package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.JobMapper;
import cn.sdu.radar.pojo.Job;
import cn.sdu.radar.pojo.dto.JobImportDTO;
import cn.sdu.radar.pojo.vo.JobImportResultVO;
import cn.sdu.radar.search.JobDocument;
import cn.sdu.radar.search.JobSearchRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobImportService {
    private static final int MAX_BATCH_SIZE = 50;

    private final JobMapper jobMapper;
    private final JobSearchRepository jobSearchRepository;

    @Autowired
    public JobImportService(JobMapper jobMapper, JobSearchRepository jobSearchRepository) {
        this.jobMapper = jobMapper;
        this.jobSearchRepository = jobSearchRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = {"jobs", "job-detail"}, allEntries = true)
    public JobImportResultVO importJobs(List<JobImportDTO> jobs) {
        if (jobs == null || jobs.isEmpty() || jobs.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(400, "每次必须导入 1 到 50 个岗位");
        }

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        List<Job> imported = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < jobs.size(); index++) {
            try {
                JobImportDTO item = jobs.get(index);
                validate(item);
                Job existing = findExisting(item.getSource().trim(), item.getExternalId().trim());
                Job target = existing == null ? new Job() : existing;
                copyFields(item, target, now);
                if (existing == null) {
                    jobMapper.insert(target);
                    created++;
                } else {
                    jobMapper.updateById(target);
                    updated++;
                }
                imported.add(target);
            } catch (IllegalArgumentException exception) {
                errors.add("第 " + (index + 1) + " 条：" + exception.getMessage());
            }
        }

        if (!imported.isEmpty()) {
            try {
                jobSearchRepository.saveAll(imported.stream()
                        .map(JobDocument::from)
                        .collect(Collectors.toList()));
            } catch (RuntimeException exception) {
                log.warn("Imported jobs were saved to MySQL but Elasticsearch indexing failed: {}",
                        exception.getMessage());
            }
        }

        JobImportResultVO result = new JobImportResultVO();
        result.setCreated(created);
        result.setUpdated(updated);
        result.setRejected(errors.size());
        result.setErrors(errors);
        return result;
    }

    private Job findExisting(String source, String externalId) {
        QueryWrapper<Job> query = new QueryWrapper<>();
        query.eq("source", source).eq("external_id", externalId);
        return jobMapper.selectOne(query);
    }

    private void validate(JobImportDTO item) {
        if (item == null) throw new IllegalArgumentException("岗位数据不能为空");
        require(item.getSource(), "来源", 20);
        if (!"BOSS".equals(item.getSource().trim())) {
            throw new IllegalArgumentException("仅支持 BOSS 来源");
        }
        require(item.getExternalId(), "外部职位编号", 100);
        require(item.getTitle(), "职位标题", 100);
        require(item.getCompany(), "公司名称", 100);
        require(item.getCity(), "城市", 50);
        require(item.getEducation(), "学历", 50);
        require(item.getDescription(), "职位描述", 1000);
        require(item.getRequirements(), "职位要求", 1000);
        optional(item.getWelfareTags(), "福利标签", 300);
        optional(item.getSourceUrl(), "来源链接", 1000);
        optional(item.getSalaryText(), "薪资文本", 50);
        int salaryMin = item.getSalaryMin() == null ? 0 : item.getSalaryMin();
        int salaryMax = item.getSalaryMax() == null ? 0 : item.getSalaryMax();
        if (salaryMin < 0 || salaryMax < salaryMin) {
            throw new IllegalArgumentException("薪资范围不正确");
        }
    }

    private void require(String value, String name, int maxLength) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(name + "不能为空");
        optional(value, name, maxLength);
    }

    private void optional(String value, String name, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(name + "长度不能超过 " + maxLength);
        }
    }

    private void copyFields(JobImportDTO source, Job target, LocalDateTime now) {
        target.setSource(source.getSource().trim());
        target.setExternalId(source.getExternalId().trim());
        target.setTitle(source.getTitle().trim());
        target.setCompany(source.getCompany().trim());
        target.setCity(source.getCity().trim());
        target.setSalaryMin(source.getSalaryMin() == null ? 0 : source.getSalaryMin());
        target.setSalaryMax(source.getSalaryMax() == null ? 0 : source.getSalaryMax());
        target.setSalaryText(source.getSalaryText() == null ? "" : source.getSalaryText().trim());
        target.setExperienceYears(source.getExperienceYears() == null ? 0 : source.getExperienceYears());
        target.setEducation(source.getEducation().trim());
        target.setDescription(source.getDescription().trim());
        target.setRequirements(source.getRequirements().trim());
        target.setWelfareTags(source.getWelfareTags() == null ? "" : source.getWelfareTags().trim());
        target.setSourceUrl(source.getSourceUrl() == null ? "" : source.getSourceUrl().trim());
        target.setStatus(StringUtils.hasText(source.getStatus()) ? source.getStatus().trim() : "OPEN");
        target.setImportedAt(now);
        if (target.getPostedAt() == null) target.setPostedAt(now);
    }
}

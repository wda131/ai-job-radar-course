package cn.sdu.radar.service.impl;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.JobMapper;
import cn.sdu.radar.pojo.Job;
import cn.sdu.radar.search.JobSearchService;
import cn.sdu.radar.service.JobService;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final JobSearchService jobSearchService;

    @Autowired
    public JobServiceImpl(JobMapper jobMapper, JobSearchService jobSearchService) {
        this.jobMapper = jobMapper;
        this.jobSearchService = jobSearchService;
    }

    @Override
    @Cacheable(cacheNames = "jobs", key = "#keyword + ':' + #city + ':' + #minSalary + ':' + #page + ':' + #size")
    public PageResult<JobSummaryVO> search(String keyword, String city, Integer minSalary,
                                           long page, long size) {
        if (page < 1 || size < 1 || size > 50) {
            throw new BusinessException(400, "分页参数不正确");
        }
        if (StringUtils.hasText(keyword)) {
            try {
                PageResult<JobSummaryVO> result = jobSearchService.search(
                        keyword, city, minSalary, page, size);
                if (result != null) {
                    return result;
                }
            } catch (RuntimeException exception) {
                // Elasticsearch不可用时继续执行原MyBatis Plus查询。
            }
        }
        QueryWrapper<Job> query = new QueryWrapper<>();
        query.eq("status", "OPEN");
        if (StringUtils.hasText(keyword)) {
            String term = keyword.trim();
            query.and(wrapper -> wrapper.like("title", term).or().like("company", term));
        }
        if (StringUtils.hasText(city)) {
            query.eq("city", city.trim());
        }
        if (minSalary != null && minSalary > 0) {
            query.ge("salary_min", minSalary);
        }
        query.orderByDesc("posted_at");

        Page<Job> result = jobMapper.selectPage(new Page<>(page, size), query);
        List<JobSummaryVO> records = result.getRecords().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    @Override
    @Cacheable(cacheNames = "job-detail", key = "#id")
    public JobSummaryVO getById(Long id) {
        Job job = id == null ? null : jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        return toSummary(job);
    }

    private JobSummaryVO toSummary(Job job) {
        JobSummaryVO result = new JobSummaryVO();
        result.setId(job.getId());
        result.setTitle(job.getTitle());
        result.setCompany(job.getCompany());
        result.setCity(job.getCity());
        result.setSalaryMin(job.getSalaryMin());
        result.setSalaryMax(job.getSalaryMax());
        result.setExperienceYears(job.getExperienceYears());
        result.setEducation(job.getEducation());
        result.setDescription(job.getDescription());
        result.setRequirements(job.getRequirements());
        result.setWelfareTags(job.getWelfareTags());
        result.setStatus(job.getStatus());
        result.setPostedAt(job.getPostedAt());
        return result;
    }
}

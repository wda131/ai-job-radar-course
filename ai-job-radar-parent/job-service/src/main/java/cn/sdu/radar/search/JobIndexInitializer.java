package cn.sdu.radar.search;

import cn.sdu.radar.mapper.JobMapper;
import cn.sdu.radar.pojo.Job;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobIndexInitializer {
    private final JobMapper jobMapper;
    private final JobSearchRepository repository;

    @Autowired
    public JobIndexInitializer(JobMapper jobMapper, JobSearchRepository repository) {
        this.jobMapper = jobMapper;
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildIndex() {
        try {
            List<Job> jobs = jobMapper.selectList(new QueryWrapper<Job>().eq("status", "OPEN"));
            repository.saveAll(jobs.stream().map(JobDocument::from).collect(Collectors.toList()));
        } catch (RuntimeException exception) {
            // Elasticsearch是增强能力，索引失败时保留MySQL查询路径。
        }
    }
}

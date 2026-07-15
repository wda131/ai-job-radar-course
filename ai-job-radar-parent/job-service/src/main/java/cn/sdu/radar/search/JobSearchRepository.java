package cn.sdu.radar.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, Long> {
}

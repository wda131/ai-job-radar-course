package cn.sdu.radar.search;

import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.PageResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;

@Service
public class JobSearchService {
    private final ElasticsearchOperations operations;

    @Autowired
    public JobSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    public PageResult<JobSummaryVO> search(String keyword, String city, Integer minSalary,
                                           long page, long size) {
        BoolQueryBuilder bool = boolQuery().must(termQuery("status", "OPEN"));
        if (StringUtils.hasText(keyword)) {
            bool.must(multiMatchQuery(keyword.trim(),
                    "title", "company", "description", "requirements"));
        }
        if (StringUtils.hasText(city)) {
            bool.filter(termQuery("city", city.trim()));
        }
        if (minSalary != null && minSalary > 0) {
            bool.filter(rangeQuery("salaryMin").gte(minSalary));
        }
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withPageable(PageRequest.of((int) page - 1, (int) size))
                .withSort(fieldSort("postedAtEpoch").order(SortOrder.DESC))
                .build();
        SearchHits<JobDocument> hits = operations.search(query, JobDocument.class);
        List<JobSummaryVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(JobDocument::toSummary)
                .collect(Collectors.toList());
        return new PageResult<>(records, hits.getTotalHits(), page, size);
    }
}

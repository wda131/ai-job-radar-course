package cn.sdu.radar.service;

import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.PageResult;

public interface JobService {
    PageResult<JobSummaryVO> search(String keyword, String city, Integer minSalary,
                                    long page, long size);

    JobSummaryVO getById(Long id);
}

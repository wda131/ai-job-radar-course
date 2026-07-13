package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.FavoriteMapper;
import cn.sdu.radar.service.impl.FavoriteServiceImpl;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FavoriteServiceImplTest {
    private FavoriteMapper favoriteMapper;
    private JobClient jobClient;
    private FavoriteServiceImpl favoriteService;

    @BeforeEach
    void setUp() {
        favoriteMapper = mock(FavoriteMapper.class);
        jobClient = mock(JobClient.class);
        favoriteService = new FavoriteServiceImpl(favoriteMapper, jobClient);
        JobSummaryVO job = new JobSummaryVO();
        job.setId(3L);
        job.setTitle("Java开发工程师");
        when(jobClient.getJob(3L)).thenReturn(CommonResult.success(job));
    }

    @Test
    void rejectsDuplicateFavorite() {
        when(favoriteMapper.selectCount(any())).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> favoriteService.add(1L, 3L));

        assertEquals(409, exception.getCode());
    }
}

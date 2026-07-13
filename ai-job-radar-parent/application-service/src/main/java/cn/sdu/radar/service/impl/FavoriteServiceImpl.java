package cn.sdu.radar.service.impl;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.FavoriteMapper;
import cn.sdu.radar.pojo.Favorite;
import cn.sdu.radar.pojo.vo.FavoriteVO;
import cn.sdu.radar.service.FavoriteService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final JobClient jobClient;

    @Autowired
    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, JobClient jobClient) {
        this.favoriteMapper = favoriteMapper;
        this.jobClient = jobClient;
    }

    @Override
    public FavoriteVO add(Long userId, Long jobId) {
        JobSummaryVO job = requireJob(jobId);
        Integer count = favoriteMapper.selectCount(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("job_id", jobId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "该岗位已收藏");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setJobId(jobId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return toVO(favorite, job);
    }

    @Override
    public void remove(Long userId, Long jobId) {
        int deleted = favoriteMapper.delete(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("job_id", jobId));
        if (deleted == 0) {
            throw new BusinessException(404, "收藏记录不存在");
        }
    }

    @Override
    public List<FavoriteVO> list(Long userId) {
        return favoriteMapper.selectList(new QueryWrapper<Favorite>()
                        .eq("user_id", userId).orderByDesc("created_at"))
                .stream()
                .map(favorite -> toVO(favorite, requireJob(favorite.getJobId())))
                .collect(Collectors.toList());
    }

    private JobSummaryVO requireJob(Long jobId) {
        CommonResult<JobSummaryVO> result = jobClient.getJob(jobId);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(503, "岗位服务暂不可用");
        }
        return result.getData();
    }

    private FavoriteVO toVO(Favorite favorite, JobSummaryVO job) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setJob(job);
        vo.setCreatedAt(favorite.getCreatedAt());
        return vo;
    }
}

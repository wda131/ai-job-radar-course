package cn.sdu.radar.service;

import cn.sdu.radar.pojo.vo.FavoriteVO;

import java.util.List;

public interface FavoriteService {
    FavoriteVO add(Long userId, Long jobId);

    void remove(Long userId, Long jobId);

    List<FavoriteVO> list(Long userId);
}

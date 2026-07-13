package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.vo.FavoriteVO;
import cn.sdu.radar.service.FavoriteService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public CommonResult<List<FavoriteVO>> list() {
        return CommonResult.success(favoriteService.list(UserContext.getUserId()));
    }

    @PostMapping("/{jobId}")
    public CommonResult<FavoriteVO> add(@PathVariable Long jobId) {
        return CommonResult.success(favoriteService.add(UserContext.getUserId(), jobId));
    }

    @DeleteMapping("/{jobId}")
    public CommonResult<Void> remove(@PathVariable Long jobId) {
        favoriteService.remove(UserContext.getUserId(), jobId);
        return CommonResult.success(null);
    }
}

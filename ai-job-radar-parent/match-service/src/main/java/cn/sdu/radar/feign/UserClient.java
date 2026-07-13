package cn.sdu.radar.feign;

import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.UserProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserClient {
    @GetMapping("/api/user/internal/{id}")
    CommonResult<UserProfileVO> getProfile(@PathVariable("id") Long id);
}

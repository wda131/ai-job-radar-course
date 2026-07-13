package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.dto.LoginDTO;
import cn.sdu.radar.pojo.dto.ProfileUpdateDTO;
import cn.sdu.radar.pojo.vo.LoginVO;
import cn.sdu.radar.service.UserService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public CommonResult<LoginVO> login(@RequestBody LoginDTO input) {
        return CommonResult.success(userService.login(input));
    }

    @GetMapping("/profile")
    public CommonResult<UserProfileVO> profile() {
        return CommonResult.success(userService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    public CommonResult<UserProfileVO> updateProfile(@RequestBody ProfileUpdateDTO input) {
        return CommonResult.success(userService.updateProfile(UserContext.getUserId(), input));
    }

    @GetMapping("/internal/{id}")
    public CommonResult<UserProfileVO> internalProfile(@PathVariable Long id) {
        return CommonResult.success(userService.getProfile(id));
    }
}

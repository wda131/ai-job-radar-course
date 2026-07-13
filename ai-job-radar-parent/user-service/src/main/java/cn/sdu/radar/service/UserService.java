package cn.sdu.radar.service;

import cn.sdu.radar.pojo.dto.LoginDTO;
import cn.sdu.radar.pojo.dto.ProfileUpdateDTO;
import cn.sdu.radar.pojo.vo.LoginVO;
import cn.sdu.radar.vo.UserProfileVO;

public interface UserService {
    LoginVO login(LoginDTO input);

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, ProfileUpdateDTO input);
}

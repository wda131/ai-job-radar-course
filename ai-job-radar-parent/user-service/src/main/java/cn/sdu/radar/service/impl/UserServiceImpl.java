package cn.sdu.radar.service.impl;

import cn.sdu.radar.auth.JwtUtils;
import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.UserMapper;
import cn.sdu.radar.pojo.User;
import cn.sdu.radar.pojo.dto.LoginDTO;
import cn.sdu.radar.pojo.dto.ProfileUpdateDTO;
import cn.sdu.radar.pojo.vo.LoginVO;
import cn.sdu.radar.service.UserService;
import cn.sdu.radar.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginVO login(LoginDTO input) {
        if (input == null || !StringUtils.hasText(input.getUsername())
                || !StringUtils.hasText(input.getPassword())) {
            throw new BusinessException(400, "请输入用户名和密码");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", input.getUsername().trim()));
        if (user == null || !user.getPassword().equals(input.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return new LoginVO(JwtUtils.generateToken(user.getId(), user.getUsername()),
                toProfile(user));
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Override
    public UserProfileVO updateProfile(Long userId, ProfileUpdateDTO input) {
        if (input == null || !StringUtils.hasText(input.getName())
                || !StringUtils.hasText(input.getTargetRole())) {
            throw new BusinessException(400, "姓名和目标岗位不能为空");
        }
        if (input.getSalaryMin() == null || input.getSalaryMax() == null
                || input.getSalaryMin() < 0 || input.getSalaryMax() < input.getSalaryMin()) {
            throw new BusinessException(400, "薪资范围不正确");
        }
        User user = requireUser(userId);
        user.setName(input.getName().trim());
        user.setTargetRole(input.getTargetRole().trim());
        user.setCity(valueOrEmpty(input.getCity()));
        user.setSkills(valueOrEmpty(input.getSkills()));
        user.setExperienceYears(input.getExperienceYears() == null ? 0 : input.getExperienceYears());
        user.setEducation(valueOrEmpty(input.getEducation()));
        user.setSalaryMin(input.getSalaryMin());
        user.setSalaryMax(input.getSalaryMax());
        user.setIntroduction(valueOrEmpty(input.getIntroduction()));
        userMapper.updateById(user);
        return toProfile(user);
    }

    private User requireUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private UserProfileVO toProfile(User user) {
        UserProfileVO result = new UserProfileVO();
        result.setId(user.getId());
        result.setUsername(user.getUsername());
        result.setName(user.getName());
        result.setTargetRole(user.getTargetRole());
        result.setCity(user.getCity());
        result.setSkills(user.getSkills());
        result.setExperienceYears(user.getExperienceYears());
        result.setEducation(user.getEducation());
        result.setSalaryMin(user.getSalaryMin());
        result.setSalaryMax(user.getSalaryMax());
        result.setIntroduction(user.getIntroduction());
        return result;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

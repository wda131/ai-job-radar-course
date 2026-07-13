package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.UserMapper;
import cn.sdu.radar.pojo.User;
import cn.sdu.radar.pojo.dto.LoginDTO;
import cn.sdu.radar.pojo.dto.ProfileUpdateDTO;
import cn.sdu.radar.pojo.vo.LoginVO;
import cn.sdu.radar.service.impl.UserServiceImpl;
import cn.sdu.radar.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserMapper userMapper;
    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(userMapper);
        user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setPassword("123456");
        user.setName("山威同学");
        user.setTargetRole("Java后端开发");
        user.setCity("威海");
        user.setSkills("Java,Spring Boot,MySQL");
        user.setExperienceYears(1);
        user.setEducation("本科");
        user.setSalaryMin(7000);
        user.setSalaryMax(12000);
        user.setIntroduction("课程项目开发经验");
    }

    @Test
    void loginReturnsBearerTokenAndProfile() {
        when(userMapper.selectOne(any())).thenReturn(user);
        LoginDTO input = new LoginDTO();
        input.setUsername("student");
        input.setPassword("123456");

        LoginVO result = userService.login(input);

        assertTrue(result.getToken().startsWith("bearer "));
        assertEquals("山威同学", result.getProfile().getName());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user);
        LoginDTO input = new LoginDTO();
        input.setUsername("student");
        input.setPassword("wrong");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(input));

        assertEquals(401, exception.getCode());
    }

    @Test
    void updateProfileChangesOnlyProfileFields() {
        when(userMapper.selectById(1L)).thenReturn(user);
        ProfileUpdateDTO input = new ProfileUpdateDTO();
        input.setName("更新后的同学");
        input.setTargetRole("全栈开发");
        input.setCity("青岛");
        input.setSkills("Java,Vue");
        input.setExperienceYears(2);
        input.setEducation("本科");
        input.setSalaryMin(9000);
        input.setSalaryMax(14000);
        input.setIntroduction("能够独立完成前后端功能");

        UserProfileVO result = userService.updateProfile(1L, input);

        assertEquals("更新后的同学", result.getName());
        assertEquals("student", user.getUsername());
        assertEquals("123456", user.getPassword());
        verify(userMapper).updateById(user);
    }
}

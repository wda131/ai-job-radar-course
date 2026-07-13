package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.UserProfileVO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private UserProfileVO profile;
}

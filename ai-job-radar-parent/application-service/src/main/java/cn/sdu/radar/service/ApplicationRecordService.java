package cn.sdu.radar.service;

import cn.sdu.radar.pojo.dto.ApplicationCreateDTO;
import cn.sdu.radar.pojo.dto.ApplicationUpdateDTO;
import cn.sdu.radar.pojo.vo.ApplicationVO;

import java.util.List;

public interface ApplicationRecordService {
    ApplicationVO create(Long userId, ApplicationCreateDTO input);

    ApplicationVO update(Long userId, Long id, ApplicationUpdateDTO input);

    List<ApplicationVO> list(Long userId);
}

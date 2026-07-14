package cn.sdu.radar.service.impl;

import cn.sdu.radar.event.ApplicationStatusEvent;
import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.NotificationMapper;
import cn.sdu.radar.pojo.Notification;
import cn.sdu.radar.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;

    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void consume(ApplicationStatusEvent event) {
        Integer count = notificationMapper.selectCount(new QueryWrapper<Notification>()
                .eq("event_id", event.getEventId()));
        if (count != null && count > 0) {
            return;
        }
        Notification notification = new Notification();
        notification.setEventId(event.getEventId());
        notification.setUserId(event.getUserId());
        notification.setType("APPLICATION");
        notification.setTitle("投递状态更新 · " + event.getJobTitle());
        notification.setContent(event.getMessage() + "（" + event.getCompany() + "）");
        notification.setReadStatus(false);
        notification.setCreatedAt(event.getOccurredAt() == null
                ? LocalDateTime.now() : event.getOccurredAt());
        notificationMapper.insert(notification);
    }

    @Override
    public List<Notification> list(Long userId) {
        return notificationMapper.selectList(new QueryWrapper<Notification>()
                .eq("user_id", userId).orderByAsc("read_status").orderByDesc("created_at"));
    }

    @Override
    public int unreadCount(Long userId) {
        Integer count = notificationMapper.selectCount(new QueryWrapper<Notification>()
                .eq("user_id", userId).eq("read_status", false));
        return count == null ? 0 : count;
    }

    @Override
    public void markRead(Long userId, Long id) {
        Notification notification = notificationMapper.selectOne(new QueryWrapper<Notification>()
                .eq("id", id).eq("user_id", userId));
        if (notification == null) {
            throw new BusinessException(404, "消息不存在");
        }
        notification.setReadStatus(true);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new UpdateWrapper<Notification>()
                .eq("user_id", userId).eq("read_status", false).set("read_status", true));
    }

}

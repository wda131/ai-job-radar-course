package cn.sdu.radar.service;

import cn.sdu.radar.event.ApplicationStatusEvent;
import cn.sdu.radar.pojo.Notification;

import java.util.List;

public interface NotificationService {
    void consume(ApplicationStatusEvent event);
    List<Notification> list(Long userId);
    int unreadCount(Long userId);
    void markRead(Long userId, Long id);
    void markAllRead(Long userId);
}

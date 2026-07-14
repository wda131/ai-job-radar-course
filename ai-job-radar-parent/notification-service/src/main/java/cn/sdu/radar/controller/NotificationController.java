package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.Notification;
import cn.sdu.radar.service.NotificationService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public CommonResult<List<Notification>> list() {
        return CommonResult.success(notificationService.list(UserContext.getUserId()));
    }

    @GetMapping("/unread-count")
    public CommonResult<Map<String, Integer>> unreadCount() {
        return CommonResult.success(Collections.singletonMap("count",
                notificationService.unreadCount(UserContext.getUserId())));
    }

    @PutMapping("/{id}/read")
    public CommonResult<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContext.getUserId(), id);
        return CommonResult.success(null);
    }

    @PutMapping("/read-all")
    public CommonResult<Void> markAllRead() {
        notificationService.markAllRead(UserContext.getUserId());
        return CommonResult.success(null);
    }
}

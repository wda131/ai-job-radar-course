package cn.sdu.radar.service;

import cn.sdu.radar.event.ApplicationStatusEvent;
import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.NotificationMapper;
import cn.sdu.radar.pojo.Notification;
import cn.sdu.radar.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {
    private NotificationMapper mapper;
    private NotificationServiceImpl service;
    private ApplicationStatusEvent event;

    @BeforeEach
    void setUp() {
        mapper = mock(NotificationMapper.class);
        service = new NotificationServiceImpl(mapper);
        event = new ApplicationStatusEvent("event-1", 1L, 4L, "Java 开发工程师",
                "海纳科技", "APPLIED", "已成功投递", LocalDateTime.now());
    }

    @Test
    void consumesApplicationEventOnce() {
        when(mapper.selectCount(any())).thenReturn(0);

        service.consume(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(captor.capture());
        assertEquals("event-1", captor.getValue().getEventId());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(false, captor.getValue().getReadStatus());
    }

    @Test
    void ignoresDuplicateEvent() {
        when(mapper.selectCount(any())).thenReturn(1);

        service.consume(event);

        verify(mapper, never()).insert(any());
    }

    @Test
    void returnsUserNotificationsAndUnreadCount() {
        Notification notification = new Notification();
        notification.setUserId(1L);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(notification));
        when(mapper.selectCount(any())).thenReturn(2);

        assertEquals(1, service.list(1L).size());
        assertEquals(2, service.unreadCount(1L));
    }

    @Test
    void marksOwnedNotificationAsRead() {
        Notification notification = new Notification();
        notification.setId(8L);
        notification.setUserId(1L);
        notification.setReadStatus(false);
        when(mapper.selectOne(any())).thenReturn(notification);

        service.markRead(1L, 8L);

        assertEquals(true, notification.getReadStatus());
        verify(mapper).updateById(notification);
    }

    @Test
    void rejectsReadingAnotherUsersNotification() {
        when(mapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.markRead(1L, 99L));

        assertEquals(404, exception.getCode());
    }

    @Test
    void marksAllUserNotificationsAsRead() {
        service.markAllRead(1L);

        verify(mapper).update(any(), any());
    }
}

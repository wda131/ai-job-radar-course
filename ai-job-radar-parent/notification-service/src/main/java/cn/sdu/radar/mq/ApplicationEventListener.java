package cn.sdu.radar.mq;

import cn.sdu.radar.config.RabbitMqConfig;
import cn.sdu.radar.event.ApplicationStatusEvent;
import cn.sdu.radar.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventListener {
    private final NotificationService notificationService;

    @Autowired
    public ApplicationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void onApplicationChanged(ApplicationStatusEvent event) {
        notificationService.consume(event);
    }
}

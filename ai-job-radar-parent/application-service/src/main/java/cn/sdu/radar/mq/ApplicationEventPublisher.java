package cn.sdu.radar.mq;

import cn.sdu.radar.config.RabbitMqConfig;
import cn.sdu.radar.event.ApplicationStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ApplicationEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public ApplicationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ApplicationStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, event);
        } catch (AmqpException exception) {
            log.warn("RabbitMQ unavailable, application event {} was not delivered",
                    event.getEventId(), exception);
        }
    }
}

package cn.sdu.radar.config;

import cn.sdu.radar.event.ApplicationStatusEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMqConfigTest {
    @Test
    void roundTripsApplicationEventWithLocalDateTime() {
        Jackson2JsonMessageConverter converter = new RabbitMqConfig().rabbitMessageConverter();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 14, 22, 30, 15);
        ApplicationStatusEvent event = new ApplicationStatusEvent(
                "event-1", 1L, 2L, "Java开发工程师", "齐鲁软件",
                "INTERVIEW", "投递状态已更新", occurredAt);

        Message message = converter.toMessage(event, new MessageProperties());
        Object converted = converter.fromMessage(message);

        assertTrue(converted instanceof ApplicationStatusEvent);
        assertEquals(occurredAt, ((ApplicationStatusEvent) converted).getOccurredAt());
    }
}

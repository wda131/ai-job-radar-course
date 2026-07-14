package cn.sdu.radar.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long userId;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String status;
    private String message;
    private LocalDateTime occurredAt;
}

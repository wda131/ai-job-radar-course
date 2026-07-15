package cn.sdu.radar.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiQuestionResponse {
    private boolean available;
    private List<String> questions;
}

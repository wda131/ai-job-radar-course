package cn.sdu.radar.pojo.vo;

import lombok.Data;

@Data
public class InterviewQuestionVO {
    private Long id;
    private Integer questionOrder;
    private String question;
    private InterviewAnswerVO answer;
}

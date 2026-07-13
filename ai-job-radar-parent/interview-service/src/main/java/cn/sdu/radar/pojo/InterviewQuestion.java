package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("interview_questions")
public class InterviewQuestion {
    private Long id;
    private Long sessionId;
    private Integer questionOrder;
    private String question;
    private String referenceKeywords;
}

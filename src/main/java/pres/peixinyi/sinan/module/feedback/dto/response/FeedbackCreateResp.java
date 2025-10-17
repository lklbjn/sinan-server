package pres.peixinyi.sinan.module.feedback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈创建响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackCreateResp {

    /**
     * 反馈ID
     */
    private String feedbackId;

    /**
     * 成功消息
     */
    private String message;
}
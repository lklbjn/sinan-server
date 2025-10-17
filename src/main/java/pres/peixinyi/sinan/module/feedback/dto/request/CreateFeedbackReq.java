package pres.peixinyi.sinan.module.feedback.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建反馈请求DTO
 */
@Data
public class CreateFeedbackReq {

    /**
     * 联系方式（邮箱或手机号）
     */
    @NotBlank(message = "联系方式不能为空")
    @Size(max = 100, message = "联系方式长度不能超过100个字符")
    private String contact;

    /**
     * 反馈内容
     */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容长度不能超过2000个字符")
    private String content;
}
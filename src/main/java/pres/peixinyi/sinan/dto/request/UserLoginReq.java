package pres.peixinyi.sinan.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求
 */
@Data
public class UserLoginReq {

    /**
     * 登录凭证（用户名或邮箱）
     */
    @NotBlank(message = "登录凭证不能为空")
    private String credential;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}

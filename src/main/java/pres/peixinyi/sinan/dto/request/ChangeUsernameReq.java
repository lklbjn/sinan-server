package pres.peixinyi.sinan.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改用户名请求
 */
@Data
public class ChangeUsernameReq {

    /**
     * 新用户名
     */
    @NotBlank(message = "新用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String newUsername;
}
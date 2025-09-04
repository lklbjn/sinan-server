package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑标签请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class EditPasskeyDescribeReq {

    /**
     * passkey ID
     */
    @NotNull(message = "ID不能为空")
    private Long id;

    /**
     * 描述
     */
    private String describe;
}

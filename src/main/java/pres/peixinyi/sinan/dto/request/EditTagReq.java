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
public class EditTagReq {

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空")
    private String id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 标签描述
     */
    private String description;
}

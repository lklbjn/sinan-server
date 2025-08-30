package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 新增标签请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class AddTagReq {

    /**
     * 标签名称
     */
    @NotEmpty(message = "标签名称不能为空")
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

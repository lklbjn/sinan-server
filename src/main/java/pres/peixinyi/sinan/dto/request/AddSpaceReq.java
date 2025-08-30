package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 新增空间请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class AddSpaceReq {

    /**
     * 空间名称
     */
    @NotEmpty(message = "空间名称不能为空")
    private String name;

    /**
     * 空间图标
     */
    private String icon;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 空间描述
     */
    private String description;
}

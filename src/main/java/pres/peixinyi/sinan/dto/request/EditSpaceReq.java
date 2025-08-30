package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑空间请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class EditSpaceReq {

    /**
     * 空间ID
     */
    @NotNull(message = "空间ID不能为空")
    private String id;

    /**
     * 空间名称
     */
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

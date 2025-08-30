package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 空间分享更新请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/27 14:46
 * @Version : 0.0.0
 */
@Data
public class ShareSpaceUpdateReq {

    /**
     * 空间ID
     */
    @NotNull(message = "空间ID不能为空")
    private String spaceId;

    /**
     * 是否分享
     */
    @NotNull(message = "分享状态不能为空")
    private Boolean enable;

    /**
     * 分享密码
     */
    private String key;

}

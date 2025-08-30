package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移除收藏用户请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/27 15:10
 * @Version : 0.0.0
 */
@Data
public class RemoveCollectionUserReq {

    @NotNull(message = "空间ID不能为空")
    private String spaceId;

    @NotNull(message = "用户ID不能为空")
    private String userId;

}

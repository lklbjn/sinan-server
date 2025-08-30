package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 新增接收书签请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/14
 * @Version : 0.0.0
 */
@Data
public class AddReceivedBookmarkReq {

    /**
     * 书签名称
     */
    @NotEmpty(message = "书签名称不能为空")
    private String name;

    /**
     * 书签URL
     */
    @NotEmpty(message = "书签URL不能为空")
    private String url;

    /**
     * 书签描述
     */
    private String description;

    /**
     * 书签空间
     */
    private String namespaceId;

    /**
     * 书签组
     */
    private String group;

    /**
     * 书签Icon
     */
    private Integer icon;

    /**
     * 书签标签
     */
    private String tag;
}

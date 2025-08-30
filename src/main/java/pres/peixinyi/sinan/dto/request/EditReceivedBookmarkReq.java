package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑接收书签请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/14
 * @Version : 0.0.0
 */
@Data
public class EditReceivedBookmarkReq {

    /**
     * 书签ID
     */
    @NotNull(message = "书签ID不能为空")
    private String id;

    /**
     * 书签名称
     */
    private String name;

    /**
     * 书签URL
     */
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

package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13 20:50
 * @Version : 0.0.0
 */
@Data
public class EditBookmarkReq {

    @NotNull
    private String id;

    /**
     * 书签名称
     */
    private String name;

    /**
     * 书签URL
     */
    private String url;

    private String icon;

    /**
     * 书签描述
     */
    private String description;

    /**
     * 书签空间
     */
    private String namespaceId;

    /**
     * 书签标签
     */
    private List<String> tags;

}

package pres.peixinyi.sinan.dto.request;

import jakarta.validation.constraints.NotEmpty;
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
public class AddBookmarkReq {

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
     * 书签标签
     */
    private List<String> tagsIds;

    /**
     * 新建空间信息
     */
    private NewSpace newSpace;

    /**
     * 新建标签信息
     */
    private List<NewTag> newTags;

    /**
     * 新建空间信息
     */
    @Data
    public static class NewSpace {
        /**
         * 空间名称
         */
        @NotEmpty(message = "空间名称不能为空")
        private String name;

        /**
         * 空间描述
         */
        private String description;

        /**
         * 空间图标
         */
        private String icon;
    }

    /**
     * 新建标签信息
     */
    @Data
    public static class NewTag {
        /**
         * 标签名称
         */
        @NotEmpty(message = "标签名称不能为空")
        private String name;

        /**
         * 标签颜色
         */
        @NotEmpty(message = "标签颜色不能为空")
        private String color;

        /**
         * 标签描述
         */
        private String description;
    }

}

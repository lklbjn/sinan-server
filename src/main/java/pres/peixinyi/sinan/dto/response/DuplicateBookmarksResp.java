package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 重复书签响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class DuplicateBookmarksResp {

    /**
     * 重复书签组列表
     */
    private List<DuplicateGroupResp> duplicates;

    /**
     * 统计信息
     */
    private StatsResp stats;

    @Data
    public static class DuplicateGroupResp {

        /**
         * 重复组标识（URL）
         */
        private String group;

        /**
         * 重复书签列表
         */
        private List<DuplicateBookmarkResp> bookmarks;
    }

    @Data
    public static class DuplicateBookmarkResp {

        /**
         * 书签ID
         */
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
         * 图标URL
         */
        private String icon;

        /**
         * 所属空间名称
         */
        private SpaceResp space;

        /**
         * 标签数组
         */
        private List<TagResp> tags;

        /**
         * 创建时间
         */
        private String createTime;
    }

    @Data
    public static class StatsResp {

        /**
         * 总书签数
         */
        private Long totalBookmarks;

        /**
         * 重复组数
         */
        private Integer duplicateGroups;

        /**
         * 重复书签总数
         */
        private Long duplicateCount;
    }
}
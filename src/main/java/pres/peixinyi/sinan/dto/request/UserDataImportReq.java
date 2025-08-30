package pres.peixinyi.sinan.dto.request;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 用户数据导入请求
 */
@Data
public class UserDataImportReq {

    /**
     * 标签列表
     */
    private List<TagImportData> tags;

    /**
     * 空间列表
     */
    private List<SpaceImportData> space;

    /**
     * 书签列表
     */
    private List<BookmarkImportData> bookmark;

    /**
     * 标签导入数据
     */
    @Data
    public static class TagImportData {
        private String id;
        private String name;
        private String color;
        private Integer sort;
        private String description;
        private Date createTime;
        private Date updateTime;
    }

    /**
     * 空间导入数据
     */
    @Data
    public static class SpaceImportData {
        private String id;
        private String name;
        private String icon;
        private Integer sort;
        private String description;
        private Date createTime;
        private Date updateTime;
    }

    /**
     * 书签导入数据
     */
    @Data
    public static class BookmarkImportData {
        private String id;
        private String userId;
        private String spaceId;
        private String name;
        private String description;
        private String url;
        private String icon;
        private Integer num;
        private Boolean star;
        private Date createTime;
        private Date updateTime;
        private Integer deleted;
        private List<String> tags;
    }
}

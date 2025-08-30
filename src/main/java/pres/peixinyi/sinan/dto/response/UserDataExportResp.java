package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.model.sinan.entity.SnSpace;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;

import java.util.Date;
import java.util.List;

/**
 * 用户数据导出响应
 */
@Data
public class UserDataExportResp {

    /**
     * 标签列表
     */
    private List<TagExportData> tags;

    /**
     * 空间列表
     */
    private List<SpaceExportData> space;

    /**
     * 书签列表
     */
    private List<BookmarkExportData> bookmark;

    /**
     * 标签导出数据
     */
    @Data
    public static class TagExportData {
        private String id;
        private String name;
        private String color;
        private Integer sort;
        private String description;
        private Date createTime;
        private Date updateTime;

        public static TagExportData from(SnTag tag) {
            TagExportData data = new TagExportData();
            data.setId(tag.getId());
            data.setName(tag.getName());
            data.setColor(tag.getColor());
            data.setSort(tag.getSort());
            data.setDescription(tag.getDescription());
            data.setCreateTime(tag.getCreateTime());
            data.setUpdateTime(tag.getUpdateTime());
            return data;
        }
    }

    /**
     * 空间导出数据
     */
    @Data
    public static class SpaceExportData {
        private String id;
        private String name;
        private String icon;
        private Integer sort;
        private String description;
        private Date createTime;
        private Date updateTime;

        public static SpaceExportData from(SnSpace space) {
            SpaceExportData data = new SpaceExportData();
            data.setId(space.getId());
            data.setName(space.getName());
            data.setIcon(space.getIcon());
            data.setSort(space.getSort());
            data.setDescription(space.getDescription());
            data.setCreateTime(space.getCreateTime());
            data.setUpdateTime(space.getUpdateTime());
            return data;
        }
    }

    /**
     * 书签导出数据
     */
    @Data
    public static class BookmarkExportData {
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

        public static BookmarkExportData from(SnBookmark bookmark, List<String> tagIds) {
            BookmarkExportData data = new BookmarkExportData();
            data.setId(bookmark.getId());
            data.setUserId(bookmark.getUserId());
            data.setSpaceId(bookmark.getSpaceId());
            data.setName(bookmark.getName());
            data.setDescription(bookmark.getDescription());
            data.setUrl(bookmark.getUrl());
            data.setIcon(bookmark.getIcon());
            data.setNum(bookmark.getNum());
            data.setStar(bookmark.getStar());
            data.setCreateTime(bookmark.getCreateTime());
            data.setUpdateTime(bookmark.getUpdateTime());
            data.setDeleted(bookmark.getDeleted());
            data.setTags(tagIds);
            return data;
        }
    }
}

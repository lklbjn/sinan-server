package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnSpace;

import java.util.List;

/**
 * 书签树响应DTO
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/22
 * @Version : 1.0.0
 */
@Data
public class BookmarkTreeResp {

    /**
     * 空间ID
     */
    private String spaceId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间描述
     */
    private String spaceDescription;

    private Boolean shared;

    private String key;

    /**
     * 空间下的书签列表
     */
    private List<BookmarkResp> bookmarks;

    /**
     * 从空间实体创建响应对象
     */
    public static BookmarkTreeResp from(SnSpace space, List<BookmarkResp> bookmarks) {
        BookmarkTreeResp resp = new BookmarkTreeResp();
        resp.setSpaceId(space.getId());
        resp.setSpaceName(space.getName());
        resp.setSpaceDescription(space.getDescription());
        resp.setShared(space.getShare());
        resp.setKey(space.getShareKey());
        resp.setBookmarks(bookmarks != null ? bookmarks : List.of());
        return resp;
    }
}

package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;

import java.util.List;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13 20:43
 * @Version : 0.0.0
 */
@Data
public class BookmarkResp {

    /**
     * 书签ID
     */
    private String id;

    /**
     * 空间
     */
    private String namespaceId;

    /**
     * 书签名称
     */
    private String name;

    /**
     * 书签描述
     */
    private String description;

    /**
     * 书签url
     */
    private String url;

    /**
     * 书签Icon
     */
    private String icon;

    /**
     * 使用次数
     */
    private Integer num;

    /**
     * 星标
     */
    private Boolean star;

    /**
     * 书签标签列表
     */
    private List<TagResp> tags;

    public static BookmarkResp from(SnBookmark snBookmark) {
        BookmarkResp bookmarkVO = new BookmarkResp();
        bookmarkVO.setId(snBookmark.getId());
        bookmarkVO.setNamespaceId(snBookmark.getSpaceId());
        bookmarkVO.setName(snBookmark.getName());
        bookmarkVO.setDescription(snBookmark.getDescription());
        bookmarkVO.setUrl(snBookmark.getUrl());
        bookmarkVO.setIcon(snBookmark.getIcon());
        bookmarkVO.setNum(snBookmark.getNum());
        bookmarkVO.setStar(snBookmark.getStar());
        return bookmarkVO;
    }

    /**
     * 从书签实体和标签列表创建响应对象
     *
     * @param snBookmark 书签实体
     * @param tags       标签列表
     * @return 书签响应对象
     */
    public static BookmarkResp from(SnBookmark snBookmark, List<SnTag> tags) {
        BookmarkResp bookmarkResp = from(snBookmark);
        if (tags != null && !tags.isEmpty()) {
            bookmarkResp.setTags(tags.stream()
                    .map(TagResp::from)
                    .toList());
        } else {
            bookmarkResp.setTags(List.of());
        }
        return bookmarkResp;
    }

}

package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnReceivedBookmark;

import java.util.Date;

/**
 * 接收书签响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/14
 * @Version : 0.0.0
 */
@Data
public class ReceivedBookmarkResp {

    /**
     * 书签ID
     */
    private String id;

    /**
     * 空间ID
     */
    private String namespaceId;

    /**
     * 书签组
     */
    private String group;

    /**
     * 书签名称
     */
    private String name;

    /**
     * 书签描述
     */
    private String description;

    /**
     * 书签URL
     */
    private String url;

    /**
     * 书签Icon
     */
    private Integer icon;

    /**
     * 书签标签
     */
    private String tag;

    /**
     * 状态 1为接收，2为已确认，3为删除
     */
    private Integer state;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 从实体类转换为响应对象
     *
     * @param receivedBookmark 接收书签实体
     * @return 接收书签响应对象
     */
    public static ReceivedBookmarkResp from(SnReceivedBookmark receivedBookmark) {
        ReceivedBookmarkResp resp = new ReceivedBookmarkResp();
        resp.setId(receivedBookmark.getId());
        resp.setNamespaceId(receivedBookmark.getNamespaceId());
        resp.setGroup(receivedBookmark.getGroup());
        resp.setName(receivedBookmark.getName());
        resp.setDescription(receivedBookmark.getDescription());
        resp.setUrl(receivedBookmark.getUrl());
        resp.setTag(receivedBookmark.getTag());
        resp.setState(receivedBookmark.getState());
        resp.setCreateTime(receivedBookmark.getCreateTime());
        resp.setUpdateTime(receivedBookmark.getUpdateTime());
        return resp;
    }
}

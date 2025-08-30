package pres.peixinyi.sinan.model.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 书签
 */
@Data
@TableName(value = "sn_received_bookmark")
public class SnReceivedBookmark {
    /**
     * 书签ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 空间
     */
    @TableField(value = "namespace_id")
    private String namespaceId;

    /**
     * 书签组
     */
    @TableField(value = "`group`")
    private String group;

    /**
     * 书签名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * 书签描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 书签url
     */
    @TableField(value = "url")
    private String url;

    /**
     * 书签标签
     */
    @TableField(value = "tag")
    private String tag;

    /**
     * 状态 1为接收，2,为已确认 3,为删除
     */
    @TableField(value = "`state`")
    private Integer state;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
package pres.peixinyi.sinan.model.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 书签关联标签
 */
@Data
@TableName(value = "sn_bookmark_ass_tag")
public class SnBookmarkAssTag {
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
     * 书签ID
     */
    @TableField(value = "bookmark_id")
    private String bookmarkId;

    /**
     * 标签ID
     */
    @TableField(value = "tag_id")
    private String tagId;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
package pres.peixinyi.sinan.module.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 */
@Data
@TableName(value = "sn_user")
public class SnUser {
    /**
     * 书签ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 空间名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * 用户头像
     */
    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
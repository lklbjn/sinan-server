package pres.peixinyi.sinan.model.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 空间
 */
@Data
@TableName(value = "sn_space")
public class SnSpace {
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
     * 空间名称
     */
    @TableField(value = "`name`")
    private String name;

    /**
     * 拼音
     */
    @TableField(value = "pinyin")
    private String pinyin;

    /**
     * 简写-自动生成首字母
     */
    @TableField(value = "abbreviation")
    private String abbreviation;

    @TableField(value = "icon")
    private String icon;

    /**
     * 排序
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 是否分享
     */
    @TableField(value = "`share`")
    private Boolean share;

    /**
     * 分享密码
     */
    @TableField(value = "share_key")
    private String shareKey;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
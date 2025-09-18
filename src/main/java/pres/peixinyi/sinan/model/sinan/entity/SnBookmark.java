package pres.peixinyi.sinan.model.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

/**
 * 书签
 */
@Data
@TableName(value = "sn_bookmark")
public class SnBookmark {
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
    @TableField(value = "space_id")
    private String spaceId;

    /**
     * 书签名称
     */
    @TableField(value = "`name`")
    private String name;

    @TableField(value = "pinyin")
    private String pinyin;

    /**
     * 简写-自动生成首字母
     */
    @TableField(value = "abbreviation")
    private String abbreviation;

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
     * 书签Icon
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 使用次数
     */
    @TableField(value = "num")
    private Integer num;

    /**
     * 是否检查重复时忽略
     */
    @TableField(value = "ignore_duplicate")
    private Boolean ignoreDuplicate;

    @TableField(value = "star")
    private Boolean star;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SnBookmark that = (SnBookmark) o;
        return Objects.equals(name, that.name) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }
}
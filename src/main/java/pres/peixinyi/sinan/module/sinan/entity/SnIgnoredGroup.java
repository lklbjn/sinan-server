package pres.peixinyi.sinan.module.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 忽略组实体
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
@TableName("sn_ignored_groups")
public class SnIgnoredGroup {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 删除标记 (0:未删除, 1:已删除)
     */
    private Integer deleted;
}
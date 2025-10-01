package pres.peixinyi.sinan.module.sinan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 凭证
 */
@Data
@TableName(value = "sn_credential")
public class SnCredential {
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
     * 凭证类型
     */
    @TableField(value = "credential_type")
    private String credentialType;

    /**
     * 凭证
     */
    @TableField(value = "credential")
    private String credential;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
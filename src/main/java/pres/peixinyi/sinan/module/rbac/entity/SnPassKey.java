package pres.peixinyi.sinan.module.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yubico.webauthn.data.ByteArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sn_pass_key
 * @author wangbinzhe
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "sn_pass_key")
public class SnPassKey implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 展示名称
     */
    @TableField(value = "display_name")
    private String displayName;

    /**
     * Passkey公钥
     */
    @TableField(value = "public_key")
    private ByteArray publicKey;

    /**
     * WebAuthn凭证ID
     */
    @TableField(value = "credential_id")
    private ByteArray credentialId;

    /**
     * 签名计数
     */
    @TableField(value = "sign_count")
    private Long signCount;

    /**
     * 用户句柄
     */
    @TableField(value = "user_handle")
    private ByteArray userHandle;

    /**
     * 描述
     */
    @TableField(value = "`describe`")
    private String describe;

    /**
     * createTime
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * updateTime
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * lastUsed
     */
    @TableField(value = "last_used")
    private Date lastUsed;

    /**
     * deleted
     */
    @TableField(value = "deleted")
    private Integer deleted;

    public static final String COL_ID = "id";

    public static final String COL_DISPLAY_NAME = "display_name";

    public static final String COL_PUBLIC_KEY = "public_key";

    public static final String COL_CREDENTIAL_ID = "credential_id";

    public static final String COL_SIGN_COUNT = "sign_count";

    public static final String COL_USER_HANDLE = "user_handle";

    public static final String COL_CREATE_TIME = "create_time";

    public static final String COL_UPDATE_TIME = "update_time";

    public static final String COL_LAST_USED = "last_used";

    public static final String COL_DELETED = "deleted";
}
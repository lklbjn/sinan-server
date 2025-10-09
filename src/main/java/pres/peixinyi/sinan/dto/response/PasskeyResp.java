package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.module.rbac.entity.SnPassKey;

import java.util.Date;

/**
 * @author wangbinzhe
 */
@Data
public class PasskeyResp {
    /**
     * id
     */
    private String id;

    /**
     * 描述
     */
    private String describe;

    /**
     * createTime
     */
    private Date createTime;

    /**
     * lastUsed
     */
    private Date lastUsed;


    public static PasskeyResp from(SnPassKey snPassKey) {
        if (snPassKey == null) {
            return null;
        }
        PasskeyResp passkeyResp = new PasskeyResp();
        passkeyResp.setId(snPassKey.getId());
        passkeyResp.setDescribe(snPassKey.getDescribe());
        passkeyResp.setCreateTime(snPassKey.getCreateTime());
        passkeyResp.setLastUsed(snPassKey.getLastUsed());

        // Not mapped SnPassKey fields:
        // userId
        // displayName
        // publicKey
        // credentialId
        // signCount
        // userHandle
        // lastUsed
        // deleted
        return passkeyResp;
    }
}

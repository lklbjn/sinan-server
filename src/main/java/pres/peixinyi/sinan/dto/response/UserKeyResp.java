package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.module.rbac.entity.SnUserKey;

import java.util.Date;

/**
 * 用户密钥响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/22
 * @Version : 1.0.0
 */
@Data
public class UserKeyResp {

    /**
     * 密钥ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    private String keyName;

    private String description;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 从实体对象创建响应对象
     */
    public static UserKeyResp fromEntity(SnUserKey userKey) {
        UserKeyResp resp = new UserKeyResp();
        resp.setId(userKey.getId());
        resp.setUserId(userKey.getUserId());
        resp.setKeyName(userKey.getName());
        resp.setDescription(userKey.getDescription());
        resp.setAccessKey(userKey.getAccessKey());
        resp.setCreateTime(userKey.getCreateTime());
        resp.setUpdateTime(userKey.getUpdateTime());
        return resp;
    }

    /**
     * 从实体对象创建脱敏的响应对象
     *
     * @param userKey 用户密钥实体
     * @return 脱敏后的响应对象
     */
    public static UserKeyResp fromEntityWithMasking(SnUserKey userKey) {
        UserKeyResp resp = new UserKeyResp();
        resp.setId(userKey.getId());
        resp.setUserId(userKey.getUserId());
        resp.setKeyName(userKey.getName());
        resp.setDescription(userKey.getDescription());
        resp.setAccessKey(maskAccessKey(userKey.getAccessKey()));
        resp.setCreateTime(userKey.getCreateTime());
        resp.setUpdateTime(userKey.getUpdateTime());
        return resp;
    }

    /**
     * 对访问密钥进行脱敏处理
     *
     * @param accessKey 原始访问密钥
     * @return 脱敏后的访问密钥
     */
    private static String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.length() <= 8) {
            return accessKey;
        }

        // 保留前4位和后4位，中间用星号替换
        String prefix = accessKey.substring(0, 4);
        String suffix = accessKey.substring(accessKey.length() - 4);
        int maskLength = accessKey.length() - 8;

        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < maskLength; i++) {
            masked.append("*");
        }
        masked.append(suffix);

        return masked.toString();
    }
}

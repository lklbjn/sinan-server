package pres.peixinyi.sinan.model.rbac.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Date;
import java.util.UUID;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import pres.peixinyi.sinan.model.rbac.mapper.SnUserKeyMapper;
import pres.peixinyi.sinan.model.rbac.entity.SnUserKey;

@Service
public class SnUserKeyService extends ServiceImpl<SnUserKeyMapper, SnUserKey> {

    /**
     * 为用户创建新的访问密钥
     *
     * @param userId 用户ID
     * @return 创建的用户密钥对象
     */
    public SnUserKey createUserKey(String userId, String keyName, String description) {
        // 生成bk-uuid格式的访问密钥
        String accessKey = "bk-" + UUID.randomUUID().toString().replace("-", "");

        // 创建用户密钥对象
        SnUserKey userKey = new SnUserKey();
        userKey.setUserId(userId);
        userKey.setName(keyName);
        userKey.setAccessKey(accessKey);
        userKey.setDescription(description);
        userKey.setCreateTime(new Date());
        userKey.setUpdateTime(new Date());
        userKey.setDeleted(0);

        // 保存到数据库
        this.save(userKey);

        return userKey;
    }

    /**
     * 根据用户ID获取用户的所有密钥
     *
     * @param userId 用户ID
     * @return 用户密钥列表
     */
    public List<SnUserKey> getUserKeys(String userId) {
        return this.lambdaQuery()
                .eq(SnUserKey::getUserId, userId)
                .eq(SnUserKey::getDeleted, 0)
                .orderByDesc(SnUserKey::getCreateTime)
                .list();
    }

    /**
     * 删除用户密钥（软删除）
     *
     * @param keyId  密钥ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否删除成功
     */
    public boolean deleteUserKey(String keyId, String userId) {
        SnUserKey userKey = this.lambdaQuery()
                .eq(SnUserKey::getId, keyId)
                .eq(SnUserKey::getUserId, userId)
                .eq(SnUserKey::getDeleted, 0)
                .one();

        if (userKey != null) {
            return lambdaUpdate()
                    .eq(SnUserKey::getId, keyId)
                    .eq(SnUserKey::getId, keyId)
                    .eq(SnUserKey::getUserId, userId)
                    .eq(SnUserKey::getDeleted, 0)
                    .remove();
        }
        return false;
    }

    /**
     * 根据访问密钥获取用户ID
     *
     * @param accessKey 访问密钥
     * @return 用户ID，如果密钥无效则返回null
     */
    public String getUserIdByAccessKey(String accessKey) {
        if (accessKey == null || accessKey.trim().isEmpty()) {
            return null;
        }

        SnUserKey userKey = this.lambdaQuery()
                .eq(SnUserKey::getAccessKey, accessKey)
                .eq(SnUserKey::getDeleted, 0)
                .one();

        return userKey != null ? userKey.getUserId() : null;
    }

    /**
     * 验证访问密钥是否有效
     *
     * @param accessKey 访问密钥
     * @return 是否有效
     */
    public boolean isValidAccessKey(String accessKey) {
        return getUserIdByAccessKey(accessKey) != null;
    }
}
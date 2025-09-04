package pres.peixinyi.sinan.model.rbac.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.model.rbac.domain.CredentialType;
import pres.peixinyi.sinan.model.rbac.entity.SnUserCredential;
import pres.peixinyi.sinan.model.rbac.exception.UserRuntionException;
import pres.peixinyi.sinan.model.rbac.mapper.SnUserCredentialMapper;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SnUserCredentialService extends ServiceImpl<SnUserCredentialMapper, SnUserCredential> {

    /**
     * 创建用户凭证
     *
     * @param userId         用户ID
     * @param credentialType 凭证类型
     * @param credential     凭证值
     */
    public void createCredential(String userId, CredentialType type, String credential) {
        createCredential(userId, type, credential, null);
    }

    /**
     * 创建用户凭证
     *
     * @param userId     用户ID
     * @param type       凭证类型
     * @param credential 凭证值
     */
    public void createCredential(String userId, CredentialType type, String credential, String params) {
        SnUserCredential userCredential = new SnUserCredential();
        userCredential.setUserId(userId);
        userCredential.setCredentialType(type.name());
        userCredential.setCredential(credential);
        userCredential.setParams(params);
        save(userCredential);
    }

    /**
     * 检查凭证是否已存在
     *
     * @param credentialType 凭证类型
     * @param credential     凭证值
     * @return 是否存在
     */
    public boolean checkCredentialExists(CredentialType type, String credential) {
        return lambdaQuery()
                .eq(SnUserCredential::getCredentialType, type)
                .eq(SnUserCredential::getCredential, credential)
                .eq(SnUserCredential::getDeleted, 0)
                .exists();
    }

    /**
     * 根据凭证获取用户ID
     *
     * @param credentialType 凭证类型
     * @param credential     凭证值
     * @return 用户ID
     */
    public String getUserIdByCredential(CredentialType type, String credential) {
        SnUserCredential userCredential = lambdaQuery()
                .eq(SnUserCredential::getCredentialType, type)
                .eq(SnUserCredential::getCredential, credential)
                .eq(SnUserCredential::getDeleted, 0)
                .one();

        return userCredential != null ? userCredential.getUserId() : null;
    }

    /**
     * 根据用户ID获取密码
     *
     * @param userId 用户ID
     * @return 加密后的密码
     */
    public String getPasswordByUserId(String userId) {
        SnUserCredential userCredential = lambdaQuery()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, "password")
                .eq(SnUserCredential::getDeleted, 0)
                .one();

        return userCredential != null ? userCredential.getCredential() : null;
    }

    /**
     * 更新用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码（已加密）
     * @return 更新是否成功
     */
    public boolean updatePassword(String userId, String newPassword) {
        return lambdaUpdate()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, "password")
                .eq(SnUserCredential::getDeleted, 0)
                .set(SnUserCredential::getCredential, newPassword)
                .set(SnUserCredential::getUpdateTime, new Date())
                .update();
    }

    /**
     * 根据用户ID获取用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    public String getUsernameByUserId(String userId) {
        SnUserCredential userCredential = lambdaQuery()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, "username")
                .eq(SnUserCredential::getDeleted, 0)
                .one();

        return userCredential != null ? userCredential.getCredential() : null;
    }

    /**
     * 根据用户ID获取邮箱
     *
     * @param userId 用户ID
     * @return 邮箱
     */
    public String getEmailByUserId(String userId) {
        SnUserCredential userCredential = lambdaQuery()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, "email")
                .eq(SnUserCredential::getDeleted, 0)
                .one();

        return userCredential != null ? userCredential.getCredential() : null;
    }

    /**
     * 根据用户ID获取密码Secret
     *
     * @param userId 用户ID
     * @return 加密后的密码Secret
     */
    public String getPasswordSecretByUserId(String userId) {
        SnUserCredential userCredential = lambdaQuery()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, CredentialType.PASSWORD)
                .eq(SnUserCredential::getDeleted, 0)
                .one();
        return userCredential != null ? userCredential.getCredential() : null;
    }

    /**
     * 更新用户密码Secret
     *
     * @param userId            用户ID
     * @param newPasswordSecret 新密码（已加密）
     * @return 更新是否成功
     */
    public boolean updatePasswordSecret(String userId, String newPasswordSecret) {
        return lambdaUpdate()
                .eq(SnUserCredential::getUserId, userId)
                .eq(SnUserCredential::getCredentialType, CredentialType.PASSWORD)
                .set(SnUserCredential::getCredential, newPasswordSecret)
                .update();
    }

    public Boolean checkEmailExist(String email) {
        return lambdaQuery()
                .eq(SnUserCredential::getCredentialType, "email")
                .eq(SnUserCredential::getCredential, email)
                .exists();
    }

    public SnUserCredential getCredential(CredentialType credentialType, String identifier) {
        return lambdaQuery()
                .eq(SnUserCredential::getCredentialType, credentialType.name())
                .eq(SnUserCredential::getCredential, identifier)
                .one();
    }

    public SnUserCredential getCredentialByUserId(CredentialType credentialType, String userId) {
        return lambdaQuery()
                .eq(SnUserCredential::getCredentialType, credentialType.name())
                .eq(SnUserCredential::getUserId, userId)
                .last("limit 1")
                .one();
    }

    public Map<CredentialType, SnUserCredential> getCredentialByUserId(String userId) {
        return lambdaQuery()
                .eq(SnUserCredential::getUserId, userId)
                .list()
                .stream()
                .collect(Collectors.toMap(item -> CredentialType.from(item.getCredentialType()), Function.identity()));
    }

    public String getUserIdByEmail(String email) {
        return lambdaQuery()
                .eq(SnUserCredential::getCredentialType, CredentialType.EMAIL)
                .eq(SnUserCredential::getCredential, email)
                .oneOpt().map(SnUserCredential::getUserId).orElseThrow(() -> new UserRuntionException("用户不存在"));
    }
}

package pres.peixinyi.sinan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.utils.RedisUtils;

import java.util.concurrent.TimeUnit;

/**
 * 密码重置服务
 */
@Service
public class PasswordResetService {

    private static final String RESET_CODE_PREFIX = "password_reset:";
    private static final String USER_CODE_PREFIX = "password_reset_user:";

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 创建密码重置验证码
     *
     * @param userId 用户ID
     * @param code   验证码
     * @param expireMinutes 过期时间（分钟）
     */
    public void createPasswordResetCode(String userId, String code, int expireMinutes) {
        // 删除用户现有的密码重置验证码
        deletePasswordResetCodeByUserId(userId);

        // 保存验证码 -> 用户ID的映射，设置过期时间
        String codeKey = RESET_CODE_PREFIX + code;
        redisUtils.setEx(codeKey, userId, expireMinutes, TimeUnit.MINUTES);

        // 保存用户ID -> 验证码的映射，用于删除用户现有验证码
        String userKey = USER_CODE_PREFIX + userId;
        redisUtils.setEx(userKey, code, expireMinutes, TimeUnit.MINUTES);
    }

    /**
     * 验证密码重置验证码
     *
     * @param code 验证码
     * @return 用户ID，如果验证码无效或过期返回null
     */
    public String validatePasswordResetCode(String code) {
        String codeKey = RESET_CODE_PREFIX + code;
        String userId = redisUtils.get(codeKey);

        // 如果验证码存在且未过期，返回用户ID
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        return null;
    }

    /**
     * 删除密码重置验证码
     *
     * @param code 验证码
     */
    public void deletePasswordResetCode(String code) {
        String codeKey = RESET_CODE_PREFIX + code;
        String userId = redisUtils.get(codeKey);

        if (userId != null) {
            // 删除验证码 -> 用户ID的映射
            redisUtils.delete(codeKey);

            // 删除用户ID -> 验证码的映射
            String userKey = USER_CODE_PREFIX + userId;
            redisUtils.delete(userKey);
        }
    }

    /**
     * 根据用户ID删除密码重置验证码
     *
     * @param userId 用户ID
     */
    public void deletePasswordResetCodeByUserId(String userId) {
        String userKey = USER_CODE_PREFIX + userId;
        String code = redisUtils.get(userKey);

        if (code != null) {
            // 删除验证码 -> 用户ID的映射
            String codeKey = RESET_CODE_PREFIX + code;
            redisUtils.delete(codeKey);

            // 删除用户ID -> 验证码的映射
            redisUtils.delete(userKey);
        }
    }

    /**
     * 检查用户是否已有重置验证码
     *
     * @param userId 用户ID
     * @return 是否存在验证码
     */
    public boolean hasPasswordResetCode(String userId) {
        String userKey = USER_CODE_PREFIX + userId;
        return redisUtils.hasKey(userKey);
    }
}
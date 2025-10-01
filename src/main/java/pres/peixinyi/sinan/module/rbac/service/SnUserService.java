package pres.peixinyi.sinan.module.rbac.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import pres.peixinyi.sinan.module.rbac.domain.CredentialType;
import pres.peixinyi.sinan.module.rbac.entity.SnUser;
import pres.peixinyi.sinan.module.rbac.mapper.SnUserMapper;
import pres.peixinyi.sinan.exception.UserCredentialException;
import pres.peixinyi.sinan.service.EmailService;
import pres.peixinyi.sinan.service.PasswordResetService;

import static pres.peixinyi.sinan.module.rbac.domain.CredentialType.EMAIL;
import static pres.peixinyi.sinan.module.rbac.domain.CredentialType.USERNAME;

@Service
public class SnUserService extends ServiceImpl<SnUserMapper, SnUser> {

    private final SnUserCredentialService snUserCredentialService;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public SnUserService(SnUserCredentialService snUserCredentialService, EmailService emailService, PasswordResetService passwordResetService) {
        this.snUserCredentialService = snUserCredentialService;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * 密码加密（使用盐值增强安全性）
     *
     * @param password 原始密码
     * @param userId   用户ID作为盐值
     * @return 加密后的密码
     */
    private String encodePassword(String password, String userId) {
        try {
            // 使用用户ID作为盐值，增强安全性
            String saltedPassword = password + userId + "sinan_secret_salt";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new UserCredentialException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @param userId          用户ID
     * @return 是否匹配
     */
    private boolean matchesPassword(String rawPassword, String encodedPassword, String userId) {
        return encodePassword(rawPassword, userId).equals(encodedPassword);
    }

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param email    邮箱
     * @param password 密码
     * @return 创建的用户
     */
    public SnUser registerUser(String username, String email, String password) {
        // 检查用户名是否已存在
        if (snUserCredentialService.checkCredentialExists(USERNAME, username)) {
            throw new UserCredentialException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (snUserCredentialService.checkCredentialExists(EMAIL, email)) {
            throw new UserCredentialException("邮箱已被注册");
        }

        // 创建用户
        SnUser user = new SnUser();
        user.setName(username != null && !username.trim().isEmpty() ? username.trim() : username);
        user.setAvatar(""); // 默认头像
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setDeleted(0);

        // 保存用户
        save(user);

        // 创建用户名凭证
        snUserCredentialService.createCredential(user.getId(), USERNAME, username);

        // 创建邮箱凭证
        snUserCredentialService.createCredential(user.getId(), EMAIL, email);

        // 创建密码Secret - 使用增强的加密算法
        String hashedPassword = encodePassword(password, user.getId());
        snUserCredentialService.createCredential(user.getId(), CredentialType.PASSWORD, hashedPassword);


        return user;
    }

    /**
     * 用户登录验证
     *
     * @param credential 登录凭证（用户名或邮箱）
     * @param password   密码
     * @return 用户信息，如果验证失败返回null
     */
    public SnUser authenticateUser(String credential, String password) {
        // 根据凭证查找用户
        SnUser user = findUserByCredential(credential);
        if (user == null) {
            return null;
        }

        // 验证密码Secret
        String storedPasswordSecret = snUserCredentialService.getPasswordSecretByUserId(user.getId());
        if (storedPasswordSecret == null || !matchesPassword(password, storedPasswordSecret, user.getId())) {
            return null;
        }

        return user;
    }

    /**
     * 根据凭证查找用户
     *
     * @param credential 凭证（用户名或邮箱）
     * @return 用户信息
     */
    public SnUser findUserByCredential(String credential) {
        // 先尝试按用户名查找
        String userId = snUserCredentialService.getUserIdByCredential(USERNAME, credential);
        if (userId == null) {
            // 再尝试按邮箱查找
            userId = snUserCredentialService.getUserIdByCredential(EMAIL, credential);
        }

        if (userId != null) {
            return getById(userId);
        }

        return null;
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public SnUser getUserById(String userId) {
        return lambdaQuery().eq(SnUser::getId, userId).eq(SnUser::getDeleted, 0).one();
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param name   昵称
     * @param avatar 头像
     * @return 更新是否成功
     */
    public boolean updateUser(String userId, String name, String avatar) {
        return lambdaUpdate().eq(SnUser::getId, userId).eq(SnUser::getDeleted, 0).set(name != null, SnUser::getName, name).set(avatar != null, SnUser::getAvatar, avatar).set(SnUser::getUpdateTime, new Date()).update();
    }

    /**
     * 修改用户密码或创建密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码（如果用户没有密码则可为null或空）
     * @param newPassword 新密码
     * @return 修改是否成功
     */
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        // 获取当前存储的密码
        String storedPasswordSecret = snUserCredentialService.getPasswordSecretByUserId(userId);

        // 如果用户还没有密码，直接创建新密码
        if (storedPasswordSecret == null || storedPasswordSecret.trim().isEmpty()) {
            // 加密新密码并创建
            String newHashedPassword = encodePassword(newPassword, userId);
            snUserCredentialService.createCredential(userId, CredentialType.PASSWORD, newHashedPassword);
            return true;
        }

        // 如果用户已有密码，需要验证旧密码
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new UserCredentialException("用户已设置密码，请提供当前密码");
        }

        if (!matchesPassword(oldPassword, storedPasswordSecret, userId)) {
            throw new UserCredentialException("原密码错误");
        }

        // 加密新密码并更新
        String newHashedPassword = encodePassword(newPassword, userId);
        return snUserCredentialService.updatePasswordSecret(userId, newHashedPassword);
    }

    public SnUser createUser(SnUser snUser) {
        save(snUser);
        return snUser;
    }

    public SnUser createUser(String name) {
        SnUser snUser = new SnUser();
        snUser.setName(name);
        snUser.setCreateTime(new Date());
        save(snUser);
        return snUser;
    }

    public Boolean checkEmailExist(String email) {
        return snUserCredentialService.checkCredentialExists(EMAIL, email);
    }

    public SnUser getUserByName(String name) {
        return lambdaQuery()
                .eq(SnUser::getName, name)
                .last("limit 1")
                .one();
    }

    /**
     * 更新用户头像URL
     *
     * @param userId    用户ID
     * @param avatarUrl 头像URL
     * @return 更新是否成功
     */
    public boolean updateUserAvatar(String userId, String avatarUrl) {
        return lambdaUpdate()
                .eq(SnUser::getId, userId)
                .set(SnUser::getAvatar, avatarUrl)
                .update();
    }

    /**
     * 修改用户名
     *
     * @param userId      用户ID
     * @param newUsername 新用户名
     * @return 修改是否成功
     */
    public boolean changeUsername(String userId, String newUsername) {
        // 检查新用户名是否已存在
        if (snUserCredentialService.checkCredentialExists(USERNAME, newUsername)) {
            throw new UserCredentialException("用户名已存在");
        }

        // 获取当前用户名
        String currentUsername = snUserCredentialService.getUsernameByUserId(userId);
        if (currentUsername != null && currentUsername.equals(newUsername)) {
            throw new UserCredentialException("新用户名不能与当前用户名相同");
        }

        // 更新用户名凭证
        boolean credentialUpdated = snUserCredentialService.updateUsername(userId, newUsername);

        if (credentialUpdated) {
            // 同时更新用户表中的name字段
            return lambdaUpdate()
                    .eq(SnUser::getId, userId)
                    .eq(SnUser::getDeleted, 0)
                    .set(SnUser::getName, newUsername)
                    .set(SnUser::getUpdateTime, new Date())
                    .update();
        }

        return false;
    }

    /**
     * 请求密码重置
     *
     * @param email 用户邮箱
     * @return 是否成功发送重置邮件
     */
    public boolean requestPasswordReset(String email) {
        // 查找用户
        String userId = snUserCredentialService.getUserIdByCredential(EMAIL, email);
        if (userId == null) {
            throw new UserCredentialException("邮箱不存在");
        }

        // 生成重置验证码（32位UUID）
        String resetCode = UUID.randomUUID().toString().replace("-", "");

        try {
            // 将重置验证码保存到Redis，设置15分钟过期时间
            passwordResetService.createPasswordResetCode(userId, resetCode, 15);

            // 发送重置邮件
            emailService.sendPasswordResetEmail(email, resetCode);

            return true;
        } catch (Exception e) {
            throw new UserCredentialException("发送重置邮件失败: " + e.getMessage());
        }
    }

    /**
     * 重置密码
     *
     * @param code        重置验证码
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    public boolean resetPassword(String code, String newPassword) {
        // 验证重置验证码
        String userId = passwordResetService.validatePasswordResetCode(code);
        if (userId == null) {
            throw new UserCredentialException("验证码无效或已过期");
        }

        try {
            // 加密新密码
            String hashedPassword = encodePassword(newPassword, userId);

            // 更新密码
            boolean success = snUserCredentialService.updatePasswordSecret(userId, hashedPassword);

            if (success) {
                // 删除重置验证码
                passwordResetService.deletePasswordResetCode(code);
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new UserCredentialException("重置密码失败: " + e.getMessage());
        }
    }
}

package pres.peixinyi.sinan.module.rbac.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import net.coobird.thumbnailator.Thumbnails;
import pres.peixinyi.sinan.config.UploadProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.*;
import pres.peixinyi.sinan.dto.response.*;
import pres.peixinyi.sinan.module.rbac.entity.SnUser;
import pres.peixinyi.sinan.module.rbac.service.UserDataExportService;
import pres.peixinyi.sinan.module.rbac.service.SnUserService;
import pres.peixinyi.sinan.module.rbac.service.SnUserCredentialService;
import pres.peixinyi.sinan.module.rbac.service.SnUserKeyService;
import pres.peixinyi.sinan.module.rbac.entity.SnUserKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import pres.peixinyi.sinan.module.rbac.service.passkey.PasskeyAuthorizationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 用户控制层
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/12 21:37
 * @Version : 0.0.0
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserDataExportService userDataExportService;

    @Resource
    private SnUserService userService;

    @Resource
    private SnUserCredentialService snUserCredentialService;

    @Resource
    private SnUserKeyService snUserKeyService;

    @Resource
    private PasskeyAuthorizationService passkeyAuthorizationService;

    @Resource
    private UploadProperties uploadProperties;

    @Value("${sinan.server.base-url}")
    private String baseUrl;

    /**
     * 用户注册
     *
     * @param req 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<UserLoginResp> register(@Valid @RequestBody UserRegisterReq req) {
        try {
            // 执行注册
            SnUser user = userService.registerUser(
                    req.getUsername(),
                    req.getEmail(),
                    req.getPassword()
            );

            // 自动登录
            StpUtil.login(user.getId());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

            // 构建响应
            UserLoginResp loginResp = UserLoginResp.create(user, tokenInfo);

            return Result.success(loginResp);

        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * @param req 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<UserLoginResp> login(@Valid @RequestBody UserLoginReq req) {
        try {
            // 验证用户凭证
            SnUser user = userService.authenticateUser(req.getCredential(), req.getPassword());

            if (user == null) {
                return Result.fail("用户名/邮箱或密码错误");
            }

            // 执行登录
            StpUtil.login(user.getId());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

            // 构建响应
            UserLoginResp loginResp = UserLoginResp.create(user, tokenInfo);

            return Result.success(loginResp);

        } catch (Exception e) {
            return Result.fail("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        try {
            StpUtil.logout();
            return Result.success("登出成功");
        } catch (Exception e) {
            return Result.fail("登出失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<UserResp> getUserInfo() {
        try {
            String currentUserId = StpUtil.getLoginIdAsString();
            SnUser user = userService.getUserById(currentUserId);

            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 使用SnCredentialService获取用户名和邮箱
            String email = snUserCredentialService.getEmailByUserId(currentUserId);

            UserResp userResp = new UserResp();
            userResp.setName(user.getName());
            userResp.setEmail(email);
            userResp.setAvatar(user.getAvatar());

            return Result.success(userResp);

        } catch (Exception e) {
            return Result.fail("获取用户信息失败: " + e.getMessage());
        }
    }


    /**
     * 导出当前用户的所有数据为JSON文件
     *
     * @return JSON文件下载响应
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUserData() {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 获取用户数据
            UserDataExportResp exportData = userDataExportService.exportUserData(currentUserId);

            // 将数据转换为格式化的JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            // 转换为字节数组
            byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            // 生成文件名，包含当前时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String filename = "user_data_export_" + sdf.format(new Date()) + ".json";

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(jsonBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(jsonBytes);

        } catch (Exception e) {
            // 如果出错，返回错误信息文件
            String errorMessage = "导出数据失败: " + e.getMessage();
            byte[] errorBytes = errorMessage.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "export_error.txt");
            headers.setContentLength(errorBytes.length);

            return ResponseEntity.status(500)
                    .headers(headers)
                    .body(errorBytes);
        }
    }

    /**
     * 导入用户数据
     *
     * @param file 上传的JSON文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public Result<UserDataImportResp> importUserData(@RequestParam("file") MultipartFile file) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 验证文件类型
            if (file.isEmpty()) {
                return Result.fail("上传文件不能为空");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".json")) {
                return Result.fail("请上传JSON格式的文件");
            }

            // 读取文件内容
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 解析JSON为导入请求对象
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            UserDataImportReq importData = objectMapper.readValue(jsonContent, UserDataImportReq.class);

            // 执行导入
            UserDataImportResp importResult = userDataExportService.importUserData(importData, currentUserId);

            if (importResult.isSuccess()) {
                return Result.success(importResult);
            } else {
                return Result.fail(importResult.getErrorMessage());
            }

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return Result.fail("JSON文件格式错误: " + e.getMessage());
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 清空当前用户的所有数据
     * 这是一个危险操作，将删除用户的所有标签、空间、书签及其关联关系
     *
     * @return 清空结果
     */
    @DeleteMapping("/clear")
    public Result<UserDataImportResp> clearUserData() {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 执行清空操作
            UserDataImportResp clearResult = userDataExportService.clearUserData(currentUserId);

            if (clearResult.isSuccess()) {
                return Result.success(clearResult);
            } else {
                return Result.fail(clearResult.getErrorMessage());
            }

        } catch (Exception e) {
            return Result.fail("清空数据失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码或创建密码
     *
     * @param req 修改密码请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 验证新密码和确认密码是否一致
            if (!req.getNewPassword().equals(req.getConfirmPassword())) {
                return Result.fail("新密码与确认密码不一致");
            }

            // 检查用户是否已有密码
            String storedPasswordSecret = snUserCredentialService.getPasswordSecretByUserId(currentUserId);
            boolean hasExistingPassword = storedPasswordSecret != null && !storedPasswordSecret.trim().isEmpty();

            // 如果用户已有密码，验证新密码与当前密码不能相同
            if (hasExistingPassword && req.getCurrentPassword() != null &&
                    req.getCurrentPassword().equals(req.getNewPassword())) {
                return Result.fail("新密码不能与当前密码相同");
            }

            // 执行密码修改或创建
            boolean success = userService.changePassword(currentUserId, req.getCurrentPassword(), req.getNewPassword());

            if (success) {
                if (hasExistingPassword) {
                    return Result.success("密码修改成功");
                } else {
                    return Result.success("密码创建成功");
                }
            } else {
                return Result.fail("密码操作失败");
            }

        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("密码操作失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否已设置密码
     *
     * @return 密码状态信息
     */
    @GetMapping("/password/status")
    public Result<Boolean> checkPasswordStatus() {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 获取用户的密码信息
            String storedPasswordSecret = snUserCredentialService.getPasswordSecretByUserId(currentUserId);

            // 判断用户是否已设置密码
            boolean hasPassword = storedPasswordSecret != null && !storedPasswordSecret.trim().isEmpty();

            return Result.success(hasPassword);

        } catch (Exception e) {
            return Result.fail("获取密码状态失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户名
     *
     * @param req 修改用户名请求
     * @return 修改结果
     */
    @PostMapping("/change-username")
    public Result<String> changeUsername(@Valid @RequestBody ChangeUsernameReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 执行用户名修改
            boolean success = userService.changeUsername(currentUserId, req.getNewUsername());

            if (success) {
                return Result.success("用户名修改成功");
            } else {
                return Result.fail("用户名修改失败");
            }

        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("用户名修改失败: " + e.getMessage());
        }
    }

    /**
     * 忘记密码 - 发送重置邮件
     *
     * @param req 忘记密码请求
     * @return 发送结果
     */
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@Valid @RequestBody ForgotPasswordReq req) {
        try {
            boolean success = userService.requestPasswordReset(req.getEmail());

            if (success) {
                return Result.success("重置邮件已发送，请检查您的邮箱");
            } else {
                return Result.fail("发送重置邮件失败");
            }

        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("发送重置邮件失败: " + e.getMessage());
        }
    }

    /**
     * 重置密码
     *
     * @param req 重置密码请求
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        try {
            // 验证新密码和确认密码是否一致
            if (!req.getNewPassword().equals(req.getConfirmPassword())) {
                return Result.fail("新密码与确认密码不一致");
            }

            // 执行密码重置
            boolean success = userService.resetPassword(req.getCode(), req.getNewPassword());

            if (success) {
                return Result.success("密码重置成功");
            } else {
                return Result.fail("密码重置失败");
            }

        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("密码重置失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户Key
     *
     * @param req 创建用户Key请求
     * @return 用户Key信息
     */
    @PostMapping("/key")
    public Result<UserKeyResp> createUserKey(@Valid @RequestBody CreateUserKeyReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 创建用户Key
            SnUserKey userKey = snUserKeyService.createUserKey(currentUserId, req.getKeyName(), req.getDescription());

            // 使用静态方法构建响应
            UserKeyResp userKeyResp = UserKeyResp.fromEntity(userKey);

            return Result.success(userKeyResp);

        } catch (Exception e) {
            return Result.fail("创建用户Key失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有Key
     *
     * @return 用户Key列表
     */
    @GetMapping("/keys")
    public Result<List<UserKeyResp>> getUserKeys() {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 获取用户的所有Key
            List<SnUserKey> userKeys = snUserKeyService.getUserKeys(currentUserId);

            // 转换为脱敏的响应对象
            List<UserKeyResp> userKeyResps = userKeys.stream()
                    .map(UserKeyResp::fromEntityWithMasking)
                    .toList();

            return Result.success(userKeyResps);

        } catch (Exception e) {
            return Result.fail("获取用户Key失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户Key
     *
     * @param keyId 密钥ID
     * @return 删除结果
     */
    @DeleteMapping("/key/{keyId}")
    public Result<String> deleteUserKey(@PathVariable("keyId") String keyId) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            // 删除用户Key
            boolean success = snUserKeyService.deleteUserKey(keyId, currentUserId);

            if (success) {
                return Result.success("用户Key删除成功");
            } else {
                return Result.fail("用户Key删除失败，密钥不存在或权限不足");
            }

        } catch (Exception e) {
            return Result.fail("删除用户Key失败: " + e.getMessage());
        }
    }


    /**
     * 获取Passkey注册选项
     *
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 14:35 2025/8/29
     */
    @GetMapping("/passkey/registration/options")
    public Result<String> getPasskeyRegistrationOptions() {
        try {
            String options = passkeyAuthorizationService.startPasskeyRegistration();
            if (options.isEmpty()) {
                return Result.fail("Failed to generate registration options");
            }
            return Result.ok(options);
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * Passkey注册
     *
     * @param request
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 14:35 2025/8/29
     */
    @PostMapping("/passkey/registration")
    public Result<String> verifyPasskeyRegistration(@RequestBody PasskeyRegistrationRequest request) {
        try {
            passkeyAuthorizationService.finishPasskeyRegistration(request);
            return Result.ok("Passkey registration successful");
        } catch (IOException e) {
            return Result.fail("Invalid credential format");
        } catch (RegistrationFailedException e) {
            return Result.fail("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * 获取Passkey登录选项
     *
     * @param httpServletRequest
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 14:35 2025/8/29
     */
    @GetMapping("/passkey/login/options")
    public Result<String> getPasskeyAssertionOptions(HttpServletRequest httpServletRequest) {
        try {
            String sessionId = httpServletRequest.getSession().getId();
            String options = passkeyAuthorizationService.startPasskeyAssertion(sessionId);
            return Result.ok(options);
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * Passkey登录
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param credential
     * @return pres.peixinyi.sinan.common.Result<java.lang.Object>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 14:35 2025/8/29
     */
    @PostMapping("/passkey/login")
    public Result<UserLoginResp> verifyPasskeyAssertion(HttpServletRequest httpServletRequest,
                                                        @RequestBody String credential) {
        try {
            String sessionId = httpServletRequest.getSession().getId();
            var auth = passkeyAuthorizationService.finishPasskeyAssertion(sessionId, credential);

            // 获取用户信息
            var user = userService.getUserById(auth.getUserId());
            if (user == null) {
                return Result.fail("User not found");
            }

            // 执行登录
            StpUtil.login(auth.getUserId());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

            // 构建响应
            UserLoginResp loginResp = UserLoginResp.create(user, tokenInfo);

            return Result.success(loginResp);
        } catch (IOException e) {
            return Result.fail("Invalid credential format");
        } catch (AssertionFailedException e) {
            return Result.fail("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * 获取Passkey列表
     *
     * @return pres.peixinyi.sinan.common.Result<java.util.List < pres.peixinyi.sinan.dto.response.PasskeyResp>>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 16:45 2025/8/29
     */
    @GetMapping("/passkeys")
    public Result<List<PasskeyResp>> getPasskeyList() {
        try {
            return Result.ok(passkeyAuthorizationService.getPasskeyList());
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * 更新Passkey描述
     *
     * @param req
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 16:50 2025/8/29
     */
    @PatchMapping("/passkey/describe")
    public Result<String> updatePasskeyDescribe(@RequestBody EditPasskeyDescribeReq req) {
        try {
            passkeyAuthorizationService.updatePasskeyDescribe(req);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * 删除Passkey
     *
     * @param passkeyId
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author wangbinzhe
     * @version 1.0.0.0
     * @since 16:52 2025/8/29
     */
    @DeleteMapping("/passkey/{passkeyId}")
    public Result<String> deletePasskey(@PathVariable("passkeyId") Long passkeyId) {
        try {
            passkeyAuthorizationService.deletePasskey(passkeyId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("Error: " + e.getMessage());
        }
    }

    /**
     * 上传用户头像
     *
     * @param file 上传的头像文件
     * @return 上传结果，包含头像的访问路径
     */
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的头像");
        }

        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名不能为空");
        }

        String lowerCaseFilename = originalFilename.toLowerCase();
        if (!lowerCaseFilename.endsWith(".jpg") && !lowerCaseFilename.endsWith(".jpeg")
                && !lowerCaseFilename.endsWith(".png") && !lowerCaseFilename.endsWith(".gif")
                && !lowerCaseFilename.endsWith(".bmp") && !lowerCaseFilename.endsWith(".webp")) {
            return Result.fail("只支持 JPG、PNG、GIF、BMP、WEBP 格式的图片");
        }

        // 检查文件大小（限制为5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail("头像大小不能超过5MB");
        }

        try {
            // 读取图片
            BufferedImage srcImg = ImageIO.read(file.getInputStream());
            if (srcImg == null) {
                return Result.fail("图片格式不正确或已损坏");
            }

            // 获取图片的宽高
            int width = srcImg.getWidth();
            int height = srcImg.getHeight();

            // 计算裁剪区域（取中心正方形区域）
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;

            // 裁剪为正方形
            BufferedImage squareImg = srcImg.getSubimage(x, y, size, size);

            // 压缩为256x256并转换为PNG格式
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(squareImg)
                    .size(256, 256)
                    .outputFormat("png")
                    .outputQuality(0.8)
                    .toOutputStream(out);

            // 生成文件名（使用用户ID前缀避免冲突）
            String fileName = "avatar_" + currentUserId + "_" + System.currentTimeMillis() + ".png";

            // 使用配置的上传路径
            Path uploadDir = Paths.get(uploadProperties.getAvatarUploadPath());
            Files.createDirectories(uploadDir);

            Path savePath = uploadDir.resolve(fileName);
            Files.write(savePath, out.toByteArray());

            // 更新用户头像URL
            String avatarUrl = uploadProperties.getAvatarFullUrl(baseUrl, fileName);
            boolean updated = userService.updateUserAvatar(currentUserId, avatarUrl);

            if (!updated) {
                return Result.fail("头像上传成功但更新用户信息失败");
            }

            return Result.success(avatarUrl);

        } catch (Exception e) {
            return Result.fail("头像处理失败: " + e.getMessage());
        }
    }

    /**
     * 读取用户头像
     *
     * @param fileName 头像文件名
     * @return 头像文件的字节流
     */
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable("fileName") String fileName) {
        try {
            // 参数校验
            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 安全检查：防止路径遍历攻击
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            // 检查文件扩展名
            if (!fileName.toLowerCase().endsWith(".png") && !fileName.toLowerCase().endsWith(".jpg")
                && !fileName.toLowerCase().endsWith(".jpeg") && !fileName.toLowerCase().endsWith(".gif")
                && !fileName.toLowerCase().endsWith(".bmp") && !fileName.toLowerCase().endsWith(".webp")) {
                return ResponseEntity.badRequest().build();
            }

            // 构建文件路径
            Path filePath = Paths.get(uploadProperties.getAvatarUploadPath()).resolve(fileName);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(filePath);

            // 根据文件扩展名设置正确的Content-Type
            String contentType = "image/png";
            if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (fileName.toLowerCase().endsWith(".bmp")) {
                contentType = "image/bmp";
            } else if (fileName.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(contentType));
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("public, max-age=31536000"); // 缓存一年

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}

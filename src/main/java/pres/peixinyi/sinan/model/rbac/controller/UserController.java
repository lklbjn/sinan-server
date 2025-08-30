package pres.peixinyi.sinan.model.rbac.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.ChangePasswordReq;
import pres.peixinyi.sinan.dto.request.CreateUserKeyReq;
import pres.peixinyi.sinan.dto.request.UserDataImportReq;
import pres.peixinyi.sinan.dto.request.UserLoginReq;
import pres.peixinyi.sinan.dto.request.UserRegisterReq;
import pres.peixinyi.sinan.dto.response.UserResp;
import pres.peixinyi.sinan.dto.response.UserKeyResp;
import pres.peixinyi.sinan.dto.response.UserDataExportResp;
import pres.peixinyi.sinan.dto.response.UserDataImportResp;
import pres.peixinyi.sinan.dto.response.UserLoginResp;
import pres.peixinyi.sinan.model.rbac.entity.SnUser;
import pres.peixinyi.sinan.model.rbac.service.UserDataExportService;
import pres.peixinyi.sinan.model.rbac.service.SnUserService;
import pres.peixinyi.sinan.model.rbac.service.SnUserCredentialService;
import pres.peixinyi.sinan.model.rbac.service.SnUserKeyService;
import pres.peixinyi.sinan.model.rbac.entity.SnUserKey;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * 旧的登录接口（保持兼容）
     *
     * @return void
     * @author peixinyi
     * @since 21:38 2025/8/12
     */
    @PostMapping("/doLogin")
    public Result<SaTokenInfo> doLogin() {
        StpUtil.login("admin");
        return Result.ok(StpUtil.getTokenInfo());
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
}

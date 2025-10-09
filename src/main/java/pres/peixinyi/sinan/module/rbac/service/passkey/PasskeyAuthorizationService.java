package pres.peixinyi.sinan.module.rbac.service.passkey;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.dto.request.EditPasskeyDescribeReq;
import pres.peixinyi.sinan.dto.request.PasskeyRegistrationRequest;
import pres.peixinyi.sinan.dto.response.PasskeyResp;
import pres.peixinyi.sinan.module.rbac.domain.CredentialType;
import pres.peixinyi.sinan.module.rbac.entity.SnPassKey;
import pres.peixinyi.sinan.module.rbac.service.SnPassKeyService;
import pres.peixinyi.sinan.module.rbac.service.SnUserCredentialService;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lklbjn
 */
@Service
public class PasskeyAuthorizationService {

    @Resource
    private RelyingParty relyingParty;
    @Resource
    private SnPassKeyService passKeyService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SnUserCredentialService userCredentialService;
    private static final String REDIS_PASSKEY_ASSERTION_KEY = "passkey:assertion";
    private static final String REDIS_PASSKEY_REGISTRATION_KEY = "passkey:registration";

    /**
     * 开始 Passkey 注册流程
     *
     * @param userId
     * @return java.lang.String
     * @author lklbjn
     * @version 1.0.0.0
     * @since 10:36 2025/6/6
     */
    public String startPasskeyRegistration() {
        // 获取用户信息，创建注册选项，将选项存储在 Redis 中，返回给客户端用于创建凭证的 JSON
        var userId = StpUtil.getLoginId().toString();
        var userCredential = userCredentialService.getCredentialByUserId(userId);


        var options = relyingParty.startRegistration(StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                        .name(userCredential.get(CredentialType.EMAIL).getCredential())
                        .displayName(userCredential.get(CredentialType.USERNAME).getCredential())
                        .id(generateUserHandle())
                        .build())
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .build())
                .build());
        try {
            stringRedisTemplate.opsForHash().put(REDIS_PASSKEY_REGISTRATION_KEY, userId, options.toJson());
            // 设置5分钟的过期时间
            stringRedisTemplate.expire(REDIS_PASSKEY_REGISTRATION_KEY, Duration.ofMinutes(5));
            return options.toCredentialsCreateJson();
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /**
     * 完成 Passkey 注册流程
     *
     * @param passkey
     * @return void
     * @author lklbjn
     * @version 1.0.0.0
     * @since 10:36 2025/6/6
     */
    public void finishPasskeyRegistration(PasskeyRegistrationRequest passkey) throws IOException, RegistrationFailedException {
        // 解析客户端返回的凭证，从 Redis 获取之前的请求，验证并完成注册，最后存储凭证
        var userId = StpUtil.getLoginId().toString();
        var pkc = PublicKeyCredential.parseRegistrationResponseJson(passkey.getCredential());

        var request = PublicKeyCredentialCreationOptions.fromJson((String)
                stringRedisTemplate.opsForHash().get(REDIS_PASSKEY_REGISTRATION_KEY, userId));

        var result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                .request(request)
                .response(pkc)
                .build());
        stringRedisTemplate.opsForHash().delete(REDIS_PASSKEY_REGISTRATION_KEY, userId);

        storeCredential(userId, passkey.getDescribe(), request, result);
    }

    /**
     * 开始 Passkey 断言（登录）流程
     *
     * @param identifier
     * @return java.lang.String
     * @author lklbjn
     * @version 1.0.0.0
     * @since 10:37 2025/6/6
     */
    public String startPasskeyAssertion(String identifier) throws JsonProcessingException {
        // 创建断言选项，将选项存储在 Redis 中，返回给客户端用于获取凭证的 JSON
        var options = relyingParty.startAssertion(StartAssertionOptions.builder().build());

        stringRedisTemplate.opsForHash().put(REDIS_PASSKEY_ASSERTION_KEY, identifier, options.toJson());
        // 设置5分钟的过期时间
        stringRedisTemplate.expire(REDIS_PASSKEY_ASSERTION_KEY, Duration.ofMinutes(5));

        return options.toCredentialsGetJson();
    }

    /**
     * 完成 Passkey 断言（登录）流程
     *
     * @param identifier
     * @param credential
     * @return io.github.lklbjn.passkey.passkeydemo.entity.User
     * @author lklbjn
     * @version 1.0.0.0
     * @since 10:37 2025/6/6
     */
    public SnPassKey finishPasskeyAssertion(String identifier, String credential) throws IOException, AssertionFailedException {
        // 解析客户端返回的凭证，从 Redis 获取之前的请求，验证断言，更新凭证，返回用户 ID
        var request = AssertionRequest.fromJson((String)
                stringRedisTemplate.opsForHash().get(REDIS_PASSKEY_ASSERTION_KEY, identifier));
        var pkc = PublicKeyCredential.parseAssertionResponseJson(credential);

        var result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                .request(request)
                .response(pkc)
                .build());

        stringRedisTemplate.opsForHash().delete(REDIS_PASSKEY_ASSERTION_KEY, identifier);

        if (!result.isSuccess()) {
            throw new AssertionFailedException("Verify failed");
        }

        // 从结果中获取用户ID
        return updateCredential(result.getCredential().getCredentialId(), result);
    }

    /**
     * 存储用户的凭证信息到数据库
     *
     * @param id
     * @param describe
     * @param request
     * @param result
     * @return void
     * @author lklbjn
     * @version 1.0.0.0
     * @since 15:35 2025/6/11
     */
    private void storeCredential(String userId,
                                 @NotNull String describe,
                                 @NotNull PublicKeyCredentialCreationOptions request,
                                 @NotNull RegistrationResult result) {
        passKeyService.save(fromFinishPasskeyRegistration(userId, describe, request, result));
    }

    /**
     * 更新用户的凭证信息（主要是签名计数）
     *
     * @param credentialId
     * @param result
     */
    private SnPassKey updateCredential(@NotNull ByteArray credentialId,
                                       @NotNull AssertionResult result) {
        var entity = passKeyService.getByCredentialId(credentialId);
        entity.setSignCount(result.getSignatureCount());
        entity.setLastUsed(new Date());
        passKeyService.saveOrUpdate(entity);
        return entity;
    }

    /**
     * 从注册结果创建 WebauthnCredential 对象
     *
     * @param id
     * @param describe
     * @param request
     * @param result
     * @return
     */
    @NotNull
    private static SnPassKey fromFinishPasskeyRegistration(String userId,
                                                           String describe,
                                                           PublicKeyCredentialCreationOptions request,
                                                           RegistrationResult result) {
        UserIdentity user = request.getUser();
        return SnPassKey.builder().userId(userId)
                .displayName(user.getDisplayName()).credentialId(result.getKeyId().getId())
                .publicKey(result.getPublicKeyCose())
                .signCount(result.getSignatureCount())
                .userHandle(request.getUser().getId())
                .describe(describe)
                .createTime(new Date())
                .build();
    }


    public ByteArray generateUserHandle() {
        return new ByteArray(randomBytes(32));
    }

    /**
     * 随机bytes
     *
     * @param length 长度
     * @return bytes
     */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public List<PasskeyResp> getPasskeyList() {
        return passKeyService.lambdaQuery().eq(SnPassKey::getUserId, StpUtil.getLoginId())
                .list().stream().map(PasskeyResp::from).toList();
    }

    public void updatePasskeyDescribe(EditPasskeyDescribeReq req) {
        passKeyService.lambdaUpdate().eq(SnPassKey::getUserId, StpUtil.getLoginId())
                .eq(SnPassKey::getId, req.getId())
                .set(SnPassKey::getDescribe, req.getDescribe())
                .update();
    }

    public void deletePasskey(Long passkeyId) {
        passKeyService.lambdaUpdate().eq(SnPassKey::getUserId, StpUtil.getLoginId())
                .eq(SnPassKey::getId, passkeyId)
                .set(SnPassKey::getDeleted, 1)
                .update();
    }
}
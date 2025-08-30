package pres.peixinyi.sinan.model.common;

import com.alibaba.fastjson2.JSON;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pres.peixinyi.sinan.model.rbac.domain.CredentialType;
import pres.peixinyi.sinan.model.rbac.entity.SnUser;
import pres.peixinyi.sinan.model.rbac.entity.SnUserCredential;
import pres.peixinyi.sinan.model.rbac.exception.UserRuntionException;
import pres.peixinyi.sinan.model.rbac.service.SnUserCredentialService;
import pres.peixinyi.sinan.model.rbac.service.SnUserKeyService;
import pres.peixinyi.sinan.model.rbac.service.SnUserService;
import pres.peixinyi.sinan.utils.NameUtils;

import java.util.List;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/28 16:41
 * @Version : 0.0.0
 */
@Service
public class IUserService {

    @Resource
    SnUserService userService;

    @Resource
    SnUserCredentialService credentialService;

    @Resource
    SnUserKeyService keyService;

    public Boolean checkEmailExist(String email) {
        return credentialService.checkEmailExist(email);
    }

    public SnUser getUserByCredential(CredentialType credentialType, @Valid @NotNull String email) {
        SnUserCredential credential = credentialService.getCredential(credentialType, email);
        if (credential == null) {
            throw new UserRuntionException("USER NOT FOUND : " + email);
        }
        return userService.getById(credential.getUserId());
    }

    public SnUser getUserByName(String name) {
        return userService.getUserByName(name);
    }

    public SnUserCredential getCredential(CredentialType credentialType, String identifier) {
        return credentialService.getCredential(credentialType, identifier);
    }

    public SnUserCredential getCredentialByUserId(CredentialType credentialType, String userId) {
        return credentialService.getCredentialByUserId(credentialType, userId);
    }

    public SnUser getUserById(String userId) {
        return userService.getById(userId);
    }
}

package pres.peixinyi.sinan.module.rbac.service.passkey;


import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.module.rbac.entity.SnPassKey;
import pres.peixinyi.sinan.module.rbac.service.SnPassKeyService;
import pres.peixinyi.sinan.module.rbac.service.SnUserCredentialService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lklbjn
 */
@Service
public class WebAuthnCredentialRepository implements CredentialRepository {

    @Resource
    private SnPassKeyService passKeyService;
    @Resource
    private SnUserCredentialService userCredentialService;

    private static final Set<AuthenticatorTransport> TRANSPORTS = Collections.unmodifiableSet(new TreeSet<>(
            Arrays.asList(AuthenticatorTransport.HYBRID, AuthenticatorTransport.INTERNAL)
    ));

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return passKeyService.findAllByUserId(userCredentialService.getUserIdByEmail(username)).stream()
                .map(it -> PublicKeyCredentialDescriptor.builder()
                        .id(it.getCredentialId())
                        .transports(TRANSPORTS)
                        .build())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        var userId = userCredentialService.getUserIdByEmail(username);
        return Optional.ofNullable(passKeyService.getUserHandleByUserId(userId));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        var userId = passKeyService.getUserIdByUserHandle(userHandle);
        return Optional.ofNullable(userCredentialService.getEmailByUserId(userId));
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Optional<SnPassKey> registrationMaybe = passKeyService.lambdaQuery()
                .eq(SnPassKey::getCredentialId, credentialId)
                .eq(SnPassKey::getUserHandle, userHandle).list().stream().findAny();
        return registrationMaybe.map(it ->
                RegisteredCredential.builder()
                        .credentialId(it.getCredentialId())
                        .userHandle(userHandle)
                        .publicKeyCose(it.getPublicKey())
                        .signatureCount(it.getSignCount())
                        .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        List<SnPassKey> credentials = passKeyService.lambdaQuery()
                .eq(SnPassKey::getCredentialId, credentialId).list();
        if (credentials.isEmpty()) {
            return Set.of();
        }
        return credentials.stream().map(it ->
                        RegisteredCredential.builder()
                                .credentialId(it.getCredentialId())
                                .userHandle(it.getUserHandle())
                                .publicKeyCose(it.getPublicKey())
                                .signatureCount(it.getSignCount())
                                .build())
                .collect(Collectors.toUnmodifiableSet());
    }

}
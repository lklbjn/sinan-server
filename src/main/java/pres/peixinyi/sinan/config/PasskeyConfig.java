package pres.peixinyi.sinan.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pres.peixinyi.sinan.module.rbac.service.passkey.WebAuthnCredentialRepository;

/**
 * @author lklbjn
 * @DATE 2025/6/5 13:42
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class PasskeyConfig {

    private PasskeyProperties properties;

    // 核心服务类
    @Bean
    public RelyingParty relyingParty(WebAuthnCredentialRepository credentialRepo) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(properties.getId())
                        .name(properties.getName())
                        .build())
                .credentialRepository(credentialRepo)
                .origins(properties.getOrigins()) // 允许的前端源
                .build();
    }

}

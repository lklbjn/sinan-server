package pres.peixinyi.sinan.module.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:43
 * @Version : 0.0.0
 */
@Data
@Configuration
@ConfigurationProperties("github.oauth2")
public class GithubOAuth2Property {

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端密钥
     */
    private String clientSecret;

    /**
     * 重定向地址
     */
    private String redirectUri;

    /**
     * 授权范围
     */
    private String scope = "user:email";


}

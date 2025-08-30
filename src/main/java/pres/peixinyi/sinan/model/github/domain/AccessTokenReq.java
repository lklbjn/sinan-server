package pres.peixinyi.sinan.model.github.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:56
 * @Version : 0.0.0
 */
@NoArgsConstructor
@Data
public class AccessTokenReq {
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("client_secret")
    private String clientSecret;
    @JsonProperty("code")
    private String code;
    @JsonProperty("redirect_uri")
    private String redirectUri;
}

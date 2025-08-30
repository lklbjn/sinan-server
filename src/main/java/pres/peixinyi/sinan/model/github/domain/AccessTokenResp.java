package pres.peixinyi.sinan.model.github.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:55
 * @Version : 0.0.0
 */
@NoArgsConstructor
@Data
public class AccessTokenResp {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("scope")
    private String scope;
}

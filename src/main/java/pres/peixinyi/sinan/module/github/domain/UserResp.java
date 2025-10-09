package pres.peixinyi.sinan.module.github.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:59
 * @Version : 0.0.0
 */
@NoArgsConstructor
@Data
public class UserResp {


    @JsonProperty("id")
    private Integer id;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("name")
    private String name;
    @JsonProperty("email")
    private String email;

}

package pres.peixinyi.sinan.dto.request;

import lombok.Data;

/**
 * 创建用户密钥请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/22
 * @Version : 1.0.0
 */
@Data
public class CreateUserKeyReq {

    /**
     * 密钥名称/描述（可选）
     */
    private String keyName;

    /**
     * 密钥用途描述（可选）
     */
    private String description;
}

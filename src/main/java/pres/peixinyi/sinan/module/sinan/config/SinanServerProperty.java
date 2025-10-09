package pres.peixinyi.sinan.module.sinan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 司南服务配置
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/27 14:37
 * @Version : 0.0.0
 */
@Data
@Configuration
@ConfigurationProperties("sinan.server")
public class SinanServerProperty {

    /**
     * 司南服务基础URL
     */
    private String baseUrl;

}

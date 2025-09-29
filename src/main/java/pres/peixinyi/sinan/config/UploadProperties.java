package pres.peixinyi.sinan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上传文件配置属性
 *
 * @author peixinyi
 * @since 2025/9/4
 */
@Data
@Component
@ConfigurationProperties(prefix = "sinan.upload")
public class UploadProperties {

    /**
     * 上传文件的基础路径
     */
    private String basePath = "./upload";

    /**
     * 图标上传路径
     */
    private String iconPath = "icons";

    /**
     * 头像上传路径
     */
    private String avatarPath = "avatars";

    /**
     * 访问URL前缀
     */
    private String urlPrefix = "/upload";

    /**
     * 获取图标的完整上传路径
     *
     * @return 图标上传的完整路径
     */
    public String getIconUploadPath() {
        return basePath + "/" + iconPath;
    }

    /**
     * 获取图标的访问URL前缀
     *
     * @return 图标访问的URL前缀
     */
    public String getIconUrlPrefix() {
        return urlPrefix + "/" + iconPath;
    }

    /**
     * 获取图标的完整访问URL
     *
     * @param baseUrl 服务器基础URL
     * @param fileName 文件名
     * @return 图标的完整访问URL
     */
    public String getIconFullUrl(String baseUrl, String fileName) {
        // 确保baseUrl不以/结尾，urlPrefix以/开头
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBaseUrl + getIconUrlPrefix() + "/" + fileName;
    }

    /**
     * 获取头像的完整上传路径
     *
     * @return 头像上传的完整路径
     */
    public String getAvatarUploadPath() {
        return basePath + "/" + avatarPath;
    }

    /**
     * 获取头像的访问URL前缀
     *
     * @return 头像访问的URL前缀
     */
    public String getAvatarUrlPrefix() {
        return urlPrefix + "/" + avatarPath;
    }

    /**
     * 获取头像的完整访问URL
     *
     * @param baseUrl 服务器基础URL
     * @param fileName 文件名
     * @return 头像的完整访问URL
     */
    public String getAvatarFullUrl(String baseUrl, String fileName) {
        // 确保baseUrl不以/结尾，urlPrefix以/开头
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBaseUrl + getAvatarUrlPrefix() + "/" + fileName;
    }
}

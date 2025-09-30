package pres.peixinyi.sinan.model.rbac.domain;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/28 17:01
 * @Version : 0.0.0
 */
public enum CredentialType {

    EMAIL,
    USERNAME,
    GITHUB,
    PASSKEY,
    PASSWORD;

    public static CredentialType from(String type) {
        for (CredentialType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}

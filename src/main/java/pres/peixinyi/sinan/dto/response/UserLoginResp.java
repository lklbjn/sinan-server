package pres.peixinyi.sinan.dto.response;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;
import pres.peixinyi.sinan.module.rbac.entity.SnUser;

/**
 * 用户登录响应
 */
@Data
public class UserLoginResp {

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * Token信息
     */
    private SaTokenInfo tokenInfo;

    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        private String id;
        private String name;
        private String avatar;

        public static UserInfo from(SnUser user) {
            UserInfo userInfo = new UserInfo();
            userInfo.setName(user.getName());
            userInfo.setAvatar(user.getAvatar());
            return userInfo;
        }
    }

    public static UserLoginResp create(SnUser user, SaTokenInfo tokenInfo) {
        UserLoginResp resp = new UserLoginResp();
        resp.setUserInfo(UserInfo.from(user));
        resp.setTokenInfo(tokenInfo);
        return resp;
    }
}

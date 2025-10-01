package pres.peixinyi.sinan.module.github.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import pres.peixinyi.sinan.dto.response.UserLoginResp;
import pres.peixinyi.sinan.module.rbac.entity.SnUser;
import pres.peixinyi.sinan.module.github.GithubOAuth2Url;
import pres.peixinyi.sinan.module.github.config.GithubOAuth2Property;
import pres.peixinyi.sinan.module.github.domain.AccessTokenReq;
import pres.peixinyi.sinan.module.github.domain.AccessTokenResp;
import pres.peixinyi.sinan.module.github.domain.UserResp;
import pres.peixinyi.sinan.module.rbac.service.SnUserCredentialService;
import pres.peixinyi.sinan.module.rbac.service.SnUserService;
import pres.peixinyi.sinan.utils.HttpUtil;

import java.util.Date;
import java.util.UUID;

import static pres.peixinyi.sinan.module.rbac.domain.CredentialType.EMAIL;
import static pres.peixinyi.sinan.module.rbac.domain.CredentialType.GITHUB;

/**
 * Github  OAuth2 认证服务
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:26
 * @Version : 0.0.0
 */
@Service
public class GithubOAuth2Service {

    @Resource
    GithubOAuth2Property property;
    @Autowired
    private SnUserCredentialService snUserCredentialService;
    @Autowired
    private SnUserService snUserService;

    /**
     * 获取重定向地址
     *
     * @return java.lang.String
     * @author peixinyi
     * @since 20:54 2025/8/19
     */
    public String getRedirectUrl() {
        return GithubOAuth2Url.AUTHORIZE_URL +
                "?client_id=" + property.getClientId() +
                "&redirect_uri=" + property.getRedirectUri() +
                "&scope=" + property.getScope() +
                "&state=" + UUID.randomUUID().toString();
    }

    /**
     * 获取AccessToken
     *
     * @param code
     * @return pres.peixinyi.sinan.model.github.domain.AccessTokenResp
     * @author peixinyi
     * @since 21:00 2025/8/19
     */
    public AccessTokenResp getAccessToken(String code) {
        AccessTokenReq accessTokenReq = new AccessTokenReq();

        accessTokenReq.setClientId(property.getClientId());
        accessTokenReq.setClientSecret(property.getClientSecret());
        accessTokenReq.setCode(code);
        accessTokenReq.setRedirectUri(property.getRedirectUri());

        return HttpUtil.builder()
                .url(GithubOAuth2Url.TOKEN_URL)
                .jsonBody(accessTokenReq)
                .header("Accept", "application/json")
                .postWithType(new TypeReference<AccessTokenResp>() {
                });
    }

    /**
     * 获取用户信息
     *
     * @param accessToken
     * @return pres.peixinyi.sinan.model.github.domain.UserResp
     * @author peixinyi
     * @since 21:00 2025/8/19
     */
    public UserResp getUser(String accessToken) {
        UserResp userResp = HttpUtil.builder()
                .url(GithubOAuth2Url.USER_INFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .getWithType(new TypeReference<UserResp>() {
                });
        return userResp;
    }

    public UserLoginResp login(String code) {
        AccessTokenResp accessToken = getAccessToken(code);
        UserResp userResp = getUser(accessToken.getAccessToken());
        if (ObjectUtils.isEmpty(userResp)) {
            throw new RuntimeException("获取用户信息失败，可能是AccessToken无效或已过期");
        }
        String userId;
        //检查SnUserCredential是否存在
        userId = snUserCredentialService
                .getUserIdByCredential(GITHUB, userResp.getId().toString());
        //如果Github凭证不存在，且没有关联的邮箱则创建用户
        if (ObjectUtils.isEmpty(userId)) {
            //检查用户是否关联了邮箱
            String userIdByEmail = snUserCredentialService.getUserIdByCredential(EMAIL, userResp.getEmail());
            if (ObjectUtils.isEmpty(userIdByEmail)) {
                //创建账户
                SnUser snUser = new SnUser();
                snUser.setName(userResp.getName());
                snUser.setAvatar(userResp.getAvatarUrl());
                snUser.setCreateTime(new Date());
                snUser.setUpdateTime(new Date());
                snUser = snUserService.createUser(snUser);
                userId = snUser.getId();
                //创建凭证
                snUserCredentialService.createCredential(snUser.getId(), GITHUB, userResp.getId().toString());
            } else {
                userId = userIdByEmail;
                snUserCredentialService.createCredential(userIdByEmail, GITHUB, userResp.getId().toString());
            }
        }
        //构造返回信息并登录
        StpUtil.login(userId);
        SnUser snUser = snUserService.getUserById(userId);
        UserLoginResp userLoginResp = new UserLoginResp();
        userLoginResp.setUserInfo(UserLoginResp.UserInfo.from(snUser));
        userLoginResp.setTokenInfo(StpUtil.getTokenInfo());
        return userLoginResp;
    }
}

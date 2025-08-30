package pres.peixinyi.sinan.model.github.controller;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.response.UserLoginResp;
import pres.peixinyi.sinan.model.github.service.GithubOAuth2Service;

/**
 * GitHub OAuth2 认证控制器
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 21:00
 * @Version : 0.0.0
 */
@RestController
@RequestMapping("/user/github/oauth2")
public class GitHubOAuth2Controller {

    @Resource
    GithubOAuth2Service githubOAuth2Service;


    /**
     * 获取重定向地址
     *
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 21:02 2025/8/19
     */
    @RequestMapping("/redirect")
    public Result<String> getRedirectUrl() {
        String redirectUrl = githubOAuth2Service.getRedirectUrl();
        return Result.ok(redirectUrl);
    }

    /**
     * 完成登录操作
     *
     * @param code
     * @return pres.peixinyi.sinan.common.Result<pres.peixinyi.sinan.dto.response.UserLoginResp>
     * @author peixinyi
     * @since 21:03 2025/8/19
     */
    @GetMapping("/login")
    @Transactional(rollbackFor = Exception.class)
    public Result<UserLoginResp> login(@RequestParam("code") String code) {
        UserLoginResp userLoginResp = githubOAuth2Service.login(code);
        return Result.ok(userLoginResp);
    }

}

package pres.peixinyi.sinan.dto.response;

import lombok.Data;

/**
 * GitHub OAuth2 用户信息响应
 */
@Data
public class GitHubUserInfo {

    /**
     * GitHub用户ID
     */
    private Long id;

    /**
     * GitHub用户名
     */
    private String login;

    /**
     * 用户昵称
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar_url;

    /**
     * 个人主页
     */
    private String html_url;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 公司
     */
    private String company;

    /**
     * 位置
     */
    private String location;
}

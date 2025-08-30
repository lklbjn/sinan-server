package pres.peixinyi.sinan.model.github;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19 20:44
 * @Version : 0.0.0
 */
public class GithubOAuth2Url {

    private static final String BASE_URL = "https://github.com";
    private static final String API_BASE_URL = "https://api.github.com";

    public static final String AUTHORIZE_URL = BASE_URL + "/login/oauth/authorize";
    public static final String TOKEN_URL = BASE_URL + "/login/oauth/access_token";
    public static final String USER_INFO_URL = API_BASE_URL + "/user";

}

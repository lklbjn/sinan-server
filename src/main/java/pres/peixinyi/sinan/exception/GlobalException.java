package pres.peixinyi.sinan.exception;

import cn.dev33.satoken.exception.NotLoginException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pres.peixinyi.sinan.common.Result;

/**
 * 全局异常处理器
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/17 21:04
 * @Version : 0.0.0
 */
@RestControllerAdvice
public class GlobalException {

    /**
     * 处理未登录异常，返回401错误
     *
     * @param e 未登录异常
     * @return 401错误响应
     * @author peixinyi
     * @since 21:05 2025/8/17
     */
    @ExceptionHandler(value = NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<String> NoLoginHandler(NotLoginException e) {
        String message = "用户未登录或登录已过期";
        String description = "请先登录后再进行操作";

        // 根据具体的未登录类型返回更详细的错误信息
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                description = "请求中缺少token";
                break;
            case NotLoginException.INVALID_TOKEN:
                description = "token格式不正确";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                description = "token已过期，请重新登录";
                break;
            case NotLoginException.BE_REPLACED:
                description = "账号在其他设备登录，当前登录已失效";
                break;
            case NotLoginException.KICK_OUT:
                description = "账号已被强制下线";
                break;
            default:
                description = "登录状态异常，请重新登录";
                break;
        }

        return Result.error(401, message, description);
    }

}

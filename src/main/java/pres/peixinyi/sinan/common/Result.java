package pres.peixinyi.sinan.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @Author : PeiXinyi
 * @Date : 2025/2/17 09:22
 * @Version : V1.0
 */
@Data
public class Result<T> implements Serializable {


    public static final long serialVersionUID = 4328741;

    /**
     * 状态码
     */
    public static final Integer SUCCESS_CODE = 0;

    /**
     * 消息
     */
    public static final String SUCCESS_MESSAGE = "success";

    /**
     * 状态码
     */
    public static final Integer FAIL_CODE = -1;


    public Result(Integer code, String message, String description, T data) {
        this.code = code;
        this.message = message;
        this.description = StringUtils.isEmpty(description) ? SUCCESS_MESSAGE : description;
        this.data = data;
        this.date = System.currentTimeMillis();
        this.flag = code >= 0;
        String messageId = null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
            messageId = request.getParameter("messageId");
        } catch (Exception ignored) {
        }
        this.messageId = messageId;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.description = "";
        this.data = data;
        this.date = System.currentTimeMillis();
        this.flag = code >= 0;
        String messageId = null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
            messageId = request.getParameter("messageId");
        } catch (Exception ignored) {
        }
        this.messageId = messageId;
    }


    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 异常描述
     */
    private String description;

    /**
     * 数据
     */
    private T data;

    /**
     * 时间
     */
    private Long date;

    /**
     * 状态
     */
    private boolean flag;

    /**
     * 消息ID
     */
    private String messageId;


    /**
     * 返回成功结果
     *
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:25 2022-4-29
     */
    public static Result<String> ok() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, "", "");
    }

    /**
     * 返回成功结果
     *
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:25 2022-4-29
     */
    public static Result<String> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, "");
    }

    /**
     * 返回成功结果并包含参数
     *
     * @param t 返回内容
     * @return com.hengyun.ms.common.vo.Result<T>
     * @author PeiXy_J
     * @since 09:25 2022-4-29
     */
    public static <T> Result<T> ok(T t) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, t);
    }

    public Result<T> set(String key, String value) {
        if (this.data == null) {
            this.data = (T) new HashMap<String, String>();
        }
        if (this.data instanceof HashMap) {
            ((HashMap<String, String>) this.data).put(key, value);
        } else {
            throw new IllegalArgumentException("Data is not a HashMap, cannot set key-value pair.");
        }
        return this;
    }

    /**
     * 返回成功结果并包含参数
     *
     * @param t 返回内容
     * @return com.hengyun.ms.common.vo.Result<T>
     * @author PeiXy_J
     * @since 09:25 2022-4-29
     */
    public static <T> Result<T> success(T t) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, t);
    }


    /**
     * 返回失败结果并包含描述
     *
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(FAIL_CODE, message, null);
    }


    /**
     * 返回失败结果并包含描述
     *
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> error(String message, String description) {
        return new Result<>(FAIL_CODE, message, description, null);
    }

    /**
     * 返回失败结果并包含描述
     *
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(FAIL_CODE, message, null);
    }

    /**
     * 返回失败结果并包含描述
     *
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> fail(String message, String description) {
        return new Result<>(FAIL_CODE, message, description, null);
    }

    /**
     * 返回失败结果并包含错误码和错误信息
     *
     * @param code    错误码
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 返回失败结果并包含错误码和错误信息
     *
     * @param code    错误码
     * @param message 描述
     * @return com.hengyun.ms.common.vo.Result<java.lang.String>
     * @author PeiXy_J
     * @since 09:26 2022-4-29
     */
    public static <T> Result<T> error(Integer code, String message, String description) {
        return new Result<>(code, message, description, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message, String description) {
        return new Result<>(code, message, description, null);
    }

    /**
     * 根据条件返回对应状态
     *
     * @param condition 条件
     * @param message   描述
     * @return com.hengyun.ms.common.vo.Result<?>
     * @author PeiXy_J
     * @since 09:27 2022-4-29
     */
    public static Result<String> condition(Boolean condition, String message) {
        if (Boolean.TRUE.equals(condition)) {
            return Result.ok();
        } else {
            return Result.error(message);
        }
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code >= SUCCESS_CODE;
    }

    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }

    /**
     * 根据Boolean返回结果
     *
     * @param condition 条件
     * @return com.hengyun.ms.common.vo.Result<?>
     * @author PeiXy_J
     * @since 13:47 2022-6-15
     */
    public static Result<String> condition(Boolean condition) {
        if (Boolean.TRUE.equals(condition)) {
            return Result.ok();
        } else {
            return Result.error("fault");
        }
    }

    public Result<String> convert() {
        return Result.error(this.code, this.message, this.description);
    }

}

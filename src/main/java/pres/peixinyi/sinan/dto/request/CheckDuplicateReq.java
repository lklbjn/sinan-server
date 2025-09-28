package pres.peixinyi.sinan.dto.request;

import lombok.Data;

/**
 * 重复书签检查请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class CheckDuplicateReq {

    /**
     * 要检查的URL
     */
    private String url;

    /**
     * 匹配级别 (1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配)
     */
    private int level = 1;

}
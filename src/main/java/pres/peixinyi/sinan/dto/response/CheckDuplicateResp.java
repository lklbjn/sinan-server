package pres.peixinyi.sinan.dto.response;

import lombok.Data;

/**
 * 重复书签检查响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class CheckDuplicateResp {

    /**
     * 是否存在重复
     */
    private boolean duplicate;

    /**
     * 匹配的URL/域名
     */
    private String matchKey;

    /**
     * 重复的书签数量
     */
    private int count;

}
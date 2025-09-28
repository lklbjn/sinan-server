package pres.peixinyi.sinan.dto.request;

import lombok.Data;

/**
 * 添加忽略组请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class AddIgnoredGroupReq {

    /**
     * 组名称
     */
    private String groupName;
}
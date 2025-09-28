package pres.peixinyi.sinan.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 设置忽略组请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class SetIgnoredGroupsReq {

    /**
     * 组名称列表
     */
    private List<String> groupNames;
}
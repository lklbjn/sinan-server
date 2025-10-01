package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.module.sinan.entity.SnIgnoredGroup;

import java.util.Date;

/**
 * 忽略组响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Data
public class IgnoredGroupResp {

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 从实体类转换为响应对象
     *
     * @param snIgnoredGroup 忽略组实体
     * @return 忽略组响应对象
     */
    public static IgnoredGroupResp from(SnIgnoredGroup snIgnoredGroup) {
        IgnoredGroupResp resp = new IgnoredGroupResp();
        resp.setGroupName(snIgnoredGroup.getGroupName());
        resp.setCreatedAt(snIgnoredGroup.getCreatedAt());
        resp.setUpdatedAt(snIgnoredGroup.getUpdatedAt());
        return resp;
    }
}
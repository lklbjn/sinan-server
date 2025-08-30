package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnSpace;

/**
 * 空间简化响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/18
 * @Version : 0.0.0
 */
@Data
public class SpaceSimpleResp {
    /**
     * 空间ID
     */
    private String id;

    /**
     * 空间名称
     */
    private String name;

    /**
     * 空间图标
     */
    private String icon;

    /**
     * 从SnSpace实体转换
     */
    public static SpaceSimpleResp from(SnSpace space) {
        SpaceSimpleResp resp = new SpaceSimpleResp();
        resp.setId(space.getId());
        resp.setName(space.getName());
        resp.setIcon(space.getIcon());
        return resp;
    }
}

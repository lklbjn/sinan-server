package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;

import java.util.Date;

/**
 * 空间响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class SpaceResp {

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
     * 排序值
     */
    private Integer sort;

    private Boolean shared;

    private String key;

    /**
     * 空间描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 从实体类转换为响应对象
     *
     * @param snSpace 空间实体
     * @return 空间响应对象
     */
    public static SpaceResp from(SnSpace snSpace) {
        SpaceResp spaceResp = new SpaceResp();
        spaceResp.setId(snSpace.getId());
        spaceResp.setName(snSpace.getName());
        spaceResp.setShared(snSpace.getShare());
        spaceResp.setKey(snSpace.getShareKey());
        spaceResp.setIcon(snSpace.getIcon());
        spaceResp.setSort(snSpace.getSort());
        spaceResp.setDescription(snSpace.getDescription());
        spaceResp.setCreateTime(snSpace.getCreateTime());
        spaceResp.setUpdateTime(snSpace.getUpdateTime());
        return spaceResp;
    }
}

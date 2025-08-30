package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;

import java.util.Date;

/**
 * 标签响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13
 * @Version : 0.0.0
 */
@Data
public class TagResp {

    /**
     * 标签ID
     */
    private String id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    private Integer sort;

    /**
     * 标签描述
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
     * @param snTag 标签实体
     * @return 标签响应对象
     */
    public static TagResp from(SnTag snTag) {
        TagResp tagResp = new TagResp();
        tagResp.setId(snTag.getId());
        tagResp.setName(snTag.getName());
        tagResp.setColor(snTag.getColor());
        tagResp.setSort(snTag.getSort());
        tagResp.setDescription(snTag.getDescription());
        tagResp.setCreateTime(snTag.getCreateTime());
        tagResp.setUpdateTime(snTag.getUpdateTime());
        return tagResp;
    }
}

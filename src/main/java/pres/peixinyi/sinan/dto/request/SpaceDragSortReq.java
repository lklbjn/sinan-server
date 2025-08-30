package pres.peixinyi.sinan.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 空间拖拽排序请求
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/19
 * @Version : 0.0.0
 */
@Data
public class SpaceDragSortReq {

    /**
     * 拖拽的空间ID
     */
    private String draggedSpaceId;

    /**
     * 目标位置索引（从0开始）
     */
    private Integer targetIndex;

    /**
     * 或者直接传递重新排序后的空间ID列表
     */
    private List<String> sortedSpaceIds;
}

package pres.peixinyi.sinan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 网站分析响应DTO
 * 用于AI分析网站后返回的结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteAnalysisResponse {

    /**
     * 网站URL
     */
    private String url;

    /**
     * 网站名称
     */
    private String name;

    /**
     * 网站描述
     */
    private String description;

    /**
     * 分类名称
     */
    private String spaces;

    /**
     * 标签列表（可能包含 new: 前缀表示新标签）
     */
    private List<String> tags;
}

package pres.peixinyi.sinan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 导入书签响应
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/18
 * @Version : 0.0.0
 */
@Data
@AllArgsConstructor
public class ImportBookmarkResp {
    /**
     * 导入成功的书签数量
     */
    private int successCount;

    /**
     * 导入失败的书签数量
     */
    private int failCount;

    /**
     * 总数量
     */
    private int totalCount;

    private int skippedCount;

    /**
     * 导入结果消息
     */
    private String message;

    public static ImportBookmarkResp success(int successCount, int totalCount, int skippedCount) {
        return new ImportBookmarkResp(successCount, totalCount - successCount, totalCount, skippedCount,
                String.format("导入完成，成功导入 %d 个书签，跳过 %d 个书签，失败 %d 个", successCount, skippedCount, totalCount - successCount));
    }
}

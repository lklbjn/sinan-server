package pres.peixinyi.sinan.dto.response;

import lombok.Data;

/**
 * 用户数据导入响应
 */
@Data
public class UserDataImportResp {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 导入的标签数量
     */
    private int importedTagsCount;

    /**
     * 导入的空间数量
     */
    private int importedSpacesCount;

    /**
     * 导入的书签数量
     */
    private int importedBookmarksCount;

    /**
     * 跳过的标签数量（已存在）
     */
    private int skippedTagsCount;

    /**
     * 跳过的空间数量（已存在）
     */
    private int skippedSpacesCount;

    /**
     * 跳过的书签数量（已存在）
     */
    private int skippedBookmarksCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 详细信息
     */
    private String details;

    public static UserDataImportResp success(int importedTags, int importedSpaces, int importedBookmarks,
                                           int skippedTags, int skippedSpaces, int skippedBookmarks) {
        UserDataImportResp resp = new UserDataImportResp();
        resp.setSuccess(true);
        resp.setImportedTagsCount(importedTags);
        resp.setImportedSpacesCount(importedSpaces);
        resp.setImportedBookmarksCount(importedBookmarks);
        resp.setSkippedTagsCount(skippedTags);
        resp.setSkippedSpacesCount(skippedSpaces);
        resp.setSkippedBookmarksCount(skippedBookmarks);
        resp.setDetails(String.format("导入成功：标签 %d 个，空间 %d 个，书签 %d 个；跳过：标签 %d 个，空间 %d 个，书签 %d 个",
                importedTags, importedSpaces, importedBookmarks, skippedTags, skippedSpaces, skippedBookmarks));
        return resp;
    }

    public static UserDataImportResp failure(String errorMessage) {
        UserDataImportResp resp = new UserDataImportResp();
        resp.setSuccess(false);
        resp.setErrorMessage(errorMessage);
        return resp;
    }
}

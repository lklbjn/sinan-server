package pres.peixinyi.sinan.module.rbac.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.dto.request.UserDataImportReq;
import pres.peixinyi.sinan.dto.response.UserDataExportResp;
import pres.peixinyi.sinan.dto.response.UserDataImportResp;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;
import pres.peixinyi.sinan.module.sinan.entity.SnTag;
import pres.peixinyi.sinan.module.sinan.service.SnBookmarkAssTagService;
import pres.peixinyi.sinan.module.sinan.service.SnBookmarkService;
import pres.peixinyi.sinan.module.sinan.service.SnSpaceService;
import pres.peixinyi.sinan.module.sinan.service.SnTagService;
import pres.peixinyi.sinan.utils.PinyinUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户数据导出服务
 */
@Service
@Slf4j
public class UserDataExportService {

    @Autowired
    private SnTagService tagService;

    @Autowired
    private SnSpaceService spaceService;

    @Autowired
    private SnBookmarkService bookmarkService;

    @Autowired
    private SnBookmarkAssTagService bookmarkAssTagService;

    /**
     * 导出用户的所有数据
     *
     * @param userId 用户ID
     * @return 用户数据导出响应
     */
    public UserDataExportResp exportUserData(String userId) {
        try {
            log.info("开始导出用户数据，用户ID: {}", userId);

            // 获取用户的所有标签
            List<SnTag> userTags = tagService.getUserTags(userId);
            List<UserDataExportResp.TagExportData> tagExportData = userTags.stream()
                    .map(UserDataExportResp.TagExportData::from)
                    .collect(Collectors.toList());

            // 获取用户的所有空间
            List<SnSpace> userSpaces = spaceService.getUserSpaces(userId);
            List<UserDataExportResp.SpaceExportData> spaceExportData = userSpaces.stream()
                    .map(UserDataExportResp.SpaceExportData::from)
                    .collect(Collectors.toList());

            // 获取用户的所有书签
            List<SnBookmark> userBookmarks = bookmarkService.getUserBookmarks(userId);

            // 获取书签与标签的关联关系
            Map<String, List<String>> bookmarkTagMap = bookmarkAssTagService.getBookmarkTagMap(userId);

            // 构建书签导出数据
            List<UserDataExportResp.BookmarkExportData> bookmarkExportData = userBookmarks.stream()
                    .map(bookmark -> {
                        List<String> tagIds = bookmarkTagMap.getOrDefault(bookmark.getId(), List.of());
                        return UserDataExportResp.BookmarkExportData.from(bookmark, tagIds);
                    })
                    .collect(Collectors.toList());

            // 构建导出响应
            UserDataExportResp exportResp = new UserDataExportResp();
            exportResp.setTags(tagExportData);
            exportResp.setSpace(spaceExportData);
            exportResp.setBookmark(bookmarkExportData);

            log.info("用户数据导出完成，用户ID: {}，标签数: {}，空间数: {}，书签数: {}",
                    userId, tagExportData.size(), spaceExportData.size(), bookmarkExportData.size());

            return exportResp;

        } catch (Exception e) {
            log.error("导出用户数据失败，用户ID: {}", userId, e);
            throw new RuntimeException("导出数据失败: " + e.getMessage());
        }
    }

    /**
     * 导入用户数据
     *
     * @param importData 导入数据
     * @param userId     用户ID
     * @return 导入结果
     */
    public UserDataImportResp importUserData(UserDataImportReq importData, String userId) {
        try {
            log.info("开始导入用户数据，用户ID: {}", userId);

            int importedTags = 0, importedSpaces = 0, importedBookmarks = 0;
            int skippedTags = 0, skippedSpaces = 0, skippedBookmarks = 0;

            // 1. 导入标签
            if (importData.getTags() != null) {
                for (UserDataImportReq.TagImportData tagData : importData.getTags()) {
                    // 检查标签名称是否已存在
                    if (tagService.isTagNameExists(userId, tagData.getName(), null)) {
                        skippedTags++;
                        log.debug("标签已存在，跳过: {}", tagData.getName());
                        continue;
                    }

                    // 创建新标签
                    SnTag tag = new SnTag();
                    tag.setUserId(userId);
                    tag.setName(tagData.getName());
                    tag.setPinyin(PinyinUtils.toPinyin(tagData.getName()));
                    tag.setAbbreviation(PinyinUtils.toPinyinFirstLetter(tagData.getName()));
                    tag.setColor(tagData.getColor());
                    tag.setSort(tagData.getSort());
                    tag.setDescription(tagData.getDescription());

                    if (tagService.addTag(tag) != null) {
                        importedTags++;
                    }
                }
            }

            // 2. 导入空间
            if (importData.getSpace() != null) {
                for (UserDataImportReq.SpaceImportData spaceData : importData.getSpace()) {
                    // 检查空间名称是否已存在
                    if (spaceService.isSpaceNameExists(userId, spaceData.getName(), null)) {
                        skippedSpaces++;
                        log.debug("空间已存在，跳过: {}", spaceData.getName());
                        continue;
                    }

                    // 创建新空间
                    SnSpace space = new SnSpace();
                    space.setUserId(userId);
                    space.setName(spaceData.getName());
                    space.setPinyin(PinyinUtils.toPinyin(spaceData.getName()));
                    space.setAbbreviation(PinyinUtils.toPinyinFirstLetter(spaceData.getName()));
                    space.setIcon(spaceData.getIcon());
                    space.setSort(spaceData.getSort());
                    space.setDescription(spaceData.getDescription());

                    if (spaceService.addSpace(space) != null) {
                        importedSpaces++;
                    }
                }
            }

            // 3. 导入书签
            if (importData.getBookmark() != null) {
                for (UserDataImportReq.BookmarkImportData bookmarkData : importData.getBookmark()) {
                    // 检查书签URL是否已存在（同一用户下）
                    if (isBookmarkUrlExists(userId, bookmarkData.getUrl())) {
                        skippedBookmarks++;
                        log.debug("书签已存在，跳过: {}", bookmarkData.getUrl());
                        continue;
                    }

                    // 验证空间ID是否存在（如果指定了spaceId）
                    String validSpaceId = null;
                    if (bookmarkData.getSpaceId() != null && !bookmarkData.getSpaceId().isEmpty()) {
                        // 通过空间名称查找空间ID（因为导入时ID可能不同）
                        List<SnSpace> userSpaces = spaceService.getUserSpaces(userId);
                        for (SnSpace space : userSpaces) {
                            // 这里需要根据原始数据中的空间信息来匹配
                            if (importData.getSpace() != null) {
                                for (UserDataImportReq.SpaceImportData spaceData : importData.getSpace()) {
                                    if (spaceData.getId().equals(bookmarkData.getSpaceId()) &&
                                            space.getName().equals(spaceData.getName())) {
                                        validSpaceId = space.getId();
                                        break;
                                    }
                                }
                            }
                            if (validSpaceId != null) break;
                        }
                    }

                    // 创建新书签
                    SnBookmark bookmark = new SnBookmark();
                    bookmark.setUserId(userId);
                    bookmark.setSpaceId(validSpaceId);
                    bookmark.setName(bookmarkData.getName());
                    bookmark.setPinyin(PinyinUtils.toPinyin(bookmarkData.getName()));
                    bookmark.setAbbreviation(PinyinUtils.toPinyinFirstLetter(bookmarkData.getName()));
                    bookmark.setDescription(bookmarkData.getDescription());
                    bookmark.setUrl(bookmarkData.getUrl());
                    bookmark.setIcon(bookmarkData.getIcon());
                    bookmark.setNum(bookmarkData.getNum() != null ? bookmarkData.getNum() : 0);
                    bookmark.setStar(bookmarkData.getStar() != null ? bookmarkData.getStar() : false);

                    SnBookmark savedBookmark = bookmarkService.addBookmark(bookmark);
                    if (savedBookmark != null) {
                        importedBookmarks++;

                        // 处理书签标签关联
                        if (bookmarkData.getTags() != null && !bookmarkData.getTags().isEmpty()) {
                            List<String> validTagIds = new java.util.ArrayList<>();

                            // 根据标签名称查找标签ID
                            for (String originalTagId : bookmarkData.getTags()) {
                                if (importData.getTags() != null) {
                                    for (UserDataImportReq.TagImportData tagData : importData.getTags()) {
                                        if (tagData.getId().equals(originalTagId)) {
                                            // 查找用户的标签中是否有同名标签
                                            List<SnTag> userTags = tagService.getUserTags(userId);
                                            for (SnTag tag : userTags) {
                                                if (tag.getName().equals(tagData.getName())) {
                                                    validTagIds.add(tag.getId());
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }

                            // 创建书签标签关联
                            if (!validTagIds.isEmpty()) {
                                bookmarkAssTagService.updateBookmarkTagAssociations(
                                        savedBookmark.getId(), userId, validTagIds);
                            }
                        }
                    }
                }
            }

            log.info("用户数据导入完成，用户ID: {}，导入：标签 {} 个，空间 {} 个，书签 {} 个；跳过：标签 {} 个，空间 {} 个，书签 {} 个",
                    userId, importedTags, importedSpaces, importedBookmarks,
                    skippedTags, skippedSpaces, skippedBookmarks);

            return UserDataImportResp.success(importedTags, importedSpaces, importedBookmarks,
                    skippedTags, skippedSpaces, skippedBookmarks);

        } catch (Exception e) {
            log.error("导入用户数据失败，用户ID: {}", userId, e);
            return UserDataImportResp.failure("导入数据失败: " + e.getMessage());
        }
    }

    /**
     * 清空用户的所有数据
     *
     * @param userId 用户ID
     * @return 清空结果
     */
    public UserDataImportResp clearUserData(String userId) {
        try {
            log.info("开始清空用户数据，用户ID: {}", userId);

            int deletedTags = 0, deletedSpaces = 0, deletedBookmarks = 0;

            // 1. 获取用户的所有数据数量（用于统计）
            List<SnTag> userTags = tagService.getUserTags(userId);
            List<SnSpace> userSpaces = spaceService.getUserSpaces(userId);
            List<SnBookmark> userBookmarks = bookmarkService.getUserBookmarks(userId);

            // 2. 删除所有书签标签关联关系
            for (SnBookmark bookmark : userBookmarks) {
                bookmarkAssTagService.deleteBookmarkTagAssociations(bookmark.getId(), userId);
            }

            // 3. 删除所有书签
            for (SnBookmark bookmark : userBookmarks) {
                if (bookmarkService.deleteBookmark(bookmark.getId(), userId)) {
                    deletedBookmarks++;
                }
            }

            // 4. 删除所有标签
            for (SnTag tag : userTags) {
                if (tagService.deleteTag(tag.getId(), userId)) {
                    deletedTags++;
                }
            }

            // 5. 删除所有空间
            for (SnSpace space : userSpaces) {
                if (spaceService.deleteSpace(space.getId(), userId)) {
                    deletedSpaces++;
                }
            }

            log.info("用户数据清空完成，用户ID: {}，删除：标签 {} 个，空间 {} 个，书签 {} 个",
                    userId, deletedTags, deletedSpaces, deletedBookmarks);

            // 构建成功响应
            UserDataImportResp resp = new UserDataImportResp();
            resp.setSuccess(true);
            resp.setImportedTagsCount(0);
            resp.setImportedSpacesCount(0);
            resp.setImportedBookmarksCount(0);
            resp.setSkippedTagsCount(deletedTags);
            resp.setSkippedSpacesCount(deletedSpaces);
            resp.setSkippedBookmarksCount(deletedBookmarks);
            resp.setDetails(String.format("数据清空成功：删除标签 %d 个，空间 %d 个，书签 %d 个",
                    deletedTags, deletedSpaces, deletedBookmarks));
            return resp;

        } catch (Exception e) {
            log.error("清空用户数据失败，用户ID: {}", userId, e);
            return UserDataImportResp.failure("清空数据失败: " + e.getMessage());
        }
    }

    /**
     * 检查书签URL是否已存在
     */
    private boolean isBookmarkUrlExists(String userId, String url) {
        return bookmarkService.lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getUrl, url)
                .eq(SnBookmark::getDeleted, 0)
                .exists();
    }
}

package pres.peixinyi.sinan.model.sinan.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddBookmarkReq;
import pres.peixinyi.sinan.dto.response.BookmarkResp;
import pres.peixinyi.sinan.dto.response.BookmarkTreeResp;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmarkAssTag;
import pres.peixinyi.sinan.model.sinan.entity.SnSpace;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;
import pres.peixinyi.sinan.model.sinan.service.SnBookmarkAssTagService;
import pres.peixinyi.sinan.model.sinan.service.SnBookmarkService;
import pres.peixinyi.sinan.model.sinan.service.SnSpaceService;
import pres.peixinyi.sinan.model.sinan.service.SnTagService;
import pres.peixinyi.sinan.model.rbac.service.SnUserKeyService;
import pres.peixinyi.sinan.utils.PinyinUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于SnUserKey认证的API控制器
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/22
 * @Version : 1.0.0
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class ApiController {

    @Resource
    private SnUserKeyService userKeyService;

    @Resource
    private SnBookmarkService bookmarkService;

    @Resource
    private SnSpaceService spaceService;

    @Resource
    private SnTagService tagService;

    @Resource
    private SnBookmarkAssTagService bookmarkAssTagService;

    /**
     * 验证访问密钥并获取用户ID
     *
     * @param accessKey 访问密钥
     * @return 用户ID，如果验证失败返回null
     */
    private String authenticateUser(String accessKey) {
        if (accessKey == null || accessKey.trim().isEmpty()) {
            return null;
        }
        return userKeyService.getUserIdByAccessKey(accessKey);
    }

    /**
     * 添加书签
     *
     * @param accessKey 访问密钥
     * @param req       添加书签请求
     * @return 添加结果
     */
    @PostMapping("/bookmark")
    public Result<BookmarkResp> addBookmark(
            @RequestHeader("X-Access-Key") String accessKey,
            @Valid @RequestBody AddBookmarkReq req) {

        // 验证访问密钥
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }

        try {
            // 检查是否已存在相同URL的书签
            if (bookmarkService.isUrlExistsForUser(req.getUrl(), userId)) {
                // 获取已存在的书签信息
                SnBookmark existingBookmark = bookmarkService.getBookmarkByUrlAndUser(req.getUrl(), userId);
                List<SnTag> existingTags = bookmarkService.getBookmarkTags(existingBookmark.getId());
                BookmarkResp existingBookmarkResp = BookmarkResp.from(existingBookmark, existingTags);
                return Result.ok(existingBookmarkResp);
            }

            // 检查namespace是否存在和属于当前用户
            if (req.getNamespaceId() != null && !req.getNamespaceId().isEmpty()) {
                if (!spaceService.isNamespaceBelongsToUser(req.getNamespaceId(), userId)) {
                    return Result.fail("命名空间不存在或无权限访问");
                }
            }

            // 检查Tags是否存在和属于当前用户
            if (req.getTagsIds() != null && !req.getTagsIds().isEmpty()) {
                if (!tagService.areAllTagsBelongToUser(req.getTagsIds(), userId)) {
                    return Result.fail("部分标签不存在或无权限访问");
                }
            }

            // 初始化参数
            SnBookmark bookmark = new SnBookmark();
            bookmark.setUserId(userId);
            bookmark.setSpaceId(req.getNamespaceId());
            // 截取书签名称到30位
            String bookmarkName = req.getName();
            if (bookmarkName != null && bookmarkName.length() > 30) {
                bookmarkName = bookmarkName.substring(0, 30);
            }
            bookmark.setName(bookmarkName);
            bookmark.setDescription(req.getDescription());
            bookmark.setUrl(req.getUrl());
            bookmark.setNum(0);

            SnBookmark savedBookmark = bookmarkService.addBookmark(bookmark);

            // 处理标签关联
            if (req.getTagsIds() != null && !req.getTagsIds().isEmpty()) {
                for (String tagId : req.getTagsIds()) {
                    SnBookmarkAssTag assTag = new SnBookmarkAssTag();
                    assTag.setUserId(userId);
                    assTag.setBookmarkId(savedBookmark.getId());
                    assTag.setTagId(tagId);
                    assTag.setCreateTime(new Date());
                    assTag.setUpdateTime(new Date());
                    assTag.setDeleted(0);
                    bookmarkAssTagService.save(assTag);
                }
            }

            // 获取书签的标签信息
            List<SnTag> tags = bookmarkService.getBookmarkTags(savedBookmark.getId());

            // 构建响应对象
            BookmarkResp bookmarkResp = BookmarkResp.from(savedBookmark, tags);
            return Result.success(bookmarkResp);

        } catch (Exception e) {
            return Result.fail("添加书签失败: " + e.getMessage());
        }
    }

    /**
     * 构建格式化的书签名称
     * 格式：默认名称-名称拼音-空间名称-空间拼音-标签名称-标签拼音 (根据includePinyin参数决定是否包含拼音)
     *
     * @param bookmark      书签对象
     * @param space         空间对象
     * @param tags          标签列表
     * @param includePinyin 是否包含拼音
     * @return 格式化的名称
     */
    private String buildFormattedBookmarkName(SnBookmark bookmark, SnSpace space, List<SnTag> tags, boolean includePinyin) {
        StringBuilder nameBuilder = new StringBuilder();

        // 默认名称
        String originalName = bookmark.getName() != null ? bookmark.getName() : "";
        nameBuilder.append(originalName);

        // 名称拼音
        if (includePinyin) {
            String namePinyin = PinyinUtils.toPinyin(originalName);
            String firstPinyin = PinyinUtils.toPinyinFirstLetter(originalName);
            if (!namePinyin.isEmpty()) {
                nameBuilder.append("-").append(namePinyin);
            }
            if (!firstPinyin.isEmpty()) {
                nameBuilder.append("-").append(firstPinyin);
            }
        }

        // 空间名称和拼音
        if (space != null) {
            String spaceName = space.getName() != null ? space.getName() : "";
            nameBuilder.append("-").append(spaceName);

            if (includePinyin) {
                String spacePinyin = PinyinUtils.toPinyin(spaceName);
                String spaceFirstPinyin = PinyinUtils.toPinyinFirstLetter(spaceName);
                if (!spacePinyin.isEmpty()) {
                    nameBuilder.append("-").append(spacePinyin);
                }
                if (!spaceFirstPinyin.isEmpty()) {
                    nameBuilder.append("-").append(spaceFirstPinyin);
                }
            }
        } else {
            // 如果没有空间，添加"未分类"
            if (includePinyin) {
                nameBuilder.append("-未分类-weifenlei");
            } else {
                nameBuilder.append("-未分类");
            }
        }

        // 标签名称和拼音 - 支持多个标签连续拼接
        if (tags != null && !tags.isEmpty()) {
            for (SnTag tag : tags) {
                String tagName = tag.getName() != null ? tag.getName() : "";
                if (!tagName.isEmpty()) {
                    nameBuilder.append("-").append(tagName);

                    if (includePinyin) {
                        String tagPinyin = PinyinUtils.toPinyin(tagName);
                        String tagFirstPinyin = PinyinUtils.toPinyinFirstLetter(tagName);
                        if (!tagPinyin.isEmpty()) {
                            nameBuilder.append("-").append(tagPinyin);
                        }
                        if (!tagFirstPinyin.isEmpty()) {
                            nameBuilder.append("-").append(tagFirstPinyin);
                        }
                    }
                }
            }
        }

        return nameBuilder.toString();
    }

    /**
     * 获取书签树（根据Space区分）
     *
     * @param accessKey 访问密钥
     * @return 书签树列表
     */
    @GetMapping("/bookmark")
    public Result<List<BookmarkTreeResp>> getBookmarkTree(
            @RequestParam(value = "pinyin", required = false, defaultValue = "true") boolean pinyin,
            @RequestHeader("X-Access-Key") String accessKey) {

        // 验证访问密钥
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }

        try {
            // 获取用户的所有空间
            List<SnSpace> spaces = spaceService.getUserSpaces(userId);

            // 构建书签树
            List<BookmarkTreeResp> bookmarkTree = spaces.stream()
                    .map(space -> {
                        // 获取该空间下的所有书签
                        List<SnBookmark> bookmarks = bookmarkService.getBookmarksBySpaceId(space.getId(), userId);

                        if (bookmarks.isEmpty()) {
                            return BookmarkTreeResp.from(space, List.of());
                        }

                        // 批量获取书签的标签信息
                        List<String> bookmarkIds = bookmarks.stream()
                                .map(SnBookmark::getId)
                                .toList();
                        Map<String, List<SnTag>> bookmarkTagsMap =
                                bookmarkService.getBatchBookmarkTags(bookmarkIds);

                        // 构建书签响应对象，使用格式化的名称
                        List<BookmarkResp> bookmarkResps = bookmarks.stream()
                                .map(bookmark -> {
                                    List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                                    BookmarkResp resp = BookmarkResp.from(bookmark, tags);

                                    // 设置格式化的名称
                                    String formattedName = buildFormattedBookmarkName(bookmark, space, tags, pinyin);
                                    resp.setName(formattedName);

                                    return resp;
                                })
                                .toList();

                        return BookmarkTreeResp.from(space, bookmarkResps);
                    })
                    .collect(Collectors.toList());

            // 处理没有空间的书签（无命名空间的书签）
            List<SnBookmark> noSpaceBookmarks = bookmarkService.getNoNamespaceBookmarks(userId);
            if (!noSpaceBookmarks.isEmpty()) {
                // 批量获取书签的标签信息
                List<String> bookmarkIds = noSpaceBookmarks.stream()
                        .map(SnBookmark::getId)
                        .toList();
                Map<String, List<SnTag>> bookmarkTagsMap =
                        bookmarkService.getBatchBookmarkTags(bookmarkIds);

                // 构建书签响应对象，使用格式化的名称
                List<BookmarkResp> bookmarkResps = noSpaceBookmarks.stream()
                        .map(bookmark -> {
                            List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                            BookmarkResp resp = BookmarkResp.from(bookmark, tags);

                            // 设置格式化的名称（没有空间的情况）
                            String formattedName = buildFormattedBookmarkName(bookmark, null, tags, pinyin);
                            resp.setName(formattedName);

                            return resp;
                        })
                        .toList();

                // 创建一个虚拟的"未分类"空间
                BookmarkTreeResp unclassified = new BookmarkTreeResp();
                unclassified.setSpaceId(null);
                unclassified.setSpaceName("未分类");
                unclassified.setSpaceDescription("没有分配到任何空间的书签");
                unclassified.setBookmarks(bookmarkResps);

                bookmarkTree.add(unclassified);
            }

            return Result.success(bookmarkTree);

        } catch (Exception e) {
            return Result.fail("获取书签树失败: " + e.getMessage());
        }
    }

    /**
     * 搜索书签
     *
     * @param accessKey
     * @param search
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.BookmarkResp>>
     * @author peixinyi
     * @since 10:41 2025/8/27
     */
    @GetMapping("/bookmarks")
    public Result<List<BookmarkResp>> getBookmark(
            @RequestHeader("X-Access-Key") String accessKey,
            @RequestParam(value = "search", required = false) String search) {
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }
        try {
            List<SnBookmark> bookmarks;
            if (search != null && !search.trim().isEmpty()) {
                bookmarks = bookmarkService.searchBookmarks(userId, search.trim());
            } else {
                bookmarks = bookmarkService.getAllBookmarks(userId);
            }

            if (bookmarks.isEmpty()) {
                return Result.ok(List.of());
            }

            // 批量获取书签的标签信息
            List<String> bookmarkIds = bookmarks.stream()
                    .map(SnBookmark::getId)
                    .toList();
            Map<String, List<SnTag>> bookmarkTagsMap =
                    bookmarkService.getBatchBookmarkTags(bookmarkIds);

            // 构建书签响应对象
            List<BookmarkResp> bookmarkResps = bookmarks.stream()
                    .map(bookmark -> {
                        List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                        return BookmarkResp.from(bookmark, tags);
                    })
                    .toList();

            return Result.success(bookmarkResps);
        } catch (Exception e) {
            return Result.fail("获取书签失败: " + e.getMessage());
        }
    }
}

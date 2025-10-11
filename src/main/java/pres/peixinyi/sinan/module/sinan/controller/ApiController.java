package pres.peixinyi.sinan.module.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddBookmarkReq;
import pres.peixinyi.sinan.dto.response.BookmarkResp;
import pres.peixinyi.sinan.dto.response.BookmarkTreeResp;
import pres.peixinyi.sinan.dto.response.TagResp;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmarkAssTag;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;
import pres.peixinyi.sinan.module.sinan.entity.SnTag;
import pres.peixinyi.sinan.module.sinan.service.*;
import pres.peixinyi.sinan.module.rbac.service.SnUserKeyService;
import pres.peixinyi.sinan.module.favicon.service.FaviconService;
import pres.peixinyi.sinan.service.WebsiteAnalysisService;
import pres.peixinyi.sinan.common.UrlValidator;
import pres.peixinyi.sinan.utils.PinyinUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
@Slf4j
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

    @Resource
    private FaviconService faviconService;

    @Resource
    private SnShareSpaceAssUserService snShareSpaceAssUserService;

    @Resource
    private WebsiteAnalysisService websiteAnalysisService;

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

            // 处理命名空间 - 支持多种方式
            String namespaceId = null;

            // 1. 处理传统的 :new 格式
            if (req.getNamespaceId() != null && req.getNamespaceId().contains(":new")) {
                // 解析命名空间名称
                String spaceName = req.getNamespaceId().replace(":new", "").trim();
                if (!spaceName.isEmpty()) {
                    // 检查是否已存在同名空间
                    if (!spaceService.isSpaceNameExists(userId, spaceName, null)) {
                        // 创建新空间
                        SnSpace newSpace = new SnSpace();
                        newSpace.setUserId(userId);
                        newSpace.setName(spaceName);
                        newSpace.setIcon("StarsIcon");
                        newSpace = spaceService.addSpace(newSpace);
                        namespaceId = newSpace.getId();
                    } else {
                        // 如果空间已存在，查找并使用现有的空间
                        SnSpace existingSpace = spaceService.lambdaQuery()
                                .eq(SnSpace::getUserId, userId)
                                .eq(SnSpace::getName, spaceName)
                                .eq(SnSpace::getDeleted, 0)
                                .one();
                        if (existingSpace != null) {
                            namespaceId = existingSpace.getId();
                        }
                    }
                } else {
                    return Result.fail("命名空间名称不能为空");
                }
            }
            // 2. 处理 newSpace 对象格式
            else if (req.getNewSpace() != null) {
                AddBookmarkReq.NewSpace newSpaceInfo = req.getNewSpace();
                String spaceName = newSpaceInfo.getName();

                // 检查是否已存在同名空间
                if (!spaceService.isSpaceNameExists(userId, spaceName, null)) {
                    // 创建新空间
                    SnSpace newSpace = new SnSpace();
                    newSpace.setUserId(userId);
                    newSpace.setName(spaceName.replace(":new", ""));
                    newSpace.setDescription(newSpaceInfo.getDescription());
                    newSpace.setIcon("StarsIcon");
                    newSpace = spaceService.addSpace(newSpace);
                    namespaceId = newSpace.getId();
                } else {
                    // 如果空间已存在，查找并使用现有的空间
                    SnSpace existingSpace = spaceService.lambdaQuery()
                            .eq(SnSpace::getUserId, userId)
                            .eq(SnSpace::getName, spaceName)
                            .eq(SnSpace::getDeleted, 0)
                            .one();
                    if (existingSpace != null) {
                        namespaceId = existingSpace.getId();
                    }
                }
            }
            // 3. 处理普通的 namespaceId
            else {
                namespaceId = req.getNamespaceId();
            }

            // 检查namespace是否存在和属于当前用户
            if (namespaceId != null) {
                if (!spaceService.isNamespaceBelongsToUser(namespaceId, userId)) {
                    return Result.fail("命名空间不存在或无权限访问");
                }
            }

            // 处理标签 - 支持多种方式
            List<String> tagIds = new ArrayList<>();

            // 1. 处理传统的 tagsIds 列表
            if (req.getTagsIds() != null && !req.getTagsIds().isEmpty()) {
                for (String tagInfo : req.getTagsIds()) {
                    if (tagInfo.contains(":new:#")) {
                        // 解析标签信息: 格式为 "标签名:new:#颜色代码"
                        String[] parts = tagInfo.split(":new:#");
                        if (parts.length == 2) {
                            String tagName = parts[0].trim();
                            String tagColor = parts[1].trim();

                            if (!tagName.isEmpty() && !tagColor.isEmpty()) {
                                // 检查是否已存在同名标签
                                if (!tagService.isTagNameExists(userId, tagName, null)) {
                                    // 创建新标签
                                    SnTag newTag = new SnTag();
                                    newTag.setUserId(userId);
                                    newTag.setName(tagName);
                                    newTag.setColor(tagColor);
                                    newTag = tagService.addTag(newTag);
                                    tagIds.add(newTag.getId());
                                } else {
                                    // 如果标签已存在，查找并使用现有的标签
                                    SnTag existingTag = tagService.lambdaQuery()
                                            .eq(SnTag::getUserId, userId)
                                            .eq(SnTag::getName, tagName)
                                            .eq(SnTag::getDeleted, 0)
                                            .one();
                                    if (existingTag != null) {
                                        tagIds.add(existingTag.getId());
                                    }
                                }
                            } else {
                                return Result.fail("标签名称或颜色不能为空");
                            }
                        } else {
                            return Result.fail("标签格式错误，应为: 标签名:new:#颜色代码");
                        }
                    } else {
                        // 普通标签ID，直接添加到列表
                        tagIds.add(tagInfo);
                    }
                }
            }

            // 2. 处理 newTags 对象格式
            if (req.getNewTags() != null && !req.getNewTags().isEmpty()) {
                for (AddBookmarkReq.NewTag newTagInfo : req.getNewTags()) {
                    String tagName = newTagInfo.getName();
                    String tagColor = newTagInfo.getColor();

                    // 检查是否已存在同名标签
                    if (!tagService.isTagNameExists(userId, tagName, null)) {
                        // 创建新标签
                        SnTag newTag = new SnTag();
                        newTag.setUserId(userId);
                        newTag.setName(tagName);
                        newTag.setColor(tagColor);
                        newTag.setDescription(newTagInfo.getDescription());
                        newTag = tagService.addTag(newTag);
                        tagIds.add(newTag.getId());
                    } else {
                        // 如果标签已存在，查找并使用现有的标签
                        SnTag existingTag = tagService.lambdaQuery()
                                .eq(SnTag::getUserId, userId)
                                .eq(SnTag::getName, tagName)
                                .eq(SnTag::getDeleted, 0)
                                .one();
                        if (existingTag != null) {
                            tagIds.add(existingTag.getId());
                        }
                    }
                }
            }

            // 检查所有标签是否存在和属于当前用户
            if (!tagIds.isEmpty()) {
                if (!tagService.areAllTagsBelongToUser(tagIds, userId)) {
                    return Result.fail("部分标签不存在或无权限访问");
                }
            }

            // 初始化参数
            SnBookmark bookmark = new SnBookmark();
            bookmark.setUserId(userId);
            bookmark.setSpaceId(namespaceId);
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
            if (!tagIds.isEmpty()) {
                for (String tagId : tagIds) {
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

            // 构建书签树（用户自己的空间）
            List<BookmarkTreeResp> bookmarkTree = spaces.stream()
                    .map(space -> {
                        List<SnBookmark> bookmarks = bookmarkService.getBookmarksBySpaceId(space.getId(), userId);
                        if (bookmarks.isEmpty()) {
                            return BookmarkTreeResp.from(space, List.of());
                        }
                        List<String> bookmarkIds = bookmarks.stream().map(SnBookmark::getId).toList();
                        Map<String, List<SnTag>> bookmarkTagsMap = bookmarkService.getBatchBookmarkTags(bookmarkIds);
                        List<BookmarkResp> bookmarkResps = bookmarks.stream()
                                .map(bookmark -> {
                                    List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                                    BookmarkResp resp = BookmarkResp.from(bookmark, tags);
                                    String formattedName = buildFormattedBookmarkName(bookmark, space, tags, pinyin);
                                    resp.setName(formattedName);
                                    return resp;
                                })
                                .toList();
                        return BookmarkTreeResp.from(space, bookmarkResps);
                    })
                    .collect(Collectors.toList());

            // 获取用户订阅空间ID列表
            List<String> subscribedSpaceIds = snShareSpaceAssUserService.getByUserId(userId)
                .stream()
                .map(ass -> ass.getSpaceId())
                .distinct()
                .toList();
            if (subscribedSpaceIds != null && !subscribedSpaceIds.isEmpty()) {
                // 查询订阅空间信息
                List<SnSpace> subSpaces = spaceService.getSpacesByIds(subscribedSpaceIds);
                for (SnSpace subSpace : subSpaces) {
                    // 获取订阅空间下所有书签（不限制userId）
                    List<SnBookmark> subBookmarks = bookmarkService.getBookmarksBySpaceId(subSpace.getId());
                    List<String> subBookmarkIds = subBookmarks.stream().map(SnBookmark::getId).toList();
                    Map<String, List<SnTag>> subBookmarkTagsMap = bookmarkService.getBatchBookmarkTags(subBookmarkIds);
                    List<BookmarkResp> subBookmarkResps = subBookmarks.stream()
                            .map(bookmark -> {
                                List<SnTag> tags = subBookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                                BookmarkResp resp = BookmarkResp.from(bookmark, tags);
                                // 格式化名称
                                String formattedName = buildFormattedBookmarkName(bookmark, subSpace, tags, pinyin);
                                resp.setName(formattedName);
                                return resp;
                            })
                            .toList();
                    // 构建订阅空间节点，空间名称加SUB-前缀
                    BookmarkTreeResp subTree = new BookmarkTreeResp();
                    subTree.setSpaceId(subSpace.getId());
                    subTree.setSpaceName("SUB-" + subSpace.getName());
                    subTree.setSpaceDescription(subSpace.getDescription());
                    subTree.setBookmarks(subBookmarkResps);
                    bookmarkTree.add(subTree);
                }
            }

            // 处理没有空间的书签（无命名空间的书签）
            List<SnBookmark> noSpaceBookmarks = bookmarkService.getNoNamespaceBookmarks(userId);
            if (!noSpaceBookmarks.isEmpty()) {
                List<String> bookmarkIds = noSpaceBookmarks.stream().map(SnBookmark::getId).toList();
                Map<String, List<SnTag>> bookmarkTagsMap = bookmarkService.getBatchBookmarkTags(bookmarkIds);
                List<BookmarkResp> bookmarkResps = noSpaceBookmarks.stream()
                        .map(bookmark -> {
                            List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                            BookmarkResp resp = BookmarkResp.from(bookmark, tags);
                            String formattedName = buildFormattedBookmarkName(bookmark, null, tags, pinyin);
                            resp.setName(formattedName);
                            return resp;
                        })
                        .toList();
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
            // 获取用户订阅空间ID列表
            List<String> subscribedSpaceIds = snShareSpaceAssUserService.getByUserId(userId)
                .stream()
                .map(ass -> ass.getSpaceId())
                .toList();

            List<SnBookmark> bookmarks = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                // 用户自己的书签
                bookmarks.addAll(bookmarkService.searchBookmarks(userId, search.trim()));
                // 订阅空间的书签
                if (subscribedSpaceIds != null && !subscribedSpaceIds.isEmpty()) {
                    for (String spaceId : subscribedSpaceIds) {
                        bookmarks.addAll(bookmarkService.searchBookmarksBySpaceId(spaceId, search.trim()));
                    }
                }
            } else {
                // 用户自己的全部书签
                bookmarks.addAll(bookmarkService.getAllBookmarks(userId));
                // 订阅空间的全部书签
                if (subscribedSpaceIds != null && !subscribedSpaceIds.isEmpty()) {
                    for (String spaceId : subscribedSpaceIds) {
                        bookmarks.addAll(bookmarkService.getBookmarksBySpaceId(spaceId));
                    }
                }
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

    /**
     * 增加书签使用次数
     *
     * @param id 书签ID
     * @return 操作结果
     */
    @GetMapping("increment-usage")
    public Result<String> incrementBookmarkUsage(
            @RequestHeader("X-Access-Key") String accessKey,
            @RequestParam("id") String id) {
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }
        // 检查书签是否存在且属于当前用户
        SnBookmark bookmark = bookmarkService.getBookmarkByUserAndId(id, userId);
        if (bookmark == null) {
            return Result.fail("书签不存在或无权限访问");
        }

        // 增加使用次数
        boolean success = bookmarkService.incrementUsageCount(id, userId);

        if (success) {
            return Result.success("书签使用次数增加成功");
        } else {
            return Result.fail("书签使用次数增加失败");
        }
    }


    /**
     * 获取最常使用的书签
     *
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.BookmarkVO>>
     * @author peixinyi
     * @since 20:45 2025/8/13
     */
    @GetMapping("/most-visited")
    public Result<List<BookmarkResp>> getMostVisitedBookmarks(
            @RequestHeader("X-Access-Key") String accessKey,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "search", required = false) String search) {
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }
        // 获取最常访问的书签
        List<SnBookmark> bookmarks = bookmarkService.getMostVisitedBookmarks(limit, search, userId);

        if (bookmarks.isEmpty()) {
            return Result.success(List.of());
        }

        // 批量获取书签的标签信息
        List<String> bookmarkIds = bookmarks.stream()
                .map(SnBookmark::getId)
                .toList();
        Map<String, List<SnTag>> bookmarkTagsMap =
                bookmarkService.getBatchBookmarkTags(bookmarkIds);

        // 构建响应对象，包含标签信息
        List<BookmarkResp> bookmarkResponses = bookmarks.stream()
                .map(bookmark -> {
                    List<SnTag> tags =
                            bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                    return BookmarkResp.from(bookmark, tags);
                })
                .toList();

        return Result.success(bookmarkResponses);
    }

    /**
     * 根据域名获取favicon图标
     *
     * @param accessKey 访问密钥
     * @param domain    域名，如 vitepress.dev
     * @param sz        图标尺寸，可选参数，默认返回最优尺寸
     * @return favicon图片文件
     */
    @GetMapping("/favicon/icon")
    public ResponseEntity<org.springframework.core.io.Resource> getFaviconByDomain(
            @RequestHeader("X-Access-Key") String accessKey,
            @RequestParam("domain") String domain,
            @RequestParam(value = "sz", required = false) Integer sz) {

        // 验证访问密钥
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 检查域名是否被标记为失败
            if (faviconService.isDomainMarkedAsFailed(domain)) {
                return ResponseEntity.notFound().build();
            }

            String cachedFilePath = faviconService.getFaviconByDomainAndSize(domain, sz);
            if (cachedFilePath != null) {
                // 直接返回缓存的文件
                Path filePath = Paths.get(cachedFilePath);
                File file = filePath.toFile();

                if (file.exists() && file.isFile()) {
                    org.springframework.core.io.Resource resource = new FileSystemResource(file);

                    // 根据文件扩展名确定Content-Type
                    String fileName = file.getName();
                    String contentType = determineContentType(fileName);

                    // 设置缓存头
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(contentType));
                    headers.setCacheControl("public, max-age=2592000"); // 缓存1个月（30天 * 24小时 * 60分钟 * 60秒）

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource);
                }
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名确定Content-Type
     *
     * @param filename 文件名
     * @return MIME类型字符串
     */
    private String determineContentType(String filename) {
        if (filename == null) {
            return "image/png";
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFilename.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/png";
    }

    /**
     * 获取用户的所有空间
     *
     * @param accessKey     访问密钥
     * @return 空间列表
     */
    @GetMapping("/spaces")
    public Result<List<SnSpace>> getAllSpaces(
            @RequestHeader(value = "X-Access-Key", required = false) String accessKey) {

        // 支持两种认证方式：1. X-Access-Key 2. Authorization (SA-Token)
        String userId = null;

        // 优先使用X-Access-Key认证
        if (accessKey != null && !accessKey.trim().isEmpty()) {
            userId = authenticateUser(accessKey);
        }

        // 两种认证方式都失败
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }

        try {
            List<SnSpace> spaces = spaceService.getUserSpaces(userId);
            return Result.success(spaces);
        } catch (Exception e) {
            return Result.fail("获取空间列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有标签
     *
     * @param accessKey 访问密钥
     * @return 标签列表
     */
    @GetMapping("/tags")
    public Result<List<TagResp>> getAllTags(
            @RequestHeader("X-Access-Key") String accessKey) {

        // 验证访问密钥
        String userId = authenticateUser(accessKey);
        if (userId == null) {
            return Result.fail("无效的访问密钥");
        }

        try {
            // 获取用户的所有标签
            List<SnTag> tags = tagService.getUserTags(userId);

            // 转换为响应对象
            List<TagResp> tagRespList = tags.stream()
                    .map(TagResp::from)
                    .toList();

            return Result.success(tagRespList);
        } catch (Exception e) {
            return Result.fail("获取标签列表失败: " + e.getMessage());
        }
    }

    /**
     * 使用AI分析网站并通过SSE流式返回分类和标签建议
     * 注意: EventSource只支持GET请求且不支持自定义Header
     * 需要通过URL参数传递accessKey: ?accessKey=YOUR_ACCESS_KEY
     *
     * @param url 要分析的网站URL
     * @param accessKey 访问密钥 (从URL参数获取)
     * @return SSE流式响应
     */
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/analyze-website", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeWebsite(
            @RequestParam("url") String url,
            @RequestParam("accessKey") String accessKey) {

        // 通过accessKey获取用户ID
        String currentUserId;
        try {
            currentUserId = authenticateUser(accessKey);
            if (currentUserId == null) {
                SseEmitter emitter = new SseEmitter(1000L);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "type", "error",
                                    "message", "访问密钥无效或已过期",
                                    "timestamp", System.currentTimeMillis()
                            )));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                return emitter;
            }
        } catch (Exception e) {
            log.warn("访问密钥验证失败: {}", e.getMessage());
            SseEmitter emitter = new SseEmitter(1000L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "type", "error",
                                "message", "认证失败: " + e.getMessage(),
                                "timestamp", System.currentTimeMillis()
                        )));
                emitter.complete();
            } catch (Exception sendError) {
                emitter.completeWithError(sendError);
            }
            return emitter;
        }

        // 创建SSE发射器，设置超时时间为60秒
        SseEmitter emitter = new SseEmitter(60000L);

        // URL验证
        String validationError = UrlValidator.validate(url);
        if (validationError != null) {
            log.warn("URL验证失败: {} - {}", url, validationError);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "type", "error",
                                "message", validationError,
                                "timestamp", System.currentTimeMillis()
                        )));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 异步执行分析任务
        new Thread(() -> {
            try {
                // 获取用户现有的分类列表
                List<SnSpace> spaces = spaceService.getUserSpaces(currentUserId);
                List<String> existingSpaces = spaces.stream()
                        .map(SnSpace::getName)
                        .toList();

                // 获取用户现有的标签列表
                List<SnTag> tags = tagService.getUserTags(currentUserId);
                List<String> existingTags = tags.stream()
                        .map(SnTag::getName)
                        .toList();

                // 调用流式AI分析服务
                websiteAnalysisService.analyzeWebsiteStreaming(
                        url.trim(),
                        existingSpaces,
                        existingTags,
                        emitter
                );

            } catch (Exception e) {
                log.error("分析网站失败: {}", url, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "type", "error",
                                    "message", "分析失败: " + e.getMessage(),
                                    "timestamp", System.currentTimeMillis()
                            )));
                } catch (Exception sendError) {
                    log.error("发送错误事件失败", sendError);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

}

package pres.peixinyi.sinan.module.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddBookmarkReq;
import pres.peixinyi.sinan.dto.request.EditBookmarkReq;
import pres.peixinyi.sinan.dto.response.BookmarkResp;
import pres.peixinyi.sinan.dto.response.DuplicateBookmarksResp;
import pres.peixinyi.sinan.dto.response.ImportBookmarkResp;
import pres.peixinyi.sinan.dto.response.SpaceResp;
import pres.peixinyi.sinan.dto.response.TagResp;
import pres.peixinyi.sinan.dto.request.CheckDuplicateReq;
import pres.peixinyi.sinan.dto.response.CheckDuplicateResp;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmarkAssTag;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;
import pres.peixinyi.sinan.module.sinan.entity.SnTag;
import pres.peixinyi.sinan.module.sinan.service.*;
import pres.peixinyi.sinan.config.UploadProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 书签控制层
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13 20:39
 * @Version : 0.0.0
 */
@Slf4j
@RestController
@RequestMapping("/bookmark")
public class BookmarkController {

    @Resource
    SnBookmarkService bookmarkService;

    @Resource
    SnTagService tagService;

    @Resource
    SnSpaceService spaceService;

    @Resource
    SnBookmarkAssTagService bookmarkAssTagService;

    @Resource
    SnShareSpaceAssUserService snShareSpaceAssUserService;

    @Resource
    UploadProperties uploadProperties;

    @Value("${sinan.server.base-url}")
    private String baseUrl;

    /**
     * 增加书签使用次数
     *
     * @param id 书签ID
     * @return 操作结果
     */
    @PostMapping("/{id}/increment-usage")
    public Result<String> incrementBookmarkUsage(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查书签是否存在且属于当前用户
        SnBookmark bookmark = bookmarkService.getBookmarkByUserAndId(id, currentUserId);
        if (bookmark == null) {
            return Result.fail("书签不存在或无权限访问");
        }

        // 增加使用次数
        boolean success = bookmarkService.incrementUsageCount(id, currentUserId);

        if (success) {
            return Result.success("书签使用次数增加成功");
        } else {
            return Result.fail("书签使用次数增加失败");
        }
    }

    /**
     * 获取书签
     *
     * @param id
     * @return pres.peixinyi.sinan.common.Result<pres.peixinyi.sinan.dto.response.BookmarkResp>
     * @author peixinyi
     * @since 19:55 2025/8/16
     */
    @GetMapping("/{id}")
    public Result<BookmarkResp> getBookmarkById(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查书签是否存在且属于当前用户
        SnBookmark bookmark = bookmarkService.getBookmarkByUserAndId(id, currentUserId);
        if (bookmark == null) {
            return Result.fail("书签不存在或无权限访问");
        }

        // 获取书签的标签信息
        List<SnTag> tags = bookmarkService.getBookmarkTags(id);

        // 构建响应对象
        BookmarkResp bookmarkResp = BookmarkResp.from(bookmark, tags);
        return Result.success(bookmarkResp);
    }

    /**
     * 获取最常使用的书签
     *
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.BookmarkVO>>
     * @author peixinyi
     * @since 20:45 2025/8/13
     */
    @GetMapping("/most-visited")
    public Result<List<BookmarkResp>> getMostVisitedBookmarks(@RequestParam(value = "limit", defaultValue = "10") int limit,
                                                              @RequestParam(value = "search", required = false) String search) {
        String userId = StpUtil.getLoginIdAsString();
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

        // 构建响应对象，包含标签信息和订阅状态
        List<BookmarkResp> bookmarkResponses = bookmarks.stream()
                .map(bookmark -> {
                    List<SnTag> tags =
                            bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());

                    // 检查订阅状态
                    Boolean subscribed = false;
                    String spaceId = bookmark.getSpaceId();
                    if (spaceId != null && !spaceId.isEmpty()) {
                        // 如果书签有空间，检查当前用户是否订阅了该空间
                        subscribed = snShareSpaceAssUserService.isCollection(spaceId, userId);
                    }

                    return BookmarkResp.from(bookmark, tags, subscribed);
                })
                .toList();

        return Result.success(bookmarkResponses);
    }

    /**
     * 获取没有NameSpace的书签
     *
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.BookmarkResp>>
     * @author peixinyi
     * @since 21:03 2025/8/17
     */
    @GetMapping("/no-namespace")
    public Result<List<BookmarkResp>> getNoSpaceBookmarks() {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 获取没有命名空间的书签
        List<SnBookmark> bookmarks = bookmarkService.getNoNamespaceBookmarks(currentUserId);

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
     * 获取没有NameSpace的书签分页
     *
     * @return pres.peixinyi.sinan.common.Result<Page<BookmarkResp>>
     * @author peixinyi
     * @since 16:26 2025/8/18
     */
    @GetMapping("/no-namespace/page")
    public Result<IPage<BookmarkResp>> getNoSpaceBookmarksPage(@RequestParam("page") String page,
                                                               @RequestParam("size") String size,
                                                               @RequestParam(value = "search", required = false) String search) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 获取没有命名空间的书签分页
        IPage<SnBookmark> bookmarkPage = bookmarkService.getNoNamespaceBookmarksPage(currentUserId, page, size, search);

        if (bookmarkPage.getRecords().isEmpty()) {
            return Result.success(bookmarkPage.convert(BookmarkResp::from));
        }

        // 批量获取书签的标签信息
        List<String> bookmarkIds = bookmarkPage.getRecords().stream()
                .map(SnBookmark::getId)
                .toList();
        Map<String, List<SnTag>> bookmarkTagsMap =
                bookmarkService.getBatchBookmarkTags(bookmarkIds);

        // 构建响应对象，包含标签信息
        IPage<BookmarkResp> responsePage = bookmarkPage.convert(bookmark -> {
            List<SnTag> tags =
                    bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
            return BookmarkResp.from(bookmark, tags);
        });

        return Result.success(responsePage);

    }

    /**
     * 新增书签
     *
     * @param req
     * @return pres.peixinyi.sinan.common.Result<pres.peixinyi.sinan.model.sinan.entity.SnBookmark>
     * @author peixinyi
     * @since 20:54 2025/8/13
     */
    @PostMapping
    public Result<SnBookmark> addBookmark(@RequestBody AddBookmarkReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        //检查namespace是否存在和属于当前用户
        if (req.getNamespaceId() != null) {
            if (!spaceService.isNamespaceBelongsToUser(req.getNamespaceId(), currentUserId)) {
                return Result.fail("命名空间不存在或无权限访问");
            }
        }

        //检查Tags是否存在和属于当前用户
        if (!req.getNamespaceId().isEmpty()) {
            if (!tagService.areAllTagsBelongToUser(req.getTagsIds(), currentUserId)) {
                return Result.fail("部分标签不存在或无权限访问");
            }
        }


        //初始化参数
        SnBookmark bookmark = new SnBookmark();
        bookmark.setUserId(currentUserId);
        bookmark.setSpaceId(req.getNamespaceId());
        bookmark.setName(req.getName());
        bookmark.setDescription(req.getDescription());
        bookmark.setUrl(req.getUrl());
        bookmark.setNum(0);

        SnBookmark savedBookmark = bookmarkService.addBookmark(bookmark);

        //处理标签关联
        if (req.getTagsIds() != null && !req.getTagsIds().isEmpty()) {
            for (String tagId : req.getTagsIds()) {
                SnBookmarkAssTag assTag = new SnBookmarkAssTag();
                assTag.setUserId(currentUserId);
                assTag.setBookmarkId(savedBookmark.getId());
                assTag.setTagId(tagId);
                assTag.setCreateTime(new Date());
                assTag.setUpdateTime(new Date());
                assTag.setDeleted(0);
                bookmarkAssTagService.save(assTag);
            }
        }

        return Result.success(savedBookmark);
    }

    /**
     * 删除书签
     *
     * @param id 书签ID
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 2025/8/13
     */
    @DeleteMapping
    public Result<String> deleteBookmark(@RequestParam(value = "id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (id == null || id.isEmpty()) {
            return Result.fail("书签ID不能为空");
        }

        // 检查书签是否存在且属于当前用户
        if (!bookmarkService.isBookmarkBelongsToUser(id, currentUserId)) {
            return Result.fail("书签不存在或无权限删除");
        }

        // 删除书签的标签关联
        bookmarkAssTagService.deleteBookmarkTagAssociations(id, currentUserId);

        // 删除书签
        boolean deleted = bookmarkService.deleteBookmark(id, currentUserId);

        if (deleted) {
            return Result.success("书签删除成功");
        } else {
            return Result.fail("书签删除失败");
        }
    }

    /**
     * 编辑书签
     *
     * @param req 编辑书签请求
     * @return pres.peixinyi.sinan.common.Result<pres.peixinyi.sinan.model.sinan.entity.SnBookmark>
     * @author peixinyi
     * @since 2025/8/13
     */
    @PutMapping
    public Result<SnBookmark> editBookmark(@RequestBody EditBookmarkReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (req.getId() == null || req.getId().isEmpty()) {
            return Result.fail("书签ID不能为空");
        }

        // 检查书签是否存在且属于当前用户
        SnBookmark existingBookmark = bookmarkService.getBookmarkByUserAndId(req.getId(), currentUserId);
        if (existingBookmark == null) {
            return Result.fail("书签不存在或无权限编辑");
        }

        // 如果更新了命名空间，检查新命名空间是否存在和属于当前用户
        if (req.getNamespaceId() != null && !req.getNamespaceId().isEmpty()) {
            if (!spaceService.isNamespaceBelongsToUser(req.getNamespaceId(), currentUserId)) {
                return Result.fail("命名空间不存在或无权限访问");
            }
        }

        // 检查标签是否存在和属于当前用户
        if (!tagService.areAllTagsBelongToUser(req.getTags(), currentUserId)) {
            return Result.fail("部分标签不存在或无权限访问");
        }

        // 更新书签基本信息
        boolean updated = bookmarkService.updateBookmarkByUser(
                req.getId(),
                currentUserId,
                req.getName(),
                req.getUrl(),
                req.getIcon(),
                req.getDescription(),
                req.getNamespaceId()
        );

        if (!updated) {
            return Result.fail("书签更新失败");
        }

        // 更新标签关联
        bookmarkAssTagService.updateBookmarkTagAssociations(req.getId(), currentUserId, req.getTags());

        // 获取更新后的书签信息
        SnBookmark updatedBookmark = bookmarkService.getBookmarkByUserAndId(req.getId(), currentUserId);

        return Result.success(updatedBookmark);
    }

    /**
     * 给书签加星标
     *
     * @param id 书签ID
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 2025/8/13
     */
    @PostMapping("/star")
    public Result<String> starBookmark(@RequestParam(value = "id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (id == null || id.isEmpty()) {
            return Result.fail("书签ID不能为空");
        }

        // 检查书签是否存在且属于当前用户
        if (!bookmarkService.isBookmarkBelongsToUser(id, currentUserId)) {
            return Result.fail("书签不存在或无权限操作");
        }

        // 加星标
        boolean starred = bookmarkService.starBookmark(id, currentUserId);

        if (starred) {
            return Result.success("书签已加星标");
        } else {
            return Result.fail("加星标失败");
        }
    }

    /**
     * 取消书签星标
     *
     * @param id 书签ID
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 2025/8/13
     */
    @DeleteMapping("/star")
    public Result<String> unstarBookmark(@RequestParam(value = "id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (id == null || id.isEmpty()) {
            return Result.fail("书签ID不能为空");
        }

        // 检查书签是否存在且属于当前用户
        if (!bookmarkService.isBookmarkBelongsToUser(id, currentUserId)) {
            return Result.fail("书签不存在或无权限操作");
        }

        // 取消星标
        boolean unstarred = bookmarkService.unstarBookmark(id, currentUserId);

        if (unstarred) {
            return Result.success("书签已取消星标");
        } else {
            return Result.fail("取消星标失败");
        }
    }

    /**
     * 切换书签星标状态
     *
     * @param id 书签ID
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 2025/8/13
     */
    @PutMapping("/star")
    public Result<String> toggleBookmarkStar(@RequestParam(value = "id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (id == null || id.isEmpty()) {
            return Result.fail("书签ID不能为空");
        }

        // 检查书签是否存在且属于当前用户
        SnBookmark bookmark = bookmarkService.getBookmarkByUserAndId(id, currentUserId);
        if (bookmark == null) {
            return Result.fail("书签不存在或无权限操作");
        }

        // 切换星标状态
        boolean toggled = bookmarkService.toggleBookmarkStar(id, currentUserId);

        if (toggled) {
            // 重新获取书签信息以获得最新的星标状态
            SnBookmark updatedBookmark = bookmarkService.getBookmarkByUserAndId(id, currentUserId);
            boolean newStarStatus = Boolean.TRUE.equals(updatedBookmark.getStar());
            String message = newStarStatus ? "书签已加星标" : "书签已取消星标";
            return Result.success(message);
        } else {
            return Result.fail("切换星标状态失败");
        }
    }

    /**
     * 获取星标书签
     *
     * @param limit 限制数量，默认10
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.BookmarkResp>>
     * @author peixinyi
     * @since 2025/8/13
     */
    @GetMapping("/starred")
    public Result<List<BookmarkResp>> getStarredBookmarks(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        String currentUserId = StpUtil.getLoginIdAsString();

        List<SnBookmark> starredBookmarks = bookmarkService.getStarredBookmarks(currentUserId, limit);

        if (starredBookmarks.isEmpty()) {
            return Result.success(List.of());
        }

        // 批量获取书签的标签信息
        List<String> bookmarkIds = starredBookmarks.stream()
                .map(SnBookmark::getId)
                .toList();
        Map<String, List<SnTag>> bookmarkTagsMap =
                bookmarkService.getBatchBookmarkTags(bookmarkIds);

        // 构建响应对象，包含标签信息和订阅状态
        List<BookmarkResp> bookmarkResponses = starredBookmarks.stream()
                .map(bookmark -> {
                    List<SnTag> tags =
                            bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());

                    // 检查订阅状态
                    Boolean subscribed = false;
                    String spaceId = bookmark.getSpaceId();
                    if (spaceId != null && !spaceId.isEmpty()) {
                        // 如果书签有空间，检查当前用户是否订阅了该空间
                        subscribed = snShareSpaceAssUserService.isCollection(spaceId, currentUserId);
                    }

                    return BookmarkResp.from(bookmark, tags, subscribed);
                })
                .toList();

        return Result.success(bookmarkResponses);
    }

    /**
     * 获取重复书签
     *
     * @param level 1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配
     * @return 重复书签信息
     */
    @GetMapping("/duplicates")
    public Result<DuplicateBookmarksResp> getDuplicateBookmarks(@RequestParam(value = "level",defaultValue = "1") int level) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 验证level参数范围
        if (level < 1 || level > 3) {
            return Result.fail("level参数必须在1-3之间");
        }

        // 获取重复分组信息（基于level）
        Map<String, Long> duplicateGroupsByLevel = bookmarkService.getDuplicateGroupsByLevel(currentUserId, level);

        // 获取总书签数
        long totalBookmarks = bookmarkService.lambdaQuery()
                .eq(SnBookmark::getUserId, currentUserId)
                .eq(SnBookmark::getDeleted, 0)
                .count();

        // 获取所有重复书签
        List<SnBookmark> duplicateBookmarks = bookmarkService.getDuplicateBookmarks(currentUserId, level);

        // 获取所有相关的空间ID
        List<String> spaceIds = duplicateBookmarks.stream()
                .map(SnBookmark::getSpaceId)
                .filter(spaceId -> spaceId != null && !spaceId.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 获取空间信息映射
        Map<String, SnSpace> spaceMap = new HashMap<>();
        if (!spaceIds.isEmpty()) {
            List<SnSpace> spaces = spaceService.getSpacesByIds(spaceIds);
            spaceMap = spaces.stream()
                    .collect(Collectors.toMap(SnSpace::getId, space -> space));
        }

        // 构建重复组信息
        List<DuplicateBookmarksResp.DuplicateGroupResp> duplicateGroups = new ArrayList<>();

        for (Map.Entry<String, Long> entry : duplicateGroupsByLevel.entrySet()) {
            String groupKey = entry.getKey();

            // 获取该分组键的所有书签
            List<SnBookmark> groupBookmarks = bookmarkService.getBookmarksByGroupKey(groupKey, currentUserId, level);

            // 获取书签ID列表
            List<String> bookmarkIds = groupBookmarks.stream()
                    .map(SnBookmark::getId)
                    .toList();

            // 批量获取标签信息
            Map<String, List<SnTag>> bookmarkTagsMap = bookmarkService.getBatchBookmarkTags(bookmarkIds);

            // 构建书签列表
            List<DuplicateBookmarksResp.DuplicateBookmarkResp> bookmarks = new ArrayList<>();
            for (SnBookmark bookmark : groupBookmarks) {
                List<SnTag> tags = bookmarkTagsMap.getOrDefault(bookmark.getId(), List.of());
                DuplicateBookmarksResp.DuplicateBookmarkResp bookmarkInfo = new DuplicateBookmarksResp.DuplicateBookmarkResp();
                bookmarkInfo.setId(bookmark.getId());
                bookmarkInfo.setName(bookmark.getName());
                bookmarkInfo.setUrl(bookmark.getUrl());
                bookmarkInfo.setIcon(bookmark.getIcon());

                // 设置空间信息
                String spaceId = bookmark.getSpaceId();
                if (spaceId != null && !spaceId.isEmpty()) {
                    SnSpace space = spaceMap.get(spaceId);
                    bookmarkInfo.setSpace(space != null ? SpaceResp.from(space) : null);
                } else {
                    bookmarkInfo.setSpace(null);
                }

                // 设置标签信息
                bookmarkInfo.setTags(tags.stream()
                        .map(TagResp::from)
                        .collect(Collectors.toList()));

                bookmarkInfo.setCreateTime(bookmark.getCreateTime().toString());
                bookmarks.add(bookmarkInfo);
            }

            // 构建重复组信息
            DuplicateBookmarksResp.DuplicateGroupResp duplicateGroup = new DuplicateBookmarksResp.DuplicateGroupResp();
            duplicateGroup.setGroup(groupKey);
            duplicateGroup.setBookmarks(bookmarks);

            duplicateGroups.add(duplicateGroup);
        }

        // 构建统计信息
        DuplicateBookmarksResp.StatsResp stats = new DuplicateBookmarksResp.StatsResp();
        stats.setTotalBookmarks(totalBookmarks);
        stats.setDuplicateGroups(duplicateGroupsByLevel.size());
        stats.setDuplicateCount(duplicateGroupsByLevel.values().stream().mapToLong(Long::longValue).sum());

        // 构建响应数据
        DuplicateBookmarksResp response = new DuplicateBookmarksResp();
        response.setDuplicates(duplicateGroups);
        response.setStats(stats);

        return Result.success(response);
    }

    /**
     * 检查URL是否重复
     *
     * @param req 检查重复请求
     * @return 检查重复响应
     */
    @PostMapping("/check-duplicate")
    public Result<CheckDuplicateResp> checkDuplicate(@RequestBody CheckDuplicateReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (req.getUrl() == null || req.getUrl().trim().isEmpty()) {
            return Result.fail("URL不能为空");
        }

        if (req.getLevel() < 1 || req.getLevel() > 3) {
            return Result.fail("匹配级别必须在1-3之间");
        }

        // 调用服务层检查重复
        CheckDuplicateResp result = bookmarkService.checkDuplicate(req.getUrl(), req.getLevel(), currentUserId);
        return Result.success(result);
    }

    /**
     * 根据空间ID获取书签列表
     *
     * @param spaceId 空间ID
     * @param search  搜索关键字（可选）
     * @return 书签列表
     */
    @GetMapping("/space/{spaceId}")
    public Result<List<BookmarkResp>> getBookmarksBySpaceId(
            @PathVariable("spaceId") String spaceId,
            @RequestParam(value = "search", required = false) String search) {
        String currentUserId = StpUtil.getLoginIdAsString();

        Boolean spaceBelongsToUserFlag = spaceService.isNamespaceBelongsToUser(spaceId, currentUserId);
        Boolean isCollection = snShareSpaceAssUserService.isCollection(spaceId, currentUserId);

        // 检查空间是否存在且属于当前用户
        if (!spaceBelongsToUserFlag && !isCollection) {
            return Result.fail("空间不存在或无权限访问");
        }

        List<SnBookmark> bookmarks = new ArrayList<>();
        if (spaceBelongsToUserFlag) {
            if (search != null && !search.trim().isEmpty()) {
                bookmarks = bookmarkService.searchBookmarksBySpaceId(spaceId, currentUserId, search.trim());
            } else {
                bookmarks = bookmarkService.getBookmarksBySpaceId(spaceId, currentUserId);
            }
        } else if (isCollection) {
            if (search != null && !search.trim().isEmpty()) {
                bookmarks = bookmarkService.searchBookmarksBySpaceId(spaceId, search.trim());
            } else {
                bookmarks = bookmarkService.getBookmarksBySpaceId(spaceId);
            }
        }

        // 获取书签列表
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
     * 获取空间的书签统计信息
     *
     * @param spaceId 空间ID
     * @return 书签统计
     */
    @GetMapping("/space/{spaceId}/stats")
    public Result<SpaceBookmarkStatsResp> getSpaceBookmarkStats(@PathVariable("spaceId") String spaceId) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查空间是否存在且属于当前用户
        if (!spaceService.isNamespaceBelongsToUser(spaceId, currentUserId)) {
            return Result.fail("空间不存在或无权限访问");
        }

        // 获取统计信息
        long totalCount = bookmarkService.getBookmarkCountBySpaceId(spaceId, currentUserId);

        SpaceBookmarkStatsResp stats = new SpaceBookmarkStatsResp();
        stats.setSpaceId(spaceId);
        stats.setTotalCount(totalCount);

        return Result.success(stats);
    }

    /**
     * 根据标签ID获取关联的书签列表
     *
     * @param tagId  标签ID
     * @param search 搜索关键字（可选）
     * @return 书签列表
     */
    @GetMapping("/tag/{tagId}")
    public Result<List<BookmarkResp>> getBookmarksByTagId(
            @PathVariable("tagId") String tagId,
            @RequestParam(value = "search", required = false) String search) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查标签是否存在且属于当前用户
        if (!tagService.isTagBelongsToUser(tagId, currentUserId)) {
            return Result.fail("标签不存在或无权限访问");
        }

        // 获取书签列表
        List<SnBookmark> bookmarks;
        if (search != null && !search.trim().isEmpty()) {
            bookmarks = bookmarkService.searchBookmarksByTagId(tagId, currentUserId, search.trim());
        } else {
            bookmarks = bookmarkService.getBookmarksByTagId(tagId, currentUserId);
        }

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
     * 获取标签的书签统计信息
     *
     * @param tagId 标签ID
     * @return 书签统计
     */
    @GetMapping("/tag/{tagId}/stats")
    public Result<TagBookmarkStatsResp> getTagBookmarkStats(@PathVariable("tagId") String tagId) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查标签是否存在且属于当前用户
        if (!tagService.isTagBelongsToUser(tagId, currentUserId)) {
            return Result.fail("标签不存在或无权限访问");
        }

        // 获取统计信息
        long totalCount = bookmarkService.getBookmarkCountByTagId(tagId, currentUserId);

        TagBookmarkStatsResp stats = new TagBookmarkStatsResp();
        stats.setTagId(tagId);
        stats.setTotalCount(totalCount);

        return Result.success(stats);
    }

    /**
     * 上传书签图标
     *
     * @param file 上传的图片文件
     * @return 上传结果，包含图片的访问路径
     * @author peixinyi
     * @since 2025/9/4
     */
    @PostMapping("/upload/icon")
    public Result<String> uploadIcon(@RequestParam("file") MultipartFile file) {
        // 参数校验
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的图片");
        }

        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名不能为空");
        }

        String lowerCaseFilename = originalFilename.toLowerCase();
        if (!lowerCaseFilename.endsWith(".jpg") && !lowerCaseFilename.endsWith(".jpeg")
                && !lowerCaseFilename.endsWith(".png") && !lowerCaseFilename.endsWith(".gif")
                && !lowerCaseFilename.endsWith(".bmp") && !lowerCaseFilename.endsWith(".webp")) {
            return Result.fail("只支持 JPG、PNG、GIF、BMP、WEBP 格式的图片");
        }

        // 检查文件大小（限制为5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail("图片大小不能超过5MB");
        }

        try {
            // 读取图片
            BufferedImage srcImg = ImageIO.read(file.getInputStream());
            if (srcImg == null) {
                return Result.fail("图片格式不正确或已损坏");
            }

            // 获取图片的宽高
            int width = srcImg.getWidth();
            int height = srcImg.getHeight();

            // 计算裁剪区域（取中心正方形区域）
            int size = Math.min(width, height);
            int x = (width - size) / 2;
            int y = (height - size) / 2;

            // 裁剪为正方形
            BufferedImage squareImg = srcImg.getSubimage(x, y, size, size);

            // 压缩为256x256并转换为PNG格式
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(squareImg)
                    .size(256, 256)
                    .outputFormat("png")
                    .outputQuality(0.8)
                    .toOutputStream(out);

            // 生成文件名
            String fileName = "icon_" + System.currentTimeMillis() + ".png";

            // 使用配置的上传路径
            Path uploadDir = Paths.get(uploadProperties.getIconUploadPath());
            Files.createDirectories(uploadDir);

            Path savePath = uploadDir.resolve(fileName);
            Files.write(savePath, out.toByteArray());

            // 返回完整的图片访问URL
            String iconUrl = uploadProperties.getIconFullUrl(baseUrl, fileName);
            return Result.success(iconUrl);

        } catch (Exception e) {
            return Result.fail("图片处理失败: " + e.getMessage());
        }
    }

    /**
     * 读取书签图标
     *
     * @param fileName 图标文件名
     * @return 图标文件的字节流
     * @author peixinyi
     * @since 2025/9/4
     */
    @GetMapping("/icons/{fileName}")
    public ResponseEntity<byte[]> getIcon(@PathVariable("fileName") String fileName) {
        try {
            // 参数校验
            if (fileName == null || fileName.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 安全检查：防止路径遍历攻击
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            // 检查文件扩展名
            if (!fileName.toLowerCase().endsWith(".png") && !fileName.toLowerCase().endsWith(".jpg")
                    && !fileName.toLowerCase().endsWith(".jpeg") && !fileName.toLowerCase().endsWith(".gif")
                    && !fileName.toLowerCase().endsWith(".bmp") && !fileName.toLowerCase().endsWith(".webp")) {
                return ResponseEntity.badRequest().build();
            }

            // 构建文件路径
            Path filePath = Paths.get(uploadProperties.getIconUploadPath()).resolve(fileName);

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            log.info("读取图标文件: {}", filePath.toString());
            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(filePath);

            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileContent.length)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // 缓存一年
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件名获取Content-Type
     *
     * @param fileName 文件名
     * @return Content-Type
     */
    private String getContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerCaseFileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerCaseFileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 导入Chrome收藏夹HTML文件
     *
     * @param file 上传的HTML文件
     * @return 导入结果
     */
    @PostMapping("/import/chrome")
    public Result<ImportBookmarkResp> importChromeBookmarks(@RequestParam("file") MultipartFile file) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }

        // 检查文件类型
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".html")) {
            return Result.fail("请上传HTML格式的收藏夹文件");
        }

        // 检查文件大小（限制为10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail("文件大小不能超过10MB");
        }

        try {
            // 调用服务层方法导入书签
            ImportBookmarkResp result = bookmarkService.importChromeBookmarks(file, currentUserId);

            if (result.getSuccessCount() > 0) {
                return Result.success(result);
            } else {
                return Result.fail(result.getMessage());
            }
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 空间书签统计响应内部类
     */
    public static class SpaceBookmarkStatsResp {
        private String spaceId;
        private long totalCount;

        public String getSpaceId() {
            return spaceId;
        }

        public void setSpaceId(String spaceId) {
            this.spaceId = spaceId;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }

    /**
     * 标签书签统计响应内部类
     */
    public static class TagBookmarkStatsResp {
        private String tagId;
        private long totalCount;

        public String getTagId() {
            return tagId;
        }

        public void setTagId(String tagId) {
            this.tagId = tagId;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }

}

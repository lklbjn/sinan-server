package pres.peixinyi.sinan.module.sinan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.ObjectUtils;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmarkAssTag;
import pres.peixinyi.sinan.module.sinan.entity.SnShareSpaceAssUser;
import pres.peixinyi.sinan.module.sinan.mapper.SnBookmarkMapper;
import pres.peixinyi.sinan.module.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.module.sinan.entity.SnTag;
import pres.peixinyi.sinan.dto.response.ImportBookmarkResp;
import pres.peixinyi.sinan.dto.response.CheckDuplicateResp;
import jakarta.annotation.Resource;
import pres.peixinyi.sinan.utils.PinyinUtils;

@Service
@Slf4j
public class SnBookmarkService extends ServiceImpl<SnBookmarkMapper, SnBookmark> {

    @Resource
    private SnBookmarkAssTagService bookmarkAssTagService;

    @Resource
    private SnTagService tagService;

    @Resource
    private SnShareSpaceAssUserService snShareSpaceAssUserService;

    @Resource
    private SnIgnoredGroupService ignoredGroupService;

    public List<SnBookmark> getMostVisitedBookmarks(int limit, String search, String userId) {
        // 获取用户订阅空间ID列表
        List<String> subscribedSpaceIds = snShareSpaceAssUserService.getByUserId(userId)
            .stream()
            .map(SnShareSpaceAssUser::getSpaceId)
            .collect(Collectors.toList());

        // 构建查询条件：用户自己的书签 或 订阅空间的书签
        return lambdaQuery()
                .eq(SnBookmark::getDeleted, 0)
                .and(wrapper -> {
                    wrapper.eq(SnBookmark::getUserId, userId);
                    if (subscribedSpaceIds != null && !subscribedSpaceIds.isEmpty()) {
                        wrapper.or().in(SnBookmark::getSpaceId, subscribedSpaceIds);
                    }
                })
                .and(search != null && !search.isEmpty(), wrapper -> {
                    wrapper.like(SnBookmark::getName, search)
                        .or().like(SnBookmark::getUrl, search)
                        .or().like(SnBookmark::getDescription, search)
                        .or().like(SnBookmark::getPinyin, search)
                        .or().like(SnBookmark::getAbbreviation, search);
                })
                .orderByDesc(SnBookmark::getStar)
                .orderByDesc(SnBookmark::getNum)
                .orderByDesc(SnBookmark::getCreateTime)
                .last("limit " + limit)
                .list();
    }

    public SnBookmark addBookmark(SnBookmark bookmark) {
        bookmark.setPinyin(PinyinUtils.toPinyin(bookmark.getName()));
        bookmark.setAbbreviation(PinyinUtils.toPinyinFirstLetter(bookmark.getName()));
        bookmark.setCreateTime(new Date());
        bookmark.setUpdateTime(new Date());
        bookmark.setDeleted(0);
        save(bookmark);
        return bookmark;
    }

    /**
     * 检查书签是否存在且属于指定用户
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return 书签对象，如果不存��或不属于用户则返回null
     */
    public SnBookmark getBookmarkByUserAndId(String bookmarkId, String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .one();
    }

    /**
     * 检查书签是否属于指定用户
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 如果书签存在且属于用户，否则返回false
     */
    public boolean isBookmarkBelongsToUser(String bookmarkId, String userId) {
        return getBookmarkByUserAndId(bookmarkId, userId) != null;
    }

    /**
     * 删除书签（逻辑删除）
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 删除成功，false 删除失��
     */
    public boolean deleteBookmark(String bookmarkId, String userId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(SnBookmark::getDeleted, 1)
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 更新书签
     *
     * @param bookmark 书签对象
     * @return true 更新成功，false 更新失败
     */
    public boolean updateBookmark(SnBookmark bookmark) {
        bookmark.setPinyin(PinyinUtils.toPinyin(bookmark.getName()));
        bookmark.setAbbreviation(PinyinUtils.toPinyinFirstLetter(bookmark.getName()));
        bookmark.setUpdateTime(new Date());
        return updateById(bookmark);
    }

    /**
     * 增加书签使用次数
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 增加成功，false 增加失败
     */
    public boolean incrementUsageCount(String bookmarkId, String userId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .setSql("num = IFNULL(num, 0) + 1")
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 更新指������户的书签
     *
     * @param bookmarkId  书签ID
     * @param userId      用户ID
     * @param name        书签名称
     * @param url         书签URL
     * @param description 书签描述
     * @param namespaceId 命名空间ID
     * @return true 更新成功，false 更新失败
     */
    public boolean updateBookmarkByUser(String bookmarkId, String userId, String name, String url, String icon, String description, String namespaceId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(name != null, SnBookmark::getName, name)
                .set(name != null, SnBookmark::getPinyin, PinyinUtils.toPinyin(name))
                .set(name != null, SnBookmark::getAbbreviation, PinyinUtils.toPinyinFirstLetter(name))
                .set(url != null, SnBookmark::getUrl, url)
                .set(icon != null, SnBookmark::getIcon, icon)
                .set(description != null, SnBookmark::getDescription, description)
                .set(namespaceId != null, SnBookmark::getSpaceId, namespaceId)
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 给书签加星标
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 操作成功，false 操作失败
     */
    public boolean starBookmark(String bookmarkId, String userId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(SnBookmark::getStar, true)
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 取消书签星标
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 操作成功，false 操作失败
     */
    public boolean unstarBookmark(String bookmarkId, String userId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(SnBookmark::getStar, false)
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 切换书签星标状态
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 操作成功，false 操作失败
     */
    public boolean toggleBookmarkStar(String bookmarkId, String userId) {
        SnBookmark bookmark = getBookmarkByUserAndId(bookmarkId, userId);
        if (bookmark == null) {
            return false;
        }

        boolean newStarStatus = !Boolean.TRUE.equals(bookmark.getStar());
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(SnBookmark::getStar, newStarStatus)
                .set(SnBookmark::getUpdateTime, new Date())
                .update();
    }

    /**
     * 获取用户的星标书签
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 星标书签列表
     */
    public List<SnBookmark> getStarredBookmarks(String userId, int limit) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getStar, true)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .last("limit " + limit)
                .list();
    }

    /**
     * 获取书签的标签列表
     *
     * @param bookmarkId 书签ID
     * @return 标签列表
     */
    public List<SnTag> getBookmarkTags(String bookmarkId) {
        // 获取书签关联的标签ID列表
        List<String> tagIds = bookmarkAssTagService.lambdaQuery()
                .eq(SnBookmarkAssTag::getBookmarkId, bookmarkId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list()
                .stream()
                .map(SnBookmarkAssTag::getTagId)
                .collect(Collectors.toList());

        if (tagIds.isEmpty()) {
            return List.of();
        }

        // 根据标签ID获取标签信息
        return tagService.lambdaQuery()
                .in(SnTag::getId, tagIds)
                .eq(SnTag::getDeleted, 0)
                .list();
    }

    /**
     * 批量获取多个书签的标签信息
     *
     * @param bookmarkIds 书签ID列表
     * @return Map<书签ID, 标签列表>
     */
    public Map<String, List<SnTag>> getBatchBookmarkTags(List<String> bookmarkIds) {
        if (bookmarkIds.isEmpty()) {
            return Map.of();
        }

        // 获取所有书签关联的标签关系
        List<SnBookmarkAssTag> associations = bookmarkAssTagService.lambdaQuery()
                .in(SnBookmarkAssTag::getBookmarkId, bookmarkIds)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list();

        // 获取所有相关的标签ID
        List<String> tagIds = associations.stream()
                .map(SnBookmarkAssTag::getTagId)
                .distinct()
                .collect(Collectors.toList());

        if (tagIds.isEmpty()) {
            return bookmarkIds.stream().collect(Collectors.toMap(id -> id, id -> List.of()));
        }

        // 获取所有标签信息
        Map<String, SnTag> tagMap = tagService.lambdaQuery()
                .in(SnTag::getId, tagIds)
                .eq(SnTag::getDeleted, 0)
                .list()
                .stream()
                .collect(Collectors.toMap(SnTag::getId, tag -> tag));

        // 构建���签ID到标签列表的映射
        Map<String, List<SnTag>> result = bookmarkIds.stream()
                .collect(Collectors.toMap(id -> id, id -> new ArrayList<>()));

        // 填充标签数据
        for (SnBookmarkAssTag association : associations) {
            String bookmarkId = association.getBookmarkId();
            String tagId = association.getTagId();
            SnTag tag = tagMap.get(tagId);
            if (tag != null) {
                if (result.get(bookmarkId) == null) {
                    result.put(bookmarkId, new ArrayList<>());
                    result.get(bookmarkId).add(tag);
                } else {
                    List<SnTag> snTags = result.get(bookmarkId);
                    if (!snTags.contains(tag)) {
                        snTags.add(tag);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 根据空间ID获取书签列表
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @return 书签列表
     */
    public List<SnBookmark> getBookmarksBySpaceId(String spaceId, String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getSpaceId, spaceId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据空间ID获取书签列表
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @return 书签列表
     */
    public List<SnBookmark> getBookmarksBySpaceId(String spaceId) {
        return lambdaQuery()
                .eq(SnBookmark::getSpaceId, spaceId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据空间ID搜索书签
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @param search  搜索关键字
     * @return 书签列表
     */
    public List<SnBookmark> searchBookmarksBySpaceId(String spaceId, String userId, String search) {
        return lambdaQuery()
                .eq(SnBookmark::getSpaceId, spaceId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnBookmark::getName, search)
                        .or()
                        .like(SnBookmark::getDescription, search)
                        .or()
                        .like(SnBookmark::getUrl, search)
                )
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据空间ID搜索书签
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @param search  搜索关键字
     * @return 书签列表
     */
    public List<SnBookmark> searchBookmarksBySpaceId(String spaceId, String search) {
        return lambdaQuery()
                .eq(SnBookmark::getSpaceId, spaceId)
                .eq(SnBookmark::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnBookmark::getName, search)
                        .or()
                        .like(SnBookmark::getDescription, search)
                        .or()
                        .like(SnBookmark::getUrl, search)
                )
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 获取空间下的书签数量
     *
     * @param spaceId 空��ID
     * @param userId  用户ID
     * @return 书签数量
     */
    public long getBookmarkCountBySpaceId(String spaceId, String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getSpaceId, spaceId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .count();
    }

    /**
     * 根据标签ID获取关联的书签列表
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return 书签列表
     */
    public List<SnBookmark> getBookmarksByTagId(String tagId, String userId) {
        // 获取标签关联的书签ID列表
        List<String> bookmarkIds = bookmarkAssTagService.lambdaQuery()
                .eq(SnBookmarkAssTag::getTagId, tagId)
                .eq(SnBookmarkAssTag::getUserId, userId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list()
                .stream()
                .map(SnBookmarkAssTag::getBookmarkId)
                .collect(Collectors.toList());

        if (bookmarkIds.isEmpty()) {
            return List.of();
        }

        // 根据书签ID获取书签信息
        return lambdaQuery()
                .in(SnBookmark::getId, bookmarkIds)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据标签ID搜索关联的书签
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @param search 搜索关键字
     * @return 书签列表
     */
    public List<SnBookmark> searchBookmarksByTagId(String tagId, String userId, String search) {
        // 获取标签关联的书签ID列表
        List<String> bookmarkIds = bookmarkAssTagService.lambdaQuery()
                .eq(SnBookmarkAssTag::getTagId, tagId)
                .eq(SnBookmarkAssTag::getUserId, userId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list()
                .stream()
                .map(SnBookmarkAssTag::getBookmarkId)
                .collect(Collectors.toList());

        if (bookmarkIds.isEmpty()) {
            return List.of();
        }

        // 根据书签ID和搜索���件获取书签信息
        return lambdaQuery()
                .in(SnBookmark::getId, bookmarkIds)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnBookmark::getName, search)
                        .or()
                        .like(SnBookmark::getDescription, search)
                        .or()
                        .like(SnBookmark::getUrl, search)
                )
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 获取标签下的书签数量
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return 书签数量
     */
    public long getBookmarkCountByTagId(String tagId, String userId) {
        // 获取标签关联的书签ID列表
        List<String> bookmarkIds = bookmarkAssTagService.lambdaQuery()
                .eq(SnBookmarkAssTag::getTagId, tagId)
                .eq(SnBookmarkAssTag::getUserId, userId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list()
                .stream()
                .map(SnBookmarkAssTag::getBookmarkId)
                .collect(Collectors.toList());

        if (bookmarkIds.isEmpty()) {
            return 0;
        }

        // 统计有效书签数量
        return lambdaQuery()
                .in(SnBookmark::getId, bookmarkIds)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .count();
    }

    public List<SnBookmark> getNoNamespaceBookmarks(String currentUserId) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, currentUserId)
                .isNull(SnBookmark::getSpaceId)
                .or()
                .eq(SnBookmark::getUserId, currentUserId)
                .eq(SnBookmark::getSpaceId, "")
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    public IPage<SnBookmark> getNoNamespaceBookmarksPage(String currentUserId, String page, String size, String search) {
        int pageNum = Integer.parseInt(page);
        int pageSize = Integer.parseInt(size);

        return lambdaQuery()
                .eq(SnBookmark::getUserId, currentUserId)
                .isNull(SnBookmark::getSpaceId)
                .like(!ObjectUtils.isEmpty(search), SnBookmark::getName, search)
                .like(!ObjectUtils.isEmpty(search), SnBookmark::getUrl, search)
                .or()
                .eq(SnBookmark::getUserId, currentUserId)
                .eq(SnBookmark::getSpaceId, "")
                .like(!ObjectUtils.isEmpty(search), SnBookmark::getName, search)
                .like(!ObjectUtils.isEmpty(search), SnBookmark::getUrl, search)
                .orderByDesc(SnBookmark::getUpdateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    /**
     * 导入Chrome收藏夹HTML文件
     *
     * @param file   上传的HTML文件
     * @param userId 用户ID
     * @return 导入结果
     */
    public ImportBookmarkResp importChromeBookmarks(MultipartFile file, String userId) {
        try {
            // 读取HTML文件内容
            String htmlContent = readFileContent(file);

            // 解析书签
            List<SnBookmark> bookmarks = parseBookmarksFromHtml(htmlContent, userId);
            List<SnBookmark> existBookmarks = getBookmarkByUserId(userId);
            Integer totalCount = bookmarks.size();
            Integer skipCount = 0;

            //过滤已经存在的书签
            bookmarks = bookmarks.stream().filter(bookmark -> !existBookmarks.contains(bookmark)).toList();
            skipCount = totalCount - bookmarks.size();

            if (bookmarks.isEmpty()) {
                return ImportBookmarkResp.success(0, 0, skipCount);
            }

            // 批量保存书签
            int successCount = 0;
            for (SnBookmark bookmark : bookmarks) {
                try {
                    if (save(bookmark)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    // 记录失败的书签，继续处理其他书签
                    log.warn("保存书签失败: {}", bookmark.getName(), e);
                }
            }

            return ImportBookmarkResp.success(successCount, bookmarks.size(), skipCount);

        } catch (Exception e) {
            log.error("导入Chrome书签失败", e);
            return new ImportBookmarkResp(0, 0, 0, 0, "导入失败: " + e.getMessage());
        }
    }

    private List<SnBookmark> getBookmarkByUserId(String userId) {
        return lambdaQuery().eq(SnBookmark::getUserId, userId).list();
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 从HTML内容中解析书签
     */
    private List<SnBookmark> parseBookmarksFromHtml(String htmlContent, String userId) {
        List<SnBookmark> bookmarks = new ArrayList<>();

        // 正则表达式匹配Chrome书签格式，包含ICON属性
        // <A HREF="url" ADD_DATE="timestamp" ICON="icon">title</A>
        Pattern bookmarkPattern = Pattern.compile(
                "<A\\s+HREF=\"([^\"]+)\"(?:[^>]*ADD_DATE=\"([^\"]*)\"|[^>]*)(?:[^>]*ICON=\"([^\"]*)\"|[^>]*)>([^<]+)</A>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = bookmarkPattern.matcher(htmlContent);

        while (matcher.find()) {
            String url = matcher.group(1);
            String iconData = matcher.group(3); // base64 icon data
            String title = matcher.group(4);

            // 跳过无效的URL
            if (url == null || url.trim().isEmpty() ||
                    title == null || title.trim().isEmpty()) {
                continue;
            }

            // 创建书签对象
            SnBookmark bookmark = new SnBookmark();
            bookmark.setUserId(userId);
            bookmark.setName(title.trim());
            bookmark.setPinyin(PinyinUtils.toPinyin(title));
            bookmark.setAbbreviation(PinyinUtils.toPinyinFirstLetter(title));
            bookmark.setUrl(url.trim());
            bookmark.setDescription("");
            bookmark.setSpaceId(null); // Space设置为空

            // 处理ICON数据 - 直接存储base64数据
            if (iconData != null && !iconData.trim().isEmpty()) {
                // Chrome导出的ICON通常是data:image格式的base64数据
                bookmark.setIcon(iconData.trim());
            } else {
                bookmark.setIcon(null);
            }

            bookmark.setNum(0);
            bookmark.setStar(false);
            bookmark.setCreateTime(new Date());
            bookmark.setUpdateTime(new Date());
            bookmark.setDeleted(0);

            bookmarks.add(bookmark);
        }

        return bookmarks;
    }

    public void clearSpaceInBookmarks(String spaceId, String currentUserId) {
        // 清除书签中的空间引用
        lambdaUpdate()
                .eq(SnBookmark::getUserId, currentUserId)
                .eq(SnBookmark::getSpaceId, spaceId)
                .set(SnBookmark::getSpaceId, null)
                .update();
    }

    /**
     * 获取用户的所有书签
     *
     * @param userId 用户ID
     * @return 书签列表
     */
    public List<SnBookmark> getUserBookmarks(String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getCreateTime)
                .list();
    }

    /**
     * 检查用户是否已有相同URL的书签
     *
     * @param url    书签URL
     * @param userId 用户ID
     * @return 是否存在相同URL的书签
     */
    public boolean isUrlExistsForUser(String url, String userId) {
        if (url == null || url.trim().isEmpty() || userId == null) {
            return false;
        }

        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getUrl, url.trim())
                .eq(SnBookmark::getDeleted, 0)
                .exists();
    }

    /**
     * 根据URL和用户ID获取书签
     *
     * @param url    书签URL
     * @param userId 用户ID
     * @return 书签对象，如果不存在返回null
     */
    public SnBookmark getBookmarkByUrlAndUser(String url, String userId) {
        if (url == null || url.trim().isEmpty() || userId == null) {
            return null;
        }

        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getUrl, url.trim())
                .eq(SnBookmark::getDeleted, 0)
                .last("limit 1")
                .one();
    }

    public List<SnBookmark> searchBookmarks(String userId, String trim) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnBookmark::getName, trim)
                        .or()
                        .like(SnBookmark::getDescription, trim)
                        .or()
                        .like(SnBookmark::getUrl, trim)
                        .or()
                        .like(SnBookmark::getPinyin, trim)
                        .or()
                        .like(SnBookmark::getAbbreviation, trim)
                )
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    public List<SnBookmark> getAllBookmarks(String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 获取用户的重复书签
     *
     * @param userId 用户ID
     * @return 重复书签列表，按URL分组
     */
    public List<SnBookmark> getDuplicateBookmarks(String userId) {
        // 使用子查询找出重复的URL
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .in(SnBookmark::getUrl,
                    lambdaQuery()
                        .select(SnBookmark::getUrl)
                        .eq(SnBookmark::getUserId, userId)
                        .eq(SnBookmark::getDeleted, 0)
                        .groupBy(SnBookmark::getUrl)
                        .having("count(*) > 1")
                        .list()
                        .stream()
                        .map(SnBookmark::getUrl)
                        .toList())
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据匹配等级获取重复书签
     *
     * @param userId 用户ID
     * @param level  匹配等级 (2-4, 2=1级域名, 3=2级域名, 4=3级域名)
     * @return 重复书签列表
     */
    /**
     * 根据匹配等级获取重复书签
     *
     * @param userId 用户ID
     * @param level 匹配等级 (1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配)
     * @return 重复书签列表
     */
    public List<SnBookmark> getDuplicateBookmarks(String userId, int level) {
        // 获取用户的所有忽略组
        List<String> ignoredGroups = ignoredGroupService.getUserIgnoredGroups(userId);

        // 获取所有书签
        List<SnBookmark> allBookmarks = lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();

        // 根据匹配等级分组书签
        Map<String, List<SnBookmark>> groupedBookmarks = groupBookmarksByLevel(allBookmarks, level);

        // 返回重复的书签（分组中超过1个的书签），过滤掉被忽略的组
        return groupedBookmarks.values().stream()
                .filter(bookmarks -> bookmarks.size() > 1)
                .filter(bookmarks -> !ignoredGroups.contains(bookmarks.get(0).getUrl())) // 使用URL作为分组键的近似判断
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 获取重复书签分组信息
     *
     * @param userId 用户ID
     * @return 按URL分组的重复书签统计
     */
    public Map<String, Long> getDuplicateUrlGroups(String userId) {
        // 获取重复URL列表
        List<String> duplicateUrls = lambdaQuery()
                .select(SnBookmark::getUrl)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .groupBy(SnBookmark::getUrl)
                .having("count(*) > 1")
                .list()
                .stream()
                .map(SnBookmark::getUrl)
                .distinct()
                .collect(Collectors.toList());

        // 统计每个URL的数量
        Map<String, Long> result = new HashMap<>();
        for (String url : duplicateUrls) {
            long count = lambdaQuery()
                    .eq(SnBookmark::getUserId, userId)
                    .eq(SnBookmark::getUrl, url)
                    .eq(SnBookmark::getDeleted, 0)
                    .count();
            result.put(url, count);
        }
        return result;
    }

    /**
     * 根据匹配等级获取重复书签分组信息
     *
     * @param userId 用户ID
     * @param level  匹配等级 (2-4, 2=1级域名, 3=2级域名, 4=3级域名)
     * @return 按分组键分组的重复书签统计
     */
    /**
     * 根据匹配等级获取重复分组
     *
     * @param userId 用户ID
     * @param level 匹配等级 (1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配)
     * @return 重复分组映射
     */
    public Map<String, Long> getDuplicateGroupsByLevel(String userId, int level) {
        // 获取用户的所有忽略组
        List<String> ignoredGroups = ignoredGroupService.getUserIgnoredGroups(userId);

        // 获取所有书签
        List<SnBookmark> allBookmarks = lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .list();

        // 根据匹配等级分组书签
        Map<String, List<SnBookmark>> groupedBookmarks = groupBookmarksByLevel(allBookmarks, level);

        // 统计每个分组的数量，过滤掉被忽略的组
        Map<String, Long> result = new HashMap<>();
        groupedBookmarks.forEach((key, bookmarks) -> {
            if (bookmarks.size() > 1 && !ignoredGroups.contains(key)) {
                result.put(key, (long) bookmarks.size());
            }
        });

        return result;
    }

    /**
     * 根据匹配等级获取指定分组的书签
     *
     * @param groupKey 分组键
     * @param userId   用户ID
     * @param level    匹配等级 (2-4, 2=1级域名, 3=2级域名, 4=3级域名)
     * @return 书签列表
     */
    /**
     * 根据分组键获取书签列表
     *
     * @param groupKey 分组键
     * @param userId 用户ID
     * @param level 匹配等级 (1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配)
     * @return 书签列表
     */
    public List<SnBookmark> getBookmarksByGroupKey(String groupKey, String userId, int level) {
        // 获取所有书签
        List<SnBookmark> allBookmarks = lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .list();

        List<SnBookmark> result = new ArrayList<>();

        for (SnBookmark bookmark : allBookmarks) {
            String url = bookmark.getUrl();
            if (url == null || url.trim().isEmpty()) {
                continue;
            }

            boolean shouldInclude = false;

            switch (level) {
                case 1:
                    // Level 1: 完整URL匹配
                    shouldInclude = groupKey.equals(normalizeUrl(url));
                    break;

                case 2:
                    // Level 2: 二级域名匹配
                    String firstLevelDomain = getFirstLevelDomain(url);
                    shouldInclude = groupKey.equals(firstLevelDomain);
                    break;

                case 3:
                    // Level 3: 三级域名匹配，但分组键仍为二级域名
                    String secondLevelDomain = getSecondLevelDomain(url);
                    String firstLevelDomainForLevel3 = getFirstLevelDomain(url);
                    // 只有当书签的三级域名与groupKey匹配，且分组键为二级域名时才包含
                    shouldInclude = groupKey.equals(firstLevelDomainForLevel3) && secondLevelDomain != null;
                    break;
            }

            if (shouldInclude) {
                result.add(bookmark);
            }
        }

        return result;
    }

    /**
     * 根据匹配等级按URL获取重复书签
     *
     * @param url    书签URL
     * @param userId 用户ID
     * @return 重复书签列表
     */
    public List<SnBookmark> getBookmarksByUrl(String url, String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getUrl, url)
                .eq(SnBookmark::getDeleted, 0)
                .orderByDesc(SnBookmark::getUpdateTime)
                .list();
    }

    /**
     * 根据匹配等级分组书签
     *
     * @param bookmarks 书签列表
     * @param level     匹配等级 (2-4, 2=1级域名, 3=2级域名, 4=3级域名)
     * @return 按分组键分组的书签映射
     */
    private Map<String, List<SnBookmark>> groupBookmarksByLevel(List<SnBookmark> bookmarks, int level) {
        Map<String, List<SnBookmark>> result = new HashMap<>();

        for (SnBookmark bookmark : bookmarks) {
            String url = bookmark.getUrl();
            if (url == null || url.trim().isEmpty()) {
                continue;
            }

            String groupKey = getGroupKeyByUrl(url, level);
            if (groupKey != null) {
                result.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(bookmark);
            }
        }

        return result;
    }

    /**
     * 根据URL和匹配等级获取分组键
     *
     * @param url   书签URL
     * @param level 匹配等级 (1-3)
     * @return 分组键，对于域名匹配始终返回二级域名
     */
    private String getGroupKeyByUrl(String url, int level) {
        try {
            switch (level) {
                case 1:
                    // Level 1: 完整URL匹配
                    return normalizeUrl(url);

                case 2:
                case 3:
                    // Level 2&3: 都使用二级域名作为分组键 (例如 google.com)
                    return getFirstLevelDomain(url);

                default:
                    // 默认使用完整URL匹配
                    return normalizeUrl(url);
            }
        } catch (Exception e) {
            // URL解析失败时返回null，跳过该书签
            return null;
        }
    }

    /**
     * 标准化URL
     *
     * @param url 原始URL
     * @return 标准化后的URL
     */
    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        String normalized = url.trim().toLowerCase();

        // 移除协议
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }

        // 移除www.前缀
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }

        // 移除端口号
        int portIndex = normalized.indexOf(':');
        if (portIndex > 0) {
            int pathIndex = normalized.indexOf('/');
            if (pathIndex == -1 || portIndex < pathIndex) {
                normalized = normalized.substring(0, portIndex) + normalized.substring(pathIndex);
            }
        }

        // 移除路径结尾的斜杠
        normalized = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;

        return normalized;
    }

    /**
     * 获取1级域名 (example.com) - 现在作为Level 2
     *
     * @param url 原始URL
     * @return 1级域名
     */
    private String getFirstLevelDomain(String url) {
        // 标准化URL处理
        String normalizedUrl = normalizeUrl(url);
        // 移除路径部分
        String domain = extractDomainPart(normalizedUrl);
        if (domain == null) return null;

        // 分割域名部分
        String[] parts = domain.split("\\.");
        if (parts.length < 2) return domain;

        // 获取最后两部分作为1级域名
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    /**
     * 获取2级域名 (sub.example.com) - 现在作为Level 3
     *
     * @param url 原始URL
     * @return 2级域名
     */
    private String getSecondLevelDomain(String url) {
        // 标准化URL处理
        String normalizedUrl = normalizeUrl(url);
        // 移除路径部分
        String domain = extractDomainPart(normalizedUrl);
        if (domain == null) return null;

        // 分割域名部分
        String[] parts = domain.split("\\.");
        if (parts.length < 2) return domain;

        // 特殊处理：处理数字前缀的情况（如 1.www.google.com -> www.google.com）
        // 如果第一个部分是数字，则跳过它
        int startIndex = 0;
        if (parts.length > 2 && isNumeric(parts[0])) {
            startIndex = 1;
        }

        // 计算有效的域名部分数量
        int effectiveLength = parts.length - startIndex;

        if (effectiveLength >= 3) {
            // 有足够的部分构成2级域名
            return parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else if (effectiveLength == 2) {
            // 只有主域名，返回1级域名
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else {
            // 只有单个部分，返回整个域名
            return domain;
        }
    }

    /**
     * 获取3级域名 (sub.sub.example.com) - 现在作为Level 4
     *
     * @param url 原始URL
     * @return 3级域名
     */
    private String getThirdLevelDomain(String url) {
        // 标准化URL处理
        String normalizedUrl = normalizeUrl(url);
        // 移除路径部分
        String domain = extractDomainPart(normalizedUrl);
        if (domain == null) return null;

        // 分割域名部分
        String[] parts = domain.split("\\.");
        if (parts.length < 2) return domain;

        // 特殊处理：处理数字前缀的情况（如 1.www.google.com -> www.google.com）
        // 如果第一个部分是数字，则跳过它
        int startIndex = 0;
        if (parts.length > 2 && isNumeric(parts[0])) {
            startIndex = 1;
        }

        // 计算有效的域名部分数量
        int effectiveLength = parts.length - startIndex;

        if (effectiveLength >= 4) {
            // 有足够的部分构成3级域名
            return parts[parts.length - 4] + "." + parts[parts.length - 3] + "." +
                   parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else if (effectiveLength == 3) {
            // 只有2级域名，返回2级域名
            return parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else if (effectiveLength == 2) {
            // 只有主域名，返回1级域名
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else {
            // 只有单个部分，返回整个域名
            return domain;
        }
    }

    /**
     * 检查字符串是否为数字
     *
     * @param str 字符串
     * @return 是否为数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 从标准化URL中提取域名部分
     *
     * @param normalizedUrl 标准化URL
     * @return 域名部分
     */
    private String extractDomainPart(String normalizedUrl) {
        if (normalizedUrl == null || normalizedUrl.trim().isEmpty()) {
            return null;
        }

        // 查找第一个斜杠，分离域名和路径
        int slashIndex = normalizedUrl.indexOf('/');
        if (slashIndex > 0) {
            return normalizedUrl.substring(0, slashIndex);
        }

        return normalizedUrl;
    }

    /**
     * 检查URL是否重复
     *
     * @param url    要检查的URL
     * @param level  匹配级别 (1: 完整URL匹配，2: 二级域名匹配，3: 三级域名匹配)
     * @param userId 用户ID
     * @return 检查重复响应
     */
    public CheckDuplicateResp checkDuplicate(String url, int level, String userId) {
        // 获取用户的所有忽略组
        List<String> ignoredGroups = ignoredGroupService.getUserIgnoredGroups(userId);

        // 获取用户的所有书签
        List<SnBookmark> userBookmarks = lambdaQuery()
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .list();

        CheckDuplicateResp response = new CheckDuplicateResp();
        response.setDuplicate(false);

        // 根据匹配级别检查重复
        switch (level) {
            case 1:
                // Level 1: 完整URL匹配
                response = checkCompleteUrlMatch(url, userBookmarks, ignoredGroups);
                break;
            case 2:
                // Level 2: 二级域名匹配
                response = checkSecondLevelDomainMatch(url, userBookmarks, ignoredGroups);
                break;
            case 3:
                // Level 3: 三级域名匹配
                response = checkThirdLevelDomainMatch(url, userBookmarks, ignoredGroups);
                break;
            default:
                // 默认使用完整URL匹配
                response = checkCompleteUrlMatch(url, userBookmarks, ignoredGroups);
        }

        return response;
    }

    /**
     * 检查完整URL匹配
     *
     * @param url            要检查的URL
     * @param userBookmarks  用户书签列表
     * @param ignoredGroups  忽略组列表
     * @return 检查重复响应
     */
    private CheckDuplicateResp checkCompleteUrlMatch(String url, List<SnBookmark> userBookmarks, List<String> ignoredGroups) {
        CheckDuplicateResp response = new CheckDuplicateResp();
        response.setDuplicate(false);

        String normalizedInputUrl = normalizeUrl(url);

        // 检查是否在忽略组中
        if (ignoredGroups.contains(normalizedInputUrl)) {
            return response;
        }

        // 查找匹配的书签
        List<SnBookmark> matchingBookmarks = userBookmarks.stream()
                .filter(bookmark -> {
                    String bookmarkUrl = bookmark.getUrl();
                    if (bookmarkUrl == null || bookmarkUrl.trim().isEmpty()) {
                        return false;
                    }
                    return normalizedInputUrl.equals(normalizeUrl(bookmarkUrl));
                })
                .toList();

        if (!matchingBookmarks.isEmpty()) {
            response.setDuplicate(true);
            response.setMatchKey(normalizedInputUrl);
            response.setCount(matchingBookmarks.size());
        }

        return response;
    }

    /**
     * 检查二级域名匹配
     *
     * @param url            要检查的URL
     * @param userBookmarks  用户书签列表
     * @param ignoredGroups  忽略组列表
     * @return 检查重复响应
     */
    private CheckDuplicateResp checkSecondLevelDomainMatch(String url, List<SnBookmark> userBookmarks, List<String> ignoredGroups) {
        CheckDuplicateResp response = new CheckDuplicateResp();
        response.setDuplicate(false);

        String inputDomain = getFirstLevelDomain(url);
        if (inputDomain == null) {
            return response;
        }

        // 检查是否在忽略组中
        if (ignoredGroups.contains(inputDomain)) {
            return response;
        }

        // 查找匹配的书签
        List<SnBookmark> matchingBookmarks = userBookmarks.stream()
                .filter(bookmark -> {
                    String bookmarkUrl = bookmark.getUrl();
                    if (bookmarkUrl == null || bookmarkUrl.trim().isEmpty()) {
                        return false;
                    }
                    String bookmarkDomain = getFirstLevelDomain(bookmarkUrl);
                    return inputDomain.equals(bookmarkDomain);
                })
                .toList();

        if (!matchingBookmarks.isEmpty()) {
            response.setDuplicate(true);
            response.setMatchKey(inputDomain);
            response.setCount(matchingBookmarks.size());
        }

        return response;
    }

    /**
     * 检查三级域名匹配
     *
     * @param url            要检查的URL
     * @param userBookmarks  用户书签列表
     * @param ignoredGroups  忽略组列表
     * @return 检查重复响应
     */
    private CheckDuplicateResp checkThirdLevelDomainMatch(String url, List<SnBookmark> userBookmarks, List<String> ignoredGroups) {
        CheckDuplicateResp response = new CheckDuplicateResp();
        response.setDuplicate(false);

        String inputSecondLevelDomain = getSecondLevelDomain(url);
        String inputFirstLevelDomain = getFirstLevelDomain(url);

        if (inputSecondLevelDomain == null || inputFirstLevelDomain == null) {
            return response;
        }

        // 检查是否在忽略组中（使用二级域名作为分组键）
        if (ignoredGroups.contains(inputFirstLevelDomain)) {
            return response;
        }

        // 查找匹配的书签
        List<SnBookmark> matchingBookmarks = userBookmarks.stream()
                .filter(bookmark -> {
                    String bookmarkUrl = bookmark.getUrl();
                    if (bookmarkUrl == null || bookmarkUrl.trim().isEmpty()) {
                        return false;
                    }
                    String bookmarkSecondLevelDomain = getSecondLevelDomain(bookmarkUrl);
                    return inputSecondLevelDomain.equals(bookmarkSecondLevelDomain);
                })
                .toList();

        if (!matchingBookmarks.isEmpty()) {
            response.setDuplicate(true);
            response.setMatchKey(inputSecondLevelDomain);
            response.setCount(matchingBookmarks.size());
        }

        return response;
    }

}

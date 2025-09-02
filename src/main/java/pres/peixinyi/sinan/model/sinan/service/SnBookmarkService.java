package pres.peixinyi.sinan.model.sinan.service;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.ObjectUtils;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmarkAssTag;
import pres.peixinyi.sinan.model.sinan.mapper.SnBookmarkMapper;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmark;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;
import pres.peixinyi.sinan.dto.response.ImportBookmarkResp;
import jakarta.annotation.Resource;
import pres.peixinyi.sinan.utils.PinyinUtils;

@Service
@Slf4j
public class SnBookmarkService extends ServiceImpl<SnBookmarkMapper, SnBookmark> {

    @Resource
    private SnBookmarkAssTagService bookmarkAssTagService;

    @Resource
    private SnTagService tagService;

    public List<SnBookmark> getMostVisitedBookmarks(int limit, String search, String userId) {
        return lambdaQuery()
                .eq(SnBookmark::getDeleted, 0)
                .eq(SnBookmark::getUserId, userId)
                .like(search != null && !search.isEmpty(), SnBookmark::getName, search)
                .or()
                .eq(SnBookmark::getUserId, userId)
                .like(search != null && !search.isEmpty(), SnBookmark::getUrl, search)
                .or()
                .eq(SnBookmark::getUserId, userId)
                .like(search != null && !search.isEmpty(), SnBookmark::getDescription, search)
                .or()
                .eq(SnBookmark::getUserId, userId)
                .like(search != null && !search.isEmpty(), SnBookmark::getPinyin, search)
                .or()
                .eq(SnBookmark::getUserId, userId)
                .like(search != null && !search.isEmpty(), SnBookmark::getAbbreviation, search)
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
    public boolean updateBookmarkByUser(String bookmarkId, String userId, String name, String url, String description, String namespaceId) {
        return lambdaUpdate()
                .eq(SnBookmark::getId, bookmarkId)
                .eq(SnBookmark::getUserId, userId)
                .eq(SnBookmark::getDeleted, 0)
                .set(name != null, SnBookmark::getName, name)
                .set(name != null, SnBookmark::getPinyin, PinyinUtils.toPinyin(name))
                .set(name != null, SnBookmark::getAbbreviation, PinyinUtils.toPinyinFirstLetter(name))
                .set(url != null, SnBookmark::getUrl, url)
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

}

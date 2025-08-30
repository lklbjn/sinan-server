package pres.peixinyi.sinan.model.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddTagReq;
import pres.peixinyi.sinan.dto.request.EditTagReq;
import pres.peixinyi.sinan.dto.response.TagResp;
import pres.peixinyi.sinan.model.sinan.entity.SnTag;
import pres.peixinyi.sinan.model.sinan.service.SnBookmarkAssTagService;
import pres.peixinyi.sinan.model.sinan.service.SnTagService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 标签控制层
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    SnTagService tagService;
    @Autowired
    private SnBookmarkAssTagService snBookmarkAssTagService;

    /**
     * 分页获取用户的所有标签
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @param search 搜索关键字（可选）
     * @return 分页标签列表
     */
    @GetMapping
    public Result<IPage<TagResp>> getUserTags(
            @RequestParam(value = "page", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "size", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "search", required = false) String search) {

        String currentUserId = StpUtil.getLoginIdAsString();

        // 参数校验
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        IPage<SnTag> tagPage = tagService.getUserTagsPage(currentUserId, pageNum, pageSize, search);
        IPage<TagResp> convert = tagPage.convert(TagResp::from);
        return Result.success(convert);
    }

    /**
     * 根据ID获取标签详情
     *
     * @param id 标签ID
     * @return 标签详情
     */
    @GetMapping("/{id}")
    public Result<TagResp> getTagById(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        SnTag tag = tagService.getTagByUserAndId(id, currentUserId);
        if (tag == null) {
            return Result.fail("标签不存在或无权限访问");
        }

        return Result.success(TagResp.from(tag));
    }

    /**
     * 新增标签
     *
     * @param req 新增标签请求
     * @return 创建的标签信息
     */
    @PostMapping
    public Result<TagResp> addTag(@Valid @RequestBody AddTagReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查标签名称是否已存在
        if (tagService.isTagNameExists(currentUserId, req.getName(), null)) {
            return Result.fail("标签名称已存在");
        }

        // 创建标签对象
        SnTag tag = new SnTag();
        tag.setUserId(currentUserId);
        tag.setName(req.getName());
        tag.setColor(req.getColor());
        tag.setDescription(req.getDescription());

        SnTag savedTag = tagService.addTag(tag);

        return Result.success(TagResp.from(savedTag));
    }

    /**
     * 编辑标签
     *
     * @param req 编辑标签请求
     * @return 更新结果
     */
    @PutMapping
    public Result<TagResp> editTag(@Valid @RequestBody EditTagReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查标签是否存在且属于当前用户
        SnTag existingTag = tagService.getTagByUserAndId(req.getId(), currentUserId);
        if (existingTag == null) {
            return Result.fail("标签不存在或无权限编辑");
        }

        // 如果修改了名称，检查新名称是否已存在
        if (req.getName() != null && !req.getName().equals(existingTag.getName())) {
            if (tagService.isTagNameExists(currentUserId, req.getName(), req.getId())) {
                return Result.fail("标签名称已存在");
            }
        }

        // 更新标签信息
        boolean updated = tagService.updateTag(
                req.getId(),
                currentUserId,
                req.getName(),
                req.getColor(),
                req.getDescription()
        );

        if (!updated) {
            return Result.fail("标签更新失败");
        }

        // 获取更新后的标签信息
        SnTag updatedTag = tagService.getTagByUserAndId(req.getId(), currentUserId);

        return Result.success(TagResp.from(updatedTag));
    }

    /**
     * 删除标签
     *
     * @param id 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteTag(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查标签是否存在且属于当前用户
        if (!tagService.isTagBelongsToUser(id, currentUserId)) {
            return Result.fail("标签不存在或无权限删除");
        }

        // 删除标签 关联的书签标签关系
        snBookmarkAssTagService.deleteByTagId(id);
        boolean deleted = tagService.deleteTag(id, currentUserId);

        if (deleted) {
            return Result.success("标签删除成功");
        } else {
            return Result.fail("标签删除失败");
        }
    }

    /**
     * 获取用户标签统计信息
     *
     * @return 标签统计
     */
    @GetMapping("/stats")
    public Result<TagStatsResp> getTagStats() {
        String currentUserId = StpUtil.getLoginIdAsString();

        long tagCount = tagService.getUserTagCount(currentUserId);

        TagStatsResp stats = new TagStatsResp();
        stats.setTotalCount(tagCount);

        return Result.success(stats);
    }

    /**
     * 获取用户的所有标签列表
     * 不分页，返回所有标签
     *
     * @return 标签列表
     */
    @GetMapping("/all")
    public Result<List<TagResp>> getAllUserTags() {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 获取用户的所有标签
        List<SnTag> tags = tagService.getUserTags(currentUserId);

        // 转换为响应对象
        List<TagResp> tagRespList = tags.stream()
                .map(TagResp::from)
                .toList();

        return Result.success(tagRespList);
    }

    /**
     * 批量更新标签排序
     *
     * @param sortUpdates 排序更新列表，格式：[{"id": "标签ID", "sort": 排序值}]
     * @return 更新结果
     */
    @PutMapping("/sort")
    public Result<String> updateTagSort(@RequestBody List<SortUpdateReq> sortUpdates) {
        String currentUserId = StpUtil.getLoginIdAsString();

        for (SortUpdateReq sortUpdate : sortUpdates) {
            // 检查标签是否属于当前用户
            if (!tagService.isTagBelongsToUser(sortUpdate.getId(), currentUserId)) {
                return Result.fail("标签 " + sortUpdate.getId() + " 不存在或无权限操作");
            }

            // 更新排序值
            tagService.updateTagSort(sortUpdate.getId(), currentUserId, sortUpdate.getSort());
        }

        return Result.success("排序更新成功");
    }

    /**
     * 拖拽排序标签
     * 支持两种方式：1. 传递拖拽的标签ID和目标位置 2. 传递重新排序后的完整ID列表
     *
     * @param req 拖拽排序请求
     * @return 排序结果
     */
    @PutMapping("/drag-sort")
    public Result<String> dragSortTags(@RequestBody TagDragSortReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            if (req.getSortedTagIds() != null && !req.getSortedTagIds().isEmpty()) {
                // 方式1：直接使用重新排序后的ID列表
                return handleSortByIdList(req.getSortedTagIds(), currentUserId);
            } else if (req.getDraggedTagId() != null && req.getTargetIndex() != null) {
                // 方式2：根据拖拽的标签ID和目标位置重新排序
                return handleDragToPosition(req.getDraggedTagId(), req.getTargetIndex(), currentUserId);
            } else {
                return Result.fail("请提供有效的排序参数");
            }
        } catch (Exception e) {
            return Result.fail("排序失败: " + e.getMessage());
        }
    }

    /**
     * 处理通过ID列表排序
     */
    private Result<String> handleSortByIdList(List<String> sortedTagIds, String currentUserId) {
        // 验证所有标签是否属于当前用户
        for (String tagId : sortedTagIds) {
            if (!tagService.isTagBelongsToUser(tagId, currentUserId)) {
                return Result.fail("标签 " + tagId + " 不存在或无权限操作");
            }
        }

        // 更新排序
        for (int i = 0; i < sortedTagIds.size(); i++) {
            String tagId = sortedTagIds.get(i);
            tagService.updateTagSort(tagId, currentUserId, i + 1);
        }

        return Result.success("排序更新成功");
    }

    /**
     * 处理拖拽到指定位置
     */
    private Result<String> handleDragToPosition(String draggedTagId, Integer targetIndex, String currentUserId) {
        // 获取用户所有标签
        List<SnTag> allTags = tagService.getUserTagsOrderBySort(currentUserId);

        // 验证拖拽的标签是否存在
        SnTag draggedTag = allTags.stream()
                .filter(tag -> tag.getId().equals(draggedTagId))
                .findFirst()
                .orElse(null);

        if (draggedTag == null) {
            return Result.fail("被拖拽的标签不存在或无权限操作");
        }

        // 验证目标索引
        if (targetIndex < 0 || targetIndex >= allTags.size()) {
            return Result.fail("目标位置无效");
        }

        // 重新排列标签
        allTags.remove(draggedTag);
        allTags.add(targetIndex, draggedTag);

        // 更新排序值
        for (int i = 0; i < allTags.size(); i++) {
            tagService.updateTagSort(allTags.get(i).getId(), currentUserId, i + 1);
        }

        return Result.success("排序更新成功");
    }

    /**
     * 排序更新请求内部类
     */
    public static class SortUpdateReq {
        private String id;
        private Integer sort;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }

    /**
     * 拖拽排序请求内部类
     */
    public static class TagDragSortReq {
        private List<String> sortedTagIds;  // 方式1：重新排序后的完整ID列表
        private String draggedTagId;        // 方式2：被拖拽的标签ID
        private Integer targetIndex;        // 方式2：目标位置索引

        public List<String> getSortedTagIds() {
            return sortedTagIds;
        }

        public void setSortedTagIds(List<String> sortedTagIds) {
            this.sortedTagIds = sortedTagIds;
        }

        public String getDraggedTagId() {
            return draggedTagId;
        }

        public void setDraggedTagId(String draggedTagId) {
            this.draggedTagId = draggedTagId;
        }

        public Integer getTargetIndex() {
            return targetIndex;
        }

        public void setTargetIndex(Integer targetIndex) {
            this.targetIndex = targetIndex;
        }
    }

    /**
     * 标签统计响应内部类
     */
    public static class TagStatsResp {
        private long totalCount;

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }

}

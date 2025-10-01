package pres.peixinyi.sinan.module.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddSpaceReq;
import pres.peixinyi.sinan.dto.request.EditSpaceReq;
import pres.peixinyi.sinan.dto.request.SpaceDragSortReq;
import pres.peixinyi.sinan.dto.response.SpaceResp;
import pres.peixinyi.sinan.dto.response.SpaceSimpleResp;
import pres.peixinyi.sinan.module.sinan.entity.SnShareSpaceAssUser;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;
import pres.peixinyi.sinan.module.sinan.service.SnBookmarkService;
import pres.peixinyi.sinan.module.sinan.service.SnShareSpaceAssUserService;
import pres.peixinyi.sinan.module.sinan.service.SnSpaceService;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 空间控制层
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/13 21:38
 * @Version : 0.0.0
 */
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    SnSpaceService spaceService;

    @Resource
    SnBookmarkService snBookmarkService;

    @Resource
    SnShareSpaceAssUserService snShareSpaceAssUserService;

    /**
     * 分页获取用户的所有空间
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页大小，默认10
     * @param search   搜索关键字（可选）
     * @return 分页空间列表
     */
    @GetMapping
    public Result<IPage<SpaceResp>> getUserSpaces(
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

        IPage<SnSpace> spacePage = spaceService.getUserSpacesPage(currentUserId, pageNum, pageSize, search);
        IPage<SpaceResp> convert = spacePage.convert(SpaceResp::from);
        return Result.success(convert);
    }

    /**
     * 根据ID获取空间详情
     *
     * @param id 空间ID
     * @return 空间详情
     */
    @GetMapping("/{id}")
    public Result<SpaceResp> getSpaceById(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();
        SnSpace space = spaceService.getNamespaceByUserAndId(id, currentUserId);
        if (!ObjectUtils.isEmpty(space)) {
            return Result.success(SpaceResp.from(space));
        }
        if (space == null) {
            List<SnShareSpaceAssUser> shareSpaceAssUsers = snShareSpaceAssUserService.getByUserId(currentUserId);
            List<String> spaceIds = shareSpaceAssUsers.stream().map(SnShareSpaceAssUser::getSpaceId).toList();
            if (spaceIds.contains(id)) {
                space = spaceService.getById(id);
            } else {
                return Result.fail("空间不存在或无权限查看");
            }
        }
        return Result.success(SpaceResp.from(space));
    }

    /**
     * 新增空间
     *
     * @param req 新增空间请求
     * @return 创建的空间信息
     */
    @PostMapping
    public Result<SpaceResp> addSpace(@Valid @RequestBody AddSpaceReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查空间名称是否已存在
        if (spaceService.isSpaceNameExists(currentUserId, req.getName(), null)) {
            return Result.fail("空间名称已存在");
        }

        // 创建空间对象
        SnSpace space = new SnSpace();
        space.setUserId(currentUserId);
        space.setName(req.getName());
        space.setIcon(req.getIcon());
        space.setSort(req.getSort());
        space.setDescription(req.getDescription());

        SnSpace savedSpace = spaceService.addSpace(space);

        return Result.success(SpaceResp.from(savedSpace));
    }

    /**
     * 编辑空间
     *
     * @param req 编辑空间请求
     * @return 更新结果
     */
    @PutMapping
    public Result<SpaceResp> editSpace(@Valid @RequestBody EditSpaceReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查空间是否存在且属于当前用户
        SnSpace existingSpace = spaceService.getNamespaceByUserAndId(req.getId(), currentUserId);
        if (existingSpace == null) {
            return Result.fail("空间不存在或无权限编辑");
        }

        // 如果修改了名称，检查新名称是否已存在
        if (req.getName() != null && !req.getName().equals(existingSpace.getName())) {
            if (spaceService.isSpaceNameExists(currentUserId, req.getName(), req.getId())) {
                return Result.fail("空间名称已存在");
            }
        }

        // 更新空间信息
        boolean updated = spaceService.updateSpace(
                req.getId(),
                currentUserId,
                req.getName(),
                req.getIcon(),
                req.getSort(),
                req.getDescription()
        );

        if (!updated) {
            return Result.fail("空���更新失败");
        }

        // 获取更新后的空间信息
        SnSpace updatedSpace = spaceService.getNamespaceByUserAndId(req.getId(), currentUserId);

        return Result.success(SpaceResp.from(updatedSpace));
    }

    /**
     * 删除空间
     *
     * @param id 空间ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteSpace(@PathVariable("id") String id) {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 检查空间是否存在且属于当前用户
        if (!spaceService.isNamespaceBelongsToUser(id, currentUserId)) {
            return Result.fail("空间不存在或无权限删除");
        }
        //将Bookmark中的空间置空
        snBookmarkService.clearSpaceInBookmarks(id, currentUserId);

        // 删除空间
        boolean deleted = spaceService.deleteSpace(id, currentUserId);


        if (deleted) {
            return Result.success("空间删除成功");
        } else {
            return Result.fail("空间删除失败");
        }
    }

    /**
     * 批量更新空间排序
     *
     * @param sortUpdates 排序更新列表，格式：[{"id": "空间ID", "sort": 排序值}]
     * @return 更新结果
     */
    @PutMapping("/sort")
    public Result<String> updateSpaceSort(@RequestBody List<SortUpdateReq> sortUpdates) {
        String currentUserId = StpUtil.getLoginIdAsString();

        for (SortUpdateReq sortUpdate : sortUpdates) {
            // 检查空间是否属于当前用户
            if (!spaceService.isNamespaceBelongsToUser(sortUpdate.getId(), currentUserId)) {
                return Result.fail("空间 " + sortUpdate.getId() + " 不存在或无权限操作");
            }

            // 更新排序值
            spaceService.updateSpace(
                    sortUpdate.getId(),
                    currentUserId,
                    null, null, sortUpdate.getSort(), null
            );
        }

        return Result.success("排序更新成功");
    }

    /**
     * 获取用户的所有空间简化列表
     * 只返回空间的ID、名称和图标
     *
     * @return 空间简化列表
     */
    @GetMapping("/all")
    public Result<List<SpaceSimpleResp>> getAllUserSpaces() {
        String currentUserId = StpUtil.getLoginIdAsString();

        // 获取用户的所有空间
        List<SnSpace> spaces = spaceService.getUserSpaces(currentUserId);

        // 转换为简化响应对象
        List<SpaceSimpleResp> spaceSimpleList = spaces.stream()
                .map(SpaceSimpleResp::from)
                .toList();

        return Result.success(spaceSimpleList);
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
     * 拖拽排序空间
     * 支持两种方式：1. 传递拖拽的空间ID和目标位置 2. 传递重新排序后的完整ID列表
     *
     * @param req 拖拽排序请求
     * @return 排序结果
     */
    @PutMapping("/drag-sort")
    public Result<String> dragSortSpaces(@RequestBody SpaceDragSortReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        try {
            if (req.getSortedSpaceIds() != null && !req.getSortedSpaceIds().isEmpty()) {
                // 方式1：直接使用重新排序后的ID列表
                return handleSortByIdList(req.getSortedSpaceIds(), currentUserId);
            } else if (req.getDraggedSpaceId() != null && req.getTargetIndex() != null) {
                // 方式2：根据拖拽的空间ID和目标位置重新排序
                return handleDragToPosition(req.getDraggedSpaceId(), req.getTargetIndex(), currentUserId);
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
    private Result<String> handleSortByIdList(List<String> sortedSpaceIds, String currentUserId) {
        // 验证所有空间是否属于当前用户
        for (String spaceId : sortedSpaceIds) {
            if (!spaceService.isNamespaceBelongsToUser(spaceId, currentUserId)) {
                return Result.fail("空间 " + spaceId + " 不存在或无权限操作");
            }
        }

        // 更新排序
        for (int i = 0; i < sortedSpaceIds.size(); i++) {
            String spaceId = sortedSpaceIds.get(i);
            spaceService.updateSpace(spaceId, currentUserId, null, null, i + 1, null);
        }

        return Result.success("排序更新成功");
    }

    /**
     * 处理拖拽到指定位置
     */
    private Result<String> handleDragToPosition(String draggedSpaceId, int targetIndex, String currentUserId) {
        // 验证拖拽的空间是否属于当前用户
        if (!spaceService.isNamespaceBelongsToUser(draggedSpaceId, currentUserId)) {
            return Result.fail("空间不存在或无权限操作");
        }

        // 获取用户的所有空间
        List<SnSpace> allSpaces = spaceService.getUserSpaces(currentUserId);

        // 验证目标位置是否有效
        if (targetIndex < 0 || targetIndex >= allSpaces.size()) {
            return Result.fail("目标位置无效");
        }

        // 找到被拖拽的空间并从列表中移除
        SnSpace draggedSpace = null;
        for (int i = 0; i < allSpaces.size(); i++) {
            if (allSpaces.get(i).getId().equals(draggedSpaceId)) {
                draggedSpace = allSpaces.remove(i);
                break;
            }
        }

        if (draggedSpace == null) {
            return Result.fail("未找到被拖拽的空间");
        }

        // 将被拖拽的空间插入到目标位置
        allSpaces.add(targetIndex, draggedSpace);

        // 重新分配排序值
        for (int i = 0; i < allSpaces.size(); i++) {
            String spaceId = allSpaces.get(i).getId();
            int sortValue = i + 1; // 从1开始

            spaceService.updateSpace(spaceId, currentUserId, null, null, sortValue, null);
        }

        return Result.success("拖拽排序成功");
    }


}

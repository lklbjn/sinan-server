package pres.peixinyi.sinan.model.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.dto.request.AddIgnoredGroupReq;
import pres.peixinyi.sinan.dto.request.SetIgnoredGroupsReq;
import pres.peixinyi.sinan.model.sinan.service.SnIgnoredGroupService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 忽略组控制器
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@RestController
@RequestMapping("/bookmark/ignored-groups")
public class IgnoredGroupController {

    @Autowired
    private SnIgnoredGroupService ignoredGroupService;

    /**
     * 获取用户的所有忽略组
     *
     * @return 忽略组名称列表
     */
    @GetMapping
    public Result<List<String>> getIgnoredGroups() {
        String currentUserId = StpUtil.getLoginIdAsString();
        List<String> ignoredGroups = ignoredGroupService.getUserIgnoredGroups(currentUserId);
        return Result.success(ignoredGroups);
    }

    /**
     * 添加忽略组
     *
     * @param req 添加忽略组请求
     * @return 操作结果
     */
    @PostMapping
    public Result<String> addIgnoredGroup(@RequestBody AddIgnoredGroupReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        if (req.getGroupName() == null || req.getGroupName().trim().isEmpty()) {
            return Result.fail("组名称不能为空");
        }

        String groupName = req.getGroupName().trim();
        boolean success = ignoredGroupService.addIgnoredGroup(currentUserId, groupName);

        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.fail("组名称已存在或添加失败");
        }
    }

    /**
     * 移除忽略组
     *
     * @param groupName 组名称
     * @return 操作结果
     */
    @DeleteMapping("/{groupName}")
    public Result<String> removeIgnoredGroup(@PathVariable String groupName) {
        String currentUserId = StpUtil.getLoginIdAsString();

        if (groupName == null || groupName.trim().isEmpty()) {
            return Result.fail("组名称不能为空");
        }

        boolean success = ignoredGroupService.removeIgnoredGroup(currentUserId, groupName.trim());

        if (success) {
            return Result.success("移除成功");
        } else {
            return Result.fail("组名称不存在或移除失败");
        }
    }

    /**
     * 批量设置忽略组
     *
     * @param req 设置忽略组请求
     * @return 操作结果
     */
    @PutMapping
    public Result<String> setIgnoredGroups(@RequestBody SetIgnoredGroupsReq req) {
        String currentUserId = StpUtil.getLoginIdAsString();

        if (req.getGroupNames() == null) {
            return Result.fail("组名称列表不能为空");
        }

        // 过滤掉空值并去重
        List<String> groupNames = req.getGroupNames().stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        boolean success = ignoredGroupService.setIgnoredGroups(currentUserId, groupNames);

        if (success) {
            return Result.success("设置成功");
        } else {
            return Result.fail("设置失败");
        }
    }
}
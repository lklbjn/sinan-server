package pres.peixinyi.sinan.module.sinan.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.module.sinan.entity.SnIgnoredGroup;
import pres.peixinyi.sinan.module.sinan.mapper.SnIgnoredGroupMapper;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 忽略组服务
 *
 * @Author : PeiXinyi
 * @Date : 2025/9/28
 * @Version : 0.0.0
 */
@Service
public class SnIgnoredGroupService extends ServiceImpl<SnIgnoredGroupMapper, SnIgnoredGroup> {

    /**
     * 获取用户的所有忽略组名称
     *
     * @param userId 用户ID
     * @return 忽略组名称列表
     */
    public List<String> getUserIgnoredGroups(String userId) {
        return lambdaQuery()
                .eq(SnIgnoredGroup::getUserId, userId)
                .eq(SnIgnoredGroup::getDeleted, 0)
                .list()
                .stream()
                .map(SnIgnoredGroup::getGroupName)
                .collect(Collectors.toList());
    }

    /**
     * 添加忽略组
     *
     * @param userId    用户ID
     * @param groupName 组名称
     * @return 是否添加成功
     */
    public boolean addIgnoredGroup(String userId, String groupName) {
        // 检查是否已存在
        boolean exists = lambdaQuery()
                .eq(SnIgnoredGroup::getUserId, userId)
                .eq(SnIgnoredGroup::getGroupName, groupName)
                .eq(SnIgnoredGroup::getDeleted, 0)
                .exists();

        if (exists) {
            return false; // 已存在，添加失败
        }

        // 创建新的忽略组
        SnIgnoredGroup ignoredGroup = new SnIgnoredGroup();
        ignoredGroup.setUserId(userId);
        ignoredGroup.setGroupName(groupName);
        ignoredGroup.setCreatedAt(new Date());
        ignoredGroup.setUpdatedAt(new Date());
        ignoredGroup.setDeleted(0);

        return save(ignoredGroup);
    }

    /**
     * 移除忽略组
     *
     * @param userId    用户ID
     * @param groupName 组名称
     * @return 是否移除成功
     */
    public boolean removeIgnoredGroup(String userId, String groupName) {
        return lambdaUpdate()
                .eq(SnIgnoredGroup::getUserId, userId)
                .eq(SnIgnoredGroup::getGroupName, groupName)
                .eq(SnIgnoredGroup::getDeleted, 0)
                .set(SnIgnoredGroup::getDeleted, 1)
                .set(SnIgnoredGroup::getUpdatedAt, new Date())
                .update();
    }

    /**
     * 批量设置忽略组（先清空再添加）
     *
     * @param userId     用户ID
     * @param groupNames 组名称列表
     * @return 是否设置成功
     */
    public boolean setIgnoredGroups(String userId, List<String> groupNames) {
        try {
            // 先逻辑删除用户的所有忽略组
            lambdaUpdate()
                    .eq(SnIgnoredGroup::getUserId, userId)
                    .eq(SnIgnoredGroup::getDeleted, 0)
                    .set(SnIgnoredGroup::getDeleted, 1)
                    .set(SnIgnoredGroup::getUpdatedAt, new Date())
                    .update();

            // 添加新的忽略组
            Date now = new Date();
            List<SnIgnoredGroup> ignoredGroups = groupNames.stream()
                    .map(groupName -> {
                        SnIgnoredGroup group = new SnIgnoredGroup();
                        group.setUserId(userId);
                        group.setGroupName(groupName);
                        group.setCreatedAt(now);
                        group.setUpdatedAt(now);
                        group.setDeleted(0);
                        return group;
                    })
                    .collect(Collectors.toList());

            return saveBatch(ignoredGroups);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查指定组是否被忽略
     *
     * @param userId    用户ID
     * @param groupName 组名称
     * @return 是否被忽略
     */
    public boolean isGroupIgnored(String userId, String groupName) {
        return lambdaQuery()
                .eq(SnIgnoredGroup::getUserId, userId)
                .eq(SnIgnoredGroup::getGroupName, groupName)
                .eq(SnIgnoredGroup::getDeleted, 0)
                .exists();
    }

    /**
     * 清空用户的所有忽略组
     *
     * @param userId 用户ID
     * @return 是否清空成功
     */
    public boolean clearUserIgnoredGroups(String userId) {
        return lambdaUpdate()
                .eq(SnIgnoredGroup::getUserId, userId)
                .eq(SnIgnoredGroup::getDeleted, 0)
                .set(SnIgnoredGroup::getDeleted, 1)
                .set(SnIgnoredGroup::getUpdatedAt, new Date())
                .update();
    }
}
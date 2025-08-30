package pres.peixinyi.sinan.model.sinan.service;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import pres.peixinyi.sinan.dto.request.ShareSpaceUpdateReq;
import pres.peixinyi.sinan.model.sinan.entity.SnSpace;
import pres.peixinyi.sinan.model.sinan.mapper.SnSpaceMapper;
import pres.peixinyi.sinan.utils.PinyinUtils;

@Service
public class SnSpaceService extends ServiceImpl<SnSpaceMapper, SnSpace> {

    public SnSpaceService() {
    }

    /**
     * 检查命名空间是否存在且属于指定用户
     *
     * @param namespaceId 命名空间ID
     * @param userId      用户ID
     * @return 命名空间对象，如果不存在或不属于用户则返回null
     */
    public SnSpace getNamespaceByUserAndId(String namespaceId, String userId) {
        return lambdaQuery()
                .eq(SnSpace::getId, namespaceId)
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .one();
    }

    /**
     * 检查命名空间是否属于指定用户
     *
     * @param namespaceId 命名空间ID
     * @param userId      用户ID
     * @return true 如果命名空间存在且属于用户，否则返回false
     */
    public boolean isNamespaceBelongsToUser(String namespaceId, String userId) {
        return getNamespaceByUserAndId(namespaceId, userId) != null;
    }

    /**
     * 新增空间
     *
     * @param space 空间对象
     * @return 保存后的空间对象
     */
    public SnSpace addSpace(SnSpace space) {
        space.setPinyin(PinyinUtils.toPinyin(space.getName()));
        space.setAbbreviation(PinyinUtils.toPinyinFirstLetter(space.getName()));
        space.setCreateTime(new Date());
        space.setUpdateTime(new Date());
        space.setDeleted(0);

        // 如果没有设置排序值，设置为最大值+1
        if (space.getSort() == null) {
            Integer maxSort = getMaxSortByUser(space.getUserId());
            space.setSort(maxSort == null ? 1 : maxSort + 1);
        }

        save(space);
        return space;
    }

    /**
     * 获取用户的最大排序值
     *
     * @param userId 用户ID
     * @return 最大排序值
     */
    public Integer getMaxSortByUser(String userId) {
        SnSpace space = lambdaQuery()
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .orderByDesc(SnSpace::getSort)
                .last("limit 1")
                .one();
        return space != null ? space.getSort() : null;
    }

    /**
     * 更新空间
     *
     * @param spaceId     空间ID
     * @param userId      用户ID
     * @param name        空间名称
     * @param icon        空间图标
     * @param sort        排序值
     * @param description 描述
     * @return true 更新成功，false 更新失败
     */
    public boolean updateSpace(String spaceId, String userId, String name, String icon, Integer sort, String description) {
        return lambdaUpdate()
                .eq(SnSpace::getId, spaceId)
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .set(name != null, SnSpace::getName, name)
                .set(name != null, SnSpace::getPinyin, PinyinUtils.toPinyin(name))
                .set(name != null, SnSpace::getAbbreviation, PinyinUtils.toPinyinFirstLetter(name))
                .set(icon != null, SnSpace::getIcon, icon)
                .set(sort != null, SnSpace::getSort, sort)
                .set(description != null, SnSpace::getDescription, description)
                .set(SnSpace::getUpdateTime, new Date())
                .update();
    }

    /**
     * 删除空间（逻辑删除）
     *
     * @param spaceId 空间ID
     * @param userId  用户ID
     * @return true 删除成功，false 删除失败
     */
    public boolean deleteSpace(String spaceId, String userId) {
        return lambdaUpdate()
                .eq(SnSpace::getId, spaceId)
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .set(SnSpace::getDeleted, 1)
                .set(SnSpace::getUpdateTime, new Date())
                .update();
    }

    /**
     * 获取用户的所有空间（按排序值排序）
     *
     * @param userId 用户ID
     * @return 空间列表
     */
    public List<SnSpace> getUserSpaces(String userId) {
        return lambdaQuery()
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .orderByAsc(SnSpace::getSort)
                .orderByDesc(SnSpace::getCreateTime)
                .list();
    }

    /**
     * 搜索用户的空间
     *
     * @param userId 用户ID
     * @param search 搜索关键字
     * @return 空间列表
     */
    public List<SnSpace> searchUserSpaces(String userId, String search) {
        return lambdaQuery()
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnSpace::getName, search)
                        .or()
                        .like(SnSpace::getDescription, search)
                )
                .orderByAsc(SnSpace::getSort)
                .orderByDesc(SnSpace::getCreateTime)
                .list();
    }

    /**
     * 检查空间名称是否已存在
     *
     * @param userId    用户ID
     * @param name      空间名称
     * @param excludeId 排除的空间ID（用于编辑时排除自己）
     * @return true 已存在，false 不存在
     */
    public boolean isSpaceNameExists(String userId, String name, String excludeId) {
        return lambdaQuery()
                .eq(SnSpace::getUserId, userId)
                .eq(SnSpace::getName, name)
                .eq(SnSpace::getDeleted, 0)
                .ne(excludeId != null, SnSpace::getId, excludeId)
                .exists();
    }

    /**
     * 分页获取用户的空间
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param search   搜索关键字（可选）
     * @return 分页结果
     */
    public IPage<SnSpace> getUserSpacesPage(String userId, int pageNum, int pageSize, String search) {
        Page<SnSpace> page = new Page<>(pageNum, pageSize);

        if (search != null && !search.trim().isEmpty()) {
            return lambdaQuery()
                    .eq(SnSpace::getUserId, userId)
                    .eq(SnSpace::getDeleted, 0)
                    .and(wrapper -> wrapper
                            .like(SnSpace::getName, search.trim())
                            .or()
                            .like(SnSpace::getDescription, search.trim())
                    )
                    .orderByAsc(SnSpace::getSort)
                    .orderByDesc(SnSpace::getCreateTime)
                    .page(page);
        } else {
            return lambdaQuery()
                    .eq(SnSpace::getUserId, userId)
                    .eq(SnSpace::getDeleted, 0)
                    .orderByAsc(SnSpace::getSort)
                    .orderByDesc(SnSpace::getCreateTime)
                    .page(page);
        }
    }

    /**
     * 分页获取用户的空间
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param search   搜索关键字（可选）
     * @return 分页结果
     */
    public IPage<SnSpace> getUserSpacesPage(int pageNum, int pageSize, String search, List<String> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        Page<SnSpace> page = new Page<>(pageNum, pageSize);

        if (search != null && !search.trim().isEmpty()) {
            return lambdaQuery()
                    .eq(SnSpace::getDeleted, 0)
                    .and(wrapper -> wrapper
                            .like(SnSpace::getName, search.trim())
                            .or()
                            .like(SnSpace::getDescription, search.trim())
                    )
                    .in(SnSpace::getId, spaceIds)
                    .eq(SnSpace::getShare, 1)
                    .orderByAsc(SnSpace::getSort)
                    .orderByDesc(SnSpace::getCreateTime)
                    .page(page);
        } else {
            return lambdaQuery()
                    .eq(SnSpace::getDeleted, 0)
                    .eq(SnSpace::getShare, 1)
                    .in(SnSpace::getId, spaceIds)
                    .orderByAsc(SnSpace::getSort)
                    .orderByDesc(SnSpace::getCreateTime)
                    .page(page);
        }
    }


    public void updateShare(@Valid ShareSpaceUpdateReq req) {
        lambdaUpdate()
                .eq(SnSpace::getId, req.getSpaceId())
                .set(SnSpace::getShare, req.getEnable())
                .set(SnSpace::getShareKey, req.getKey())
                .set(SnSpace::getUpdateTime, new Date())
                .update();
    }


}

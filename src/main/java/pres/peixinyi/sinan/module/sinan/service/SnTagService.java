package pres.peixinyi.sinan.module.sinan.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import pres.peixinyi.sinan.module.sinan.entity.SnTag;
import pres.peixinyi.sinan.module.sinan.mapper.SnTagMapper;
import pres.peixinyi.sinan.utils.PinyinUtils;

@Service
public class SnTagService extends ServiceImpl<SnTagMapper, SnTag> {

    /**
     * 检查标签是否都存在且属于指定用户
     *
     * @param tagIds 标签ID列表
     * @param userId 用户ID
     * @return 标签列表
     */
    public List<SnTag> getTagsByUserAndIds(List<String> tagIds, String userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(SnTag::getId, tagIds)
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .list();
    }

    /**
     * 验证所有标签是否都属于指定用户
     *
     * @param tagIds 标签ID列表
     * @param userId 用户ID
     * @return true 如果所有标签都存在且属于用户，否则返回false
     */
    public boolean areAllTagsBelongToUser(List<String> tagIds, String userId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        List<SnTag> tags = getTagsByUserAndIds(tagIds, userId);
        return tags.size() == tagIds.size();
    }

    /**
     * 检查标签是否存在且属于指定用户
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return 标签对象，如果不存在或不属于用户则返回null
     */
    public SnTag getTagByUserAndId(String tagId, String userId) {
        return lambdaQuery()
                .eq(SnTag::getId, tagId)
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .one();
    }

    /**
     * 检查标签是否属于指定用户
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return true 如果标签存在且属于用户，否则返回false
     */
    public boolean isTagBelongsToUser(String tagId, String userId) {
        return getTagByUserAndId(tagId, userId) != null;
    }

    /**
     * 新增标签
     *
     * @param tag 标签对象
     * @return 保存后的标签对象
     */
    public SnTag addTag(SnTag tag) {
        tag.setPinyin(PinyinUtils.toPinyin(tag.getName()));
        tag.setAbbreviation(PinyinUtils.toPinyinFirstLetter(tag.getName()));
        tag.setCreateTime(new Date());
        tag.setUpdateTime(new Date());
        tag.setDeleted(0);
        save(tag);
        return tag;
    }

    /**
     * 更新标签
     *
     * @param tagId       标签ID
     * @param userId      用户ID
     * @param name        标签名称
     * @param color       标签颜色
     * @param description 标签描述
     * @return true 更新成功，false 更新失败
     */
    public boolean updateTag(String tagId, String userId, String name, String color, String description) {
        return lambdaUpdate()
                .eq(SnTag::getId, tagId)
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .set(name != null, SnTag::getName, name)
                .set(name != null, SnTag::getPinyin, PinyinUtils.toPinyin(name))
                .set(name != null, SnTag::getAbbreviation, PinyinUtils.toPinyinFirstLetter(name))
                .set(color != null, SnTag::getColor, color)
                .set(description != null, SnTag::getDescription, description)
                .set(SnTag::getUpdateTime, new Date())
                .update();
    }

    /**
     * 更新标签排序
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @param sort   排序值
     * @return true 更新成功，false 更新失败
     */
    public boolean updateTagSort(String tagId, String userId, Integer sort) {
        return lambdaUpdate()
                .eq(SnTag::getId, tagId)
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .set(SnTag::getSort, sort)
                .set(SnTag::getUpdateTime, new Date())
                .update();
    }

    /**
     * 删除标签（逻辑删除）
     *
     * @param tagId  标签ID
     * @param userId 用户ID
     * @return true 删除成功，false 删除失败
     */
    public boolean deleteTag(String tagId, String userId) {
        return lambdaUpdate()
                .eq(SnTag::getId, tagId)
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .set(SnTag::getDeleted, 1)
                .set(SnTag::getUpdateTime, new Date())
                .update();
    }

    /**
     * 获取用户的所有标签（按排序值和创建时间排序）
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    public List<SnTag> getUserTags(String userId) {
        return lambdaQuery()
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .orderByAsc(SnTag::getSort)
                .orderByDesc(SnTag::getCreateTime)
                .list();
    }

    /**
     * 搜索用户的标签
     *
     * @param userId 用户ID
     * @param search 搜索关键字
     * @return 标签列表
     */
    public List<SnTag> searchUserTags(String userId, String search) {
        return lambdaQuery()
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .and(wrapper -> wrapper
                        .like(SnTag::getName, search)
                        .or()
                        .like(SnTag::getDescription, search)
                )
                .orderByDesc(SnTag::getCreateTime)
                .list();
    }

    /**
     * 检查标签名称是否已存在
     *
     * @param userId    用户ID
     * @param name      标签名称
     * @param excludeId 排除的标签ID（用于编辑时排除自己）
     * @return true 已存在，false 不存在
     */
    public boolean isTagNameExists(String userId, String name, String excludeId) {
        return lambdaQuery()
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getName, name)
                .eq(SnTag::getDeleted, 0)
                .ne(excludeId != null, SnTag::getId, excludeId)
                .exists();
    }

    /**
     * 获取用户标签数量
     *
     * @param userId 用户ID
     * @return 标签数量
     */
    public long getUserTagCount(String userId) {
        return lambdaQuery()
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .count();
    }

    /**
     * 分页获取用户的标签
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param search   搜索关键字（可选）
     * @return 分页结果
     */
    public IPage<SnTag> getUserTagsPage(String userId, int pageNum, int pageSize, String search) {
        Page<SnTag> page = new Page<>(pageNum, pageSize);

        if (search != null && !search.trim().isEmpty()) {
            return lambdaQuery()
                    .eq(SnTag::getUserId, userId)
                    .eq(SnTag::getDeleted, 0)
                    .and(wrapper -> wrapper
                            .like(SnTag::getName, search.trim())
                            .or()
                            .like(SnTag::getDescription, search.trim())
                    )
                    .orderByAsc(SnTag::getSort)
                    .orderByDesc(SnTag::getCreateTime)
                    .page(page);
        } else {
            return lambdaQuery()
                    .eq(SnTag::getUserId, userId)
                    .eq(SnTag::getDeleted, 0)
                    .orderByDesc(SnTag::getCreateTime)
                    .orderByAsc(SnTag::getSort)
                    .page(page);
        }
    }

    /**
     * 获取用户的所有标签（按排序值和创建时间排序）
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    public List<SnTag> getUserTagsOrderBySort(String userId) {
        return lambdaQuery()
                .eq(SnTag::getUserId, userId)
                .eq(SnTag::getDeleted, 0)
                .orderByAsc(SnTag::getSort)
                .orderByDesc(SnTag::getCreateTime)
                .list();
    }

}

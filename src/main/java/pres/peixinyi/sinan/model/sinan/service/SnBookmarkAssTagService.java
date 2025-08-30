package pres.peixinyi.sinan.model.sinan.service;

import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import pres.peixinyi.sinan.model.sinan.mapper.SnBookmarkAssTagMapper;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmarkAssTag;

import java.util.Date;

@Service
public class SnBookmarkAssTagService extends ServiceImpl<SnBookmarkAssTagMapper, SnBookmarkAssTag> {

    /**
     * 删除书签的所有标签关联（逻辑删除）
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @return true 删除成功，false 删除失败
     */
    public boolean deleteBookmarkTagAssociations(String bookmarkId, String userId) {
        return lambdaUpdate()
                .eq(SnBookmarkAssTag::getBookmarkId, bookmarkId)
                .eq(SnBookmarkAssTag::getUserId, userId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .set(SnBookmarkAssTag::getDeleted, 1)
                .set(SnBookmarkAssTag::getUpdateTime, new Date())
                .update();
    }

    /**
     * 更新书签的标签关联
     * 先删除原有关联，再添加新的关联
     *
     * @param bookmarkId 书签ID
     * @param userId     用户ID
     * @param newTagIds  新的标签ID列表
     */
    public void updateBookmarkTagAssociations(String bookmarkId, String userId, List<String> newTagIds) {
        // 先删除原有的标签关联
        deleteBookmarkTagAssociations(bookmarkId, userId);

        // 添加新的标签关联
        if (newTagIds != null && !newTagIds.isEmpty()) {
            for (String tagId : newTagIds) {
                SnBookmarkAssTag assTag = new SnBookmarkAssTag();
                assTag.setUserId(userId);
                assTag.setBookmarkId(bookmarkId);
                assTag.setTagId(tagId);
                assTag.setCreateTime(new Date());
                assTag.setUpdateTime(new Date());
                assTag.setDeleted(0);
                save(assTag);
            }
        }
    }

    public void deleteByTagId(String id) {
        lambdaUpdate()
                .eq(SnBookmarkAssTag::getTagId, id)
                .set(SnBookmarkAssTag::getDeleted, 1)
                .set(SnBookmarkAssTag::getUpdateTime, new Date())
                .update();
    }

    /**
     * 根据书签ID获取关联的标签ID列表
     *
     * @param bookmarkId 书签ID
     * @return 标签ID列表
     */
    public List<String> getTagIdsByBookmarkId(String bookmarkId) {
        return lambdaQuery()
                .eq(SnBookmarkAssTag::getBookmarkId, bookmarkId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list()
                .stream()
                .map(SnBookmarkAssTag::getTagId)
                .toList();
    }

    /**
     * 获取用户所有书签的标签关联关系
     *
     * @param userId 用户ID
     * @return 书签ID -> 标签ID列表的映射
     */
    public java.util.Map<String, List<String>> getBookmarkTagMap(String userId) {
        List<SnBookmarkAssTag> associations = lambdaQuery()
                .eq(SnBookmarkAssTag::getUserId, userId)
                .eq(SnBookmarkAssTag::getDeleted, 0)
                .list();

        return associations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    SnBookmarkAssTag::getBookmarkId,
                    java.util.stream.Collectors.mapping(
                        SnBookmarkAssTag::getTagId,
                        java.util.stream.Collectors.toList()
                    )
                ));
    }
}

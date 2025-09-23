package pres.peixinyi.sinan.model.sinan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.model.sinan.entity.SnShareSpaceAssUser;
import pres.peixinyi.sinan.model.sinan.mapper.SnShareSpaceAssUserMapper;

import java.util.List;

@Service
public class SnShareSpaceAssUserService extends ServiceImpl<SnShareSpaceAssUserMapper, SnShareSpaceAssUser> {

    public List<SnShareSpaceAssUser> getBySpaceId(String spaceId) {
        return lambdaQuery().eq(SnShareSpaceAssUser::getSpaceId, spaceId).list();
    }

    public IPage<SnShareSpaceAssUser> getBySpaceId(Integer page, Integer limit, String spaceId) {
        return lambdaQuery().eq(SnShareSpaceAssUser::getSpaceId, spaceId).page(new Page<>(page, limit));
    }

    public void removeCollectionUsers(@NotNull(message = "空间ID不能为空") String spaceId, @NotNull(message = "用户ID不能为空") String userId) {
        lambdaUpdate().eq(SnShareSpaceAssUser::getSpaceId, spaceId).eq(SnShareSpaceAssUser::getUserId, userId).remove();
    }

    public boolean isAlreadyCollected(String spaceId, String userId) {
        return lambdaQuery()
                .eq(SnShareSpaceAssUser::getSpaceId, spaceId)
                .eq(SnShareSpaceAssUser::getUserId, userId)
                .count() > 0;
    }

    public List<SnShareSpaceAssUser> getByUserId(String currentUserId) {
        return lambdaQuery().eq(SnShareSpaceAssUser::getUserId, currentUserId).list();
    }

    public boolean isCollection(String spaceId, String currentUserId) {
        return lambdaQuery()
                .eq(SnShareSpaceAssUser::getSpaceId, spaceId)
                .eq(SnShareSpaceAssUser::getUserId, currentUserId)
                .count() > 0;
    }

    public List<String> getSpaceIdsByUserId(String userId) {
        return lambdaQuery().eq(SnShareSpaceAssUser::getUserId, userId)
                .select(SnShareSpaceAssUser::getSpaceId)
                .list().stream().map(SnShareSpaceAssUser::getSpaceId).toList();
    }
}

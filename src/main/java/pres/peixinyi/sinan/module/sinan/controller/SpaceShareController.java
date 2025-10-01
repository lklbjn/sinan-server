package pres.peixinyi.sinan.module.sinan.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.module.sinan.config.SinanServerProperty;
import pres.peixinyi.sinan.dto.request.CollectionSpaceReq;
import pres.peixinyi.sinan.dto.request.RemoveCollectionUserReq;
import pres.peixinyi.sinan.dto.request.ShareSpaceUpdateReq;
import pres.peixinyi.sinan.dto.response.CollectionUserInfo;
import pres.peixinyi.sinan.dto.response.SpaceResp;
import pres.peixinyi.sinan.module.sinan.entity.SnShareSpaceAssUser;
import pres.peixinyi.sinan.module.sinan.entity.SnSpace;
import pres.peixinyi.sinan.module.rbac.entity.SnUser;
import pres.peixinyi.sinan.module.sinan.service.SnShareSpaceAssUserService;
import pres.peixinyi.sinan.module.sinan.service.SnSpaceService;
import pres.peixinyi.sinan.module.rbac.service.SnUserService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空间分享控制层
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/27 14:39
 * @Version : 0.0.0
 */
@RestController
@RequestMapping("/space-share")
public class SpaceShareController {

    @Resource
    SnUserService snUserService;
    @Resource
    SnSpaceService snSpaceService;
    @Resource
    SnShareSpaceAssUserService snShareSpaceAssUserService;
    @Resource
    SinanServerProperty sinanServerProperty;


    /**
     * 修改空间分享
     *
     * @param req
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 14:49 2025/8/27
     */
    @PatchMapping("/update")
    public Result<String> updateShare(@RequestBody @Valid ShareSpaceUpdateReq req) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(req.getSpaceId());
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (!space.getUserId().equals(userId)) {
            return Result.error("无权限操作");
        }
        snSpaceService.updateShare(req);
        return Result.success("修改成功");
    }

    /**
     * 获取分享链接
     *
     * @param spaceId
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 15:47 2025/8/27
     */
    @GetMapping("/share-url")
    public Result<String> shareUrl(@RequestParam("spaceId") String spaceId) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(spaceId);
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (!space.getUserId().equals(userId)) {
            return Result.error("无权限操作");
        }
        if (!space.getShare()) {
            return Result.error("空间未开启分享");
        }
        String baseUrl = sinanServerProperty.getBaseUrl();
        String format = String.format("%s/collection/space/%s?k=%s", baseUrl, spaceId, space.getShareKey());
        return Result.success(format);
    }


    /**
     * 获取收藏用户信息
     *
     * @param spaceId
     * @return pres.peixinyi.sinan.common.Result<java.util.List<pres.peixinyi.sinan.dto.response.CollectionUserInfo>>
     * @author peixinyi
     * @since 14:54 2025/8/27
     */
    @GetMapping("/collection-users")
    public Result<Page<CollectionUserInfo>> get(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                @RequestParam(value = "size", defaultValue = "10") Integer size,
                                                @RequestParam("spaceId") String spaceId) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(spaceId);
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (!space.getUserId().equals(userId)) {
            return Result.error("无权限操作");
        }
        IPage<SnShareSpaceAssUser> users = snShareSpaceAssUserService.getBySpaceId(page, size, spaceId);
        if (users.getTotal() == 0) {
            return Result.success(new Page<>(page, size));

        }
        Map<String, Date> userIdMap = users.getRecords().stream().collect(Collectors.toMap(SnShareSpaceAssUser::getUserId, SnShareSpaceAssUser::getCreateTime, (v1, v2) -> v1));
        List<String> userIds = users.getRecords().stream().map(SnShareSpaceAssUser::getUserId).toList();
        List<SnUser> snUsers = snUserService.listByIds(userIds);
        List<CollectionUserInfo> collectionUserInfos = CollectionUserInfo.from(snUsers, userIdMap::get);
        Page<CollectionUserInfo> result = new Page<>(page, size, users.getTotal());
        result.setRecords(collectionUserInfos);
        return Result.success(result);
    }

    /**
     * 移除收藏用户
     *
     * @param req
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 15:11 2025/8/27
     */
    @DeleteMapping("/remove")
    public Result<String> remove(@RequestBody RemoveCollectionUserReq req) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(req.getSpaceId());
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (!space.getUserId().equals(userId)) {
            return Result.error("无权限操作");
        }
        snShareSpaceAssUserService.removeCollectionUsers(req.getSpaceId(), req.getUserId());
        return Result.success("移除成功");
    }


    /**
     * 收藏空间
     *
     * @param req
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 15:24 2025/8/27
     */
    @PostMapping("/collect")
    public Result<String> collectSpace(@RequestBody CollectionSpaceReq req) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(req.getSpaceId());
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (Boolean.FALSE.equals(space.getShare())) {
            return Result.error("空间未开启分享或已经关闭分享");
        }
        if (space.getUserId().equals(userId)) {
            return Result.error("不能收藏自己的空间");
        }
        if (!space.getShareKey().equals(req.getPassword())) {
            return Result.error("分享密码错误");
        }
        boolean alreadyCollected = snShareSpaceAssUserService.isAlreadyCollected(req.getSpaceId(), userId);
        if (alreadyCollected) {
            return Result.error("已经收藏该空间");
        }
        SnShareSpaceAssUser assUser = new SnShareSpaceAssUser();
        assUser.setSpaceId(req.getSpaceId());
        assUser.setUserId(userId);
        assUser.setCreateTime(new Date());
        snShareSpaceAssUserService.save(assUser);
        return Result.success("收藏成功");
    }

    /**
     * 取消收藏
     *
     * @param spaceId
     * @return pres.peixinyi.sinan.common.Result<java.lang.String>
     * @author peixinyi
     * @since 15:38 2025/8/27
     */
    @DeleteMapping("/cancel-collect")
    public Result<String> cancelCollectSpace(@RequestParam(value = "spaceId") String spaceId) {
        String userId = StpUtil.getLoginIdAsString();
        SnSpace space = snSpaceService.getById(spaceId);
        if (space == null) {
            return Result.error("空间不存在");
        }
        if (space.getUserId().equals(userId)) {
            return Result.error("不能取消收藏自己的空间");
        }
        boolean alreadyCollected = snShareSpaceAssUserService.isAlreadyCollected(spaceId, userId);
        if (!alreadyCollected) {
            return Result.error("未收藏该空间");
        }
        snShareSpaceAssUserService.removeCollectionUsers(spaceId, userId);
        return Result.success("取消收藏成功");
    }

    /**
     * 分页获取用户的收藏的空间
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页大小，默认10
     * @param search   搜索关键字（可选）
     * @return 分页空间列表
     */
    @GetMapping("/user-spaces")
    public Result<IPage<SpaceResp>> getUserCollectionSpaces(
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
        List<SnShareSpaceAssUser> shareSpaceAssUsers = snShareSpaceAssUserService.getByUserId(currentUserId);
        List<String> spaceIds = shareSpaceAssUsers.stream().map(SnShareSpaceAssUser::getSpaceId).toList();

        IPage<SnSpace> spacePage = snSpaceService.getUserSpacesPage(pageNum, pageSize, search, spaceIds);
        IPage<SpaceResp> convert = spacePage.convert(SpaceResp::from);
        return Result.success(convert);
    }

}

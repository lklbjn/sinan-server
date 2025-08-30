package pres.peixinyi.sinan.dto.response;

import lombok.Data;
import pres.peixinyi.sinan.model.rbac.entity.SnUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 *
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/27 14:53
 * @Version : 0.0.0
 */
@Data
public class CollectionUserInfo {

    private String userId;

    private String avatar;

    private String name;

    private String collectedAt;

    public static CollectionUserInfo from(SnUser snUser, Function<String, Date> function) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        CollectionUserInfo info = new CollectionUserInfo();
        info.setUserId(snUser.getId());
        info.setAvatar(snUser.getAvatar());
        info.setName(snUser.getName());
        info.setCollectedAt(sdf.format(function.apply(snUser.getId())));
        return info;
    }

    public static List<CollectionUserInfo> from(List<SnUser> snUsers, Function<String, Date> function) {
        return snUsers.stream().map(snUser -> from(snUser, function)).toList();
    }

}

package pres.peixinyi.sinan.module.rbac.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.module.rbac.entity.SnPassKey;
import pres.peixinyi.sinan.module.rbac.exception.UserRuntionException;
import pres.peixinyi.sinan.module.rbac.mapper.SnPassKeyMapper;

import java.util.List;

@Service
public class SnPassKeyService extends ServiceImpl<SnPassKeyMapper, SnPassKey> {

    // 根据用户 ID 获取该用户的所有凭据信息
    public List<SnPassKey> findAllByUserId(String userId) {
        return lambdaQuery().in(SnPassKey::getUserId, userId).list();
    }

    public SnPassKey getByCredentialId(ByteArray credentialId) {
        return lambdaQuery().eq(SnPassKey::getCredentialId, credentialId).one();
    }

    public ByteArray getUserHandleByUserId(String userId) {
        return lambdaQuery().eq(SnPassKey::getUserId, userId)
                .oneOpt().map(SnPassKey::getUserHandle).orElse(null);
    }

    public String getUserIdByUserHandle(ByteArray userHandle) {
        return lambdaQuery().eq(SnPassKey::getUserHandle, userHandle)
                .oneOpt().map(SnPassKey::getUserId).orElseThrow(() -> new UserRuntionException("用户不存在"));
    }
}

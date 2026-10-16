package com.simple.common.auth.client.exchange.sync.receiver;

import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实例同步接收端点（/internal/sync）
 * <p>
 * 接收授权中心投递引擎的变更载荷并落地本实例缓存，HTTP 200 即送达 ACK。
 * 内网端点不鉴权（设计裁定：内网拓扑既定，内网若破则一切通道凭据均可被读取），
 * 鉴权过滤器经 AbsClientAuthConfig 统一放行 /internal/sync/**，并由部署侧 nginx 不代理清单隔离外网。
 * 服务间内部契约端点，无用户会话上下文，不挂 @HasAuthority。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "实例同步接收")
@RequestMapping("internal/sync")
@RestController
public class InternalSyncController {

    @Autowired
    private SyncPayloadApplier syncPayloadApplier;

    /**
     * 接收同步载荷（SECRET/PERMISSION/LOGOUT，类型在载荷内）
     *
     * @param payload 同步载荷
     * @return 受理结果（200=ACK，落地失败走业务异常非 200 由中心重投）
     */
    @PostMapping
    @Operation(summary = "接收变更同步载荷")
    public R<Object> receive(@RequestBody SyncPayload payload) {
        syncPayloadApplier.apply(payload);
        return R.ok();
    }

}

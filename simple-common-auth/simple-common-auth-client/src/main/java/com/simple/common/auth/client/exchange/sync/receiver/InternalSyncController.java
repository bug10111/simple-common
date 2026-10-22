package com.simple.common.auth.client.exchange.sync.receiver;

import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.HttpServletUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 实例同步接收端点（/internal/sync）
 * <p>
 * 接收授权中心投递引擎的变更载荷并落地本实例缓存，HTTP 200 即送达 ACK。
 * 内网端点不鉴权（设计裁定：内网拓扑既定，内网若破则一切通道凭据均可被读取），
 * 鉴权过滤器经 AbsClientAuthConfig 统一放行 /internal/sync/**，并由部署侧 nginx 不代理清单隔离外网。
 * 服务间内部契约端点，无用户会话上下文，不挂 @HasAuthority。
 * </p>
 * <p>
 * 纵深防御（可选，不依赖网络拓扑）：配置 {@code simple.auth.internal-sync-token} 后，
 * 回调必须携带 {@code X-Internal-Token} 请求头且与配置值恒定时间匹配，否则 401 拒绝；
 * 未配置保持既有内网隔离形态（启动输出一次性告警，提示当前仅靠拓扑隔离）。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "实例同步接收")
@RequestMapping("internal/sync")
@RestController
public class InternalSyncController {

    /**
     * 内部同步令牌请求头
     */
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final SyncPayloadApplier syncPayloadApplier;

    private final AuthProperties authProperties;

    public InternalSyncController(SyncPayloadApplier syncPayloadApplier, AuthProperties authProperties) {
        this.syncPayloadApplier = syncPayloadApplier;
        this.authProperties = authProperties;
    }

    /**
     * 接收同步载荷（SECRET/PERMISSION/LOGOUT，类型在载荷内）
     *
     * @param payload 同步载荷
     * @return 受理结果（200=ACK，落地失败走业务异常非 200 由中心重投）
     */
    @PostMapping
    @Operation(summary = "接收变更同步载荷")
    public R<Object> receive(@RequestBody SyncPayload payload) {
        verifyInternalToken();
        syncPayloadApplier.apply(payload);
        return R.ok();
    }

    /**
     * 内部令牌校验（可选纵深防御）：配置了 internal-sync-token 即强制校验，
     * 恒定时间比较防时序侧信道；未配置保持内网隔离形态（一次性告警）。
     */
    private void verifyInternalToken() {
        String configured = authProperties.getInternalSyncToken();
        if (configured == null || configured.isEmpty()) {
            log.warn("/internal/sync 未配置共享令牌（simple.auth.internal-sync-token），当前仅依赖内网隔离，建议配置以纵深防御");
            return;
        }
        String provided = HttpServletUtils.getRequest().getHeader(INTERNAL_TOKEN_HEADER);
        boolean matched = provided != null && MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            log.warn("/internal/sync 收到无效内部令牌请求，已拒绝");
            throw new IllegalStateException("内部同步令牌校验失败");
        }
    }

}

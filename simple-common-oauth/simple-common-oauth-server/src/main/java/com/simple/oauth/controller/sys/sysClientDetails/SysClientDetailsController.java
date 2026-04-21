package com.simple.oauth.controller.sys.sysClientDetails;

import com.simple.common.core.response.R;
import com.simple.oauth.common.dto.sysClientDetails.*;
import com.simple.oauth.common.service.sysClientDetails.SysClientDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客户端信息(sys_client_details)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "客户端管理")
@RequestMapping("sys/client")
@RestController
public class SysClientDetailsController {

    @Autowired
    private SysClientDetailsService sysClientDetailsService;

    @PostMapping("page")
    @Operation(summary = "分页查询")
    public R<List<SysClientDetailsPageResponse>> page(@RequestBody FindAllSysClientDetailsRequest request) {
        return R.ok(sysClientDetailsService.findAll(request).getRecords());
    }

    @GetMapping("findById")
    @Operation(summary = "根据ID查询")
    public R<SysClientDetailsInfoResponse> findById(@RequestParam String id) {
        return R.ok(sysClientDetailsService.findById(id));
    }

    @PostMapping("create")
    @Operation(summary = "新增")
    public R<Object> create(@RequestBody CreateSysClientDetailsRequest request) {
        sysClientDetailsService.save(request);
        return R.ok();
    }

    @PostMapping("update")
    @Operation(summary = "修改")
    public R<Object> update(@RequestBody UpdateSysClientDetailsRequest request) {
        sysClientDetailsService.updateById(request);
        return R.ok();
    }

    @PostMapping("create/key")
    @Operation(summary = "生成密钥信息")
    public R<Map<String, Object>> createKey() {
        return R.ok(sysClientDetailsService.createKey());
    }

    @PostMapping("reset/hmac")
    @Operation(summary = "重置HMAC密钥")
    public R<Object> resetHmacKey(@RequestParam String id) {
        sysClientDetailsService.regenerateSecret(id, new String[]{"hsKey"});
        return R.ok();
    }

    @PostMapping("reset/rsa")
    @Operation(summary = "重置RSA密钥")
    public R<Object> resetRsaKey(@RequestParam String id) {
        sysClientDetailsService.regenerateSecret(id, new String[]{"rsaPublic", "rsaPrivate"});
        return R.ok();
    }

    @PostMapping("update/hmac")
    @Operation(summary = "更新HMAC密钥")
    public R<Object> updateHmacKey(@RequestParam String id, @RequestParam String hmacKey) {
        sysClientDetailsService.updateSecret(id, hmacKey, null, null);
        return R.ok();
    }

    @PostMapping("update/rsa")
    @Operation(summary = "更新RSA密钥")
    public R<Object> updateRsaKey(@RequestParam String id, 
                                 @RequestParam String publicKey, 
                                 @RequestParam(required = false) String privateKey) {
        sysClientDetailsService.updateSecret(id, null, publicKey, privateKey);
        return R.ok();
    }

    @PostMapping("delete")
    @Operation(summary = "删除")
    public R<Object> delete(@RequestParam String id) {
        sysClientDetailsService.deleteById(id);
        return R.ok();
    }
}
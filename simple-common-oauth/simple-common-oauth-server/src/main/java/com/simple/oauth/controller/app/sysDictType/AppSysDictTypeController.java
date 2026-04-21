package com.simple.oauth.controller.app.sysDictType;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysDictData.*;
import com.simple.oauth.common.service.sysDictData.SysDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典类型(sys_dict_type)控制层
 *
 * @author qty
 */
@Slf4j
@Tag(name = "字典")
@RequestMapping("app/sys-dict-types")
@RestController
public class AppSysDictTypeController {

    @Autowired
    private SysDictDataService sysDictDataService;

    @PostMapping("labelList")
    @Operation(summary = "获取多个类型的字典数据")
    public R<Map<String,List<SysDictDatasResponse>>> labelList(@RequestBody List<String> dictValues) {
        return R.ok(sysDictDataService.labelList(dictValues));
    }

}


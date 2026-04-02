package com.simple.oauth.controller.sys.sysDictType;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.common.annotation.CsrfDefense;
import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.dto.sysDictData.*;
import com.simple.oauth.common.dto.sysDictType.*;
import com.simple.oauth.common.service.sysDictData.SysDictDataService;
import com.simple.oauth.common.service.sysDictType.SysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典类型(sys_dict_type)控制层
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Tag(name = "字典")
@RequestMapping("auth/sys-dict-types")
@RestController
public class SysDictTypeController {

    @Autowired
    private SysDictTypeService sysDictTypeService;

    @Autowired
    private SysDictDataService sysDictDataService;

    @GetMapping
    @Operation(summary = "分页查询字典类型")
    @HasAuthority("oauth")
    public R<IPage<SysDictTypePageResponse>> list(@ParameterObject FindAllSysDictTypeRequest findAllRequest) {
        return R.ok(sysDictTypeService.findAll(findAllRequest));
    }

    @PostMapping
    @Operation(summary = "创建字典类型")
    @CsrfDefense
    @HasAuthority("oauth")
    public R<String> create(@RequestBody @Validated CreateSysDictTypeRequest createRequest) {
        return R.ok(sysDictTypeService.save(createRequest));
    }

    @GetMapping("{id}")
    @Operation(summary = "查询单个字典类型")
    @HasAuthority("oauth")
    public R<SysDictTypeInfoResponse> findOne(@PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysDictTypeService.findById(id));
    }

    @PutMapping("{id}")
    @Operation(summary = "更新单个字典类型")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String id, @RequestBody @Validated UpdateSysDictTypeRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysDictTypeService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除字典类型")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        sysDictTypeService.deleteByIds(ids);
        return R.ok();
    }

    @GetMapping("{type}/data")
    @Operation(summary = "分页查询字典数据（不能用于业务的字典获取，这里只能用在字典管理的查询）")
    @HasAuthority("oauth")
    public R<IPage<SysDictDataPageResponse>> dataList(@PathVariable String type, @ParameterObject FindAllSysDictDataRequest findAllRequest) {
        AssertUtils.isTrue(type.equals(findAllRequest.getDictType()), "类型不一致");
        return R.ok(sysDictDataService.findAll(findAllRequest));
    }

    @PostMapping("labelList")
    @Operation(summary = "获取多个类型的字典数据")
    @HasAuthority("oauth")
    public R<Map<String,List<SysDictDatasResponse>>> labelList(@RequestBody List<String> dictValues) {
        return R.ok(sysDictDataService.labelList(dictValues));
    }

    @PostMapping("{type}")
    @Operation(summary = "创建字典数据")
    @HasAuthority("oauth")
    public R<String> create(@PathVariable String type, @RequestBody @Validated CreateSysDictDataRequest createRequest) {
        AssertUtils.isTrue(type.equals(createRequest.getDictType()), "类型不一致");
        return R.ok(sysDictDataService.save(createRequest));
    }

    @GetMapping("{type}/{id}")
    @Operation(summary = "查询单个字典数据")
    @HasAuthority("oauth")
    public R<SysDictDataInfoResponse> findOne(@PathVariable String type, @PathVariable String id) {
        AssertUtils.notEmpty(id, "主键不能为空");
        return R.ok(sysDictDataService.findById(id));
    }

    @PutMapping("{type}/{id}")
    @Operation(summary = "更新单个字典数据")
    @HasAuthority("oauth")
    public R<Object> update(@PathVariable String type, @PathVariable String id, @RequestBody @Validated UpdateSysDictDataRequest updateRequest) {
        AssertUtils.isTrue(updateRequest.getId().equals(id), "请求内容的ID与路径ID不同");
        sysDictDataService.updateById(updateRequest);
        return R.ok();
    }

    @DeleteMapping("{type}")
    @Operation(summary = "删除字典数据")
    @HasAuthority("oauth")
    public R<Object> deleteByIds(@PathVariable String type, @RequestBody List<String> ids) {
        AssertUtils.notEmpty(ids, "主键不能为空");
        sysDictDataService.deleteByIds(ids);
        return R.ok();
    }

}


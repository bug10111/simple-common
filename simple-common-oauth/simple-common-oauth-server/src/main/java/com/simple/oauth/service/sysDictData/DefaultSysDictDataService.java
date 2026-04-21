package com.simple.oauth.service.sysDictData;

import com.simple.common.core.utils.BeanUtils;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.dto.sysDictData.*;
import com.simple.oauth.common.entity.sysDictData.SysDictData;
import com.simple.oauth.common.manager.dict.DictCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.service.sysDictData.SysDictDataService;
import com.simple.oauth.common.view.sysDictData.SysDictDataView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典数据(sys_dict_data)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysDictDataService implements SysDictDataService {

    @Autowired
    private SysDictDataView sysDictDataView;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private DictCacheManager dictCacheManager;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysDictDataPageResponse> findAll(FindAllSysDictDataRequest findAllRequest) {
        var pageInfo = sysDictDataView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysDictDataPageResponse.class));
    }

    @Override
    public SysDictDataInfoResponse findById(String id) {
        var sysDictData = sysDictDataView.findById(id);
        return BeanUtils.copyProperties(sysDictData, SysDictDataInfoResponse.class);
    }

    @Override
    public String save(CreateSysDictDataRequest createRequest) {
        var entity = BeanUtils.copyProperties(createRequest, SysDictData.class);
        SysDictData one = sysDictDataView.findOne(new FindOneSysDictDataRequest().setDictValue(entity.getDictValue()).setDictType(entity.getDictType()));
        AssertUtils.isTrue(ObjUtil.isEmpty(one), "标签已存在");

        sysDictDataView.saveOrUpdate(entity);

        if (oauthProperties.getDictCache()) {
            stringRedisTemplate.opsForHash().put(oauthProperties.getDictKey(entity.getDictType()), entity.getDictValue(), entity.getDictLabel());
        }
        return entity.getId();
    }

    @Override
    public String updateById(UpdateSysDictDataRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysDictData.class);
        SysDictData byId = sysDictDataView.findById(entity.getId());
        AssertUtils.notEmpty(byId, "字典数据不存在");

        SysDictData one = sysDictDataView.findOne(new FindOneSysDictDataRequest().setDictValue(byId.getDictValue()).setDictType(entity.getDictType()),
                                                  new FindOneSysDictDataRequest().setId(entity.getId()));
        AssertUtils.isTrue(ObjUtil.isEmpty(one), "字典数据已存在");

        sysDictDataView.saveOrUpdate(entity);

        if (oauthProperties.getDictCache()) {
            stringRedisTemplate.opsForHash().put(oauthProperties.getDictKey(entity.getDictType()), entity.getDictValue(), entity.getDictLabel());
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void deleteByIds(List<String> ids) {
        ids.forEach(s -> {
            SysDictDataInfoResponse byId = findById(s);
            if (byId != null && oauthProperties.getDictCache()) {
                stringRedisTemplate.opsForHash().delete(oauthProperties.getDictKey(byId.getDictType()), byId.getDictValue());
            }
        });
        sysDictDataView.deleteByIds(ids);
    }

    @Override
    public Map<String, List<SysDictDatasResponse>> labelList(List<String> dictValues) {
        Map<String, List<SysDictDatasResponse>> map = new ConcurrentHashMap<>();
        dictValues.forEach(s -> {
            if (oauthProperties.getDictCache()) {
                List<SysDictDatasResponse> list = dictCacheManager.get(s);
                if (ObjUtil.isNotEmpty(list)) {
                    map.put(s, list);
                }
            } else {
                List<SysDictDatasResponse> list = sysDictDataView.labelList(s).stream().map(sysDictData -> BeanUtils.copyProperties(sysDictData, SysDictDatasResponse.class)).toList();
                if (ObjUtil.isNotEmpty(list)) {
                    map.put(s, list);
                }
            }
        });
        return map;
    }
}


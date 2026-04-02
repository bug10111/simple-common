package com.simple.oauth.manager.dict;

import com.simple.oauth.common.dto.sysDictData.SysDictDatasResponse;
import com.simple.oauth.common.entity.sysDictData.SysDictData;
import com.simple.oauth.common.manager.dict.DictCacheManager;
import com.simple.oauth.common.properties.OauthProperties;
import com.simple.oauth.common.view.sysDictData.SysDictDataView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class RedisDictCacheManager implements DictCacheManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OauthProperties oauthProperties;

    @Autowired
    private SysDictDataView sysDictDataView;

    @Override
    public List<SysDictDatasResponse> get(String type) {

        //初始化数据
        List<SysDictDatasResponse> resultList = new ArrayList<>();

        //        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
        //            for (String type : types) {
        //                connection.hashCommands().hGetAll(SerializeUtils.serialize(type));
        //            }
        //            return null;
        //        });

        // 批量获取数据

        //获取某一个type的缓存字典
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(oauthProperties.getDictKey(type));

        //空，开始查询db
        if (entries.isEmpty()) {
            synchronized (this) {
                entries = redisTemplate.opsForHash().entries(oauthProperties.getDictKey(type));
                if (entries.isEmpty()) {

                    //获取db字典
                    List<SysDictData> sysDictData = sysDictDataView.labelList(type);
                    entries = sysDictData.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));

                    if (!entries.isEmpty()) {
                        redisTemplate.opsForHash().putAll(oauthProperties.getDictKey(type), entries);
                    }
                }
            }
        }

        //采集数据
        entries.forEach((key, value) -> {
            SysDictDatasResponse response = new SysDictDatasResponse();
            response.setDictValue(key.toString());
            response.setDictLabel(value.toString());
            resultList.add(response);
        });

        return resultList;
    }
}

package com.simple.common.xxljob.manager;

import com.simple.common.core.common.entity.HttpErrorRecord;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;
import com.simple.common.xxljob.common.enums.XxlJobRequestUrl;
import com.simple.common.xxljob.common.manager.XxlJobManager;
import com.simple.common.xxljob.config.XxlJobConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class DefaultXxlJobManager implements XxlJobManager {

    @Autowired
    private XxlJobConfig xxlJobConfig;

    @Override
    public String create(CreateXxlJobTaskRequest request) {
        HttpErrorRecord record = new HttpErrorRecord();
        Optional<String> post = HttpUtils.post(XxlJobRequestUrl.ADD.url(xxlJobConfig), JsonUtils.toJsonStr(request), xxlJobConfig.getRequestTimeout(),
                                               String.class, record);
        post.orElseThrow(record::getException);
        return post.get();
    }

    @Override
    public void update(UpdateXxlJobTaskRequest request) {
        HttpUtils.post(XxlJobRequestUrl.UPDATE.url(xxlJobConfig), JsonUtils.toJsonStr(request), xxlJobConfig.getRequestTimeout());
    }

    @Override
    public void delete(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(id));
        HttpUtils.post(XxlJobRequestUrl.DELETE.url(xxlJobConfig), map, xxlJobConfig.getRequestTimeout());
    }

    @Override
    public void start(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(id));
        HttpUtils.post(XxlJobRequestUrl.START.url(xxlJobConfig), map, xxlJobConfig.getRequestTimeout());
    }

    @Override
    public void end(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(id));
        HttpUtils.post(XxlJobRequestUrl.END.url(xxlJobConfig), map, xxlJobConfig.getRequestTimeout());
    }

    @Override
    public void trigger(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(id));
        HttpUtils.post(XxlJobRequestUrl.TRIGGER.url(xxlJobConfig), map, xxlJobConfig.getRequestTimeout());
    }
}

package com.simple.common.xxljob.service;

import com.simple.common.xxljob.common.dto.CreateXxlJobTaskRequest;
import com.simple.common.xxljob.common.dto.UpdateXxlJobTaskRequest;
import com.simple.common.xxljob.common.manager.XxlJobManager;
import com.simple.common.xxljob.common.service.XxlJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultXxlJobService implements XxlJobService {

    @Autowired
    private XxlJobManager xxlJobManager;

    @Override
    public String create(CreateXxlJobTaskRequest request) {
        return xxlJobManager.create(request);
    }

    @Override
    public void update(UpdateXxlJobTaskRequest request) {
        xxlJobManager.update(request);
    }

    @Override
    public void delete(Integer id) {
        xxlJobManager.delete(id);
    }

    @Override
    public void start(Integer id) {
        xxlJobManager.start(id);
    }

    @Override
    public void end(Integer id) {
        xxlJobManager.end(id);
    }

    @Override
    public void trigger(Integer id) {
        xxlJobManager.trigger(id);
    }
}

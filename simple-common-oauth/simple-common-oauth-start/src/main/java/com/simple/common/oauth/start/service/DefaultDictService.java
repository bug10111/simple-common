package com.simple.common.oauth.start.service;

import com.simple.common.oauth.start.common.dto.SysDictDatasResponse;
import com.simple.common.oauth.start.common.manager.DictManager;
import com.simple.common.oauth.start.common.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultDictService implements DictService {

    @Autowired
    private DictManager dictManager;

    @Override
    public Map<String, List<SysDictDatasResponse>> getDict(List<String> type) {
        return dictManager.getDict(type);
    }
}

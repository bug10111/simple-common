package com.simple.oauth.view.sysConfig;

import com.simple.common.core.utils.AssertUtils;
import com.simple.oauth.common.entity.sysConfig.SysConfig;
import com.simple.oauth.common.view.sysConfig.SysConfigView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 系统配置(sys_config)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysConfigView implements SysConfigView {

    @Autowired
    private SysConfigRepository repository;

    @Override
    public SysConfig findById(String id, Boolean allowEmpty) {
        SysConfig sysConfig = repository.selectById(id);
        if (!allowEmpty && sysConfig == null) {
            AssertUtils.errorParams("主键为[{}]的数据为空", id);
        }
        return sysConfig;
    }

    @Override
    public void save(SysConfig sysConfig) {
        repository.insert(sysConfig);
    }
}


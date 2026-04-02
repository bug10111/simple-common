package com.simple.common.oauth.start.service;

import com.simple.common.oauth.start.common.dto.ApiSysClientDetailsResponse;
import com.simple.common.oauth.start.common.manager.ClientDetailsManager;
import com.simple.common.oauth.start.common.service.ClientDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultClientDetailsService implements ClientDetailsService {

    @Autowired
    private ClientDetailsManager clientDetailsManager;

    @Override
    public List<ApiSysClientDetailsResponse> list(String server) {
        return clientDetailsManager.list(server);
    }
}

package com.simple.common.oauth.start.manager;

import com.simple.common.core.common.entity.HttpRecord;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.function.HttpRecordFunction;
import com.simple.common.core.utils.HttpRequestUtils;
import com.simple.common.oauth.start.common.dto.SysDictDatasResponse;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import com.simple.common.oauth.start.common.manager.DictManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import com.simple.common.oauth.start.utils.HeadUtils;
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
public class DefaultDictManager implements DictManager {

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<SysDictDatasResponse>> getDict(List<String> type) {
        HttpRecord post = HttpRequestUtils.post(oauthStartProperties.getUrl(OauthUrl.SELECT_DICT), HeadUtils.getHead(), type, oauthStartProperties.getTimeOut());
        return post.get(Map.class, DefaultException::new);
    }

}

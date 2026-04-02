package com.simple.common.oauth.start.manager;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.core.common.entity.HttpErrorRecord;
import com.simple.common.core.common.entity.HttpRecord;
import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.function.HttpRecordFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpRequestUtils;
import com.simple.common.core.utils.HttpUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.oauth.start.common.dto.ApiSysClientDetailsResponse;
import com.simple.common.oauth.start.common.dto.SysRoleInfoResponse;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import com.simple.common.oauth.start.common.manager.ClientDetailsManager;
import com.simple.common.oauth.start.common.properties.OauthStartProperties;
import com.simple.common.oauth.start.utils.HeadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultClientDetailsManager implements ClientDetailsManager {

    @Autowired
    private OauthStartProperties oauthStartProperties;

    @Override
    public List<ApiSysClientDetailsResponse> list(String server) {
        HttpRecord httpRecord = HttpRequestUtils.get(oauthStartProperties.getUrl(OauthUrl.SELECT_CLIENT) + "/" + server, null, null, oauthStartProperties.getTimeOut());
        R<?> r = httpRecord.get(R.class, body -> {
            R<?> jsonObj = JsonUtils.toJsonObj(body, R.class);
            return new DefaultException(jsonObj.getCode(), jsonObj.getMessage());
        });
        return JsonUtils.toList(r.getData().toString(), ApiSysClientDetailsResponse.class);
    }

}

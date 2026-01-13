package com.simple.common.sms.service;

import cn.hutool.core.date.DateUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.simple.common.core.common.properties.AlibabaProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.entity.sysSmsTemplate.SysSmsTemplate;
import com.simple.common.sms.common.properties.SmsProperties;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import com.simple.common.sms.common.view.sysSmsTemplate.SysSmsTemplateView;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 * Description: 阿里巴巴短信验证码发送
 *
 * @author qty
 */
@Slf4j
@Service("aliSmsService")
public class AliSmsService extends AbsSmsService {

    @Autowired
    private SmsProperties smsProperties;

    @Autowired
    private AlibabaProperties alibabaProperties;

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Autowired
    private SysSmsTemplateView sysSmsTemplateView;

    @Override
    public void sendTemplateParam(String mobile, String sendType, String templateParam, String ip) {

        //获取短信配置
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateView.findByType(sendType);
        AssertUtils.notEmpty(sysSmsTemplate, "短信模板不存在");

        //构建请求对象
        Client client = createClient();
        SendSmsRequest sendSmsRequest = new SendSmsRequest().setPhoneNumbers(mobile)
                                                            .setSignName(sysSmsTemplate.getSignName())
                                                            .setTemplateCode(sysSmsTemplate.getTemplateCode())
                                                            .setTemplateParam(templateParam);

        //初始化返回对象
        SendSmsResponse sendSmsResponse = null;

        //初始化短信信息
        SysSmsCode codeRecord = new SysSmsCode();
        codeRecord.setDate(DateUtil.date().toDateStr());
        codeRecord.setSendType(sendType);
        codeRecord.setCode(templateParam);
        codeRecord.setPhone(mobile);
        codeRecord.setIp(ip);

        //发送短信
        try {
            sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, new RuntimeOptions());
        } catch (Exception e) {
            TeaException error = new TeaException(e.getMessage(), e);
            log.error("短信发送异常：[{}]==>", error.getMessage(), e);
            codeRecord.setReqStatus(Status.ERROR);
            codeRecord.setReqResults(error.getMessage());
            sysSmsCodeView.save(codeRecord);
            AssertUtils.error("短信发送失败");
        }

        //收集阿里云返回信息，校验短信是否发送成功
        if ("OK".equalsIgnoreCase(sendSmsResponse.getBody().getCode())) {
            codeRecord.setReqStatus(Status.OK);
            codeRecord.setReqResults(JsonUtils.toJsonStr(sendSmsResponse));
        } else {
            codeRecord.setReqStatus(Status.ERROR);
            codeRecord.setReqResults(JsonUtils.toJsonStr(sendSmsResponse));
        }
        sysSmsCodeView.save(codeRecord);
    }

    /**
     * 使用AK&SK初始化账号Client
     */
    @SneakyThrows
    protected Client createClient() {
        Config config = new Config().setAccessKeyId(alibabaProperties.getAccessKeyId()).setAccessKeySecret(alibabaProperties.getAccessKeySecret());
        // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
        config.endpoint = smsProperties.getEndpoint();
        return new Client(config);
    }
}

package com.simple.common.sms.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.simple.common.core.common.properties.AlibabaProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.sms.common.entity.sysSmsCode.SysSmsCode;
import com.simple.common.sms.common.entity.sysSmsTemplate.SysSmsTemplate;
import com.simple.common.sms.common.view.sysSmsCode.SysSmsCodeView;
import com.simple.common.sms.common.view.sysSmsTemplate.SysSmsTemplateView;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 * Description: 阿里云短信验证码发送
 *
 * @author qty
 */
@Slf4j
@Service("aliSmsService")
public class AliSmsService extends AbsSmsService {

    /**
     * 网关连接超时(毫秒)
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    /**
     * 网关读超时(毫秒);发送流程持有手机号粒度的锁,读超时必须有限,否则网关挂起会长时间阻塞同一手机号的后续发送
     */
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    @Autowired
    private AlibabaProperties alibabaProperties;

    @Autowired
    private SysSmsCodeView sysSmsCodeView;

    @Autowired
    private SysSmsTemplateView sysSmsTemplateView;

    /**
     * 网关客户端(内部持有连接池),按实例复用;每次发送新建会重复建连且无连接复用
     */
    private volatile Client client;

    @Override
    public void sendTemplateParam(String mobile, String sendType, String templateParam, String ip) {

        //获取短信配置
        SysSmsTemplate sysSmsTemplate = sysSmsTemplateView.findByType(sendType);
        AssertUtils.notEmpty(sysSmsTemplate, "短信模板不存在");

        //构建请求对象
        Client smsClient = createClient();
        SendSmsRequest sendSmsRequest = new SendSmsRequest().setPhoneNumbers(mobile)
                                                            .setSignName(sysSmsTemplate.getSignName())
                                                            .setTemplateCode(sysSmsTemplate.getTemplateCode())
                                                            .setTemplateParam(templateParam);

        //初始化返回对象
        SendSmsResponse sendSmsResponse = null;

        //初始化短信信息，code 落库原文验证码（从模板参数中提取），供验证码校验等值比对
        SysSmsCode codeRecord = new SysSmsCode();
        codeRecord.setDate(cn.hutool.core.date.DateUtil.date().toDateStr());
        codeRecord.setSendType(sendType);
        //模板参数缺失 code 时立即拒绝,避免把 null 验证码落库后在 checkSms 端比对时抛 NPE
        String verifyCode = JSONUtil.parseObj(templateParam).getStr("code");
        AssertUtils.notEmpty(verifyCode, "短信模板参数缺少code");
        codeRecord.setCode(verifyCode);
        codeRecord.setPhone(mobile);
        codeRecord.setIp(ip);

        //发送短信
        try {
            sendSmsResponse = smsClient.sendSmsWithOptions(sendSmsRequest, new com.aliyun.teautil.models.RuntimeOptions()
                    .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
                    .setReadTimeout(READ_TIMEOUT_MILLIS));
        } catch (Exception e) {
            // 直接记录原始异常,不再二次包装丢失异常类型/错误码;落库内容为异常消息(兜底类名),避免 null 落库
            log.error("短信发送异常：[{}]==>", e.getMessage(), e);
            codeRecord.setReqStatus(Status.ERROR);
            codeRecord.setReqResults(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            sysSmsCodeView.save(codeRecord);
            AssertUtils.error("短信发送失败");
        }

        //收集阿里云返回信息，校验短信是否发送成功(响应体可能为 null,不判空会把真实网关结果掩盖成 NPE)
        var smsResponseBody = sendSmsResponse == null ? null : sendSmsResponse.getBody();
        boolean sendSuccess = smsResponseBody != null && "OK".equalsIgnoreCase(smsResponseBody.getCode());
        if (sendSuccess) {
            codeRecord.setReqStatus(Status.OK);
        } else {
            codeRecord.setReqStatus(Status.ERROR);
        }
        codeRecord.setReqResults(JsonUtils.toJsonStr(sendSmsResponse));
        sysSmsCodeView.save(codeRecord);

        //网关业务失败（签名/模板未过审、限流等）时向调用方抛出，携带阿里云原始返回码与描述，避免发送失败被静默掩盖
        if (!sendSuccess) {
            AssertUtils.error("短信发送失败：[{}]-[{}]",
                              smsResponseBody == null ? "无响应体" : smsResponseBody.getCode(),
                              smsResponseBody == null ? "无响应体" : smsResponseBody.getMessage());
        }
    }

    /**
     * 使用AK&SK初始化账号Client(懒加载单例复用,内部持有连接池,重复创建会浪费连接并增加握手开销)
     */
    @SneakyThrows
    protected Client createClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    Config config = new Config()
                            .setAccessKeyId(alibabaProperties.getAccessKeyId())
                            .setAccessKeySecret(alibabaProperties.getAccessKeySecret());
                    // 发送流程持有手机号粒度的锁,必须配置超时,防止网关挂起长时间阻塞锁持有线程
                    config.readTimeout = READ_TIMEOUT_MILLIS;
                    config.connectTimeout = CONNECT_TIMEOUT_MILLIS;
                    // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
                    config.endpoint = smsProperties.getEndpoint();
                    client = new Client(config);
                }
            }
        }
        return client;
    }
}

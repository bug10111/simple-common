# simple-common-sms

## 模块介绍

短信服务模块，阿里云SMS集成，多层防刷机制(IP限流+频次控制+黑名单)。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 短信服务 | `SmsService` | 短信发送接口 |
| 防刷管理 | `SmsAntiFraudManager` | IP限流、频次控制、黑名单 |

## 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-sms</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置阿里云短信（必须）**

```yaml
simple:
  alibaba:
    # 阿里云AccessKey ID（必须配置）
    access-key-id: your_access_key_id
    # 阿里云AccessKey Secret（必须配置）
    access-key-secret: your_access_key_secret
  
  ali:
    sms:
      # 一天内IP发送短信最大次数（默认20）
      ip-send-max: 20
      # 一天内相同手机号发送短信最大次数（默认5）
      phone-send-max: 5
      # 发送最低时间间隔，单位秒（默认60）
      time-inter: 60
      # 验证码超时时间，单位秒（默认300）
      out-time: 300
      # 每次短信验证码允许的错误验证次数（默认3）
      error-sum: 3
      # 服务地址（默认dysmsapi.aliyuncs.com）
      endpoint: dysmsapi.aliyuncs.com
```

**重要说明：**
- `access-key-id` 和 `access-key-secret` 必须在阿里云控制台获取
- 建议将密钥配置在环境变量或配置中心，不要硬编码在代码中
- 防刷机制默认开启，可根据业务需求调整参数

## 使用示例

### 1. 发送短信验证码

```java
@Service
public class SmsService {
    
    @Autowired
    private com.simple.common.sms.service.SmsService smsService;
    
    public void sendVerifyCode(String phone) {
        String code = RandomUtil.randomNumbers(6);
        
        // 发送短信
        smsService.send(phone, "SMS_123456", Map.of("code", code));
        
        // 缓存验证码
        cacheManager.set("sms:" + phone, code, 300);
    }
}
```

---

[返回主文档](../README.md)

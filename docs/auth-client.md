# simple-common-auth-client

客户端认证模块，用于资源服务端的认证拦截和权限校验。提供Token解析、白名单管理、签名校验等功能。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| Token管理 | `TokenManager` | Token解析、验证、创建（addSecret方法增加broadcast参数控制是否广播） |
| 白名单管理 | `WhiteManager` | URL白名单配置 |
| 登录信息管理 | `LoginInfoManager` | 获取当前登录用户信息 |
| 签名校验 | `SignManager` | 接口签名校验（addSecret方法增加broadcast参数控制是否广播） |
| 缓存管理 | `CacheManager` | 认证相关缓存管理 |
| CSRF防御 | `CsrfService` | CSRF Token生成和验证 |
| 责任链处理 | `AuthProcess` | 认证拦截责任链处理接口 |
| 权限导出 | `PermissionExportController` | 自动扫描所有 @HasAuthority 注解，生成 SQL INSERT 语句 |

## 集成方式

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-auth-client</artifactId>
    <version>${version}</version>
</dependency>
```

### 步骤2：继承配置类

```java
@Configuration
public class MyClientAuthConfig extends AbsClientAuthConfig {
    
    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        // 开启全局登录校验
        clientAuthInfo.openLogin();
        
        // 放行公开接口
        clientAuthInfo.antMatchers("/api/public/**").permitAll();
        
        // IP白名单限制
        clientAuthInfo.antMatchers("/api/internal/**")
                      .onlyIp("192.168.1.100");
        
        // 角色限制
        clientAuthInfo.antMatchers("/api/admin/**")
                      .onlyRole("admin");
    }
}
```

## 扩展示例

### 1. 自定义认证处理器

实现 `AuthProcess` 接口，添加自定义认证逻辑：

```java
@Component
public class CustomAuthProcess implements AuthProcess {
    
    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN;
    }
    
    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, 
                       String token, String path, String ipAddr) {
        // 自定义认证逻辑
        log.info("自定义认证检查: {}", path);
    }
}
```

### 2. 自定义白名单配置

继承 `AbsClientAuthConfig`，在 `configure()` 方法中配置白名单和权限规则：

```java
@Configuration
public class MyAuthConfig extends AbsClientAuthConfig {
    
    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        // 开启全局登录校验
        clientAuthInfo.openLogin();
        
        // 放行公开接口（白名单）
        clientAuthInfo.antMatchers("/api/public/**").permitAll();
        clientAuthInfo.antMatchers("/api/login").permitAll();
        
        // IP白名单限制
        clientAuthInfo.antMatchers("/api/internal/**")
                      .onlyIp("192.168.1.100");
        
        // 角色限制
        clientAuthInfo.antMatchers("/api/admin/**")
                      .onlyRole("admin");
        
        // URL权限映射
        clientAuthInfo.addUrlOperation("/api/user/**", "user:read");
        clientAuthInfo.addUrlOperation("/api/order/**", "order:manage");
    }
}
```

## 使用示例

### 1. 获取当前登录用户信息

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/info")
    public R<UserInfo> getCurrentUser() {
        UserTemporary user = LoginUserUtils.getUserTemporary();
        
        UserInfo info = new UserInfo();
        info.setUserId(user.getUserId());
        info.setUsername(user.getUsername());
        info.setRoles(user.getRoles());
        
        return R.ok(info);
    }
}
```

### 2. 接口签名校验（使用@Sign注解）

auth-client模块提供了`@Sign`注解，自动进行签名校验：

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 启用签名校验（默认开启）
     */
    @PostMapping("/data")
    @Sign
    public R<Void> submitData(@RequestBody DataRequest request) {
        // 框架自动校验签名，无需手动处理
        // 业务逻辑
        return R.ok();
    }
    
    /**
     * 排除敏感字段不参与签名计算
     */
    @PostMapping("/payment")
    @Sign(excludeFields = {"password", "cvv"})
    public R<Void> payment(@RequestBody PaymentRequest request) {
        // password和cvv字段不参与签名计算
        return R.ok();
    }
    
    /**
     * 关闭时间戳校验（不限制请求时效性）
     */
    @PostMapping("/webhook")
    @Sign(checkTimestamp = false)
    public R<Void> webhook(@RequestBody WebhookRequest request) {
        // 不校验时间戳，适用于第三方回调
        return R.ok();
    }
    
    /**
     * 关闭nonce校验（不防重放）
     */
    @PostMapping("/notification")
    @Sign(checkNonce = false)
    public R<Void> notification(@RequestBody NotificationRequest request) {
        // 不校验nonce，允许重复请求
        return R.ok();
    }
}
```

**@Sign注解参数说明：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用签名校验 |
| `excludeFields` | String[] | {} | 排除参与签名的字段名 |
| `checkTimestamp` | boolean | true | 是否校验时间戳（时效性） |
| `checkNonce` | boolean | true | 是否校验nonce（防重放） |

### 3. 客户端调用示例

```java
@Service
public class ApiClient {
    
    @Autowired
    private SignManager signManager;
    
    public void callApi() {
        DataRequest request = new DataRequest();
        request.setData("test");
        
        // 生成签名
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        String sign = signManager.generateSign(request, timestamp, nonce);
        
        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Timestamp", String.valueOf(timestamp));
        headers.set("X-Nonce", nonce);
        headers.set("X-Sign", sign);
        
        restTemplate.exchange("/api/data", HttpMethod.POST, 
            new HttpEntity<>(request, headers), Void.class);
    }
}
```

## 前端签名构建流程（JavaScript）

前端需要按照以下步骤构建签名，确保与服务端验签逻辑一致：

```javascript
class ApiClient {
    constructor(baseUrl, secretKey) {
        this.baseUrl = baseUrl;
        this.secretKey = secretKey;  // 从服务端获取的签名密钥
    }
    
    /**
     * 发送带签名的 POST 请求
     */
    async postWithSign(path, data, excludeFields = []) {
        // 1. 生成时间戳和随机数
        const timestamp = Date.now().toString();
        const nonce = crypto.randomUUID().replace(/-/g, '');
        
        // 2. 构建业务参数字符串（排除敏感字段）
        const businessStr = this.buildBusinessString(data, excludeFields);
        
        // 3. 构建完整的待签名字符串
        // 格式: field1=value1&field2=value2&X-TIMESTAMP=timestamp&X-NONCE=nonce
        const message = `${businessStr}&X-TIMESTAMP=${timestamp}&X-NONCE=${nonce}`;
        
        // 4. 生成 HMAC-SHA256 签名（Base64编码）
        const signature = await this.generateSignature(message, this.secretKey);
        
        // 5. 发送请求
        const response = await fetch(`${this.baseUrl}${path}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-SIGN': signature,
                'X-TIMESTAMP': timestamp,
                'X-NONCE': nonce,
                'Authorization': 'Bearer ' + this.getAccessToken()
            },
            body: JSON.stringify(data)
        });
        
        return response.json();
    }
    
    /**
     * 构建业务参数字符串
     * - 按字段名 ASCII 码升序排序
     * - 对 value 进行 URL 编码（RFC 3986 标准）
     * - 排除指定字段
     */
    buildBusinessString(params, excludeFields = []) {
        // 过滤掉需要排除的字段和空值
        const filtered = Object.keys(params)
            .filter(key => !excludeFields.includes(key))
            .reduce((obj, key) => {
                obj[key] = params[key] != null ? params[key].toString() : '';
                return obj;
            }, {});
        
        // 按字段名 ASCII 排序
        const sortedKeys = Object.keys(filtered).sort();
        
        // 拼接成 field=value&field=value 格式
        return sortedKeys
            .map(key => `${key}=${this.urlEncode(filtered[key])}`)
            .join('&');
    }
    
    /**
     * URL 编码（RFC 3986 标准）
     * 与服务端 SignUtils.urlEncode 保持一致
     */
    urlEncode(value) {
        return encodeURIComponent(value)
            .replace(/\+/g, '%20')      // 空格编码为 %20
            .replace(/%21/g, '!')       // ! 不编码
            .replace(/%27/g, "'")       // ' 不编码
            .replace(/%28/g, '(')       // ( 不编码
            .replace(/%29/g, ')')       // ) 不编码
            .replace(/%7E/g, '~');      // ~ 不编码
    }
    
    /**
     * 生成 HMAC-SHA256 签名
     */
    async generateSignature(message, secretKey) {
        const encoder = new TextEncoder();
        const keyData = encoder.encode(secretKey);
        const messageData = encoder.encode(message);
        
        // 导入密钥
        const key = await crypto.subtle.importKey(
            'raw',
            keyData,
            { name: 'HMAC', hash: 'SHA-256' },
            false,
            ['sign']
        );
        
        // 生成签名
        const signature = await crypto.subtle.sign('HMAC', key, messageData);
        
        // 转换为 Base64
        return btoa(String.fromCharCode(...new Uint8Array(signature)));
    }
    
    getAccessToken() {
        return localStorage.getItem('accessToken');
    }
}

// 使用示例
const apiClient = new ApiClient('https://api.example.com', 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6');

// 创建用户（排除 password 字段不参与签名）
const result = await apiClient.postWithSign('/api/user/create', {
    username: '张三',
    email: 'zhangsan@example.com',
    phone: '13800138000',
    password: '123456'  // 这个字段不会参与签名
}, ['password']);

console.log(result);
```

**重要注意事项：**

1. **字段排序**：必须按字段名 ASCII 码升序排序
2. **URL 编码**：必须使用 RFC 3986 标准（与服务端一致）
3. **排除字段**：敏感字段（如 password）不参与签名
4. **时间戳格式**：必须是毫秒级时间戳字符串
5. **Nonce 唯一性**：每次请求必须生成新的 UUID
6. **签名算法**：HMAC-SHA256，结果 Base64 编码
7. **请求头名称**：必须与服务端配置一致（X-SIGN、X-TIMESTAMP、X-NONCE）

---

[返回主文档](../README.md) | [查看服务端模块](auth-server.md)

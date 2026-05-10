# simple-common-auth-server

认证服务器模块，负责用户认证、Token颁发、OAuth2授权。提供登录服务、Token管理、客户端管理等功能。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 登录服务 | `LoginService` | 用户登录、登出、Token刷新 |
| Token管理 | `TokenManager` | Token创建、验证、解析 |
| 客户端管理 | `ClientManager` | OAuth2客户端信息管理 |
| 登录错误处理 | `LoginErrorProcess` | 登录失败次数限制与锁定 |
| 登录成功处理 | `LoginSucProcess` | 登录成功后置处理 |
| 统一密钥管理 | `UnifiedSecretManager` | 统一管理JWT和SIGN双密钥，支持多项目隔离，提供开放式接口供集成方自定义实现 |
| 权限管理 | `PermissionManageService` | 角色权限管理，支持事件驱动实时同步 |

## 集成方式

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-auth-server</artifactId>
    <version>${version}</version>
</dependency>
```

### 步骤2：实现客户端详情服务（必须）

继承 `AbsClientDetailsService`，定义OAuth2客户端信息获取逻辑：

```java
@Service
public class MyClientDetailsService extends AbsClientDetailsService {
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Override
    public ClientDetails checkClientDetails(String clientId, String clientSecret) {
        SysClient entity = clientRepository.findByClientId(clientId);
        AssertUtils.notNull(entity, "客户端不存在");
        AssertUtils.isTrue(entity.getClientSecret().equals(clientSecret), "客户端密钥错误");
        
        ClientDetails details = new ClientDetails();
        details.setClientId(entity.getClientId());
        details.setScopes(entity.getScopes());
        details.setAccessTokenValidity(entity.getAccessTokenValidity());
        details.setRefreshTokenValidity(entity.getRefreshTokenValidity());
        
        return details;
    }
}
```

### 步骤3：实现登录管理器（必须，至少一种）

为每种登录方式实现 `LoginManager` 接口：

```java
@Component
public class PasswordLoginManager implements LoginManager {
    
    @Autowired
    private UserService userService;
    
    @Override
    public AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        PwdLoginRequest request = (PwdLoginRequest) adapter;
        
        // 查询用户
        User user = userService.getByUsername(request.getUsername());
        AssertUtils.notNull(user, "用户名或密码错误");
        
        // 校验密码
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        AssertUtils.isTrue(matches, "用户名或密码错误");
        
        // 构建用户详情
        AbsUserDetails details = new AbsUserDetails();
        details.setUserId(user.getId());
        details.setUsername(user.getUsername());
        details.setLoginRole(user.getRoles());
        details.setIsEnabled(user.getStatus());
        details.setIsAccountNonExpired(1);
        details.setIsAccountNonLocked(1);
        details.setIsCredentialsNonExpired(1);
        
        return details;
    }
    
    @Override
    public boolean support(Object adapter) {
        return adapter instanceof PwdLoginRequest;
    }
    
    @Override
    public LoginTypeAdapter getLoginType() {
        return LoginTypeAdapter.PASSWORD;
    }
}
```

### 步骤4：使用权限管理服务（直接使用）

`PermissionManageService` 由框架提供默认实现，直接注入使用即可管理角色权限：

```java
@Service
public class RoleManagementService {
    
    @Autowired
    private PermissionManageService permissionManageService;
    
    /**
     * 更新角色权限
     */
    public void updateAdminPermissions() {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("user:create", "创建用户");
        permissions.put("user:delete", "删除用户");
        permissions.put("user:update", "更新用户");
        permissions.put("role:manage", "角色管理");
        
        // 更新admin角色的权限，自动发布事件通知所有客户端刷新缓存
        permissionManageService.updateRolePermission("admin", permissions);
    }
    
    /**
     * 批量刷新多个角色的权限
     */
    public void batchRefreshRoles() {
        List<String> roleKeys = List.of("admin", "editor", "viewer");
        
        // 批量刷新，只发布一次事件
        permissionManageService.batchRefreshPermissions(roleKeys);
    }
    
    /**
     * 删除角色的某个权限
     */
    public void removePermission() {
        // 删除admin角色的"user:delete"权限
        permissionManageService.deletePermission("admin", "user:delete");
    }
    
    /**
     * 查询角色权限
     */
    public Map<Object, Object> queryRolePermissions(String roleKey) {
        return permissionManageService.getRolePermission(roleKey);
    }
}
```

**重要说明：**
- `PermissionManageService` 由框架提供默认实现，无需自定义
- 调用 `updateRolePermission()` 等方法后，会自动发布事件通知所有客户端刷新缓存
- 客户端通过事件监听器自动接收权限变更并更新本地缓存

## 密钥管理最佳实践

### 1. 服务端启动时自动加载密钥

框架会在应用启动时通过`JwtSecretServerInitializer`自动为默认项目生成并加载JWT和SIGN密钥，无需手动配置。密钥存储在内存中（ConcurrentHashMap），支持多项目隔离。

### 2. 客户端启动时远程拉取密钥

客户端会在启动时通过HTTP调用`/auth/api/secrets?projectCode={projectCode}`接口获取双密钥。项目编码从`spring.application.name`自动获取。

```yaml
# application.yaml
spring:
  application:
    name: order-service  # 项目名称，用于密钥隔离
```

### 3. 密钥轮换（集成方主动触发）

当需要定期轮换密钥时，可以创建定时任务或手动触发：

```java
@Service
public class KeyRotationService {
    
    @Autowired
    private TokenManager tokenManager;
    
    @Autowired
    private SignManager signManager;
    
    /**
     * 每月1号凌晨0点轮换密钥
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void rotateKeys() {
        // 生成新密钥
        String newJwtSecret = tokenManager.generateSecret();
        String newSignSecret = signManager.generateSecret();
        
        // broadcast=true 会通知所有客户端同步更新
        tokenManager.addSecret(newJwtSecret, true);
        signManager.addSecret(newSignSecret, true);
        
        log.info("密钥轮换完成，已广播到所有客户端");
    }
}
```

### 4. 多项目密钥隔离

每个项目根据`spring.application.name`独立管理密钥，互不影响。事件广播时会携带`projectCode`，客户端只处理属于自己项目的密钥更新事件。

示例场景：
- `order-service` 项目使用自己的JWT和SIGN密钥
- `user-service` 项目使用另一套独立的密钥
- 两个项目的密钥互不干扰，事件也不会互相影响

### 5. addSecret方法的broadcast参数说明

```java
// TokenManager和SignManager的addSecret方法签名
void addSecret(String secret, boolean broadcast);
```

**broadcast参数含义：**
- `true`：添加密钥后通过EventBus广播事件，通知所有客户端同步更新
- `false`：仅本地加载密钥，不触发任何事件广播

**使用规则：**
- ✅ 服务端/客户端启动时自加载密钥 → 使用`false`（避免无效广播）
- ✅ 集成方主动轮换密钥 → 使用`true`（需要同步到所有实例）
- ❌ 不要在启动时使用`true`（会产生不必要的EventBus事件）

## 扩展示例

### 1. 扩展新的登录方式（短信登录）

步骤1：定义登录请求对象

```java
@Data
public class SmsLoginRequest {
    private String phone;
    private String code;
}
```

步骤2：创建登录管理器

```java
@Component
public class SmsLoginManager implements LoginManager {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SmsService smsService;
    
    @Override
    public AbsUserDetails login(ClientDetails clientDetails, Object adapter) {
        SmsLoginRequest request = (SmsLoginRequest) adapter;
        
        // 校验短信验证码
        boolean valid = smsService.verifyCode(request.getPhone(), request.getCode());
        AssertUtils.isTrue(valid, "验证码错误或已过期");
        
        // 查询或创建用户
        User user = userService.getByPhone(request.getPhone());
        if (user == null) {
            user = userService.createByPhone(request.getPhone());
        }
        
        // 构建用户详情
        AbsUserDetails details = new AbsUserDetails();
        details.setUserId(user.getId());
        details.setUsername(user.getPhone());
        details.setLoginRole(user.getRoles());
        details.setIsEnabled(1);
        details.setIsAccountNonExpired(1);
        details.setIsAccountNonLocked(1);
        details.setIsCredentialsNonExpired(1);
        
        return details;
    }
    
    @Override
    public boolean support(Object adapter) {
        return adapter instanceof SmsLoginRequest;
    }
    
    @Override
    public LoginTypeAdapter getLoginType() {
        return LoginTypeAdapter.SMS;
    }
}
```

步骤3：在枚举中添加登录类型

```java
public enum LoginTypeAdapter {
    PASSWORD("密码登录"),
    SMS("短信登录");  // 新增
    
    private final String desc;
}
```

步骤4：调用登录接口

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private LoginService loginService;
    
    @PostMapping("/sms/login")
    public R<Map<String, String>> smsLogin(@RequestBody SmsLoginRequest request) {
        Map<String, String> tokens = loginService.login(request, LoginTypeAdapter.SMS);
        return R.ok(tokens);
    }
}
```

### 2. 自定义登录成功处理

实现 `LoginSucProcess` 接口，添加登录成功后的处理逻辑：

```java
@Component
public class NotificationLoginSucProcess implements LoginSucProcess {
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.NOTIFICATION;
    }
    
    @Override
    public void execute(TokenData tokenData) {
        // 发送登录通知
        notificationService.sendLoginNotification(
            tokenData.getUserId(),
            tokenData.getUsername(),
            LocalDateTime.now()
        );
    }
}
```

在枚举中注册：

```java
public enum LoginSucKindProcess implements DefaultKindProcess {
    SAVE_INFO("保存用户信息", true, 1),
    LOGIN_LOG("登陆日志", true, 2),
    NOTIFICATION("登录通知", true, 3);  // 新增
    
    private final String label;
    private final boolean execute;
    private final int order;
}
```

### 3. 自定义登录失败处理

继承 `AbsLoginErrorProcess`，添加自定义失败计数逻辑：

```java
@Component
public class DeviceLoginErrorProcess extends AbsLoginErrorProcess {
    
    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.DEVICE_ERROR;
    }
    
    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        LoginRequest request = (LoginRequest) adapter;
        // 使用设备指纹作为标识
        return request.getDeviceFingerprint();
    }
    
    @Override
    protected String getKeyPrefix() {
        return "login:error:device:";
    }
}
```

## 使用示例

### 1. 密码登录

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private LoginService loginService;
    
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody PwdLoginRequest request) {
        Map<String, String> tokens = loginService.login(request, LoginTypeAdapter.PASSWORD);
        return R.ok(tokens);
    }
}
```

返回数据：

```json
{
  "code": 200,
  "data": {
    "bearer": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "exp": 1234567890,
    "scopes": "user:read,user:write"
  }
}
```

### 2. Token刷新

```java
@PostMapping("/refresh")
public R<Map<String, String>> refresh(@RequestParam String refreshToken) {
    Map<String, String> tokens = loginService.refresh(refreshToken);
    return R.ok(tokens);
}
```

### 3. 退出登录

```java
@PostMapping("/logout")
public R<Void> logout() {
    loginService.logout();
    return R.ok();
}
```

### 4. 权限管理

```java
@Service
public class RolePermissionService {
    
    @Autowired
    private PermissionManageService permissionManageService;
    
    /**
     * 更新角色权限
     */
    public void updateAdminPermissions() {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("user:create", "创建用户");
        permissions.put("user:delete", "删除用户");
        permissions.put("user:update", "更新用户");
        
        // 自动发布事件，所有客户端实时同步
        permissionManageService.updateRolePermission("admin", permissions);
    }
    
    /**
     * 删除权限
     */
    public void removePermission() {
        permissionManageService.deletePermission("admin", "user:delete");
    }
    
    /**
     * 批量刷新
     */
    public void batchRefresh() {
        List<String> roles = List.of("admin", "editor", "viewer");
        permissionManageService.batchRefreshPermissions(roles);
    }
}
```

---

[返回主文档](../README.md) | [查看客户端模块](auth-client.md)

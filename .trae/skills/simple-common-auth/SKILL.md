---
name: "simple-common-auth"
description: "权限认证模块。提供 @HasAuthority 权限校验、@Sign 签名验证、@CsrfDefense CSRF 防御注解；LoginManager/LoginService 登录管理、TokenManager Token 管理、PermissionManageService 权限管理。当需要接口权限控制、签名校验、登录认证时使用。"
---

# simple-common-auth 认知文档

**Maven**: `simple-common-auth-client`（客户端）/ `simple-common-auth-server`（服务端）
**包路径**: `com.simple.common.auth`

## 客户端注解

### @HasAuthority — 权限校验

```java
@HasAuthority({"sys:user:page"})
@GetMapping("/page")
public R<IPage<PageSysUserResponse>> page(PageSysUserRequest req) {
    // 超级管理员自动放行，其他用户校验权限
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `value()` | `String[]` | 权限标识数组，满足任意一个即可 |

**权限值命名规则**：`@RequestMapping路径:方法路径`，`/`→`:`，剔除PathVariable。示例：`@RequestMapping("sys/department")` + `@GetMapping("tree")` → `sys:department:tree`

### @Sign — 接口签名

```java
@Sign(excludeFields = {"password", "secret"}, checkTimestamp = true, checkNonce = true)
@PostMapping("/sensitive/api")
public R<?> sensitiveApi(@RequestBody SensitiveRequest req) {
    // 自动校验：签名、时间戳有效期、nonce防重放
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled()` | `boolean` | `true` | 是否启用签名校验 |
| `excludeFields()` | `String[]` | `{}` | 参与签名字段中需排除的（如password、secret） |
| `checkTimestamp()` | `boolean` | `true` | 是否校验时间戳（防过期请求） |
| `checkNonce()` | `boolean` | `true` | 是否校验nonce（防重放攻击） |

### @CsrfDefense — CSRF防御 + 防重复提交

```java
@CsrfDefense(consume = true)
@PostMapping("/order/submit")
public R<?> submitOrder(@RequestBody SubmitOrderRequest req) {
    // consume=true: token一次性，提交后立即删除
    // consume=false: token可复用直到过期
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `consume()` | `boolean` | `true` | 校验后是否立即删除token，true=一次性 |

## LoginUserUtils — 获取当前登录用户

```java
UserTemporary user = LoginUserUtils.getUserTemporary();
String userId = user.getUserId();
String nickname = user.getNickname();
String clientId = user.getClientId();
String loginKey = user.getLoginKey();         // 登录标识（区分同一用户不同设备）
String jti = user.getJti();                   // JWT唯一标识
HashSet<String> scopes = user.getScopes();    // 权限作用域（all/write/read）
HashSet<String> loginRole = user.getLoginRole(); // 登录角色集合

// 数据权限相关（通过 DataPermission 对象获取）
DataPermission dp = user.getDataPermission();
String tenantId = dp.getTenantId();
DataScopeEnum scope = dp.getPermissionScope();
Set<String> departmentIds = dp.getDepartmentIds();  // 已展开的子部门ID
```

**UserTemporary 完整字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `String` | 用户ID |
| `nickname` | `String` | 用户昵称 |
| `loginKey` | `String` | 登录标识（区分同一用户不同登录设备） |
| `jti` | `String` | JWT唯一标识 |
| `path` | `String` | 请求路径 |
| `clientId` | `String` | 客户端ID |
| `clientName` | `String` | 客户端名称 |
| `appNames` | `String` | 可访问的微服务名称集合（逗号分隔） |
| `wxAppId` | `String` | 微信小程序AppID |
| `scopes` | `HashSet<String>` | 权限作用域 |
| `loginRole` | `HashSet<String>` | 登录角色集合 |
| `extension` | `Object` | 扩展信息 |
| `dataPermission` | `DataPermission` | 数据权限对象 |

**DataPermission 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `tenantId` | `String` | 租户ID |
| `permissionScope` | `DataScopeEnum` | 数据权限类型 |
| `departmentIds` | `Set<String>` | 部门ID集合（已展开子部门） |

## DataScopeEnum — 数据权限级别

| 枚举值 | code | 权限范围 | 说明 |
|--------|------|---------|------|
| `ALL` | 1 | 全部数据 | 仅受租户隔离约束 |
| `DEPT_AND_CHILD` | 2 | 部门及子部门 | departmentIds已在登录时展开 |
| `DEPT` | 3 | 本部门 | 仅能查看所属部门数据 |
| `SELF` | 4 | 仅本人 | 仅能查看自己创建的数据 |

```java
DataScopeEnum scope = user.getDataScope();
scope.isAll();                  // 是否全部权限
scope.isDeptScope();            // 是否需要部门过滤
scope.isSelf();                 // 是否仅本人
scope.greaterThanOrEqual(other); // 当前权限是否≥指定权限
```

## 服务端核心接口

### LoginManager — 登录管理器（需自行实现）

```java
@Component
public class PwdLoginManager extends AbsLoginManager<PwdLoginRequest> {
    @Override
    protected AbsUserDetails doLogin(PwdLoginRequest request, ClientDetails clientDetails) {
        SysUser user = userService.findByUsername(request.getUsername());
        AssertUtils.notEmpty(user, "用户不存在");
        AssertUtils.isTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()), "密码错误");
        return user;
    }
}
```

**接口方法**：

| 方法 | 签名 | 说明 |
|------|------|------|
| `support` | `boolean support(Object adapter)` | 判断是否支持该登录类型，如 `return adapter instanceof PwdLoginRequest` |
| `login` | `AbsUserDetails login(ClientDetails clientDetails, Object adapter)` | 执行登录认证，返回用户详情 |

**AbsLoginManager\<T\>**：继承后只需重写 `doLogin(T request, ClientDetails clientDetails)`，框架自动处理参数校验、Token生成、事件发布等。泛型 `T` 为登录请求类型。

### LoginService — 登录服务

```java
@Autowired
private LoginService loginService;

// 用户登录
Map<String, String> tokens = loginService.login(
    new PwdLoginRequest("admin", "123456"),  // adapter: 登录请求对象
    LoginTypeAdapter.PASSWORD                // loginType: 登录类型枚举
);
// 返回：{accessToken, refreshToken, expiresIn, userId, ...}

// 刷新Token
Map<String, String> newTokens = loginService.refresh(refreshTokenStr);

// 登出（指定用户）
loginService.logout("userId123");

// 登出（当前用户）
loginService.logout();
```

```java
Map<String, String> login(Object adapter, LoginTypeAdapter loginType);
Map<String, String> refresh(String refreshTokenStr);
void logout(String userId);
void logout();
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `adapter` | `Object` | 登录请求对象，如 `PwdLoginRequest`、`SmsLoginRequest` |
| `loginType` | `LoginTypeAdapter` | 登录类型枚举，路由到对应 LoginManager |
| `refreshTokenStr` | `String` | 刷新Token字符串 |

### LoginTypeAdapter — 登录类型枚举

```java
LoginTypeAdapter.PASSWORD    // 密码登录
LoginTypeAdapter.SMS         // 短信登录
LoginTypeAdapter.WECHAT      // 微信登录
```

### ClientManager — 客户端管理器

```java
@Autowired
private ClientManager clientManager;

// 加密客户端凭证为Token
String token = clientManager.encrypt("my-client-id", "my-client-secret");

// 解密Authorization头获取凭证
String authHeader = request.getHeader("Authorization");  // "Basic base64(...)"
Map<ClientAttribute, String> credentials = clientManager.decryptStr(authHeader);
String clientId = credentials.get(ClientAttribute.CLIENT_ID);
String clientSecret = credentials.get(ClientAttribute.CLIENT_SECRET);
```

```java
String encrypt(String clientId, String clientSecret);
Map<ClientAttribute, String> decryptStr(String header);
```

### ClientDetailsService — 客户端详情服务（需继承 AbsClientDetailsService）

```java
@Service
public class MyClientDetailsService extends AbsClientDetailsService {
    @Override
    protected ClientDetails getClientDetailsFromDB(String clientId) {
        SysClientDetails entity = clientRepository.findByClientId(clientId);
        AssertUtils.notEmpty(entity, "客户端不存在");
        return convertToClientDetails(entity);
    }
}
```

```java
ClientDetails getClientDetails(String header);                            // 从Authorization头获取
ClientDetails getClientDetails(HttpServletRequest request);               // 从HttpServletRequest获取
String getClientToken(String clientId, String clientSecret);              // 生成客户端Token
```

### PermissionManageService — 权限管理服务

```java
@Autowired
private PermissionManageService permissionManageService;

// 新增权限（按项目+角色，自动发布事件）
Map<String, String> perms = new HashMap<>();
perms.put("user:create", "创建用户");
perms.put("user:delete", "删除用户");
permissionManageService.addPermissions("xiaoyue-web", "admin", perms);

// 新增权限（不发布事件，用于登录预缓存）
permissionManageService.addPermissions("xiaoyue-web", "admin", perms, false);

// 全量替换角色权限
permissionManageService.updateRolePermission("xiaoyue-web", "editor", newPerms);

// 删除单个权限
permissionManageService.deletePermission("xiaoyue-web", "admin", "user:delete");

// 批量删除权限
permissionManageService.deletePermissions("xiaoyue-web", "admin", List.of("user:create", "user:delete"));
```

```java
void addPermissions(String projectCode, String roleKey, Map<String, String> permissions);
void addPermissions(String projectCode, String roleKey, Map<String, String> permissions, boolean publishEvent);
void updateRolePermission(String projectCode, String roleKey, Map<String, String> permissions);
void deletePermission(String projectCode, String roleKey, String permissionKey);
void deletePermissions(String projectCode, String roleKey, List<String> permissionKeys);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `projectCode` | `String` | 项目编码（clientName），如 `"xiaoyue-web"` |
| `roleKey` | `String` | 角色标识，如 `"admin"` |
| `permissions` | `Map<String,String>` | key=权限标识, value=权限描述 |
| `publishEvent` | `boolean` | 是否发布事件通知客户端，`true`=广播, `false`=仅存Redis |

### UnifiedSecretManager — 统一密钥管理器（需自行实现）

```java
@Component
public class DatabaseUnifiedSecretManager implements UnifiedSecretManager {
    @Override
    public Map<String, String> getSecrets(String projectCode) {
        ProjectClient client = repository.findByProjectCode(projectCode);
        if (client == null) {
            client = createDefaultClient(projectCode);
        }
        Map<String, String> secrets = new HashMap<>();
        secrets.put("jwt", client.getJwtSecret());   // JWT签名密钥
        secrets.put("sign", client.getSignSecret());  // API签名密钥
        return secrets;
    }
}
```

```java
Map<String, String> getSecrets(String projectCode);
// 返回：{"jwt": "64位随机字符串", "sign": "32位随机字符串"}
```

### LoginUserOperationManager — 登录用户操作管理器

```java
@Autowired
private LoginUserOperationManager loginUserOperationManager;

// 登录时保存用户信息
loginUserOperationManager.saveUserInfo(tokenData, true);  // isLogin=true登录, false=刷新

// 退出登录（清除所有登录信息）
loginUserOperationManager.loginOut("userId123");

// 退出登录（仅退出当前设备）
loginUserOperationManager.loginOut("userId123", "jti-xxx");

// 退出登录（当前用户）
loginUserOperationManager.loginOut();
```

## 客户端核心接口

### TokenManager — Token 管理器

```java
@Autowired
private TokenManager tokenManager;

// 创建Token
Map<String, Object> headers = Map.of("alg", "HS256", "typ", "JWT");
Map<String, Object> payload = Map.of("userId", "123", "username", "张三");
String token = tokenManager.create(headers, payload);

// 验证Token并解析载荷
Map<String, Object> payload = tokenManager.verify(token, isRefreshToken);
// isRefreshToken: false=AccessToken, true=RefreshToken
```

```java
String create(Map<String, Object> headers, Map<String, Object> payload);
Map<String, Object> verify(String token, boolean isRefreshToken);
```

### LoginInfoManager — 登录用户信息管理器

```java
@Autowired
private LoginInfoManager loginInfoManager;

Map<Object, Object> userInfo = loginInfoManager.getUserInfo("jti-xxx");

Set<String> roles = Set.of("admin", "editor");
Map<Object, Map<Object, Object>> authorities = loginInfoManager.getAuthorities(roles);

Map<Object, Map<Object, Object>> perms = loginInfoManager.getAuthoritiesByProjectCode(roles, "xiaoyue-web");
```

### SignManager — 签名管理器

```java
@Autowired
private SignManager signManager;

signManager.checkTimestamp("1680000000000");
signManager.checkNonce("random-uuid-string");
String signature = signManager.signWeb(message);
boolean valid = signManager.verifyWeb(message, signature);
```

```java
void checkTimestamp(String timestamp);
void checkNonce(String nonce);
String signWeb(String message);
boolean verifyWeb(String message, String signature);
```

### CsrfService — CSRF防御服务

```java
@Autowired
private CsrfService csrfService;

csrfService.saveToken("userId123", "/order/submit", "csrf-token-value");
csrfService.checkToken("userId123", "/order/submit", "csrf-token-value", true);
```

```java
void saveToken(String userId, String path, String token);
void checkToken(String userId, String path, String token, boolean consume);
```

### PermissionAutoLoader — 权限自动加载器（SPI，需自行实现）

```java
@Component
public class SysRoleMenuPermissionLoader implements PermissionAutoLoader {
    @Autowired
    private SysRoleMenuView sysRoleMenuView;

    @Override
    public Map<String, String> loadPermissions(String roleKey, String projectCode) {
        List<String> perms = sysRoleMenuView.findPermsByRoleKeyAndProjectCode(roleKey, projectCode);
        if (perms == null || perms.isEmpty()) {
            return Map.of();
        }
        return perms.stream()
            .filter(p -> p != null && !p.isEmpty())
            .collect(Collectors.toMap(p -> p, p -> ""));
    }
}
```

```java
Map<String, String> loadPermissions(String roleKey, String projectCode);
// 返回：key=权限标识, value=权限描述，不可返回null
```

### JwtUtils — JWT工具类（静态方法）

```java
String secret = JwtUtils.createJWTSignerStr();  // 64位随机字符串
JwtUtils.saveSecret(secret);
JWTSigner signer = JwtUtils.getJWTSigner();
String token = JwtUtils.create(headers, payload);
JWT jwt = JwtUtils.verify(token);
```

```java
static String createJWTSignerStr();
static void saveSecret(String JWTSigner);
static JWTSigner getJWTSigner();
static String create(Map<String, Object> headers, Map<String, Object> payload);
static JWT verify(String token);
```

## Auth 事件体系

| 事件类 | 说明 | 触发时机 |
|--------|------|---------|
| `PermissionChangeEvent` | 权限变更事件 | 管理员修改角色权限时 |
| `SecretEvent` | 密钥变更事件 | 密钥轮换时 |
| `SecretBroadcaster` | 密钥广播器 | 服务端向客户端广播新密钥 |

## 核心流程图形化

### 登录认证流程

```
┌──────────────────────────────────────────────────────────────────────┐
│ 客户端                                                          认证服务端│
│  ┌──────────┐                     ┌──────────────────────────┐           │
│  │PwdLogin  │─── POST /login ──►│  LoginService.login()    │           │
│  │Request   │                     │    │                      │           │
│  └──────────┘                     │    ▼                      │           │
│                                 │  LoginTypeAdapter         │           │
│  ┌──────────┐◄── {accessToken,   │    │ 路由到对应Manager    │           │
│  │TokenData │    refreshToken}  │    ▼                      │           │
│  │{jwt}     │                     │  LoginManager.support()   │           │
│  └──────────┘                     │    │ 判断是否支持          │           │
│                                 │    ▼                      │           │
│                                 │  AbsLoginManager.login()  │           │
│                                 │    │ 模板方法：            │           │
│                                 │    │ ① checkErrorNum()    │           │
│                                 │    │ ② doLogin() ← 子类   │           │
│                                 │    │ ③ loginSuccess()     │           │
│                                 │    │ ④ 生成JWT Token      │           │
│                                 │    │ ⑤ 保存用户信息到Redis│           │
│                                 │    │ ⑥ 发布登录事件       │           │
│                                 └──────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────┘
```

### 请求鉴权流程

```
┌──────────────────────────────────────────────────────────────────────┐
│ 客户端请求                                                              │
│  ┌──────────────────────────────────────────────────────┐           │
│  │ Authorization: Bearer {accessToken}                  │           │
│  │ X-SIGN: {signature}    (可选, @Sign)                  │           │
│  │ X-CSRF-TOKEN: {token}  (可选, @CsrfDefense)          │           │
│  └──────────────────────────────────────────────────────┘           │
│                          │                                           │
│                          ▼                                           │
│  ┌──────────────────────────────────────────────────────┐           │
│  │                  AuthInterceptor                      │           │
│  │  ① 解析Authorization头 → 获取clientId                 │           │
│  │  ② 验证JWT Token → 解析UserTemporary                  │           │
│  │  ③ 存入ThreadLocal (LoginUserUtils可用)              │           │
│  │  ④ @HasAuthority → 校验权限标识                       │           │
│  │  ⑤ @Sign → 校验签名+时间戳+nonce                      │           │
│  │  ⑥ @CsrfDefense → 校验CSRF Token                     │           │
│  └──────────────────────────────────────────────────────┘           │
│                          │                                           │
│                          ▼                                           │
│  ┌──────────────────────────────────────────────────────┐           │
│  │                   Controller                          │           │
│  │  LoginUserUtils.getUserTemporary() → 当前用户信息     │           │
│  └──────────────────────────────────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────┘
```

### 权限管理流程

```
┌──────────────────────────────────────────────────────────────────────┐
│  PermissionManageService (服务端)                                    │
│  ┌────────────────────────────────────────────────────────┐         │
│  │ addPermissions(projectCode, roleKey, perms)            │         │
│  │    │                                                   │         │
│  │    ├─► 存入Redis: "auth:permission:{projectCode}"      │         │
│  │    │                                                   │         │
│  │    └─► 发布 PermissionChangeEvent (EventBus)           │         │
│  │           │                                            │         │
│  │           ▼                                            │         │
│  │      客户端监听 → 清除本地权限缓存                      │         │
│  │      下次请求 → PermissionAutoLoader 重新加载          │         │
│  └────────────────────────────────────────────────────────┘         │
│                                                                      │
│  PermissionAutoLoader (SPI, 业务实现)                                │
│  ┌────────────────────────────────────────────────────────┐         │
│  │ loadPermissions(roleKey, projectCode)                  │         │
│  │    │                                                   │         │
│  │    └─► 从DB查角色菜单关联 → 返回 Map<权限标识, 描述>   │         │
│  └────────────────────────────────────────────────────────┘         │
└──────────────────────────────────────────────────────────────────────┘
```

## POM依赖

```xml
<!-- 客户端（业务服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-client</artifactId>
</dependency>

<!-- 服务端（认证服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-server</artifactId>
</dependency>
```
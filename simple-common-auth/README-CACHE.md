# Simple Common Auth - 缓存管理器使用指南

## 概述

simple-common-auth 模块实现了统一的缓存管理器，支持 Redis 和本地缓存两种方式，可通过配置文件自由切换。

## 架构设计

### 核心组件

1. **CacheTypeEnum** - 缓存类型枚举
   - `REDIS` - Redis缓存
   - `LOCAL` - 本地缓存（基于Caffeine）

2. **CacheManager** - 统一缓存管理器接口
   - 提供字符串操作：set、get、delete、hasKey、expire、getExpire、increment
   - 提供Hash操作：hashGetAll、hashPutAll、hashGet、hashPut、hashHasKey、hashDelete
   - 提供Set操作：setAdd、setMembers、setIsMember、setRemove

3. **RedisCacheManager** - Redis缓存实现

4. **LocalCacheManager** - 本地缓存实现

5. **CacheManagerFactory** - 缓存管理器工厂

### 配置类

- **AuthProperties** - 认证配置（包含 cacheType）
- **SignProperties** - 签名配置（包含 cacheType）
- **CsrfProperties** - CSRF配置（包含 cacheType）

## 使用方式

### 1. 配置文件

在 `application.yaml` 中添加配置：

```yaml
simple:
  auth:
    # 认证相关配置
    server-url: http://localhost:8000
    time-out: 10000
    # 缓存类型：REDIS 或 LOCAL
    cache-type: REDIS
    
  sign:
    # 签名相关配置
    sign-defense: true
    sign: X-SIGN
    timestamp: X-TIMESTAMP
    nonce: X-NONCE
    cache-time: 300
    # 缓存类型：REDIS 或 LOCAL
    cache-type: REDIS
    
  csrf:
    # CSRF相关配置
    csrf-defense: true
    csrf-header: X-CSRF-TOKEN
    cache-time: 1800
    # 缓存类型：REDIS 或 LOCAL
    cache-type: REDIS
```

### 2. 切换缓存类型

#### 方式一：全局配置

在配置文件中设置 `cache-type`：

```yaml
simple:
  auth:
    cache-type: LOCAL  # 使用本地缓存
```

#### 方式二：自动切换

系统会自动检测 Redis 是否可用：
- 如果 Redis 可用（`StringRedisTemplate` 存在），默认使用 Redis
- 如果 Redis 不可用，自动降级为本地缓存

### 3. 使用示例

#### 在代码中使用 CacheManager

```java
@Service
public class MyService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public void example() {
        // 字符串操作
        cacheManager.set("key", "value");
        cacheManager.set("key", "value", 3600); // 1小时过期
        String value = cacheManager.get("key");
        cacheManager.delete("key");
        boolean exists = cacheManager.hasKey("key");
        cacheManager.expire("key", 7200);
        
        // 自增操作
        Long count = cacheManager.increment("counter", 1);
        
        // Hash操作
        Map<Object, Object> hash = new HashMap<>();
        hash.put("field1", "value1");
        hash.put("field2", "value2");
        cacheManager.hashPutAll("hashKey", hash);
        Map<Object, Object> allValues = cacheManager.hashGetAll("hashKey");
        Object fieldValue = cacheManager.hashGet("hashKey", "field1");
        cacheManager.hashPut("hashKey", "field3", "value3");
        Boolean hasField = cacheManager.hashHasKey("hashKey", "field1");
        cacheManager.hashDelete("hashKey", "field1");
        
        // Set操作
        cacheManager.setAdd("setKey", "value1", "value2", "value3");
        Set<String> members = cacheManager.setMembers("setKey");
        Boolean isMember = cacheManager.setIsMember("setKey", "value1");
        cacheManager.setRemove("setKey", "value1");
    }
}
```

## 实现细节

### Client 模块

#### 缓存使用场景

1. **ClientLoginInfoManager** - 用户登录信息缓存
   - 使用 Hash 存储用户信息
   - 使用 Set 存储用户 Token 列表

2. **DefaultSignManager** - 签名管理
   - 使用 String 存储签名密钥

3. **DefaultCsrfService** - CSRF Token 管理
   - 使用 String 存储 CSRF Token

4. **DefaultWhiteManager** - 白名单管理
   - 使用 String 存储白名单 URL

### Server 模块

#### 缓存使用场景

1. **ServerLoginUserOperationManager** - 服务端用户操作管理
   - 使用 String 存储用户 Token 信息

2. **DefaultJwtSecretManager** - JWT 密钥管理
   - 使用 String 存储 JWT 密钥

3. **DefaultClientManager** - 客户端管理
   - 使用 Hash 存储客户端详情

4. **AbsLoginErrorProcess** - 登录错误处理
   - 使用 String 存储登录错误次数

## 注意事项

1. **本地缓存限制**
   - 本地缓存基于 Caffeine，适用于单机环境
   - 在分布式环境下，建议使用 Redis 以保证数据一致性
   - 本地缓存在应用重启后会丢失

2. **性能考虑**
   - Redis 适合大规模数据和高并发场景
   - 本地缓存适合小规模数据和低延迟要求

3. **数据一致性**
   - 切换缓存类型时，需要考虑数据迁移
   - 建议在应用启动前确定缓存类型，避免运行时切换

## 最佳实践

1. **开发环境**：使用本地缓存，简化环境配置
2. **测试环境**：使用 Redis，模拟生产环境
3. **生产环境**：使用 Redis，保证数据可靠性和一致性

## 配置示例

### 开发环境配置

```yaml
simple:
  auth:
    cache-type: LOCAL
  sign:
    cache-type: LOCAL
  csrf:
    cache-type: LOCAL
```

### 生产环境配置

```yaml
simple:
  auth:
    cache-type: REDIS
  sign:
    cache-type: REDIS
  csrf:
    cache-type: REDIS

spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 扩展说明

如需添加新的缓存使用场景：

1. 在对应的 Properties 类中添加 `cacheType` 配置（如果需要独立配置）
2. 在 Config 类中创建对应的 CacheManager Bean
3. 在业务类中注入 CacheManager 使用

示例：

```java
@Configuration
public class MyConfig {
    
    @Bean
    public CacheManager myCacheManager(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) LocalCacheFactory localCacheFactory,
            MyProperties myProperties) {
        
        CacheTypeEnum cacheType = myProperties.getCacheType();
        
        if (cacheType == null) {
            cacheType = (redisTemplate != null) ? CacheTypeEnum.REDIS : CacheTypeEnum.LOCAL;
        }
        
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate, localCacheFactory);
    }
}
```

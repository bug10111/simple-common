## 3. simple-common-cache

### 模块介绍

两级缓存模块，提供本地缓存（Caffeine）和分布式缓存（Redis + DB）的统一管理，支持缓存穿透保护和防雪崩。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 缓存工具类 | `CacheUtils` | 统一缓存访问入口 |
| 本地缓存工厂 | `LocalCacheFactory` | Caffeine缓存实例管理 |
| 分布式缓存获取 | `GetRedisFunction` | Redis缓存读取和回写逻辑 |
| 数据库查询 | `GetDBFunction` | 数据库查询逻辑 |
| 缓存配置 | `CacheConfig` | 缓存自动配置 |

### 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-cache</artifactId>
    <version>${version}</version>
</dependency>
```

**注意：** cache模块依赖simple-common-redis，需要确保Redis已正确配置。

### 使用示例

**1. 分布式缓存（Redis + DB，防穿透）**

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取用户信息（带缓存）
     */
    public User getUserById(String userId) {
        return CacheUtils.get(
            userId,  // 请求参数
            "user:" + userId,  // 锁key（必须与缓存key一致）
            // Redis获取和回写逻辑
            (request) -> {
                String key = "user:" + request;
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    return JsonUtils.parse(json, User.class);
                }
                return null;
            },
            (request) -> {
                // Redis中不存在，从数据库查询
                User user = userMapper.selectById(request);
                // 回写到Redis（包含空值处理，防止穿透）
                if (user != null) {
                    String key = "user:" + request;
                    int expireTime = CacheUtils.getCacheTime(3600, 600); // 1小时+随机0-10分钟
                    redisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(user), expireTime, TimeUnit.SECONDS);
                }
                return user;
            }
        );
    }
    
    /**
     * 更新用户信息（删除缓存）
     */
    public void updateUser(User user) {
        userMapper.updateById(user);
        
        // 主动删除缓存
        CacheUtils.evict(
            user.getId(),
            "user:" + user.getId(),
            (key) -> redisTemplate.delete(key)
        );
    }
}
```

**2. 本地缓存（Caffeine）**

```java
@Service
public class DictService {
    
    private Cache<String, Dict> dictCache;
    
    @PostConstruct
    public void init() {
        // 创建本地缓存
        dictCache = CacheUtils.createLocalCache("dictCache", spec -> {
            spec.maximumSize(1000)  // 最大容量
                 .expireAfterWrite(1, TimeUnit.HOURS);  // 写入后1小时过期
        });
    }
    
    public Dict getDict(String code) {
        // 先从本地缓存获取
        Dict dict = dictCache.getIfPresent(code);
        if (dict != null) {
            return dict;
        }
        
        // 从数据库查询
        dict = dictMapper.selectByCode(code);
        if (dict != null) {
            dictCache.put(code, dict);
        }
        
        return dict;
    }
}
```

**3. 自动加载缓存（LoadingCache）**

```java
@Service
public class ConfigService {
    
    private LoadingCache<String, Config> configCache;
    
    @PostConstruct
    public void init() {
        // 创建自动加载缓存
        configCache = CacheUtils.createLocalLoadingCache(
            "configCache",
            // 缓存加载器
            new CacheLoader<String, Config>() {
                @Override
                public Config load(String key) throws Exception {
                    // 缓存未命中时自动调用
                    return configMapper.selectByCode(key);
                }
            },
            spec -> {
                spec.maximumSize(500)
                     .expireAfterWrite(30, TimeUnit.MINUTES);
            }
        );
    }
    
    public Config getConfig(String code) {
        // 自动加载，无需手动判断
        return configCache.get(code);
    }
}
```

**重要说明：**
- 分布式缓存使用双重检查锁 + 分布式锁防止缓存击穿
- 建议设置随机过期时间防止缓存雪崩（使用`CacheUtils.getCacheTime()`）
- 本地缓存适合存储不常变化的小数据（如字典、配置）
- 分布式缓存适合存储业务数据（如用户、订单）

---

[返回主文档](../README.md)

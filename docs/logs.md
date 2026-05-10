# simple-common-logs

## 模块介绍

高性能日志采集与传输模块，采用TCP + Protobuf协议，client/proto/server三模块架构。客户端通过拦截器自动采集HTTP请求日志，异步批量发送到服务端，服务端通过WAL机制保证日志不丢失。

**架构特点：**
- **Fire-and-Forget模式**：客户端发送后不等待响应，提升吞吐量
- **异步批量处理**：减少网络IO次数，提高传输效率
- **WAL预写日志**：服务端故障恢复，保证日志不丢失
- **Protobuf序列化**：二进制协议，体积小、解析快

## 子模块

| 子模块 | 说明 |
|--------|------|
| simple-common-logs-client | 日志客户端，负责采集HTTP请求日志并异步发送 |
| simple-common-logs-proto | Protobuf协议定义（LogDataEvent） |
| simple-common-logs-server | 日志服务端，负责接收、缓冲、持久化日志 |

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| **日志拦截器** | `LogInterceptor` | **自动拦截HTTP请求，采集日志数据** |
| **日志过滤器** | `LogFilter` | **包装HttpServletRequest，缓存请求体** |
| **日志服务** | `LogService` | **日志发送接口** |
| **缓冲管理器** | `BufferedLogManager` | **异步批量发送日志到服务端** |
| **用户信息管理** | `LogUserManager` | **获取当前登录用户信息** |
| **日志保存管理器** | `AbsLogsSaveManager` | **服务端抽象类，实现日志持久化** |
| **WAL机制** | 预写日志 | **保证日志不丢失，支持故障恢复** |

## 集成方式（客户端）

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-logs-client</artifactId>
    <version>${version}</version>
</dependency>
```

### 步骤2：配置日志服务器地址（必须）

```yaml
simple:
  logs:
    client:
      # ========== 基本配置 ==========
      # 日志服务器地址（必填）
      host: 127.0.0.1
      # 日志服务器端口（必填）
      port: 9999
      
      # ========== 性能关键配置 ==========
      # TCP连接池大小（默认10）
      # ⚠️ 重要：影响并发发送能力，高并发场景建议调大至50-100
      pool-size: 10
      
      # 缓冲队列容量（默认1000）
      # ⚠️ 重要：内存占用 = 队列容量 × 平均日志大小
      # 如果设置过大可能导致OOM，过小会导致日志丢弃
      buffer-capacity: 1000
      
      # 批量发送间隔，单位毫秒（默认500）
      # ⚠️ 重要：越小实时性越好，但网络IO次数增加
      # 推荐值：500-2000ms
      send-interval: 500
      
      # 降级文件目录（默认./logs/fallback）
      # 当服务端不可用时，日志会写入本地文件
      fallback-dir: ./logs/fallback
```

**性能调优建议：**

| 场景 | pool-size | buffer-capacity | send-interval | 说明 |
|------|-----------|-----------------|---------------|------|
| 低并发（<100 QPS） | 10 | 1000 | 1000 | 默认配置即可 |
| 中并发（100-1000 QPS） | 30 | 5000 | 500 | 适当增加连接池和队列 |
| 高并发（>1000 QPS） | 50-100 | 10000 | 200-500 | 需要较大的连接池和队列 |

**⚠️ 注意事项：**
- `buffer-capacity` 过大会占用大量内存（每条日志约1-5KB）
- `send-interval` 过小会增加网络IO次数，影响性能
- `pool-size` 应该根据并发量调整，避免连接不足导致阻塞

### 步骤3：实现用户信息管理器（可选，推荐）

继承 `DefaultLogUserManager`，定义如何获取当前登录用户信息：

```java
@Component
public class CustomLogUserManager extends DefaultLogUserManager {
    
    @Override
    public String loginNickName() {
        // 从登录上下文获取用户昵称
        LoginUser user = SecurityUtils.getLoginUser();
        return user != null ? user.getNickName() : "匿名用户";
    }
    
    @Override
    public String loginUserId() {
        // 从登录上下文获取用户ID
        LoginUser user = SecurityUtils.getLoginUser();
        return user != null ? user.getUserId() : null;
    }
}
```

**重要说明：**
- 如果不实现，日志中的用户信息将显示为null和"-"，或者显示测试用户
- 框架通过`LogInterceptor`自动拦截Controller方法，无需手动调用
- `LogFilter`会自动包装HttpServletRequest，缓存请求体以便读取JSON参数

## 集成方式（服务端）

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-logs-server</artifactId>
    <version>${version}</version>
</dependency>
```

### 步骤2：配置日志服务器（必须）

```yaml
simple:
  logs:
    server:
      # ========== 基本配置 ==========
      # TCP监听端口（默认9999）
      port: 9999
      
      # ========== 性能关键配置 ==========
      # 队列容量（默认10000）
      # ⚠️ 重要：决定服务端能缓冲多少条日志
      # 内存占用 = 队列容量 × 平均日志大小（约1-5KB/条）
      # 10000条约占用10-50MB内存
      queue-capacity: 10000
      
      # 批量处理大小（默认500）
      # ⚠️ 重要：每次持久化的日志数量
      # 越大吞吐量越高，但延迟增加
      # 推荐值：200-1000
      batch-size: 500
      
      # 批量处理间隔，单位毫秒（默认1000）
      # ⚠️ 重要：即使未达到batch-size，也会定时触发持久化
      # 越小实时性越好，但数据库压力增大
      # 推荐值：500-2000ms
      batch-interval: 1000
      
      # ========== WAL配置 ==========
      # 是否启用WAL预写日志（默认true）
      # ⚠️ 重要：禁用后性能提升，但服务端崩溃时会丢失未持久化的日志
      wal-enabled: true
      
      # WAL文件目录（默认./logs/wal）
      wal-dir: ./logs/wal
      
      # WAL文件大小限制（默认10MB）
      # ⚠️ 重要：单个WAL文件达到此大小后会滚动创建新文件
      wal-file-size-limit: 10485760
```

**性能调优建议：**

| 场景 | queue-capacity | batch-size | batch-interval | wal-enabled | 说明 |
|------|----------------|------------|----------------|-------------|------|
| 低并发（<100 QPS） | 5000 | 200 | 1000 | true | 默认配置即可 |
| 中并发（100-1000 QPS） | 10000 | 500 | 500 | true | 适当增加批处理大小 |
| 高并发（>1000 QPS） | 20000-50000 | 1000 | 200-500 | false | 可考虑禁用WAL提升性能 |
| 极致性能 | 50000+ | 2000 | 100-200 | false | 牺牲可靠性换取性能 |

**⚠️ 性能影响因素：**

1. **queue-capacity**：
   - 过小：日志可能丢失（队列满时新日志会被丢弃）
   - 过大：占用大量内存，GC压力增大

2. **batch-size**：
   - 过小：频繁调用持久化方法，数据库压力大
   - 过大：延迟增加，内存占用增多

3. **batch-interval**：
   - 过小：频繁触发持久化，CPU和IO压力大
   - 过大：日志实时性差

4. **wal-enabled**：
   - true：保证日志不丢失，但每次写入都要刷盘，性能下降30%-50%
   - false：性能最佳，但服务端崩溃时会丢失未持久化的日志

**推荐配置：**
- 生产环境：`wal-enabled=true`，保证数据可靠性
- 测试环境：`wal-enabled=false`，追求性能
- 高吞吐场景：增大`batch-size`和`queue-capacity`，减小`batch-interval`

### 步骤3：实现日志持久化逻辑（必须）

继承 `AbsLogsSaveManager`，定义日志的持久化存储逻辑：

```java
@Component
public class MyLogsSaveManager extends AbsLogsSaveManager {
    
    @Autowired
    private LogTcpServerProperties properties;
    
    @Autowired
    private ElasticsearchRestTemplate esTemplate;
    
    @Override
    protected LogTcpServerProperties getProperties() {
        return properties;
    }
    
    @Override
    protected void persistence(List<LogDataEvent> logsToSave) {
        // 将日志批量保存到Elasticsearch
        List<IndexQuery> queries = new ArrayList<>();
        
        for (LogDataEvent event : logsToSave) {
            IndexQuery query = new IndexQueryBuilder()
                .withId(event.getTraceId())
                .withObject(event)
                .build();
            queries.add(query);
        }
        
        // 批量索引
        esTemplate.bulkIndex(queries, IndexCoordinates.of("app-logs"));
        
        log.info("批量保存日志到ES，数量: {}", logsToSave.size());
    }
}
```

**重要说明：**
- `persistence()` 方法必须保证幂等性（WAL恢复可能重放已成功的日志）
- 建议使用批量操作提升性能（如ES的bulkIndex、数据库的batchInsert）
- 如果持久化失败，框架会自动写入WAL文件，后续会重试

## 使用示例

### 1. 客户端自动日志收集

框架通过`LogInterceptor`自动拦截所有HTTP请求，无需手动调用：

```java
// 开发者无需编写任何代码，框架自动处理
// LogInterceptor.preHandle() - 记录开始时间、生成TraceId
// LogInterceptor.afterCompletion() - 采集日志并发送
```

**工作流程：**

1. **LogFilter** 包装HttpServletRequest，缓存请求体
2. **LogInterceptor.preHandle()** 生成TraceId，记录开始时间
3. **执行业务逻辑**
4. **LogInterceptor.afterCompletion()** 调用`LogService.send()`采集日志
5. **DefaultLogService** 构建LogDataEvent对象
6. **BufferedLogManager** 异步入队，批量发送到服务端

### 2. 日志数据结构

`LogDataEvent`包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| traceId | String | 全局唯一追踪ID |
| userId | String | 操作用户ID |
| nickname | String | 用户昵称 |
| operUrl | String | 请求URL |
| method | String | HTTP方法 |
| operIp | String | 客户端IP |
| operParam | String | 请求参数（JSON格式） |
| title | String | 接口描述（从@Operation注解获取） |
| status | int | HTTP状态码 |
| errorMsg | String | 错误消息 |
| errorData | String | 异常堆栈 |
| requestTime | long | 请求耗时（毫秒） |
| createTimestamp | long | 创建时间戳 |

### 3. 服务端日志持久化

服务端接收客户端发送的日志后，通过`AbsLogsSaveManager`进行持久化：

```java
@Component
public class DatabaseLogsSaveManager extends AbsLogsSaveManager {
    
    @Autowired
    private LogTcpServerProperties properties;
    
    @Autowired
    private SysOperLogMapper operLogMapper;
    
    @Override
    protected LogTcpServerProperties getProperties() {
        return properties;
    }
    
    @Override
    protected void persistence(List<LogDataEvent> logsToSave) {
        // 转换为数据库实体
        List<SysOperLog> operLogs = logsToSave.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
        
        // 批量插入数据库
        operLogMapper.insertBatch(operLogs);
        
        log.info("批量保存日志到数据库，数量: {}", operLogs.size());
    }
    
    private SysOperLog convertToEntity(LogDataEvent event) {
        SysOperLog log = new SysOperLog();
        log.setTraceId(event.getTraceId());
        log.setUserId(event.getUserId());
        log.setNickname(event.getNickname());
        log.setOperUrl(event.getOperUrl());
        log.setMethod(event.getMethod());
        log.setOperIp(event.getOperIp());
        log.setOperParam(event.getOperParam());
        log.setTitle(event.getTitle());
        log.setStatus(event.getStatus());
        log.setErrorMsg(event.getErrorMsg());
        log.setErrorData(event.getErrorData());
        log.setRequestTime(event.getRequestTime());
        log.setCreateTime(new Date(event.getCreateTimestamp() * 1000));
        return log;
    }
}
```

### 4. WAL机制说明

WAL（Write-Ahead Logging）预写日志机制保证日志不丢失：

- **正常流程**：日志入队 → 批量取出 → 持久化成功
- **队列满**：日志直接写入WAL文件 → 定时任务恢复
- **持久化失败**：失败的批次写入WAL文件 → 定时任务重试
- **进程崩溃**：重启后自动扫描WAL目录 → 恢复未持久化的日志

**WAL文件特点：**
- 每行一个JSON格式的LogDataEvent
- 批量flush（每100条或1秒）
- 持久化成功后自动删除WAL文件
- 支持多文件轮转

---

[返回主文档](../README.md)

# simple-common-xxljob

## 模块介绍

XXL-JOB定时任务模块封装，支持分布式任务调度、失败重试。

## 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 任务调度 | `XxlJobSpringExecutor` | XXL-JOB执行器 |
| 任务注解 | `@XxlJob` | 定时任务注解 |
| 配置类 | `XxlJobConfig` | XXL-JOB配置管理 |

## 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-xxljob</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置XXL-JOB（必须）**

```yaml
xxl:
  job:
    # 是否开启XXL-JOB（默认true）
    open: true
    # XXL-JOB Admin地址
    admin-addresses: http://127.0.0.1:8080/xxl-job-admin
    # AccessToken（与Admin配置一致）
    access-token: default_token
    executor:
      # 执行器AppName
      appname: xxl-job-executor-sample
      # 执行器端口（默认9999）
      port: 9999
      # 日志路径
      log-path: /data/applogs/xxl-job/jobhandler
      # 日志保留天数（默认30）
      log-retention-days: 30
```

**重要说明：**
- 需要先在XXL-JOB Admin后台创建执行器和任务
- `appname` 必须与Admin后台配置的执衎器名称一致
- `access-token` 必须与Admin后台配置一致
- 任务方法必须使用 `@XxlJob` 注解标注

## 使用示例

### 1. 定义定时任务

```java
@Component
public class SampleTask {
    
    /**
     * 简单任务
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        log.info("定时任务执行");
        
        // 业务逻辑
        doSomething();
    }
    
    /**
     * 分片广播任务
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        
        log.info("分片参数：当前分片={}, 总分片={}", shardIndex, shardTotal);
        
        // 根据分片参数处理数据
        List<Data> dataList = getDataByShard(shardIndex, shardTotal);
        for (Data data : dataList) {
            processData(data);
        }
    }
    
    /**
     * 带参数的任务
     */
    @XxlJob("paramJobHandler")
    public void paramJobHandler() throws Exception {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        log.info("任务参数: {}", param);
        
        // 解析参数并执行业务
        Map<String, Object> params = JsonUtils.parse(param, Map.class);
        executeBusiness(params);
    }
}
```

### 2. 任务日志

```java
@XxlJob("logJobHandler")
public void logJobHandler() throws Exception {
    // 记录任务日志
    XxlJobHelper.log("任务开始执行");
    
    try {
        // 业务逻辑
        doSomething();
        
        XxlJobHelper.log("任务执行成功");
        XxlJobHelper.handleSuccess();
    } catch (Exception e) {
        XxlJobHelper.log("任务执行失败: {}", e.getMessage());
        XxlJobHelper.handleFail(e.getMessage());
    }
}
```

### 3. 任务阻塞策略

在XXL-JOB管理后台配置：
- **单机串行**：调度请求进入单机执行队列后，串行执行
- **丢弃后续调度**：调度请求进入单机执行队列后，发现之前的调度还未执行完，则丢弃本次调度
- **覆盖之前调度**：调度请求进入单机执行队列后，发现之前的调度还未执行完，则终止之前的调度并执行本次调度

---

[返回主文档](../README.md)

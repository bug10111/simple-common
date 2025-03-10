# simple-common

# 介绍
多年的码农生涯，发现很多时候都是在重复造轮子。一直都想写一套自己的东西，让我们在开发新的项目的时候能够更快速，更便捷。同时在旧的项目里面也可以无侵入的直接引入使用部份功能。所以框架会先从公共的工具、包、组件开始，慢慢集成为框架。框架可以单体运行，也可快速拆解成微服务，尽量让更多的小伙伴都能直接使用，并且过渡公司不同的发展阶段。
# 命名说明
- service后缀：表示为本模块对外提供的核心功能，也就是说想知道模块主要提供了那些功能，看这个后缀命名的类就行。一般表现为接口，或者接口实现
- manager后缀：表示为本模块功能提供支撑的接口
- Default前缀：表示默认实现


# 功能包介绍
## 1. 文件处理包：simple-common-annex minio快速安装：https://blog.csdn.net/weixin_46551671/article/details/134285377
- 适配阿里云oss和minio，通过配置文件类型一键切换，无需其他操作
- 封装常用上传、下载、删除、公私有有文件创建等

## 2. 核心包：simple-common-core
- 统一异常
- 统一返回数据对象
- Aviator规则引擎
- 循环任务调度
- ScheduledThreadPoolExecutor 任务调度，支持延迟任务和循环任务
- 各类工具包（主要源于hutool：https://www.hutool.cn/）

## 3. doc文档操作：simple-common-doc 
- 封装doc文档常用填充功能，包括填充文案、带颜色文案、超链接、图片、表格等，通过构建器模式一行代码实现

## 4. eventbus:simple-sommon-eventbus
- 这个用的人可能比较少，玩ddd的一般不陌生。但是个人认为这才是消息队列的正确打开方式。简单介绍下EventBus（事件总线）是一种基于发布-订阅模式的通信机制，用于实现组件/模块间的解耦通信，它通过统一的事件管理中心，让事件的发送者（发布者）和接收者（订阅者）无需直接依赖对方即可交互。
- 当前模块支持基于反射的同步消息机制（不依赖任何消息队列，只能同一个运行项目之间能通讯），和基于消息队列的消息机制，消息队列目前接入了rabbitmq
- 配置文件一键切换消息机制类型，不需要任何代码改动，可以完美过度项目的不同阶段
- 后续接入rocketmq

## 5. excel：simple-common-excel
- 接入了poi和easy excel
- 封装了常用导入导出功能

## 6. 接口请求logs：simple-common-logs 
- 记录请求url、IP、用户、请求参数、请求结果、异常信息等核心数据
- 分Client 和 Server，Client收集日志，Server保存
- 通讯基于eventbus 队列机制

## 7. mybatis plus 基础依赖包：simple-common-mp postgresql快速安装：https://blog.csdn.net/weixin_46551671/article/details/133325198
- 集中化集成相关依赖，方便管理和处理漏洞
- 封装page，支持前端控制的字段排序，类似于jpa,可扩展返回查询字段（已预留，暂未实现）

## 8. rabbitmq包：simple-common-rabbitmq rabbitmq快速安装：https://blog.csdn.net/weixin_46551671/article/details/133322778
- 消息发送封装，包括延迟消息
- 注解形式，处理重复消费、消费失败、失败策略略等，可自定义重试次数等参数，可扩展定义自己的校验逻辑

## 9. redis包：simple-common-redis redis快速安装：https://blog.csdn.net/weixin_46551671/article/details/133306011
- 分布式锁，包含公平锁、非公平锁、信号量、闭锁。完整的封装，一键式调用。
- 自定义缓存注解，使用过程中常见的雪崩、穿透、击穿都做了处理
- 自定义限流注解，采用令牌桶方式。包含单位时长、单位时长允许次数、等待时间、最高等待时间后的异常信息等常用核心参数。

## 10. sms包：simple-common-sms
- 封装阿里云短信发送，包含常用的校验策略。可自由扩展。

## 11.xxl-job定时任务调度：simple-common-xxljob
- 同上simple-common-mp，属于功能集成模块，方便扩展和管理，使用时候直接引入即可（xxl-job：https://gitee.com/xuxueli0323/xxl-job） 
- 封装部份远程调用xxljob的功能接口,适配有时候需要程序中，业务自己建任务的情况，包括任务的增删改查、启动关闭

## 12. 测试包：simple-common-test
- 使用样例和测试demo，可以直接运行

# 公众号地址
 **框架的最新动态和用法，会在第一时间在公众号发布，欢迎关注点赞** 

![输入图片说明](image.png)

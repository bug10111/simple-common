package com.simple.common.rabbitmq.aspeck;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.manager.AckRMQManager;
import com.simple.common.rabbitmq.common.manager.FailedRMQManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.service.DeadLetterService;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import com.simple.common.rabbitmq.manager.RedisRepeatRMQManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: RabbitMQ消息消费处理
 *
 * @author qty
 */
@Order(1)
@Aspect
@Slf4j
@Component
public class RabbitMqProcessAspect {

    @Autowired
    private List<RabbitMqProcess> listManager;

    @Autowired
    private RedisRepeatRMQManager repeatRMQManager;

    @Autowired
    private FailedRMQManager failedRMQManager;

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    @Autowired
    private AckRMQManager ackRMQManager;

    @Autowired
    private DeadLetterService deadLetterService;

    @SneakyThrows
    @Around("@annotation(com.simple.common.rabbitmq.annotation.RabbitMqConsumption)")
    public void around(ProceedingJoinPoint joinPoint) {

        //获取注解先关参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RabbitMqConsumption rabbitMqConsumption = method.getAnnotation(RabbitMqConsumption.class);

        //获取参数
        Object[] args = joinPoint.getArgs();
        Message message = (Message) args[0];
        Channel channel = (Channel) args[1];

        String correlationId = message.getMessageProperties().getCorrelationId();

        //业务中有任何异常，都视为消费失败
        try {
            boolean register = repeatRMQManager.register(message.getMessageProperties().getConsumerQueue(), correlationId, rabbitMqConsumption.businessTime(),
                                                         rabbitMqConsumption.timeUnit());
            if (!register) {
                log.info("队列exchange[{}]=>key[{}]=>id[{}]已消费，不执行业务", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
                         correlationId);

                //手动提交，表示已经消费
                ackRMQManager.basicAck(channel, message.getMessageProperties().getDeliveryTag());
                return;
            }

            //业务执行的前置判断
            listManager.forEach(manager -> {
                if (manager.getProcess().isExecute()) {
                    manager.execution(message, channel, rabbitMqConsumption);
                    log.trace("队列exchange[{}]=>key[{}]=>id[{}]执行[{}]成功！", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
                              correlationId, manager.getProcess().getMsg());
                }
            });

            //执行业务
            joinPoint.proceed(args);

            //业务执行完毕，立即更新重复时长
            repeatRMQManager.update(message.getMessageProperties().getConsumerQueue(), correlationId, rabbitMqConsumption.businessTime(), rabbitMqConsumption.timeUnit());

            //ack
            ackRMQManager.basicAck(channel, message.getMessageProperties().getDeliveryTag());
            if (log.isDebugEnabled()) {
                log.debug("队列exchange[{}]=>key[{}]=>id[{}]==>[{}]消费成功！", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
                          correlationId, new String(message.getBody()));
            }

        } catch (Exception e) {
            log.error("队列exchange[{}]=>key[{}]=>id[{}]==>[{}]消费失败！", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
                      correlationId, new String(message.getBody()), e);

            //无法继续消费
            try {
                if (!consumptionFailed(correlationId, message, channel, rabbitMqConsumption.retryCount(), e)) {
                    if (log.isDebugEnabled()) {
                        log.debug("队列exchange[{}]=>key[{}]=>id[{}]执行保存策略成功！", message.getMessageProperties().getReceivedExchange(),
                                  message.getMessageProperties().getReceivedRoutingKey(), correlationId);
                    }
                }
            } catch (Exception ee) {
                log.error("队列exchange[{}]=>key[{}]=>id[{}]重试失败！", message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
                          correlationId);
            } finally {
                //只要接收到消息，无论是否报错都要使消息可以继续执行
                repeatRMQManager.remove(message.getMessageProperties().getConsumerQueue(), correlationId);
            }
        }
    }

    @SneakyThrows
    protected boolean consumptionFailed(String correlationId, Message message, Channel channel, int retryCount, Exception e) {

        //消息重试，Redis key
        String failedKey = failedRMQManager.getFailedMqRedisKey(rabbitMqProperties.getCalculation(), message.getMessageProperties().getConsumerQueue(), correlationId);

        //获取储存的，消费失败的次数
        Long failedSum = failedRMQManager.getFailedSum(failedKey);

        //小于最大重试次数，开始重试策略
        if (retryCount > failedSum) {

            //开始计数失败次数
            failedSum = failedRMQManager.increment(failedKey);

            //手动应答，拒绝消息并将消息放回队列
            ackRMQManager.basicNack(channel, message.getMessageProperties().getDeliveryTag(), true);
            if (log.isDebugEnabled()) {
                log.debug("队列exchange[{}]=>key[{}]=>id[{}]开始第{}次重试！，消息已放回队列！", message.getMessageProperties().getReceivedExchange(),
                          message.getMessageProperties().getReceivedRoutingKey(), correlationId, failedSum);
            }
            return true;
        }

        //放弃重试
        ackRMQManager.basicNack(channel, message.getMessageProperties().getDeliveryTag(), false);
        if (log.isDebugEnabled()) {
            log.debug("队列exchange[{}]=>key[{}]=>id[{}]重试次数已达上限，消息放弃，开始执行保存策略！", message.getMessageProperties().getReceivedExchange(),
                      message.getMessageProperties().getReceivedRoutingKey(), correlationId);
        }

        //执行数据保存
        deadLetterService.save(message, e);

        //删除重试计数
        failedRMQManager.delete(failedKey);
        return false;
    }
}

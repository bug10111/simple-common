package com.simple.common.rabbitmq.common.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

public interface RetryMessageManager {

    void handleBusinessFailureRetry(String retryKey, Message message, Channel channel, int maxRetryCount, Exception cause, DefaultMessage defaultMessage);

    void handleVerificationFailureRetry(String verifyRetryKey, Message message, Channel channel, int maxRetryCount, Exception cause, DefaultMessage defaultMessage);
}
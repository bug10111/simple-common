package com.simple.common.rabbitmq.service;

import com.simple.common.rabbitmq.common.service.DeadLetterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultDeadLetterService implements DeadLetterService {

    @Override
    public void save(String exchange, String key, String queue, String body, Exception e) {
        log.error("队列exchange[{}]=>key[{}]=>queue[{}]==>[{}]执行默认保存策略，请实现接口[DeadLetterService]进行保存！",
                exchange, key, queue, body);
    }
}
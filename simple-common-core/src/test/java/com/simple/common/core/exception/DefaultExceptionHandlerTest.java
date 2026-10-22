package com.simple.common.core.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器状态码映射单测:校验失败映射400,资源不存在映射404
 *
 * @author qty
 */
class DefaultExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * 校验失败请求体,名称留空即触发必填校验
     */
    static class CreateProbeRequest {

        @NotEmpty(message = "名称不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 异常探针控制器,分别触发校验失败与资源不存在两种异常
     */
    @RestController
    @RequestMapping("/probe")
    static class ExceptionProbeController {

        /**
         * 携带校验注解的请求体,校验失败时框架抛出 MethodArgumentNotValidException
         */
        @PostMapping("/valid")
        public String valid(@Valid @RequestBody CreateProbeRequest request) {
            return request.getName();
        }

        /**
         * 直接抛出资源不存在异常,模拟静态资源与未匹配路由场景
         */
        @GetMapping("/missing")
        public String missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "config/missing.json");
        }
    }

    /**
     * 以独立控制器装配全局异常处理器,隔离验证各异常分支的状态码映射
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionProbeController())
                .setControllerAdvice(new DefaultExceptionHandler())
                .build();
    }

    /**
     * 校验失败必须映射400,响应code与message取字段级校验消息
     */
    @Test
    void validationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("名称不能为空"));
    }

    /**
     * 资源不存在必须映射404,响应code为404且message携带资源路径
     */
    @Test
    void resourceNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("请求的资源不存在：config/missing.json"));
    }
}

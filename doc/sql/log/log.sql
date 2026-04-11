-- 创建操作日志表
CREATE TABLE sys_operation_logs
(
    id            VARCHAR(64) PRIMARY KEY, -- 主键ID
    trace_id      VARCHAR(64),             -- 追踪ID
    title         VARCHAR(200),            -- 方法名称/操作标题
    method        VARCHAR(10),             -- 请求方式
    oper_url      VARCHAR(500),            -- 请求URL
    oper_ip       VARCHAR(50),             -- 主机地址
    oper_location VARCHAR(100),            -- 操作地点
    user_id       BIGINT,                  -- 操作人员ID
    nickname      VARCHAR(100),            -- 用户名
    oper_name     VARCHAR(200),            -- 操作名称
    oper_param    TEXT,                    -- 请求参数
    status        INTEGER,                 -- 操作状态（0成功 1失败）
    error_msg     TEXT,                    -- 错误消息
    error_data    TEXT,                    -- 异常信息（堆栈）
    request_time  TIMESTAMP,               -- 接口耗时对应的请求时间
    create_time   TIMESTAMP                -- 创建时间
);

-- 添加表注释
COMMENT
ON TABLE sys_operation_logs IS '操作日志表';

-- 添加字段注释
COMMENT
ON COLUMN sys_operation_logs.id IS '主键ID';
COMMENT
ON COLUMN sys_operation_logs.trace_id IS '追踪ID';
COMMENT
ON COLUMN sys_operation_logs.title IS '方法名称/操作标题';
COMMENT
ON COLUMN sys_operation_logs.method IS '请求方式';
COMMENT
ON COLUMN sys_operation_logs.oper_url IS '请求URL';
COMMENT
ON COLUMN sys_operation_logs.oper_ip IS '主机地址';
COMMENT
ON COLUMN sys_operation_logs.oper_location IS '操作地点';
COMMENT
ON COLUMN sys_operation_logs.user_id IS '操作人员ID';
COMMENT
ON COLUMN sys_operation_logs.nickname IS '用户名';
COMMENT
ON COLUMN sys_operation_logs.oper_name IS '操作名称';
COMMENT
ON COLUMN sys_operation_logs.oper_param IS '请求参数';
COMMENT
ON COLUMN sys_operation_logs.status IS '操作状态（0成功 1失败）';
COMMENT
ON COLUMN sys_operation_logs.error_msg IS '错误消息';
COMMENT
ON COLUMN sys_operation_logs.error_data IS '异常信息（堆栈）';
COMMENT
ON COLUMN sys_operation_logs.request_time IS '接口耗时对应的请求时间';
COMMENT
ON COLUMN sys_operation_logs.create_time IS '创建时间';

-- 可选：为常用查询字段添加索引（根据实际查询场景调整）
CREATE INDEX idx_sys_operation_logs_create_time ON sys_operation_logs (create_time);
CREATE INDEX idx_sys_operation_logs_trace_id ON sys_operation_logs (trace_id);
CREATE INDEX idx_sys_operation_logs_user_id ON sys_operation_logs (user_id);
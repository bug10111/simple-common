-- 固定字段填充探针表：固定字段全部 NOT NULL，用于复现未填充时的非空约束失败
-- 注意：本表列清单必须与下方 t_fill_annotated_probe 保持同构（固定字段集合一致），
--       调整其中一张的固定字段时必须同步另一张，否则注解/非注解对照测试失去可比性
DROP TABLE IF EXISTS t_fill_probe;
CREATE TABLE t_fill_probe (
    id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    create_user_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    status INT NOT NULL,
    reserve VARCHAR(255) NOT NULL,
    remark VARCHAR(255) NOT NULL,
    -- name 是全表唯一可空列：专用于覆盖「可空列不参与填充、null 显式落库」的边界断言
    name VARCHAR(64),
    PRIMARY KEY (id)
);

-- 带 fill 注解实体对照表：验证原生填充链路不受兜底拦截器影响
-- 注意：本表列清单必须与上方 t_fill_probe 保持同构（固定字段集合一致）
DROP TABLE IF EXISTS t_fill_annotated_probe;
CREATE TABLE t_fill_annotated_probe (
    id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    create_user_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    status INT NOT NULL,
    reserve VARCHAR(255) NOT NULL,
    remark VARCHAR(255) NOT NULL,
    name VARCHAR(64),
    PRIMARY KEY (id)
);

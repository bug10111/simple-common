-- 固定字段填充探针表：固定字段全部 NOT NULL，用于复现未填充时的非空约束失败
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
    name VARCHAR(64),
    PRIMARY KEY (id)
);

-- 带 fill 注解实体对照表：验证原生填充链路不受兜底拦截器影响
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

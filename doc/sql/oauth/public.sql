/*
 Navicat Premium Dump SQL

 Source Server         : simple
 Source Server Type    : PostgreSQL
 Source Server Version : 140012 (140012)
 Source Host           : 192.168.101.234:5432
 Source Catalog        : simple-oauth
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 140012 (140012)
 File Encoding         : 65001

 Date: 18/12/2024 10:33:12
*/


-- ----------------------------
-- Table structure for sys_advertisement
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_advertisement";
CREATE TABLE "public"."sys_advertisement" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "client_id" varchar(255) COLLATE "pg_catalog"."default",
  "type" varchar(50) COLLATE "pg_catalog"."default",
  "image" varchar(255) COLLATE "pg_catalog"."default",
  "is_link" varchar(50) COLLATE "pg_catalog"."default",
  "link" varchar(2000) COLLATE "pg_catalog"."default",
  "sort" int2,
  "begin_time" timestamp(6),
  "end_time" timestamp(6),
  "status" int2,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_advertisement"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_advertisement"."name" IS '名称';
COMMENT ON COLUMN "public"."sys_advertisement"."client_id" IS '客户端id';
COMMENT ON COLUMN "public"."sys_advertisement"."type" IS '类型（字典）';
COMMENT ON COLUMN "public"."sys_advertisement"."image" IS '图片';
COMMENT ON COLUMN "public"."sys_advertisement"."is_link" IS '是否有外链';
COMMENT ON COLUMN "public"."sys_advertisement"."link" IS '外链地址';
COMMENT ON COLUMN "public"."sys_advertisement"."sort" IS '排序';
COMMENT ON COLUMN "public"."sys_advertisement"."begin_time" IS '开始时间';
COMMENT ON COLUMN "public"."sys_advertisement"."end_time" IS '结束时间';
COMMENT ON COLUMN "public"."sys_advertisement"."status" IS '状态';
COMMENT ON COLUMN "public"."sys_advertisement"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_advertisement"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_advertisement" IS '广告表';

-- ----------------------------
-- Table structure for sys_annex
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_annex";
CREATE TABLE "public"."sys_annex" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default",
  "total_size" int8,
  "algorithm_value" varchar(255) COLLATE "pg_catalog"."default",
  "algorithm_type" varchar(255) COLLATE "pg_catalog"."default",
  "suffix" varchar(50) COLLATE "pg_catalog"."default",
  "save_url" varchar(800) COLLATE "pg_catalog"."default",
  "share_type" int2,
  "application_name" varchar(255) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_annex"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_annex"."name" IS '名称';
COMMENT ON COLUMN "public"."sys_annex"."total_size" IS '文件总大小';
COMMENT ON COLUMN "public"."sys_annex"."algorithm_value" IS '摘要算法值';
COMMENT ON COLUMN "public"."sys_annex"."algorithm_type" IS '摘要算法类型';
COMMENT ON COLUMN "public"."sys_annex"."suffix" IS '文件扩展名（不带.）';
COMMENT ON COLUMN "public"."sys_annex"."save_url" IS '文件在完整url';
COMMENT ON COLUMN "public"."sys_annex"."share_type" IS '附件类型';
COMMENT ON COLUMN "public"."sys_annex"."application_name" IS '系统名称';
COMMENT ON COLUMN "public"."sys_annex"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_annex"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_annex" IS '附件';

-- ----------------------------
-- Table structure for sys_announcement
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_announcement";
CREATE TABLE "public"."sys_announcement" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "data" text COLLATE "pg_catalog"."default",
  "begin_time" timestamp(6),
  "end_time" timestamp(6),
  "status" int2 DEFAULT 1,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_announcement"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_announcement"."title" IS '标题';
COMMENT ON COLUMN "public"."sys_announcement"."data" IS '数据';
COMMENT ON COLUMN "public"."sys_announcement"."begin_time" IS '开始时间';
COMMENT ON COLUMN "public"."sys_announcement"."end_time" IS '结束时间';
COMMENT ON COLUMN "public"."sys_announcement"."status" IS '状态';
COMMENT ON COLUMN "public"."sys_announcement"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_announcement"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_announcement" IS '系统公告';

-- ----------------------------
-- Table structure for sys_client_details
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_client_details";
CREATE TABLE "public"."sys_client_details" (
  "id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "server" varchar(255) COLLATE "pg_catalog"."default",
  "server_type" varchar(255) COLLATE "pg_catalog"."default",
  "client_name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id" varchar(128) COLLATE "pg_catalog"."default",
  "client_secret" varchar(500) COLLATE "pg_catalog"."default",
  "resource_ids" varchar(128) COLLATE "pg_catalog"."default",
  "scope" varchar(128) COLLATE "pg_catalog"."default",
  "access_token_validity" int4,
  "refresh_token_validity" int4,
  "hs_key" varchar(500) COLLATE "pg_catalog"."default",
  "rsa_public" text COLLATE "pg_catalog"."default",
  "rsa_private" text COLLATE "pg_catalog"."default",
  "has_wx" varchar(20) COLLATE "pg_catalog"."default",
  "wx_app_id" varchar(200) COLLATE "pg_catalog"."default",
  "wx_app_secret" varchar(255) COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "reserve" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_client_details"."id" IS '客户端信息id';
COMMENT ON COLUMN "public"."sys_client_details"."server" IS '服务（字典）';
COMMENT ON COLUMN "public"."sys_client_details"."server_type" IS '服务类型';
COMMENT ON COLUMN "public"."sys_client_details"."client_name" IS '客户端名称';
COMMENT ON COLUMN "public"."sys_client_details"."client_id" IS '客户端（如：xiaoyue_client）';
COMMENT ON COLUMN "public"."sys_client_details"."client_secret" IS '客户端密码（要加密后存储)，即秘钥';
COMMENT ON COLUMN "public"."sys_client_details"."resource_ids" IS '预留字段，客户端能访问的资源id集合（微服务名称），多个用逗号分隔';
COMMENT ON COLUMN "public"."sys_client_details"."scope" IS '作用域all,write,read';
COMMENT ON COLUMN "public"."sys_client_details"."access_token_validity" IS '（可选）token有效时间（单位秒），不填默认(60 * 60 * 12, 12小时)';
COMMENT ON COLUMN "public"."sys_client_details"."refresh_token_validity" IS '（可选）刷新令牌的有效时间（单位秒），不填默认(60 * 60 * 24 * 30, 30天)';
COMMENT ON COLUMN "public"."sys_client_details"."hs_key" IS '32位密钥字符串';
COMMENT ON COLUMN "public"."sys_client_details"."rsa_public" IS 'RSA公钥';
COMMENT ON COLUMN "public"."sys_client_details"."rsa_private" IS 'RSA私钥';
COMMENT ON COLUMN "public"."sys_client_details"."has_wx" IS '包含微信';
COMMENT ON COLUMN "public"."sys_client_details"."wx_app_id" IS '微信appid';
COMMENT ON COLUMN "public"."sys_client_details"."wx_app_secret" IS '微信appSecret密钥';
COMMENT ON COLUMN "public"."sys_client_details"."status" IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN "public"."sys_client_details"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_client_details"."reserve" IS '扩展';
COMMENT ON COLUMN "public"."sys_client_details"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_client_details"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_client_details" IS '客户端信息';

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_data";
CREATE TABLE "public"."sys_dict_data" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "serial" int4,
  "dict_label" varchar(20) COLLATE "pg_catalog"."default",
  "dict_value" varchar(20) COLLATE "pg_catalog"."default",
  "dict_type" varchar(20) COLLATE "pg_catalog"."default",
  "deleted" int2 DEFAULT 0,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_dict_data"."id" IS '字典编码';
COMMENT ON COLUMN "public"."sys_dict_data"."serial" IS '字典排序';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_label" IS '字典标签';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_value" IS '字典键值（key）';
COMMENT ON COLUMN "public"."sys_dict_data"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."sys_dict_data"."deleted" IS '删除';
COMMENT ON COLUMN "public"."sys_dict_data"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_dict_data"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_dict_data"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_dict_data" IS '字典数据';

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_type";
CREATE TABLE "public"."sys_dict_type" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_name" varchar(20) COLLATE "pg_catalog"."default",
  "dict_type" varchar(20) COLLATE "pg_catalog"."default",
  "deleted" int2 DEFAULT 0,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_dict_type"."id" IS '字典主键';
COMMENT ON COLUMN "public"."sys_dict_type"."dict_name" IS '字典名称';
COMMENT ON COLUMN "public"."sys_dict_type"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."sys_dict_type"."deleted" IS '删除：1-已删除，0-未删除';
COMMENT ON COLUMN "public"."sys_dict_type"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_dict_type"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_dict_type"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_dict_type" IS '字典类型';

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_login_log";
CREATE TABLE "public"."sys_login_log" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "login_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8,
  "user_name" varchar(50) COLLATE "pg_catalog"."default",
  "login_ip" varchar(128) COLLATE "pg_catalog"."default",
  "login_location" varchar(255) COLLATE "pg_catalog"."default",
  "time" timestamp(6) NOT NULL,
  "create_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_login_log"."id" IS '日志主键';
COMMENT ON COLUMN "public"."sys_login_log"."login_type" IS '登录类型';
COMMENT ON COLUMN "public"."sys_login_log"."user_id" IS '用户id';
COMMENT ON COLUMN "public"."sys_login_log"."user_name" IS '用户名称';
COMMENT ON COLUMN "public"."sys_login_log"."login_ip" IS '主机ip';
COMMENT ON COLUMN "public"."sys_login_log"."login_location" IS '登录地点';
COMMENT ON COLUMN "public"."sys_login_log"."time" IS '登陆时间（年月日，用于统计）';
COMMENT ON COLUMN "public"."sys_login_log"."create_time" IS '创建时间';
COMMENT ON TABLE "public"."sys_login_log" IS '登录日志';

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_menu";
CREATE TABLE "public"."sys_menu" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "menu_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" varchar(255) COLLATE "pg_catalog"."default",
  "serial" int4,
  "path" varchar(200) COLLATE "pg_catalog"."default",
  "component" varchar(255) COLLATE "pg_catalog"."default",
  "query" varchar(255) COLLATE "pg_catalog"."default",
  "is_frame" int2,
  "redirect_url" text COLLATE "pg_catalog"."default",
  "menu_type" varchar(50) COLLATE "pg_catalog"."default",
  "perms" varchar(100) COLLATE "pg_catalog"."default",
  "icon" varchar(100) COLLATE "pg_catalog"."default",
  "reserve" text COLLATE "pg_catalog"."default",
  "remark" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_menu"."id" IS '菜单id';
COMMENT ON COLUMN "public"."sys_menu"."client_id" IS '客户端ID';
COMMENT ON COLUMN "public"."sys_menu"."menu_name" IS '菜单名称';
COMMENT ON COLUMN "public"."sys_menu"."code" IS '菜单标识';
COMMENT ON COLUMN "public"."sys_menu"."parent_id" IS '父菜单ID';
COMMENT ON COLUMN "public"."sys_menu"."serial" IS '显示顺序';
COMMENT ON COLUMN "public"."sys_menu"."path" IS '路由地址';
COMMENT ON COLUMN "public"."sys_menu"."component" IS '组件路径';
COMMENT ON COLUMN "public"."sys_menu"."query" IS '路由参数';
COMMENT ON COLUMN "public"."sys_menu"."is_frame" IS '是否为外链：1-是，0-否';
COMMENT ON COLUMN "public"."sys_menu"."redirect_url" IS '重定向路径';
COMMENT ON COLUMN "public"."sys_menu"."menu_type" IS '菜单类型（字典，例如M目录 C菜单 F按钮）';
COMMENT ON COLUMN "public"."sys_menu"."perms" IS '权限标识';
COMMENT ON COLUMN "public"."sys_menu"."icon" IS '菜单图标';
COMMENT ON COLUMN "public"."sys_menu"."reserve" IS '扩展';
COMMENT ON COLUMN "public"."sys_menu"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_menu"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_menu"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_menu" IS '菜单权限';

-- ----------------------------
-- Table structure for sys_operation_logs
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_operation_logs";
CREATE TABLE "public"."sys_operation_logs" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "method" varchar(255) COLLATE "pg_catalog"."default",
  "oper_url" varchar(255) COLLATE "pg_catalog"."default",
  "oper_ip" varchar(255) COLLATE "pg_catalog"."default",
  "oper_location" varchar(255) COLLATE "pg_catalog"."default",
  "user_id" varchar(255) COLLATE "pg_catalog"."default",
  "nickname" varchar(255) COLLATE "pg_catalog"."default",
  "oper_param" text COLLATE "pg_catalog"."default",
  "status" int2,
  "error_msg" varchar(255) COLLATE "pg_catalog"."default",
  "request_time" int4,
  "error_data" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_operation_logs"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_operation_logs"."title" IS '方法名称';
COMMENT ON COLUMN "public"."sys_operation_logs"."method" IS '请求方式';
COMMENT ON COLUMN "public"."sys_operation_logs"."oper_url" IS '请求URL';
COMMENT ON COLUMN "public"."sys_operation_logs"."oper_ip" IS '主机地址';
COMMENT ON COLUMN "public"."sys_operation_logs"."oper_location" IS '操作地点';
COMMENT ON COLUMN "public"."sys_operation_logs"."user_id" IS '操作人员id';
COMMENT ON COLUMN "public"."sys_operation_logs"."nickname" IS '用户名称';
COMMENT ON COLUMN "public"."sys_operation_logs"."oper_param" IS '请求参数';
COMMENT ON COLUMN "public"."sys_operation_logs"."status" IS '操作状态';
COMMENT ON COLUMN "public"."sys_operation_logs"."error_msg" IS '错误消息';
COMMENT ON COLUMN "public"."sys_operation_logs"."request_time" IS '接口耗时';
COMMENT ON COLUMN "public"."sys_operation_logs"."error_data" IS '异常信息详情';
COMMENT ON COLUMN "public"."sys_operation_logs"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_operation_logs"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_operation_logs" IS '操作日志';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "server" varchar(255) COLLATE "pg_catalog"."default",
  "role_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "role_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "type" int2,
  "serial" int4,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_role"."id" IS '角色id';
COMMENT ON COLUMN "public"."sys_role"."server" IS '服务（字典）';
COMMENT ON COLUMN "public"."sys_role"."role_name" IS '角色名称';
COMMENT ON COLUMN "public"."sys_role"."role_key" IS '角色权限字符串';
COMMENT ON COLUMN "public"."sys_role"."type" IS '类型';
COMMENT ON COLUMN "public"."sys_role"."serial" IS '显示顺序';
COMMENT ON COLUMN "public"."sys_role"."remark" IS '备注';
COMMENT ON COLUMN "public"."sys_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_role"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_role" IS '角色信息';

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_menu";
CREATE TABLE "public"."sys_role_menu" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "role_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "menu_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_role_menu"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_role_menu"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."sys_role_menu"."menu_id" IS '菜单ID';
COMMENT ON COLUMN "public"."sys_role_menu"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_role_menu"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_role_menu" IS '角色和菜单关联';

-- ----------------------------
-- Table structure for sys_sms_code
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_sms_code";
CREATE TABLE "public"."sys_sms_code" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "date" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "send_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "ip" varchar(20) COLLATE "pg_catalog"."default",
  "req_status" int2,
  "req_results" text COLLATE "pg_catalog"."default",
  "status" int2 NOT NULL DEFAULT 11,
  "deleted" int2 NOT NULL DEFAULT 0,
  "create_time" timestamp(6) NOT NULL,
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_sms_code"."id" IS 'id';
COMMENT ON COLUMN "public"."sys_sms_code"."date" IS '日期';
COMMENT ON COLUMN "public"."sys_sms_code"."send_type" IS '类型';
COMMENT ON COLUMN "public"."sys_sms_code"."phone" IS '电话号码';
COMMENT ON COLUMN "public"."sys_sms_code"."code" IS '验证码';
COMMENT ON COLUMN "public"."sys_sms_code"."ip" IS 'IP地址';
COMMENT ON COLUMN "public"."sys_sms_code"."req_status" IS '请求状态';
COMMENT ON COLUMN "public"."sys_sms_code"."req_results" IS '请求结果';
COMMENT ON COLUMN "public"."sys_sms_code"."status" IS '使用状态';
COMMENT ON COLUMN "public"."sys_sms_code"."deleted" IS '数据状态';
COMMENT ON COLUMN "public"."sys_sms_code"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_sms_code"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_sms_code" IS '短信验证码';

-- ----------------------------
-- Table structure for sys_sms_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_sms_template";
CREATE TABLE "public"."sys_sms_template" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "send_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sign_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "template_code" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "deleted" int2 DEFAULT 0,
  "create_time" timestamp(6) NOT NULL,
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_sms_template"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_sms_template"."send_type" IS '短信类型';
COMMENT ON COLUMN "public"."sys_sms_template"."sign_name" IS '短信签名';
COMMENT ON COLUMN "public"."sys_sms_template"."template_code" IS '模板code';
COMMENT ON COLUMN "public"."sys_sms_template"."deleted" IS '数据状态';
COMMENT ON COLUMN "public"."sys_sms_template"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_sms_template"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_sms_template" IS '短信模板';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(255) COLLATE "pg_catalog"."default",
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "password" varchar(200) COLLATE "pg_catalog"."default",
  "is_account_non_expired" int2,
  "is_account_non_locked" int2,
  "is_credentials_non_expired" int2,
  "is_enabled" int2,
  "reserve" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_user"."id" IS '用户id';
COMMENT ON COLUMN "public"."sys_user"."nickname" IS '名称';
COMMENT ON COLUMN "public"."sys_user"."username" IS '用户账号';
COMMENT ON COLUMN "public"."sys_user"."phone" IS '手机号码';
COMMENT ON COLUMN "public"."sys_user"."password" IS '密码';
COMMENT ON COLUMN "public"."sys_user"."is_account_non_expired" IS '帐户是否过期：1-未过期，0-已过期';
COMMENT ON COLUMN "public"."sys_user"."is_account_non_locked" IS '帐户是否被锁定：1-未锁定，0-已锁定';
COMMENT ON COLUMN "public"."sys_user"."is_credentials_non_expired" IS '密码是否过期：1-未过期，0-已过期';
COMMENT ON COLUMN "public"."sys_user"."is_enabled" IS '帐户是否可用：1-可用，0-禁用';
COMMENT ON COLUMN "public"."sys_user"."reserve" IS '扩展';
COMMENT ON COLUMN "public"."sys_user"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_user"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_user" IS '用户';

-- ----------------------------
-- Table structure for sys_user_login_key
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_login_key";
CREATE TABLE "public"."sys_user_login_key" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" varchar(255) COLLATE "pg_catalog"."default",
  "login_key" varchar(500) COLLATE "pg_catalog"."default",
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_user_login_key"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_user_login_key"."user_id" IS '用户Id';
COMMENT ON COLUMN "public"."sys_user_login_key"."login_key" IS '第三方登录标志';
COMMENT ON COLUMN "public"."sys_user_login_key"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_user_login_key"."update_time" IS '修改时间';
COMMENT ON TABLE "public"."sys_user_login_key" IS '用户登录标志';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
  "id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "role_id" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" timestamp(6),
  "update_time" timestamp(6)
)
;
COMMENT ON COLUMN "public"."sys_user_role"."id" IS '主键';
COMMENT ON COLUMN "public"."sys_user_role"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."sys_user_role"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."sys_user_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."sys_user_role"."update_time" IS '更新时间';
COMMENT ON TABLE "public"."sys_user_role" IS '用户和角色关联';

-- ----------------------------
-- Primary Key structure for table sys_advertisement
-- ----------------------------
ALTER TABLE "public"."sys_advertisement" ADD CONSTRAINT "sys_advertisement_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_annex
-- ----------------------------
ALTER TABLE "public"."sys_annex" ADD CONSTRAINT "sys_annex_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_announcement
-- ----------------------------
ALTER TABLE "public"."sys_announcement" ADD CONSTRAINT "sys_announcement_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_client_details
-- ----------------------------
ALTER TABLE "public"."sys_client_details" ADD CONSTRAINT "sys_client_details_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_dict_data
-- ----------------------------
CREATE INDEX "sys_dict_data_dict_type_idx" ON "public"."sys_dict_data" USING btree (
  "dict_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_dict_data
-- ----------------------------
ALTER TABLE "public"."sys_dict_data" ADD CONSTRAINT "sys_dict_data_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_dict_type
-- ----------------------------
CREATE INDEX "sys_dict_type_dict_type_idx" ON "public"."sys_dict_type" USING btree (
  "dict_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "sys_dict_type_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_login_log
-- ----------------------------
ALTER TABLE "public"."sys_login_log" ADD CONSTRAINT "sys_login_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_menu
-- ----------------------------
CREATE INDEX "sys_menu_code_idx" ON "public"."sys_menu" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "sys_menu_parent_id_idx" ON "public"."sys_menu" USING btree (
  "parent_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_menu
-- ----------------------------
ALTER TABLE "public"."sys_menu" ADD CONSTRAINT "sys_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_operation_logs
-- ----------------------------
ALTER TABLE "public"."sys_operation_logs" ADD CONSTRAINT "operation_logs_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_role_menu
-- ----------------------------
CREATE INDEX "sys_role_menu_role_id_idx" ON "public"."sys_role_menu" USING btree (
  "role_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_role_menu
-- ----------------------------
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_sms_code
-- ----------------------------
CREATE INDEX "sys_sms_code_code_idx" ON "public"."sys_sms_code" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "sys_sms_code_phone_idx" ON "public"."sys_sms_code" USING btree (
  "phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "sys_sms_code_send_type_idx" ON "public"."sys_sms_code" USING btree (
  "send_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_sms_code
-- ----------------------------
ALTER TABLE "public"."sys_sms_code" ADD CONSTRAINT "sys_code_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_sms_template
-- ----------------------------
ALTER TABLE "public"."sys_sms_template" ADD CONSTRAINT "sys_sms_template_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user_login_key
-- ----------------------------
CREATE UNIQUE INDEX "sys_user_login_key_login_key_idx" ON "public"."sys_user_login_key" USING btree (
  "login_key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table sys_user_login_key
-- ----------------------------
ALTER TABLE "public"."sys_user_login_key" ADD CONSTRAINT "sys_user_login_key_login_key_key" UNIQUE ("login_key");

-- ----------------------------
-- Primary Key structure for table sys_user_login_key
-- ----------------------------
ALTER TABLE "public"."sys_user_login_key" ADD CONSTRAINT "sys_user_login_key_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user_role
-- ----------------------------
CREATE INDEX "sys_user_role_role_id_idx" ON "public"."sys_user_role" USING btree (
  "role_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "sys_user_role_user_id_idx" ON "public"."sys_user_role" USING btree (
  "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("id");

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

 Date: 14/11/2024 17:28:14
*/


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

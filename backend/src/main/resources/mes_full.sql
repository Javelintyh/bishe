-- 轻量级生产管理平台（MySQL）
-- 说明：
-- 1) 本文件只做“结构”初始化（可重复执行）
-- 2) 不自动插入业务数据；仅提供可选的字典/角色/管理员初始化（注释掉）

-- =========================
-- 0. 系统与权限
-- =========================
DROP TABLE IF EXISTS sys_admin_invitation;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sys_user_role_code(role_code),
  UNIQUE KEY uk_sys_user_username_role(username, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_admin_invitation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(128) NOT NULL UNIQUE,
  used TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选初始化（如需请取消注释）
-- INSERT INTO sys_role(role_code, role_name) VALUES
-- ('ADMIN','管理端'),('WORKSHOP','车间端'),('WAREHOUSE','仓库端');
-- INSERT INTO sys_user(username, password_hash, role_code, enabled)
-- VALUES ('admin', '{bcrypt-hash}', 'ADMIN', 1);

-- =========================
-- 1. 基础数据
-- =========================
DROP TABLE IF EXISTS base_bom;
DROP TABLE IF EXISTS base_supplier;
DROP TABLE IF EXISTS base_customer;
DROP TABLE IF EXISTS base_material;

CREATE TABLE base_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_code VARCHAR(64) NOT NULL UNIQUE,
  material_name VARCHAR(128) NOT NULL,
  material_spec VARCHAR(255),
  unit VARCHAR(32),
  material_type VARCHAR(32) NOT NULL DEFAULT 'RAW', -- RAW/PRODUCT
  safety_stock DECIMAL(18,3) NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_material_type(material_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_bom (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_material_id BIGINT NOT NULL, -- 成品(产品)物料ID
  material_id BIGINT NOT NULL,         -- 组成物料ID
  qty DECIMAL(18,6) NOT NULL,          -- 单件用量
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bom(product_material_id, material_id),
  INDEX idx_bom_product(product_material_id),
  INDEX idx_bom_material(material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(128) NOT NULL UNIQUE,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(64),
  address VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE base_supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  supplier_name VARCHAR(128) NOT NULL UNIQUE,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(64),
  address VARCHAR(255),
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 2. 订单
-- =========================
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS order_main;

CREATE TABLE order_main (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING/PRODUCING/DONE/DELIVERED
  delivery_date DATE,
  actual_delivery_date DATE,
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_order_customer(customer_id),
  INDEX idx_order_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_material_id BIGINT NOT NULL,
  qty DECIMAL(18,3) NOT NULL,
  remark VARCHAR(255),
  INDEX idx_order_detail_order_id(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 3. 生产工单 / 工序 / 报工 / 质量
-- =========================
DROP TABLE IF EXISTS quality_reason_dict;
DROP TABLE IF EXISTS quality_record;
DROP TABLE IF EXISTS production_report;
DROP TABLE IF EXISTS production_work_order_process;
DROP TABLE IF EXISTS production_work_order;

CREATE TABLE production_work_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  product_material_id BIGINT NOT NULL,
  qty DECIMAL(18,3) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'TO_PRODUCE', -- TO_PRODUCE/PRODUCING/DONE
  due_date DATE, -- 默认可取订单交期
  assignee_user_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_wo_order(order_id),
  INDEX idx_wo_status(status),
  INDEX idx_wo_due(due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE production_work_order_process (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  process_name VARCHAR(64) NOT NULL,
  seq_no INT NOT NULL DEFAULT 1,
  planned_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_wop_wo(work_order_id),
  INDEX idx_wop_seq(work_order_id, seq_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE production_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  process_name VARCHAR(64),
  good_qty DECIMAL(18,3) NOT NULL,
  bad_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  bad_reason_code VARCHAR(32),
  bad_reason_text VARCHAR(128),
  reporter_user_id BIGINT NOT NULL,
  report_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_report_wo(work_order_id),
  INDEX idx_report_time(report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quality_reason_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL UNIQUE,
  reason_name VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quality_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  work_order_id BIGINT NOT NULL,
  report_id BIGINT,
  process_name VARCHAR(64),
  bad_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  reason_code VARCHAR(32),
  reason_text VARCHAR(128),
  reporter_user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_qc_wo(work_order_id),
  INDEX idx_qc_reason(reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选初始化（如需请取消注释）
-- INSERT INTO quality_reason_dict(reason_code, reason_name, enabled, sort_no) VALUES
-- ('SCRATCH','划伤',1,10),('DIRTY','脏污',1,20),('SIZE','尺寸不良',1,30);

-- =========================
-- 4. 库存 / 出入库流水
-- =========================
DROP TABLE IF EXISTS inventory_record;
DROP TABLE IF EXISTS inventory_stock;

CREATE TABLE inventory_stock (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL UNIQUE,
  qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  change_qty DECIMAL(18,3) NOT NULL, -- +入库/-出库
  biz_type VARCHAR(32) NOT NULL,     -- PURCHASE_IN/PRODUCTION_IN/PRODUCTION_OUT/SALES_OUT/ADJUST
  biz_id VARCHAR(64),
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_inv_rec_material(material_id),
  INDEX idx_inv_rec_type(biz_type),
  INDEX idx_inv_rec_time(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 5. 采购（简化版）
-- =========================
DROP TABLE IF EXISTS purchase_order_detail;
DROP TABLE IF EXISTS purchase_order;

CREATE TABLE purchase_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  po_no VARCHAR(64) NOT NULL UNIQUE,
  supplier_id BIGINT,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED', -- CREATED/RECEIVING/DONE
  expected_date DATE,
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_po_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_order_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  po_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  qty DECIMAL(18,3) NOT NULL,
  received_qty DECIMAL(18,3) NOT NULL DEFAULT 0,
  price DECIMAL(18,4),
  remark VARCHAR(255),
  INDEX idx_pod_po(po_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 6. 设备管理（可选）
-- =========================
DROP TABLE IF EXISTS device_status_log;
DROP TABLE IF EXISTS device_asset;

CREATE TABLE device_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_code VARCHAR(64) NOT NULL UNIQUE,
  device_name VARCHAR(128) NOT NULL,
  model VARCHAR(128),
  location VARCHAR(128),
  status VARCHAR(32) NOT NULL DEFAULT 'IDLE', -- IDLE/RUNNING/MAINTENANCE/BROKEN
  remark VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(255),
  operator_user_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dev_log_device(device_id),
  INDEX idx_dev_log_time(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- 7. 消息提醒（可选：以“查询接口+列表”形式实现）
-- =========================
DROP TABLE IF EXISTS message_notice;

CREATE TABLE message_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_type VARCHAR(32) NOT NULL,   -- WO_OVERDUE/STOCK_LOW
  title VARCHAR(128) NOT NULL,
  content VARCHAR(512),
  level VARCHAR(16) NOT NULL DEFAULT 'WARN', -- INFO/WARN/ERROR
  related_type VARCHAR(32),
  related_id VARCHAR(64),
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notice_type(notice_type),
  INDEX idx_notice_read(is_read),
  INDEX idx_notice_time(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- 示例数据（方便演示）
-- 如不需要，可手工删除本段 INSERT
-- =========================

-- 角色与用户（密码 admin123）
INSERT INTO sys_role(role_code, role_name) VALUES
('ADMIN','管理端'),
('WORKSHOP','车间端'),
('WAREHOUSE','仓库端');

INSERT INTO sys_user(username, password_hash, role_code, enabled)
VALUES
('admin', '{noop}admin123', 'ADMIN', 1),
('workshop1', '{noop}workshop123', 'WORKSHOP', 1),
('warehouse1', '{noop}warehouse123', 'WAREHOUSE', 1);

INSERT INTO sys_admin_invitation(code, used)
VALUES ('ADMIN-INIT', 0);

-- 物料（原材料 + 成品）
INSERT INTO base_material(material_code, material_name, material_spec, unit, material_type, safety_stock, enabled)
VALUES
('RM-STEEL','钢板','Q235 3mm','张','RAW',100,1),
('RM-PAINT','油漆','蓝色工业漆','桶','RAW',20,1),
('RM-SCREW','螺丝','M4*20','个','RAW',1000,1),
('FG-PROD-01','电机外壳','型号 A','件','PRODUCT',10,1),
('FG-PROD-02','支架组件','型号 B','件','PRODUCT',10,1);

-- BOM（单层）
INSERT INTO base_bom(product_material_id, material_id, qty, remark)
VALUES
((SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 (SELECT id FROM base_material WHERE material_code='RM-STEEL'), 1.200, '钢板冲压'),
((SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 (SELECT id FROM base_material WHERE material_code='RM-PAINT'), 0.050, '喷漆'),
((SELECT id FROM base_material WHERE material_code='FG-PROD-02'),
 (SELECT id FROM base_material WHERE material_code='RM-STEEL'), 0.800, '焊接'),
((SELECT id FROM base_material WHERE material_code='FG-PROD-02'),
 (SELECT id FROM base_material WHERE material_code='RM-SCREW'), 4.000, '装配');

-- 客户 / 供应商
INSERT INTO base_customer(customer_name, contact_name, contact_phone, address)
VALUES
('东莞精密电子有限公司','李工','13800000001','东莞松山湖'),
('东莞伟信塑胶厂','王厂长','13800000002','东莞虎门');

INSERT INTO base_supplier(supplier_name, contact_name, contact_phone, address, enabled)
VALUES
('东莞钢材供应商','张经理','13900000001','东莞厚街',1),
('东莞表面处理厂','刘工','13900000002','东莞长安',1);

-- 质量不良原因
INSERT INTO quality_reason_dict(reason_code, reason_name, enabled, sort_no) VALUES
('SCRATCH','划伤',1,10),
('DIRTY','脏污',1,20),
('SIZE','尺寸不良',1,30);

-- 设备与状态
INSERT INTO device_asset(device_code, device_name, model, location, status, remark)
VALUES
('EQ-PRESS-01','冲压机 1 号','JH25-80','一车间','IDLE','用于钢板冲压'),
('EQ-PAINT-01','喷涂线 1 号','PT-100','一车间','IDLE','用于外壳喷漆');

-- 简单库存初始值（原材料）
INSERT INTO inventory_stock(material_id, qty)
SELECT id, CASE material_code
             WHEN 'RM-STEEL' THEN 500
             WHEN 'RM-PAINT' THEN 50
             WHEN 'RM-SCREW' THEN 5000
             WHEN 'FG-PROD-01' THEN 200
             ELSE 0
           END
FROM base_material
WHERE material_code IN ('RM-STEEL','RM-PAINT','RM-SCREW','FG-PROD-01');

-- 示例订单 + 明细
INSERT INTO order_main(order_no, customer_id, status, delivery_date, remark)
VALUES
('SO-202603-001',
 (SELECT id FROM base_customer WHERE customer_name='东莞精密电子有限公司'),
 'PENDING','2026-03-20','电机外壳首批订单');

INSERT INTO order_detail(order_id, product_material_id, qty, remark)
VALUES
((SELECT id FROM order_main WHERE order_no='SO-202603-001'),
 (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 100,'首批试产');

-- 示例工单（供车间端“待生产工单”展示）
INSERT INTO production_work_order(work_order_no, order_id, product_material_id, qty, status, due_date)
VALUES
('WO-SAMPLE-001',
 (SELECT id FROM order_main WHERE order_no='SO-202603-001'),
 (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 100,
 'TO_PRODUCE',
 '2026-03-19');

-- 报表验证数据：用于验证管理端图表（订单准时率 / 生产完成率 / 近30天出库量）
-- 说明：日期基于执行脚本当天的 CURDATE/NOW() 生成

-- 3 条验证订单（含准时交付、延期交付、生产中）
INSERT INTO order_main(order_no, customer_id, status, delivery_date, actual_delivery_date, remark)
VALUES
('SO-VERIFY-001',
 (SELECT id FROM base_customer WHERE customer_name='东莞精密电子有限公司'),
 'DELIVERED',
 DATE_SUB(CURDATE(), INTERVAL 5 DAY),
 DATE_SUB(CURDATE(), INTERVAL 6 DAY),
 '报表验证-准时交付'),
('SO-VERIFY-002',
 (SELECT id FROM base_customer WHERE customer_name='东莞伟信塑胶厂'),
 'DELIVERED',
 DATE_SUB(CURDATE(), INTERVAL 4 DAY),
 DATE_SUB(CURDATE(), INTERVAL 2 DAY),
 '报表验证-延期交付'),
('SO-VERIFY-003',
 (SELECT id FROM base_customer WHERE customer_name='东莞精密电子有限公司'),
 'PRODUCING',
 DATE_ADD(CURDATE(), INTERVAL 7 DAY),
 NULL,
 '报表验证-生产中订单');

-- 对应订单明细
INSERT INTO order_detail(order_id, product_material_id, qty, remark)
SELECT o.id,
       (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
       120,
       '报表验证明细'
FROM order_main o
WHERE o.order_no='SO-VERIFY-001';

INSERT INTO order_detail(order_id, product_material_id, qty, remark)
SELECT o.id,
       (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
       80,
       '报表验证明细'
FROM order_main o
WHERE o.order_no='SO-VERIFY-002';

INSERT INTO order_detail(order_id, product_material_id, qty, remark)
SELECT o.id,
       (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
       200,
       '报表验证明细'
FROM order_main o
WHERE o.order_no='SO-VERIFY-003';

-- 3 条工单，对应三种状态：DONE / PRODUCING / TO_PRODUCE
INSERT INTO production_work_order(work_order_no, order_id, product_material_id, qty, status, due_date)
VALUES
('WO-VERIFY-001',
 (SELECT id FROM order_main WHERE order_no='SO-VERIFY-001'),
 (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 120,
 'DONE',
 DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
('WO-VERIFY-002',
 (SELECT id FROM order_main WHERE order_no='SO-VERIFY-002'),
 (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 80,
 'PRODUCING',
 DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
('WO-VERIFY-003',
 (SELECT id FROM order_main WHERE order_no='SO-VERIFY-003'),
 (SELECT id FROM base_material WHERE material_code='FG-PROD-01'),
 200,
 'TO_PRODUCE',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY));

-- 近 30 天库存周转验证数据（入库 / 出库混合）
INSERT INTO inventory_record(material_id, change_qty, biz_type, biz_id, remark, created_at)
SELECT (SELECT id FROM base_material WHERE material_code='RM-STEEL'),
       300,
       'PURCHASE_IN',
       'VERIFY-IN-001',
       '报表验证入库',
       DATE_SUB(NOW(), INTERVAL 12 DAY);

INSERT INTO inventory_record(material_id, change_qty, biz_type, biz_id, remark, created_at)
SELECT (SELECT id FROM base_material WHERE material_code='RM-STEEL'),
       -120,
       'PRODUCTION_OUT',
       'VERIFY-OUT-001',
       '报表验证出库',
       DATE_SUB(NOW(), INTERVAL 9 DAY);

INSERT INTO inventory_record(material_id, change_qty, biz_type, biz_id, remark, created_at)
SELECT (SELECT id FROM base_material WHERE material_code='RM-STEEL'),
       200,
       'PURCHASE_IN',
       'VERIFY-IN-002',
       '报表验证入库',
       DATE_SUB(NOW(), INTERVAL 6 DAY);

INSERT INTO inventory_record(material_id, change_qty, biz_type, biz_id, remark, created_at)
SELECT (SELECT id FROM base_material WHERE material_code='RM-STEEL'),
       -90,
       'PRODUCTION_OUT',
       'VERIFY-OUT-002',
       '报表验证出库',
       DATE_SUB(NOW(), INTERVAL 3 DAY);

INSERT INTO inventory_record(material_id, change_qty, biz_type, biz_id, remark, created_at)
SELECT (SELECT id FROM base_material WHERE material_code='RM-STEEL'),
       -60,
       'SALES_OUT',
       'VERIFY-OUT-003',
       '报表验证销售出库',
       DATE_SUB(NOW(), INTERVAL 1 DAY);

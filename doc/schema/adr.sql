-- adr 药品不良反应监测与评价管理 -- schema (chen-018)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_018

CREATE TABLE IF NOT EXISTS t_adr_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '报告处置单号',
  stage int DEFAULT NULL COMMENT '当前档次 0..3（收单/初核/评价/已结案）',
  status int DEFAULT NULL COMMENT '处置落点 0未起 1在办 2已作废',
  content varchar(255) DEFAULT NULL COMMENT '处置记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报告处置单';

CREATE TABLE IF NOT EXISTS t_adr_cycle_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '定期报告催送单号',
  due_at datetime DEFAULT NULL COMMENT '该动手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '提前几日开口催送',
  content varchar(255) DEFAULT NULL COMMENT '事由与被催单位记要',
  status int DEFAULT NULL COMMENT '条目情形 0未开口 1已催出 2催不成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定期报告催送单';

CREATE TABLE IF NOT EXISTS t_adr_org_arch (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '报告单位底册代号',
  site_name varchar(128) DEFAULT NULL COMMENT '单位全称',
  site_type varchar(32) DEFAULT NULL COMMENT '单位类别',
  road_name varchar(128) DEFAULT NULL COMMENT '落在哪一级区划(省—市—县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单卡年报告份数低界(份/年)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界(份/年)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界(份/年)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已退出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报告单位底册';

CREATE TABLE IF NOT EXISTS t_adr_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '上级中心下发册次号（进门定死，行随册走）',
  period_no varchar(16) DEFAULT NULL COMMENT '进门核对期号（哪一期进门就记哪一期）',
  row_no int DEFAULT NULL COMMENT '本册内位次（进门按先后排定，此后走到哪一步都不改）',
  item_code varchar(64) DEFAULT NULL COMMENT '被挑出的报告卡追溯码（对卡只认此码，不认单位名号）',
  qty decimal(12,2) DEFAULT NULL COMMENT '上报份数（原册所填，原样留痕）',
  link_qty decimal(12,2) DEFAULT NULL COMMENT '实际核销落账份数（受上期挂下封顶，双行只计一次）',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已挂接 2挂起',
  hold_reason varchar(255) DEFAULT NULL COMMENT '挂起缘故（四种不合之一，一句话）',
  periods_held int DEFAULT '0' COMMENT '挂起已跨过的核对期数（连跨两期搬往期）',
  rolled_into varchar(16) DEFAULT NULL COMMENT '最近一次已拨入的新期号（防同一期重复拨动）',
  merged int DEFAULT '0' COMMENT '本册同码双行并账标记 0独立行 1靠前主行 2并入主行的从行',
  dup_of bigint DEFAULT NULL COMMENT '从行并入的主行id（merged=2 时落值，留痕可溯）',
  archived int DEFAULT '0' COMMENT '是否已搬往期 0现行册面 1往期（只供事后查账）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_batch_row (batch_no, row_no),
  KEY idx_batch_code (batch_no, item_code),
  KEY idx_period_status (period_no, archived, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上级下发关联册核对行';

-- 册级账本：一册一号，进门定死；二次进门只比对不覆写；改口走作废。
CREATE TABLE IF NOT EXISTS t_adr_recon_bill (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '下发册次号（一册一个，进门定死）',
  period_no varchar(16) DEFAULT NULL COMMENT '进门核对期号',
  row_limit int DEFAULT NULL COMMENT '进门时中心所定单册行数上限（快照）',
  total_rows int DEFAULT '0' COMMENT '本册落账行数（回执口径：挂接+挂起=本册行数）',
  linked_rows int DEFAULT '0' COMMENT '已挂接行数（逐行落库后同源点算）',
  held_rows int DEFAULT '0' COMMENT '挂起行数（逐行落库后同源点算）',
  voided int DEFAULT '0' COMMENT '是否已作废 0在册 1已作废（作废后允许同号重新进门）',
  receipt_status int DEFAULT NULL COMMENT '回执情形 0空册 1核讫（空册不得记核讫）',
  compared int DEFAULT '0' COMMENT '二次进门只比对标记 0首进落账 1二次仅比对',
  compared_at datetime DEFAULT NULL COMMENT '最近一次比对时刻',
  active_batch_no varchar(64) GENERATED ALWAYS AS
      (CASE WHEN voided = 0 AND del_flag = 0 THEN batch_no ELSE NULL END) STORED
      COMMENT '在册册号（作废/删除置空；唯一索引允许多条 NULL）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_active_batch (active_batch_no),
  KEY idx_batch_no (batch_no, voided)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上级下发对照册册级账';

CREATE TABLE IF NOT EXISTS t_adr_review_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '评价结论复核签认单号',
  node_no int DEFAULT NULL COMMENT '当前所在道次 0..2（县核/市核/省定）',
  sign_mode int DEFAULT NULL COMMENT '同口并署办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本口应落名数',
  sign_count int DEFAULT NULL COMMENT '本口已落名数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价结论逐级复核签认单';

CREATE TABLE IF NOT EXISTS t_adr_signal_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '信号检出限值线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '低界比值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界比值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界比值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '让线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0现行 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信号检出限值线';

CREATE TABLE IF NOT EXISTS t_adr_term_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '术语分类卡号',
  node_no int DEFAULT NULL COMMENT '本卡版次序',
  site_id int DEFAULT NULL COMMENT '所属报告单位底册',
  site_no varchar(64) DEFAULT NULL COMMENT '所属单位底册代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡年报告份数(份/年)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本卡复核时限(工作日)',
  content varchar(255) DEFAULT NULL COMMENT '校验串（随版次序走）',
  grade_level int DEFAULT NULL COMMENT '年报告份数折到的监督层级',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='术语分类卡';

-- 初始底册数据：两条报告单位底册，一条在册、一条已退出。
-- t_adr_org_arch 两条种子底册：id=0 在册、id=1 已退出。
-- 单卡年报告份数三档上限取 20/80/300 份。
INSERT IGNORE INTO t_adr_org_arch (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'YF00', '澜川省报告单位底册（澜川市第一人民医院，在册）', '医疗机构', '澜川省—澜川市—澜川县', 20.00, 80.00, 300.00, 0, 0, 'seed', NOW()),
       (1, 'YF01', '澜川省报告单位底册（澜川制药股份有限公司，已退出）', '生产企业', '澜川省—澜川市—澜川县', 20.00, 80.00, 300.00, 1, 0, 'seed', NOW());


package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TAdrReportRowMapper;
import com.fc.v2.mapper.auto.TAdrReconBillMapper;
import com.fc.v2.mapper.auto.TAdrTermCardMapper;
import com.fc.v2.model.auto.TAdrReportRow;
import com.fc.v2.model.auto.TAdrReconBill;
import com.fc.v2.model.auto.TAdrTermCard;
import com.fc.v2.model.custom.adr.CompareReport;
import com.fc.v2.model.custom.adr.IntakeResult;
import com.fc.v2.model.custom.adr.LandedRow;
import com.fc.v2.model.custom.adr.Receipt;
import com.fc.v2.model.custom.adr.ReconReport;
import com.fc.v2.model.custom.adr.RowInput;
import com.fc.v2.service.ITAdrReportRowService;

/**
 * 上级下发关联册核对行 Service。
 * 三态落点（0待核 1已挂接 2挂起）+ 册级账规全部收在这里。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Service
public class TAdrReportRowServiceImpl implements ITAdrReportRowService {

    // ---- 行落点 ----
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_LINKED = 1;
    private static final int STATUS_HELD = 2;

    // ---- 本册并账角色 ----
    private static final int MERGED_NONE = 0;
    private static final int MERGED_MASTER = 1;
    private static final int MERGED_SLAVE = 2;

    // ---- 挂起缘故（一句话说到位） ----
    /** 追溯码那一段在本级找不到对应的卡（撞名不算对上，只认码；码段空着同样找不到） */
    public static final String HOLD_NO_CARD = "追溯码在本级找不到对应卡，挂起待查";
    /** 该填份数的格子空着 */
    public static final String HOLD_QTY_EMPTY = "份数空缺未填，挂起待补";
    /** 份数写成零或负数 */
    public static final String HOLD_QTY_INVALID = "份数为零或负数，挂起待正";
    /** 上一期同一笔事还挂着没结 */
    public static final String HOLD_PRIOR_OPEN = "上一期同码仍挂起未结，挂起待结";
    /** 本册同码双行 */
    public static final String HOLD_DUP = "本册同码双行已并入靠前行，本行留痕不另记账";
    /** 本期核销已被上期挂下数封顶 */
    public static final String HOLD_CAP = "超出上期挂下核销封顶数，不记本行不移抵他行，挂起";

    // ---- 回执口径 ----
    public static final String RECEIPT_EMPTY = "空册";
    public static final String RECEIPT_DONE = "核讫";

    // ---- 对账差行缘故 ----
    public static final String DIFF_PENDING = "PENDING";
    public static final String DIFF_SLAVE = "SLAVE";
    public static final String DIFF_ARCHIVED = "ARCHIVED";
    public static final String DIFF_VOIDED = "VOIDED";
    public static final String DIFF_DELETED = "DELETED";

    @javax.annotation.Resource
    private TAdrReportRowMapper adrReportRowMapper;

    @javax.annotation.Resource
    private TAdrReconBillMapper adrReconBillMapper;

    @javax.annotation.Resource
    private TAdrTermCardMapper adrTermCardMapper;

    @Override
    public TAdrReportRow selectTAdrReportRowById(Long id) {
        return getRowById(id);
    }

    /**
     * 一册进门。整册把关、逐行落点，同库事务：越线或出错不留半截账。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntakeResult intake(String batchNo, String periodNo, int rowLimit, List<RowInput> rows) {
        try {
            return doIntake(batchNo, periodNo, rowLimit, rows);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            // 并发同号进门：册头唯一键已挡住双建。本事务回滚后改走只比对，绝不覆先到那一遍。
            TAdrReconBill winner = findActiveBill(trimToNull(batchNo));
            if (winner != null) {
                return compareOnly(winner, rows == null ? new ArrayList<>() : rows);
            }
            throw dup;
        }
    }

    private IntakeResult doIntake(String batchNo, String periodNo, int rowLimit, List<RowInput> rows) {
        String no = trimToNull(batchNo);
        String period = trimToNull(periodNo);
        if (no == null || period == null) {
            throw new IllegalArgumentException("册号与期号进门必须给定，不许留空");
        }
        if (rowLimit <= 0) {
            throw new IllegalArgumentException("单册行数上限必须是中心定下的正数");
        }
        List<RowInput> lines = rows == null ? new ArrayList<>() : rows;

        // 同一册号已在册（未作废）：第二次进门只走比对这一层，落点不动、行不增不改。
        TAdrReconBill active = findActiveBill(no);
        if (active != null) {
            return compareOnly(active, lines);
        }

        IntakeResult result = new IntakeResult();
        result.setBatchNo(no);
        result.setPeriodNo(period);

        // 行数上限：进册先按整册把关，越线原册退回、一行不留。
        if (lines.size() > rowLimit) {
            result.setRejected(true);
            result.setMessage("本册 " + lines.size() + " 行，越过单册上限 " + rowLimit
                    + " 行，原册退回、一行不留");
            return result;
        }

        // 上期挂下（份数）：往期进门、现行仍挂起的立账行，其份数合计即本期核销封顶数。
        List<TAdrReportRow> carriedHeld = listCarriedHeldRows(period);
        BigDecimal carryover = BigDecimal.ZERO;
        java.util.Set<String> carriedCodes = new java.util.HashSet<>();
        for (TAdrReportRow held : carriedHeld) {
            if (held.getQty() != null && held.getQty().signum() > 0) {
                carryover = carryover.add(held.getQty());
            }
            if (held.getItemCode() != null) {
                carriedCodes.add(held.getItemCode());
            }
        }
        // 本期已经核销落账的份数（同期可能递几册，封顶账是本期共一口锅）。
        BigDecimal quotaLeft = carryover.subtract(sumLinkedQtyInPeriod(period));
        if (quotaLeft.signum() < 0) {
            quotaLeft = BigDecimal.ZERO;
        }

        // 本册同码双行：先点一遍各码出现次数；出现两次及以上才分主从，只现一次的是独立行。
        Map<String, Integer> codeOccur = new LinkedHashMap<>();
        for (RowInput line : lines) {
            String code = normCode(line.getItemCode());
            if (code != null) {
                codeOccur.merge(code, 1, Integer::sum);
            }
        }

        // 第一趟：进门定位次（照先后排定，此后走到哪一步都改不掉）。
        List<TAdrReportRow> entities = new ArrayList<>(lines.size());
        int seq = 0;
        for (RowInput line : lines) {
            seq++;
            TAdrReportRow row = new TAdrReportRow();
            row.setBatchNo(no);
            row.setPeriodNo(period);
            row.setRowNo(seq);
            row.setItemCode(normCode(line.getItemCode()));
            row.setQty(line.getQty());
            row.setStatus(STATUS_PENDING);
            row.setPeriodsHeld(0);
            row.setRolledInto(period); // 进门即算入本期，拨动时不重复计
            row.setMerged(MERGED_NONE);
            row.setArchived(0);
            row.setDelFlag(0);
            entities.add(row);
        }

        // 第二趟：定主从、定落点。
        java.util.Set<String> seen = new java.util.HashSet<>();
        Map<String, Long> masterIdByCode = new HashMap<>();
        for (TAdrReportRow row : entities) {
            String code = row.getItemCode();
            BigDecimal qty = row.getQty();
            boolean duplicateCode = code != null && codeOccur.get(code) > 1;
            boolean slave = duplicateCode && !seen.add(code);
            String reason = null;

            if (slave) {
                // 同一追溯码本册双行：合成一条，份数只计一次，位次留靠前那一个。
                row.setMerged(MERGED_SLAVE);
                reason = HOLD_DUP;
            } else {
                if (duplicateCode) {
                    row.setMerged(MERGED_MASTER);
                    masterIdByCode.put(code, null); // 主行 insert 后回填
                }
                // 四种不合，按序只认头一桩；不合只停这一行，同册其余各行照旧走。
                if (code == null || !cardExists(code)) {
                    // 追溯码那一段在本级找不到对应的卡；码段空着同样找不到。
                    // 撞名不算对上，只认码——不拿单位名号相近认门。
                    reason = HOLD_NO_CARD;
                } else if (qty == null) {
                    reason = HOLD_QTY_EMPTY;
                } else if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    reason = HOLD_QTY_INVALID;
                } else if (carriedCodes.contains(code)) {
                    // 上一期同一笔事（同追溯码）还挂着没结。
                    reason = HOLD_PRIOR_OPEN;
                }
            }

            if (reason == null) {
                if (quotaLeft.compareTo(qty) < 0) {
                    // 超出本期挂下封顶：落点只有三个，不许一行拆成半截核销半截挂起——
                    // 整行挂起；多出的那一截不记本行，余下封顶额也不移抵他行（仍归本期后行按先后用）。
                    reason = HOLD_CAP;
                } else {
                    row.setStatus(STATUS_LINKED);
                    row.setLinkQty(qty);
                    quotaLeft = quotaLeft.subtract(qty);
                }
            }
            if (reason != null) {
                row.setStatus(STATUS_HELD);
                row.setHoldReason(reason);
            }
        }

        // 册头先落：并发同号进门时在 uk_active_batch 上当场撞回，行一行都不会落。
        // 三数由本批实体同源点算（与 tally 同一口径：从行不立账、不计数）。
        int preTotal = 0;
        int preLinked = 0;
        int preHeld = 0;
        for (TAdrReportRow row : entities) {
            if (row.getMerged() != null && row.getMerged() == MERGED_SLAVE) {
                continue;
            }
            preTotal++;
            if (row.getStatus() == STATUS_LINKED) {
                preLinked++;
            } else if (row.getStatus() == STATUS_HELD) {
                preHeld++;
            }
        }
        TAdrReconBill bill = new TAdrReconBill();
        bill.setBatchNo(no);
        bill.setPeriodNo(period);
        bill.setRowLimit(rowLimit);
        bill.setTotalRows(preTotal);
        bill.setLinkedRows(preLinked);
        bill.setHeldRows(preHeld);
        bill.setVoided(0);
        bill.setCompared(0);
        bill.setDelFlag(0);
        insertBill(bill);

        // 整册落库；主行 id 回填后给从行认主（从行也留行，位次与缘故都在）。
        for (TAdrReportRow row : entities) {
            insertRow(row);
            if (row.getMerged() == MERGED_MASTER) {
                masterIdByCode.put(row.getItemCode(), row.getId());
            }
        }
        for (TAdrReportRow row : entities) {
            if (row.getMerged() == MERGED_SLAVE) {
                row.setDupOf(masterIdByCode.get(row.getItemCode()));
                updateRow(row);
            }
        }

        // 落库后按同一算法再点一遍回带（出回执时也以现点为准）。
        int[] tally = tally(no);

        result.setTotalRows(tally[0]);
        result.setLinkedRows(tally[1]);
        result.setHeldRows(tally[2]);
        for (TAdrReportRow row : entities) {
            result.getRows().add(toLanded(row));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBill(String batchNo) {
        String no = trimToNull(batchNo);
        TAdrReconBill active = findActiveBill(no);
        if (active == null) {
            throw new IllegalStateException("册号 " + no + " 不在册，无册可作废");
        }
        // 改口走作废：册头记作废，行软删留痕；第二遍进门永不覆第一遍。
        active.setVoided(1);
        active.setUpdateTime(new Date());
        updateBill(active);
        List<TAdrReportRow> rows = listStoredPhysicalRows(no);
        for (TAdrReportRow row : rows) {
            row.setDelFlag(1);
            updateRow(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Receipt receipt(String batchNo) {
        String no = trimToNull(batchNo);
        TAdrReconBill bill = findActiveBill(no);
        // 还有一册回执写"已核完"、系统里查无此册——这头从根上断：查无册/已作废不出回执。
        if (bill == null) {
            return null;
        }
        // 三个数与逐行落库后点出的那一回算法同源：现点现算，不拿册头旧数。
        int[] tally = tally(no);
        int total = tally[0];
        int linked = tally[1];
        int held = tally[2];
        if (linked + held != total) {
            // 同源算法下不可能不平；不平就是账被人动过，当场抛出，不糊回执。
            throw new IllegalStateException("册 " + no + " 回执口径不平：挂接 " + linked
                    + " + 挂起 " + held + " != 本册 " + total);
        }

        String mark = total == 0 ? RECEIPT_EMPTY : RECEIPT_DONE;
        bill.setTotalRows(total);
        bill.setLinkedRows(linked);
        bill.setHeldRows(held);
        bill.setReceiptStatus(total == 0 ? 0 : 1); // 空册只落空册，核讫是报完成绩用的
        bill.setUpdateTime(new Date());
        updateBill(bill);

        Receipt receipt = new Receipt();
        receipt.setBatchNo(no);
        receipt.setPeriodNo(bill.getPeriodNo());
        receipt.setTotalRows(total);
        receipt.setLinkedRows(linked);
        receipt.setHeldRows(held);
        receipt.setMark(mark);
        return receipt;
    }

    @Override
    public List<TAdrReportRow> listCurrentRows(String batchNo) {
        return rowsCurrent(batchNo);
    }

    @Override
    public List<TAdrReportRow> listHeld(String periodNo) {
        String period = trimToNull(periodNo);
        return heldRows(period);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rolloverHeld(String newPeriodNo) {
        String newPeriod = trimToNull(newPeriodNo);
        if (newPeriod == null) {
            throw new IllegalArgumentException("新期号必须给定");
        }
        // 跨入新期：已拨入过本期的不再重复拨（同一期连拨两遍不累加）；本期新进的行
        // rolledInto 就是本期，天然排除。
        List<TAdrReportRow> held = listRolloverCandidates(newPeriod);
        int archived = 0;
        for (TAdrReportRow row : held) {
            int crossed = (row.getPeriodsHeld() == null ? 0 : row.getPeriodsHeld()) + 1;
            row.setPeriodsHeld(crossed);
            row.setRolledInto(newPeriod);
            if (crossed >= 2) {
                // 连跨两个核对期仍无落点：搬往期，现行册面不再出现。
                row.setArchived(1);
                archived++;
            }
            updateRow(row);
        }
        return archived;
    }

    @Override
    public List<TAdrReportRow> listArchived(String batchNo) {
        return archivedRows(trimToNull(batchNo));
    }

    @Override
    public ReconReport reconcile(long screenTotal) {
        // 全辖有效行（在册、未作废、未删）；逐行同源加总，与各册回执同一口径。
        List<TAdrReportRow> all = listAllEffectiveRows();
        long linked = 0;
        long held = 0;
        List<ReconReport.DiffLine> diffLines = new ArrayList<>();
        for (TAdrReportRow row : all) {
            boolean slave = row.getMerged() != null && row.getMerged() == MERGED_SLAVE;
            boolean archivedRow = row.getArchived() != null && row.getArchived() == 1;
            if (archivedRow) {
                // 已搬往期：现行全辖屏上不该再含它。
                if (!slave) {
                    diffLines.add(new ReconReport.DiffLine(row.getBatchNo(), row.getRowNo(),
                            row.getItemCode(), row.getStatus(), DIFF_ARCHIVED));
                }
                continue;
            }
            if (slave) {
                // 并账从行：份数只计一次，屏上若按物理行点数就会多出这一截。
                diffLines.add(new ReconReport.DiffLine(row.getBatchNo(), row.getRowNo(),
                        row.getItemCode(), row.getStatus(), DIFF_SLAVE));
                continue;
            }
            if (row.getStatus() != null && row.getStatus() == STATUS_LINKED) {
                linked++;
            } else if (row.getStatus() != null && row.getStatus() == STATUS_HELD) {
                held++;
            } else {
                diffLines.add(new ReconReport.DiffLine(row.getBatchNo(), row.getRowNo(),
                        row.getItemCode(), row.getStatus(), DIFF_PENDING));
            }
        }
        ReconReport report = new ReconReport();
        report.setScreenTotal(screenTotal);
        report.setLinkedRows(linked);
        report.setHeldRows(held);
        report.setRowWiseTotal(linked + held);
        report.setDiff(screenTotal - (linked + held));
        report.setDiffLines(diffLines);
        report.setExplained(diffLines.size());
        return report;
    }

    // ------------------------------------------------------------------
    // 内里账法
    // ------------------------------------------------------------------

    /**
     * 第二次进门：拿册上每一行去比已落库的存量行，比一比就完事。
     * 按本册位次对齐——位次进门定死，是两边共用的对齐键。
     */
    private IntakeResult compareOnly(TAdrReconBill bill, List<RowInput> lines) {
        List<TAdrReportRow> stored = listStoredPhysicalRows(bill.getBatchNo());
        Map<Integer, TAdrReportRow> storedByPos = new HashMap<>();
        for (TAdrReportRow row : stored) {
            storedByPos.put(row.getRowNo(), row);
        }
        Map<Integer, RowInput> incomingByPos = new TreeMap<>();
        int seq = 0;
        for (RowInput line : lines) {
            seq++;
            incomingByPos.put(seq, line);
        }

        CompareReport report = new CompareReport();
        report.setIncomingRows(lines.size());
        report.setStoredRows(stored.size());
        java.util.Set<Integer> positions = new java.util.TreeSet<>();
        positions.addAll(incomingByPos.keySet());
        positions.addAll(storedByPos.keySet());
        for (Integer pos : positions) {
            RowInput in = incomingByPos.get(pos);
            TAdrReportRow db = storedByPos.get(pos);
            if (in != null && db != null) {
                String inCode = in.getItemCode() == null ? null : in.getItemCode().trim();
                if (inCode != null && inCode.isEmpty()) {
                    inCode = null;
                }
                boolean codeDiff = !eqCode(inCode, db.getItemCode());
                boolean qtyDiff = !eqQty(in.getQty(), db.getQty());
                if (!codeDiff && !qtyDiff) {
                    report.getMatched().add(new CompareReport.MatchLine(pos,
                            inCode, db.getItemCode(), str(in.getQty()), str(db.getQty()), null));
                } else {
                    report.getMismatched().add(new CompareReport.MatchLine(pos,
                            inCode, db.getItemCode(), str(in.getQty()), str(db.getQty()),
                            codeDiff ? "CODE" : "QTY"));
                }
            } else if (in != null) {
                report.getOnlyIncoming().add(pos);
            } else {
                report.getOnlyStored().add(pos);
            }
        }

        bill.setCompared(1);
        bill.setComparedAt(new Date());
        updateBill(bill);

        IntakeResult result = new IntakeResult();
        result.setBatchNo(bill.getBatchNo());
        result.setPeriodNo(bill.getPeriodNo());
        result.setCompareOnly(true);
        result.setMessage("册号已在册，本次只走比对：落点不动、行不增不改；要改口请先作废");
        result.setCompareReport(report);
        return result;
    }

    /**
     * 同源点算：[本册立账行数, 已挂接, 挂起]。
     * 立账行 = 非从行（MERGED_SLAVE 只留痕）；挂接+挂起必等于本册行数。
     */
    int[] tally(String batchNo) {
        int total = 0;
        int linked = 0;
        int held = 0;
        for (TAdrReportRow row : rowsAccountable(batchNo)) {
            total++;
            if (row.getStatus() != null && row.getStatus() == STATUS_LINKED) {
                linked++;
            } else if (row.getStatus() != null && row.getStatus() == STATUS_HELD) {
                held++;
            }
        }
        return new int[] {total, linked, held};
    }

    // ------------------------------------------------------------------
    // 数据缝：生产走 mapper，单测可以覆底座两个集合接内存账，算法一律不绕过。
    // ------------------------------------------------------------------

    /** 底座：全辖未删物理行（含从行、往期行），册号、位次升序。 */
    List<TAdrReportRow> listAllEffectiveRows() {
        return this.adrReportRowMapper.selectList(new QueryWrapper<TAdrReportRow>()
                .eq("del_flag", 0)
                .orderByAsc("batch_no", "row_no"));
    }

    /** 底座：全部册头（含已作废，作废判定在内存里做）。按 id 倒序——同号作废后重进取新头。 */
    List<TAdrReconBill> listBills() {
        return this.adrReconBillMapper.selectList(new QueryWrapper<TAdrReconBill>()
                .eq("del_flag", 0)
                .orderByDesc("id"));
    }

    TAdrReportRow getRowById(Long id) {
        return this.adrReportRowMapper.selectById(id);
    }

    int insertRow(TAdrReportRow row) {
        return this.adrReportRowMapper.insert(row);
    }

    int updateRow(TAdrReportRow row) {
        return this.adrReportRowMapper.updateById(row);
    }

    int insertBill(TAdrReconBill bill) {
        return this.adrReconBillMapper.insert(bill);
    }

    int updateBill(TAdrReconBill bill) {
        return this.adrReconBillMapper.updateById(bill);
    }

    private static boolean notSlave(TAdrReportRow row) {
        return row.getMerged() == null || row.getMerged() != MERGED_SLAVE;
    }

    private static boolean current(TAdrReportRow row) {
        return row.getArchived() == null || row.getArchived() == 0;
    }

    /** 本册立账行（未删、非从行；含往期行，需要现行口径的调用方自行再滤）。 */
    List<TAdrReportRow> rowsAccountable(String batchNo) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            if (batchNo.equals(row.getBatchNo()) && notSlave(row)) {
                out.add(row);
            }
        }
        return out;
    }

    /** 本册现行册面物理行（含从行留痕），位次升序。 */
    List<TAdrReportRow> rowsCurrent(String batchNo) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            if (batchNo.equals(row.getBatchNo()) && current(row)) {
                out.add(row);
            }
        }
        return out;
    }

    /** 现行挂起立账行（period 为空看全部），册号、位次升序。 */
    List<TAdrReportRow> heldRows(String period) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            if (current(row) && notSlave(row)
                    && row.getStatus() != null && row.getStatus() == STATUS_HELD
                    && (period == null || period.equals(row.getPeriodNo()))) {
                out.add(row);
            }
        }
        return out;
    }

    /** 往期层立账行（batchNo 为空看全辖），只供事后查账，不牵住新一年开册。 */
    List<TAdrReportRow> archivedRows(String batchNo) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            boolean archivedRow = row.getArchived() != null && row.getArchived() == 1;
            if (archivedRow && notSlave(row) && (batchNo == null || batchNo.equals(row.getBatchNo()))) {
                out.add(row);
            }
        }
        return out;
    }

    /** 本期已挂接立账行。 */
    private List<TAdrReportRow> linkedRowsInPeriod(String period) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            if (current(row) && notSlave(row)
                    && row.getStatus() != null && row.getStatus() == STATUS_LINKED
                    && period.equals(row.getPeriodNo())) {
                out.add(row);
            }
        }
        return out;
    }

    /** 上期挂下行：往期进门、现行仍挂起的立账行（从行只是留痕，不立账）。 */
    List<TAdrReportRow> listCarriedHeldRows(String currentPeriod) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : heldRows(null)) {
            if (!currentPeriod.equals(row.getPeriodNo())) {
                out.add(row);
            }
        }
        return out;
    }

    /** 本期已经核销落账的份数合计（同期几册共用封顶）。 */
    BigDecimal sumLinkedQtyInPeriod(String period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TAdrReportRow row : linkedRowsInPeriod(period)) {
            if (row.getLinkQty() != null) {
                sum = sum.add(row.getLinkQty());
            } else if (row.getQty() != null) {
                sum = sum.add(row.getQty());
            }
        }
        return sum;
    }

    /** 跨期拨动候选：现行挂起立账行、且未拨入过新期（同一期重复拨动不重复计）。 */
    List<TAdrReportRow> listRolloverCandidates(String newPeriod) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : heldRows(null)) {
            if (row.getRolledInto() == null || !newPeriod.equals(row.getRolledInto())) {
                out.add(row);
            }
        }
        return out;
    }

    /**
     * 本级有没有这张卡：只按追溯码（卡号 bill_no）认，单位名号相近不算撞对。
     * 包级可见便于单测按内存卡库判定。
     */
    boolean cardExists(String code) {
        Integer n = this.adrTermCardMapper.selectCount(new QueryWrapper<TAdrTermCard>()
                .eq("bill_no", code)
                .eq("del_flag", 0));
        return n != null && n > 0;
    }

    /** 追溯码归一：去空白，空白串视同没有码（本级找不到对应卡）。 */
    private static String normCode(String code) {
        if (code == null) {
            return null;
        }
        String t = code.trim();
        return t.isEmpty() ? null : t;
    }

    TAdrReconBill findActiveBill(String batchNo) {
        if (batchNo == null) {
            return null;
        }
        // listBills 已按进门时刻倒序：同号作废后重进，取到的是新册头。
        for (TAdrReconBill bill : listBills()) {
            if (batchNo.equals(bill.getBatchNo())
                    && bill.getVoided() != null && bill.getVoided() == 0) {
                return bill;
            }
        }
        return null;
    }

    /** 一册的全部未删物理行（含从行），按位次升序。 */
    List<TAdrReportRow> listStoredPhysicalRows(String batchNo) {
        List<TAdrReportRow> out = new ArrayList<>();
        for (TAdrReportRow row : listAllEffectiveRows()) {
            if (batchNo.equals(row.getBatchNo())) {
                out.add(row);
            }
        }
        return out;
    }

    private LandedRow toLanded(TAdrReportRow row) {
        LandedRow landed = new LandedRow();
        landed.setId(row.getId());
        landed.setRowNo(row.getRowNo());
        landed.setItemCode(row.getItemCode());
        landed.setQty(row.getQty());
        landed.setLinkQty(row.getLinkQty());
        landed.setStatus(row.getStatus());
        landed.setHoldReason(row.getHoldReason());
        landed.setMerged(row.getMerged());
        landed.setDupOf(row.getDupOf());
        return landed;
    }

    private static boolean eqCode(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean eqQty(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }

    private static String str(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

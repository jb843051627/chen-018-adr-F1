package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TAdrReportRow;
import com.fc.v2.model.custom.adr.IntakeResult;
import com.fc.v2.model.custom.adr.Receipt;
import com.fc.v2.model.custom.adr.ReconReport;
import com.fc.v2.model.custom.adr.RowInput;

/**
 * 三桩旧事与今年新账规，逐条钉死。跑的是生产真身算法，数据走内存账。
 */
class TAdrReportRowServiceImplTest {

    private static final int PENDING = 0;
    private static final int LINKED = 1;
    private static final int HELD = 2;
    private static final int NONE = 0;
    private static final int MASTER = 1;
    private static final int SLAVE = 2;

    private MemoryReconService svc() {
        MemoryReconService svc = new MemoryReconService();
        // 本级卡库：测试用到的码先挂号；没挂号的码视同本级找不到卡。
        svc.cards.addAll(Arrays.asList("OK", "C-EMPTY", "C-ZERO", "C-NEG",
                "C1", "C2", "C3", "C4", "DUP", "K1", "K2", "OPEN"));
        return svc;
    }

    private static RowInput r(String code, String qty) {
        return new RowInput(code, qty == null ? null : new BigDecimal(qty));
    }

    // ============================================================
    // 三桩旧事之一：同一个册号三天进两遍，后一遍冲掉先一遍已对上的行
    // ============================================================
    @Test
    void duplicateBatchNumber_compareOnly_neverOverwrites() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        IntakeResult first = svc.intake("B-2026Q3-01", "2026Q3", 500,
                Arrays.asList(r("K1", "5"), r("K2", "7")));
        assertFalse(first.isCompareOnly());
        assertEquals(2, first.getLinkedRows());

        // 第二遍：份数改了、还多塞一行。只许比对，存量一个字都不许动。
        IntakeResult second = svc.intake("B-2026Q3-01", "2026Q3", 500,
                Arrays.asList(r("K1", "99"), r("K2", "7"), r("C3", "1")));
        assertTrue(second.isCompareOnly());
        assertNotNull(second.getCompareReport());
        // 第 1 位次份数对不上（99 vs 5）
        assertEquals(1, second.getCompareReport().getMismatched().size());
        assertEquals(Integer.valueOf(1), second.getCompareReport().getMismatched().get(0).getRowNo());
        assertEquals("QTY", second.getCompareReport().getMismatched().get(0).getDifferAt());
        // 册上多出的第 3 行只点出，不落库
        assertEquals(Arrays.asList(3), second.getCompareReport().getOnlyIncoming());

        // 存量行原样：份数还是 5、落点还是已挂接，库里仍是两行。
        TAdrReportRow stored = svc.byPos("B-2026Q3-01", 1);
        assertEquals(new BigDecimal("5"), stored.getQty());
        assertEquals(LINKED, stored.getStatus().intValue());
        assertEquals(2, svc.listCurrentRows("B-2026Q3-01").size());
    }

    // ============================================================
    // 三桩旧事之二：同一追溯码一册双行，落账各记一份、数目虚增
    // ============================================================
    @Test
    void duplicateCodeInBook_mergesIntoOne_countedOnce_keepsEarliestPosition() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        IntakeResult res = svc.intake("B-DUP", "2026Q3", 500,
                Arrays.asList(r("DUP", "10"), r("C1", "3"), r("DUP", "10")));

        // 物理三行都在；立账只算两行（靠前行为主行，后一行从行留痕）。
        assertEquals(2, res.getTotalRows());
        assertEquals(2, res.getLinkedRows());
        assertEquals(0, res.getHeldRows());

        TAdrReportRow first = svc.byPos("B-DUP", 1);
        TAdrReportRow third = svc.byPos("B-DUP", 3);
        assertEquals(MASTER, first.getMerged().intValue());
        assertEquals(SLAVE, third.getMerged().intValue());
        assertEquals(first.getId(), third.getDupOf());
        // 份数只计一次：全辖挂接份数合计是 13，不是 23。
        assertEquals(new BigDecimal("13"), svc.sumLinkedQtyInPeriod("2026Q3"));
        // 位次留靠前那一个：主行仍是第 1 位，从行保留自己原排第 3。
        assertEquals(1, first.getRowNo().intValue());
        assertEquals(3, third.getRowNo().intValue());
        assertTrue(third.getHoldReason().contains("并入靠前"));
    }

    // ============================================================
    // 三桩旧事之三：回执写"已核完"，系统查无此册
    // ============================================================
    @Test
    void receiptForUnknownBill_isNull_neverMarkedDone() {
        MemoryReconService svc = svc();
        assertNull(svc.receipt("GHOST-BILL"));
        assertNull(svc.findActiveBill("GHOST-BILL"));
    }

    // ============================================================
    // 兜底一：行数越上限，整册退回、一行不留
    // ============================================================
    @Test
    void overLimit_wholeBookRejected_nothingLanded() {
        MemoryReconService svc = svc();
        IntakeResult res = svc.intake("B-BIG", "2026Q3", 2,
                Arrays.asList(r("K1", "1"), r("K2", "1"), r("C1", "1")));
        assertTrue(res.isRejected());
        assertNull(svc.findActiveBill("B-BIG"));
        assertTrue(svc.listCurrentRows("B-BIG").isEmpty());
        assertNull(svc.receipt("B-BIG"));
    }

    // ============================================================
    // 四种不合只停那一行；同册其余各行照旧往下走；挂起带缘故
    // ============================================================
    @Test
    void fourMismatches_holdOnlyThatRow_othersProceed() {
        MemoryReconService svc = svc();
        // 情形四的存量：上一期同码 OPEN 还挂着没结（同时给本期添 5 份封顶池）。
        svc.seedRow("OLD", "2026Q2", 1, "OPEN", "5", HELD, NONE, 0);

        IntakeResult res = svc.intake("B-FOUR", "2026Q3", 500, Arrays.asList(
                r(null, "1"),          // 1 码段空：本级找不到卡
                r("NOT-A-CARD", "1"),  // 2 有码但本级没这张卡
                r("C-EMPTY", null),    // 3 份数空着
                r("C-ZERO", "0"),      // 4 份数零
                r("C-NEG", "-3"),      // 5 份数负
                r("OPEN", "5"),        // 6 上期同一笔仍挂起
                r("OK", "2")));        // 7 合规，照常挂接

        assertEquals(7, res.getTotalRows());
        assertEquals(1, res.getLinkedRows());
        assertEquals(6, res.getHeldRows());

        assertEquals(HELD, svc.byPos("B-FOUR", 1).getStatus().intValue());
        assertTrue(svc.byPos("B-FOUR", 1).getHoldReason().contains("找不到对应卡"));
        assertTrue(svc.byPos("B-FOUR", 2).getHoldReason().contains("找不到对应卡"));
        assertTrue(svc.byPos("B-FOUR", 3).getHoldReason().contains("份数空缺"));
        assertTrue(svc.byPos("B-FOUR", 4).getHoldReason().contains("零或负数"));
        assertTrue(svc.byPos("B-FOUR", 5).getHoldReason().contains("零或负数"));
        assertTrue(svc.byPos("B-FOUR", 6).getHoldReason().contains("上一期同码"));
        // 第 7 行不受牵连：已挂接，且行位次就是 7。
        TAdrReportRow ok = svc.byPos("B-FOUR", 7);
        assertEquals(LINKED, ok.getStatus().intValue());
        assertEquals(7, ok.getRowNo().intValue());
    }

    // ============================================================
    // 册级账一：本期核销以上期挂下数封顶；多的不记本行、不滚抵
    // ============================================================
    @Test
    void capByCarryover_thirtyAtMost_excessHeldNotCarried() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "30", HELD, NONE, 0);

        IntakeResult res = svc.intake("B-CAP", "2026Q3", 500, Arrays.asList(
                r("C1", "20"),  // 挂接 20，余 10
                r("C2", "20"),  // 20 > 10：整行挂起，不许拆成 10+10
                r("C3", "10"),  // 余 10 归本行挂接
                r("C4", "15"))); // 池空：挂起

        assertEquals(2, res.getLinkedRows());
        assertEquals(2, res.getHeldRows());
        assertEquals(new BigDecimal("30"), svc.sumLinkedQtyInPeriod("2026Q3"));
        // 被顶下的行不落份数：多出的一截不记本行。
        assertNull(svc.byPos("B-CAP", 2).getLinkQty());
        assertEquals(new BigDecimal("20"), svc.byPos("B-CAP", 2).getQty());

        // 同期再递一册：锅是本期共一口，30 已用尽，新册一行都挂接不了。
        IntakeResult secondBook = svc.intake("B-CAP2", "2026Q3", 500,
                Arrays.asList(r("K1", "1")));
        assertEquals(0, secondBook.getLinkedRows());
        assertEquals(1, secondBook.getHeldRows());
        assertTrue(svc.byPos("B-CAP2", 1).getHoldReason().contains("封顶"));
    }

    // ============================================================
    // 改口走作废：作废后同号重新进门，旧行软删留痕
    // ============================================================
    @Test
    void voidBill_thenSameNumberCanReenter() {
        MemoryReconService svc = svc();
        svc.intake("B-CHG", "2026Q3", 500, Arrays.asList(r("K1", "5")));
        svc.voidBill("B-CHG");
        assertNull(svc.findActiveBill("B-CHG"));
        assertNull(svc.receipt("B-CHG"));
        assertTrue(svc.listCurrentRows("B-CHG").isEmpty());

        // 无册可作废时不允许瞎作废
        assertThrows(IllegalStateException.class, () -> svc.voidBill("B-CHG"));

        // 同号重新进门改口：新册头顶旧册头落账。
        IntakeResult again = svc.intake("B-CHG", "2026Q3", 500,
                Arrays.asList(r("K1", "9"), r("K2", "1")));
        assertFalse(again.isCompareOnly());
        assertEquals(2, svc.listCurrentRows("B-CHG").size());
        assertEquals(new BigDecimal("9"), svc.byPos("B-CHG", 1).getQty());
    }

    // ============================================================
    // 空册：回执只落「空册」，不许记核讫
    // ============================================================
    @Test
    void emptyBook_receiptMarksEmpty_notDone() {
        MemoryReconService svc = svc();
        IntakeResult res = svc.intake("B-EMPTY", "2026Q3", 500, Arrays.asList());
        assertFalse(res.isRejected()); // 空册不是越线，照常受理
        assertEquals(0, res.getTotalRows());

        Receipt receipt = svc.receipt("B-EMPTY");
        assertEquals("空册", receipt.getMark());
        assertEquals(0, receipt.getTotalRows());
        assertEquals(0, receipt.getLinkedRows() + receipt.getHeldRows());
    }

    @Test
    void normalBook_receiptMarksDone_andThreeNumbersShareOneSource() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        IntakeResult res = svc.intake("B-NORMAL", "2026Q3", 500,
                Arrays.asList(r("K1", "1"), r("NOT-A-CARD", "1")));
        Receipt receipt = svc.receipt("B-NORMAL");
        assertEquals("核讫", receipt.getMark());
        // 回执三数与进门回带、与逐行点算同一个算法
        assertEquals(res.getTotalRows(), receipt.getTotalRows());
        assertEquals(res.getLinkedRows(), receipt.getLinkedRows());
        assertEquals(res.getHeldRows(), receipt.getHeldRows());
        assertEquals(receipt.getTotalRows(),
                receipt.getLinkedRows() + receipt.getHeldRows());
        int[] tally = svc.tally("B-NORMAL");
        assertEquals(receipt.getTotalRows(), tally[0]);
        assertEquals(receipt.getLinkedRows(), tally[1]);
        assertEquals(receipt.getHeldRows(), tally[2]);
    }

    // ============================================================
    // 挂起连跨两个核对期搬往期；重复拨动不重复计；往期不牵新册
    // ============================================================
    @Test
    void heldAcrossTwoPeriods_movesToArchive_idempotent() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "STUCK", "5", HELD, NONE, 0);

        assertEquals(0, svc.rolloverHeld("2026Q3")); // 跨过一期：还在现行册面
        TAdrReportRow stuck = svc.byPos("OLD", 1);
        assertEquals(1, stuck.getPeriodsHeld().intValue());
        assertEquals(0, stuck.getArchived().intValue());
        assertEquals(1, svc.listHeld(null).size());

        assertEquals(0, svc.rolloverHeld("2026Q3")); // 同口再拨：不重复计
        assertEquals(1, stuck.getPeriodsHeld().intValue());

        assertEquals(1, svc.rolloverHeld("2026Q4")); // 跨过第二期：搬往期
        assertEquals(2, stuck.getPeriodsHeld().intValue());
        assertEquals(1, stuck.getArchived().intValue());
        assertTrue(svc.listHeld(null).isEmpty());        // 现行册面不再出现
        assertEquals(1, svc.listArchived("OLD").size()); // 往期层仍可查账
        assertEquals(0, svc.rolloverHeld("2026Q4"));     // 幂等

        // 新一年开册不受往期牵住：往期的 STUCK 既不以"上期仍挂起"拦新行，也不进封顶池。
        // 另种一笔尚在现行的挂下（POOL2），供新行正常挂接。
        svc.cards.add("STUCK");
        svc.seedRow("OLD3", "2026Q4", 1, "POOL2", "10", HELD, NONE, 0);
        IntakeResult fresh = svc.intake("B-NEW-YEAR", "2027Q1", 500,
                Arrays.asList(r("STUCK", "5")));
        TAdrReportRow newRow = svc.byPos("B-NEW-YEAR", 1);
        assertEquals(LINKED, newRow.getStatus().intValue());
        assertEquals(1, fresh.getLinkedRows());
        assertNull(newRow.getHoldReason());
        // 往期行不进封顶池：挂接只用了 POOL2 的 10 份，STUCK 自己不占自己的额度。
        assertEquals(new BigDecimal("5"), svc.sumLinkedQtyInPeriod("2027Q1"));
    }

    // ============================================================
    // 全辖对账：屏上总数与逐行同源加总不平，差在哪几行当场点出
    // ============================================================
    @Test
    void reconcile_balancedWhenScreenMatchesRowWise() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        svc.intake("B-REC", "2026Q3", 500,
                Arrays.asList(r("DUP", "10"), r("DUP", "10"), r("C1", "1")));
        // 立账行两行（一挂接一挂起：第 2 行 DUP 挂接、第 3 行 C1 挂接…重新点）
        long rowWise = svc.reconcile(0).getRowWiseTotal();
        ReconReport balanced = svc.reconcile(rowWise);
        assertEquals(0, balanced.getDiff());
    }

    @Test
    void reconcile_unbalanced_pointsOutSlaveAndArchivedByBatchAndPosition() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        // 一册含双行：主行挂接 + 从行留痕
        svc.intake("B-REC", "2026Q3", 500,
                Arrays.asList(r("DUP", "10"), r("DUP", "10")));
        // 造一条往期行
        TAdrReportRow gone = svc.seedRow("OLD2", "2026Q1", 1, "GONE", "3", HELD, NONE, 1);

        ReconReport plain = svc.reconcile(0);
        long rowWise = plain.getRowWiseTotal();
        // 屏上若按物理行点数：现行物理 2 行（含从行）+ 往期 1 行
        long physicalCount = svc.listAllEffectiveRows().size();
        ReconReport report = svc.reconcile(physicalCount);

        assertEquals(physicalCount - rowWise, report.getDiff());
        assertEquals(2, report.getDiffLines().size());
        ReconReport.DiffLine slaveLine = null;
        ReconReport.DiffLine archivedLine = null;
        for (ReconReport.DiffLine line : report.getDiffLines()) {
            if ("SLAVE".equals(line.getReason())) {
                slaveLine = line;
            }
            if ("ARCHIVED".equals(line.getReason())) {
                archivedLine = line;
            }
        }
        assertNotNull(slaveLine);
        assertNotNull(archivedLine);
        // 差行直接带册号 + 本册位次，不叫人回纸页数
        assertEquals("B-REC", slaveLine.getBatchNo());
        assertEquals(Integer.valueOf(2), slaveLine.getRowNo());
        assertEquals("OLD2", archivedLine.getBatchNo());
        assertEquals(Integer.valueOf(1), archivedLine.getRowNo());
        assertEquals(gone.getId(), svc.listArchived("OLD2").get(0).getId());
    }

    // ============================================================
    // 位次进门定死：挂起的行翻出来，屏上直接带出原排第几
    // ============================================================
    @Test
    void rowPositionFixedAtIntake_visibleOnHeldRow() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        svc.intake("B-POS", "2026Q3", 500, Arrays.asList(
                r("K1", "1"), r("K2", "1"), r("NO-SUCH", "1"), r("C1", "1")));
        List<TAdrReportRow> held = svc.listHeld("2026Q3");
        assertEquals(1, held.size());
        assertEquals(Integer.valueOf(3), held.get(0).getRowNo()); // 不用回纸页数
    }

    // ============================================================
    // 只认码：本级卡库没有的码，单位名号再像也不算对上
    // ============================================================
    @Test
    void matchingByCodeOnly_nameLookalikeDoesNotCount() {
        MemoryReconService svc = svc();
        svc.seedRow("OLD", "2026Q2", 1, "POOL", "100", HELD, NONE, 0);
        // "K1 " 带空格与 "K1" 同码归一；"k1" 是另一码，本级没挂号→找不到卡
        IntakeResult res = svc.intake("B-CODE", "2026Q3", 500, Arrays.asList(
                r("k1", "1"), r("K1 ", "1")));
        assertEquals(1, res.getLinkedRows());
        assertEquals(1, res.getHeldRows());
        assertEquals(HELD, svc.byPos("B-CODE", 1).getStatus().intValue());
        assertEquals(LINKED, svc.byPos("B-CODE", 2).getStatus().intValue());
    }

    @Test
    void rejectsBlankBatchOrPeriodOrNonPositiveLimit() {
        MemoryReconService svc = svc();
        assertThrows(IllegalArgumentException.class,
                () -> svc.intake("  ", "2026Q3", 500, Arrays.asList()));
        assertThrows(IllegalArgumentException.class,
                () -> svc.intake("B", "2026Q3", 0, Arrays.asList()));
    }
}

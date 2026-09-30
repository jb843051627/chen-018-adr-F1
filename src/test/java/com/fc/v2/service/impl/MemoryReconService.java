package com.fc.v2.service.impl;

import com.fc.v2.model.auto.TAdrReconBill;
import com.fc.v2.model.auto.TAdrReportRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存账基架：只覆数据缝（两个底座集合 + 增改 + 卡存在性），
 * 落点、并账、封顶、回执点算、对账算法全部跑生产真身。
 */
class MemoryReconService extends TAdrReportRowServiceImpl {

    final List<TAdrReportRow> rows = new ArrayList<>();
    final List<TAdrReconBill> bills = new ArrayList<>();
    final Set<String> cards = new HashSet<>();
    private final AtomicLong idSeq = new AtomicLong(1000);

    @Override
    List<TAdrReportRow> listAllEffectiveRows() {
        List<TAdrReportRow> copy = new ArrayList<>(rows);
        copy.removeIf(r -> r.getDelFlag() != null && r.getDelFlag() == 1);
        copy.sort(Comparator.comparing(TAdrReportRow::getBatchNo,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TAdrReportRow::getRowNo,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return copy;
    }

    @Override
    List<TAdrReconBill> listBills() {
        List<TAdrReconBill> copy = new ArrayList<>(bills);
        copy.removeIf(b -> b.getDelFlag() != null && b.getDelFlag() == 1);
        copy.sort(Comparator.comparing(TAdrReconBill::getId,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return copy;
    }

    @Override
    TAdrReportRow getRowById(Long id) {
        for (TAdrReportRow row : rows) {
            if (id.equals(row.getId())) {
                return row;
            }
        }
        return null;
    }

    @Override
    int insertRow(TAdrReportRow row) {
        if (row.getId() == null) {
            row.setId(idSeq.incrementAndGet());
        }
        if (row.getCreateTime() == null) {
            row.setCreateTime(new Date());
        }
        rows.add(row);
        return 1;
    }

    @Override
    int updateRow(TAdrReportRow row) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId().equals(row.getId())) {
                rows.set(i, row);
                return 1;
            }
        }
        return 0;
    }

    @Override
    int insertBill(TAdrReconBill bill) {
        if (bill.getId() == null) {
            bill.setId(idSeq.incrementAndGet());
        }
        if (bill.getCreateTime() == null) {
            bill.setCreateTime(new Date());
        }
        bills.add(bill);
        return 1;
    }

    @Override
    int updateBill(TAdrReconBill bill) {
        for (int i = 0; i < bills.size(); i++) {
            if (bills.get(i).getId().equals(bill.getId())) {
                bills.set(i, bill);
                return 1;
            }
        }
        return 0;
    }

    @Override
    boolean cardExists(String code) {
        return cards.contains(code);
    }

    // ---- 测试造数帮手 ----

    /** 直接往库里种一行（造上期挂下、往期行等存量）。 */
    TAdrReportRow seedRow(String batchNo, String periodNo, int rowNo, String code,
                          String qty, int status, int merged, int archived) {
        TAdrReportRow row = new TAdrReportRow();
        row.setId(idSeq.incrementAndGet());
        row.setBatchNo(batchNo);
        row.setPeriodNo(periodNo);
        row.setRowNo(rowNo);
        row.setItemCode(code);
        row.setQty(qty == null ? null : new java.math.BigDecimal(qty));
        row.setStatus(status);
        row.setMerged(merged);
        row.setArchived(archived);
        row.setPeriodsHeld(0);
        row.setRolledInto(periodNo);
        row.setDelFlag(0);
        row.setCreateTime(new Date());
        if (status == 1) {
            row.setLinkQty(row.getQty());
        }
        rows.add(row);
        return row;
    }

    TAdrReportRow byPos(String batchNo, int rowNo) {
        for (TAdrReportRow row : listStoredPhysicalRows(batchNo)) {
            if (row.getRowNo() == rowNo) {
                return row;
            }
        }
        return null;
    }
}

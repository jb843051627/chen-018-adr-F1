package com.fc.v2.model.custom.adr;

import java.util.ArrayList;
import java.util.List;

/**
 * 全辖对账：屏上总数与逐行同源加总接不平时，把差在哪几行当场点出来，
 * 不许拿一个成数糊过去。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class ReconReport {

    /** 屏上自点的总数（送来对账的那个数） */
    private long screenTotal;

    /** 逐行同源加总：已挂接 */
    private long linkedRows;

    /** 逐行同源加总：挂起 */
    private long heldRows;

    /** 逐行同源加总（与各册回执同一个点算法） */
    private long rowWiseTotal;

    /** 差额 = 屏上总数 - 逐行加总；为 0 即接平 */
    private long diff;

    /** 逐行点名的差行（每一行都说清为何算不进同源账） */
    private List<DiffLine> diffLines = new ArrayList<>();

    /** 差行能解释的行数；与 diff 不等时说明还有屏上/逐行两侧对不上的散差，不糊 */
    private long explained;

    public long getScreenTotal() {
        return screenTotal;
    }

    public void setScreenTotal(long screenTotal) {
        this.screenTotal = screenTotal;
    }

    public long getLinkedRows() {
        return linkedRows;
    }

    public void setLinkedRows(long linkedRows) {
        this.linkedRows = linkedRows;
    }

    public long getHeldRows() {
        return heldRows;
    }

    public void setHeldRows(long heldRows) {
        this.heldRows = heldRows;
    }

    public long getRowWiseTotal() {
        return rowWiseTotal;
    }

    public void setRowWiseTotal(long rowWiseTotal) {
        this.rowWiseTotal = rowWiseTotal;
    }

    public long getDiff() {
        return diff;
    }

    public void setDiff(long diff) {
        this.diff = diff;
    }

    public List<DiffLine> getDiffLines() {
        return diffLines;
    }

    public void setDiffLines(List<DiffLine> diffLines) {
        this.diffLines = diffLines;
    }

    public long getExplained() {
        return explained;
    }

    public void setExplained(long explained) {
        this.explained = explained;
    }

    /**
     * 一条差行：册号 + 本册位次直接定位，不叫人回纸页去数。
     */
    public static class DiffLine {
        private String batchNo;
        private Integer rowNo;
        private String itemCode;
        private Integer status;
        /** 缘故：PENDING 仍待核 / SLAVE 并账从行 / ARCHIVED 已搬往期 / VOIDED 册已作废 / DELETED 已删行 */
        private String reason;

        public DiffLine() {
        }

        public DiffLine(String batchNo, Integer rowNo, String itemCode, Integer status, String reason) {
            this.batchNo = batchNo;
            this.rowNo = rowNo;
            this.itemCode = itemCode;
            this.status = status;
            this.reason = reason;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}

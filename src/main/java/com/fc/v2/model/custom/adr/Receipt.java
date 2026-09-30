package com.fc.v2.model.custom.adr;

/**
 * 回执：本册多少行、挂接多少行、挂起多少行。
 * 三个数与逐行落库后点算出自同源——挂接 + 挂起 = 本册行数，不另算一摊。
 * 全册无行可核只落「空册」，不许记核讫。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class Receipt {

    private String batchNo;

    private String periodNo;

    /** 本册行数（从行不计，落账行才计数） */
    private int totalRows;

    private int linkedRows;

    private int heldRows;

    /** 回执口径：EMPTY 空册 / DONE 核讫 */
    private String mark;

    /** 已作废册不出回执 */
    private boolean voided;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getLinkedRows() {
        return linkedRows;
    }

    public void setLinkedRows(int linkedRows) {
        this.linkedRows = linkedRows;
    }

    public int getHeldRows() {
        return heldRows;
    }

    public void setHeldRows(int heldRows) {
        this.heldRows = heldRows;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }
}

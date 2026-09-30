package com.fc.v2.model.custom.adr;

import java.util.ArrayList;
import java.util.List;

/**
 * 进门结果。
 * 首进：accepted=true 时整册落账（含挂起行）；rejected=true 表示越上限，原册退回、一行不留。
 * 二次进：compareOnly=true，落点不动、行不增不改，结果装在 compareReport 里。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IntakeResult {

    /** 下发册号（门口定死的那个，不取行自带） */
    private String batchNo;

    /** 期号 */
    private String periodNo;

    /** 行数越上限，整册退回 */
    private boolean rejected;

    /** 册号在库（未作废）——本次只走比对 */
    private boolean compareOnly;

    /** 本册落账行数（挂接+挂起，从行不计） */
    private int totalRows;

    /** 已挂接行数 */
    private int linkedRows;

    /** 挂起行数 */
    private int heldRows;

    /** 退回/只受理时给一句话缘故 */
    private String message;

    /** 逐行落点（首进时回带，位次即原册位次） */
    private List<LandedRow> rows = new ArrayList<>();

    /** 二次进门的比对报告（仅 compareOnly 时有值） */
    private CompareReport compareReport;

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

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public boolean isCompareOnly() {
        return compareOnly;
    }

    public void setCompareOnly(boolean compareOnly) {
        this.compareOnly = compareOnly;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<LandedRow> getRows() {
        return rows;
    }

    public void setRows(List<LandedRow> rows) {
        this.rows = rows;
    }

    public CompareReport getCompareReport() {
        return compareReport;
    }

    public void setCompareReport(CompareReport compareReport) {
        this.compareReport = compareReport;
    }
}

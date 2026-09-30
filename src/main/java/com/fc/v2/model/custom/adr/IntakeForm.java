package com.fc.v2.model.custom.adr;

import java.util.ArrayList;
import java.util.List;

/**
 * 一册进门表单：册号、期号、单册上限在门口随册头给定，行内不许自带册号。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IntakeForm {

    /** 下发册号（一册一个，进门定死） */
    private String batchNo;

    /** 核对期号（如 2026Q3） */
    private String periodNo;

    /** 中心定下的单册行数上限 */
    private Integer rowLimit;

    /** 原册各行，按送达先后提交，位次照此排定 */
    private List<RowInput> rows = new ArrayList<>();

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

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public List<RowInput> getRows() {
        return rows;
    }

    public void setRows(List<RowInput> rows) {
        this.rows = rows;
    }
}

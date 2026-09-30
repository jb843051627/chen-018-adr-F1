package com.fc.v2.model.custom.adr;

import java.math.BigDecimal;

/**
 * 一行落账后的样子：位次进门定死后带到哪一步都不改。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class LandedRow {

    /** 主键（从行也留行、有 id，可溯） */
    private Long id;

    /** 本册内位次（原册第几行） */
    private Integer rowNo;

    /** 追溯码 */
    private String itemCode;

    /** 上报份数 */
    private BigDecimal qty;

    /** 实际落账份数（封顶后/双行只计一次） */
    private BigDecimal linkQty;

    /** 落点 0待核 1已挂接 2挂起 */
    private Integer status;

    /** 挂起缘故 */
    private String holdReason;

    /** 并账角色 0独立行 1主行 2从行（从行仍占原位次） */
    private Integer merged;

    /** 从行指向主行 */
    private Long dupOf;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getLinkQty() {
        return linkQty;
    }

    public void setLinkQty(BigDecimal linkQty) {
        this.linkQty = linkQty;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public Integer getMerged() {
        return merged;
    }

    public void setMerged(Integer merged) {
        this.merged = merged;
    }

    public Long getDupOf() {
        return dupOf;
    }

    public void setDupOf(Long dupOf) {
        this.dupOf = dupOf;
    }
}

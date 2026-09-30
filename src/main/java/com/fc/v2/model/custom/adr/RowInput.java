package com.fc.v2.model.custom.adr;

import java.math.BigDecimal;

/**
 * 进门一行的原始所填：追溯码一段 + 份数。
 * 册号、期号、位次均不许由行自带，进门时在门口定死。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class RowInput {

    /** 被挑出的报告卡追溯码 */
    private String itemCode;

    /** 上报份数（可空、可零可负——交由落点评判，进门不拦） */
    private BigDecimal qty;

    public RowInput() {
    }

    public RowInput(String itemCode, BigDecimal qty) {
        this.itemCode = itemCode;
        this.qty = qty;
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
}

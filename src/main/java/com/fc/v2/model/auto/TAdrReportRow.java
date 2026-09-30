package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 上级下发关联册核对行对象 t_adr_report_row
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_adr_report_row")
@ApiModel(value = "TAdrReportRow", description = "上级下发关联册核对行")
public class TAdrReportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 上级中心下发册次号（进门定死，行随册走） */
    @TableField("batch_no")
    @ApiModelProperty(value = "上级中心下发册次号")
    private String batchNo;

    /** 进门核对期号（哪一期进门就记哪一期） */
    @TableField("period_no")
    @ApiModelProperty(value = "进门核对期号")
    private String periodNo;

    /** 本册内位次（进门按先后排定，此后走到哪一步都不改） */
    @TableField("row_no")
    @ApiModelProperty(value = "本册内位次")
    private Integer rowNo;

    /** 被挑出的报告卡追溯码（对卡只认此码，不认单位名号） */
    @TableField("item_code")
    @ApiModelProperty(value = "被挑出的报告卡追溯码")
    private String itemCode;

    /** 上报份数（原册所填，原样留痕） */
    @TableField("qty")
    @ApiModelProperty(value = "上报份数")
    private BigDecimal qty;

    /** 实际核销落账份数（受上期挂下封顶，双行只计一次） */
    @TableField("link_qty")
    @ApiModelProperty(value = "实际核销落账份数")
    private BigDecimal linkQty;

    /** 行落地情形 0待核 1已挂接 2挂起 */
    @TableField("status")
    @ApiModelProperty(value = "行落地情形 0待核 1已挂接 2挂起")
    private Integer status;

    /** 挂起缘故（四种不合之一，一句话） */
    @TableField("hold_reason")
    @ApiModelProperty(value = "挂起缘故")
    private String holdReason;

    /** 挂起已跨过的核对期数（连跨两期搬往期） */
    @TableField("periods_held")
    @ApiModelProperty(value = "挂起已跨过的核对期数")
    private Integer periodsHeld;

    /** 最近一次已拨入的新期号（防同一期重复拨动） */
    @TableField("rolled_into")
    @ApiModelProperty(value = "最近一次已拨入的新期号")
    private String rolledInto;

    /** 本册同码双行并账标记 0独立行 1靠前主行 2并入主行的从行 */
    @TableField("merged")
    @ApiModelProperty(value = "本册同码双行并账标记 0独立行 1靠前主行 2并入主行的从行")
    private Integer merged;

    /** 从行并入的主行id（merged=2 时落值，留痕可溯） */
    @TableField("dup_of")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "从行并入的主行id")
    private Long dupOf;

    /** 是否已搬往期 0现行册面 1往期（只供事后查账） */
    @TableField("archived")
    @ApiModelProperty(value = "是否已搬往期 0现行册面 1往期")
    private Integer archived;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(String periodNo) {
        this.periodNo = periodNo;
    }

    public BigDecimal getLinkQty() {
        return linkQty;
    }

    public void setLinkQty(BigDecimal linkQty) {
        this.linkQty = linkQty;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public Integer getPeriodsHeld() {
        return periodsHeld;
    }

    public void setPeriodsHeld(Integer periodsHeld) {
        this.periodsHeld = periodsHeld;
    }

    public String getRolledInto() {
        return rolledInto;
    }

    public void setRolledInto(String rolledInto) {
        this.rolledInto = rolledInto;
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

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

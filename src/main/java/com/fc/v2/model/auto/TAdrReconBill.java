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
import java.util.Date;

/**
 * 上级下发对照册册级账对象 t_adr_recon_bill
 * 一册一个下发册号，进门定死；二次进门只走比对；改口走作废。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_adr_recon_bill")
@ApiModel(value = "TAdrReconBill", description = "上级下发对照册册级账")
public class TAdrReconBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 下发册次号（一册一个，进门定死） */
    @TableField("batch_no")
    @ApiModelProperty(value = "下发册次号")
    private String batchNo;

    /** 进门核对期号 */
    @TableField("period_no")
    @ApiModelProperty(value = "进门核对期号")
    private String periodNo;

    /** 进门时中心所定单册行数上限（快照） */
    @TableField("row_limit")
    @ApiModelProperty(value = "单册行数上限")
    private Integer rowLimit;

    /** 本册落账行数（回执口径：挂接+挂起=本册行数） */
    @TableField("total_rows")
    @ApiModelProperty(value = "本册行数")
    private Integer totalRows;

    /** 已挂接行数（逐行落库后同源点算） */
    @TableField("linked_rows")
    @ApiModelProperty(value = "已挂接行数")
    private Integer linkedRows;

    /** 挂起行数（逐行落库后同源点算） */
    @TableField("held_rows")
    @ApiModelProperty(value = "挂起行数")
    private Integer heldRows;

    /** 是否已作废 0在册 1已作废 */
    @TableField("voided")
    @ApiModelProperty(value = "是否已作废 0在册 1已作废")
    private Integer voided;

    /** 回执情形 0空册 1核讫（空册不得记核讫） */
    @TableField("receipt_status")
    @ApiModelProperty(value = "回执情形 0空册 1核讫")
    private Integer receiptStatus;

    /** 二次进门只比对标记 0首进落账 1二次仅比对 */
    @TableField("compared")
    @ApiModelProperty(value = "二次进门只比对标记")
    private Integer compared;

    /** 最近一次比对时刻 */
    @TableField("compared_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "最近一次比对时刻")
    private Date comparedAt;

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

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getLinkedRows() {
        return linkedRows;
    }

    public void setLinkedRows(Integer linkedRows) {
        this.linkedRows = linkedRows;
    }

    public Integer getHeldRows() {
        return heldRows;
    }

    public void setHeldRows(Integer heldRows) {
        this.heldRows = heldRows;
    }

    public Integer getVoided() {
        return voided;
    }

    public void setVoided(Integer voided) {
        this.voided = voided;
    }

    public Integer getReceiptStatus() {
        return receiptStatus;
    }

    public void setReceiptStatus(Integer receiptStatus) {
        this.receiptStatus = receiptStatus;
    }

    public Integer getCompared() {
        return compared;
    }

    public void setCompared(Integer compared) {
        this.compared = compared;
    }

    public Date getComparedAt() {
        return comparedAt;
    }

    public void setComparedAt(Date comparedAt) {
        this.comparedAt = comparedAt;
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

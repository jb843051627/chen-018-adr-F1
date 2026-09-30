package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TAdrReportRow;
import com.fc.v2.model.custom.adr.IntakeForm;
import com.fc.v2.model.custom.adr.Receipt;
import com.fc.v2.model.custom.adr.ReconReport;
import com.fc.v2.service.ITAdrReportRowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 上级下发对照册核对 Controller。
 * 进门（首进落账/二次自动只比对）、作废改口、回执三数同源、跨期归档、全辖对账。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "上级下发对照册核对")
@Controller
@RequestMapping("/adrReportRow")
public class AdrReportRowController extends BaseController {

    @Autowired
    private ITAdrReportRowService adrReportRowService;

    @Log(title = "对照册进门（首进落账/二次只比对）", action = "intake")
    @ApiOperation(value = "一册进门", notes = "同一册号第二次进门只比对，不改不增；改口请先作废")
    @PostMapping("/intake")
    @RequiresPermissions("adrReportRow:intake")
    @ResponseBody
    public AjaxResult intake(IntakeForm form) {
        if (form == null || isBlank(form.getBatchNo()) || isBlank(form.getPeriodNo())
                || form.getRowLimit() == null || form.getRowLimit() <= 0) {
            return AjaxResult.error("册号、期号、单册上限均不可空，且上限须为正数");
        }
        return AjaxResult.success(200, "进门受理",
                adrReportRowService.intake(form.getBatchNo(), form.getPeriodNo(),
                        form.getRowLimit(), form.getRows()));
    }

    @Log(title = "对照册作废", action = "void")
    @ApiOperation(value = "在册作废", notes = "同号要改口先作废，第二遍不覆第一遍")
    @PostMapping("/void")
    @RequiresPermissions("adrReportRow:intake")
    @ResponseBody
    public AjaxResult voidBill(@RequestParam String batchNo) {
        try {
            adrReportRowService.voidBill(batchNo);
        } catch (IllegalStateException e) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.success("已作废，同号可重新进门");
    }

    @Log(title = "对照册回执", action = "receipt")
    @ApiOperation(value = "出回执", notes = "三数与逐行点算同源；查无此册不出单，空册只落空册")
    @GetMapping("/receipt")
    @RequiresPermissions("adrReportRow:receipt")
    @ResponseBody
    public AjaxResult receipt(@RequestParam String batchNo) {
        Receipt receipt = adrReportRowService.receipt(batchNo);
        // 查无此册：系统不出回执，更不许记一笔核讫。
        if (receipt == null) {
            return AjaxResult.error(404, "查无在库册号，不能出回执（更不能记核讫）");
        }
        return AjaxResult.success(200, receipt.getMark(), receipt);
    }

    @ApiOperation(value = "现行册面各行", notes = "位次进门定死，翻挂起一笔屏上直接带出原排第几")
    @GetMapping("/currentRows")
    @RequiresPermissions("adrReportRow:list")
    @ResponseBody
    public AjaxResult currentRows(@RequestParam String batchNo) {
        List<TAdrReportRow> rows = adrReportRowService.listCurrentRows(batchNo);
        return AjaxResult.success(200, "现行册面", rows);
    }

    @ApiOperation(value = "挂起行", notes = "可按期号过滤；每行带本册位次与挂起缘故")
    @GetMapping("/held")
    @RequiresPermissions("adrReportRow:list")
    @ResponseBody
    public AjaxResult held(@RequestParam(required = false) String periodNo) {
        return AjaxResult.success(200, "挂起行", adrReportRowService.listHeld(periodNo));
    }

    @Log(title = "挂起跨期拨动", action = "rollover")
    @ApiOperation(value = "跨入新一期拨动挂起", notes = "连跨两期仍无落点的搬往期；同一期重复拨动不重复计")
    @PostMapping("/rollover")
    @RequiresPermissions("adrReportRow:rollover")
    @ResponseBody
    public AjaxResult rollover(@RequestParam String periodNo) {
        int archived = adrReportRowService.rolloverHeld(periodNo);
        return AjaxResult.success(200, "本期搬往期 " + archived + " 行", archived);
    }

    @ApiOperation(value = "往期层查账", notes = "只供事后查账，不牵住新一年开册")
    @GetMapping("/archived")
    @RequiresPermissions("adrReportRow:list")
    @ResponseBody
    public AjaxResult archived(@RequestParam(required = false) String batchNo) {
        return AjaxResult.success(200, "往期行", adrReportRowService.listArchived(batchNo));
    }

    @Log(title = "全辖对账", action = "reconcile")
    @ApiOperation(value = "全辖对账", notes = "屏上总数对逐行同源加总，不平当场点出差在哪几行")
    @PostMapping("/reconcile")
    @RequiresPermissions("adrReportRow:reconcile")
    @ResponseBody
    public AjaxResult reconcile(@RequestParam long screenTotal) {
        ReconReport report = adrReportRowService.reconcile(screenTotal);
        String msg = report.getDiff() == 0 ? "账接平" : "不平，差额 " + report.getDiff() + "，差行已逐行点出";
        return AjaxResult.success(200, msg, report);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TAdrReportRow;
import com.fc.v2.model.custom.adr.IntakeResult;
import com.fc.v2.model.custom.adr.Receipt;
import com.fc.v2.model.custom.adr.ReconReport;
import com.fc.v2.model.custom.adr.RowInput;

/**
 * 上级下发关联册核对行 Service。
 *
 * 进门规矩：
 * 1. 一册一个下发册号，册号进门定死；一册的行不许混进另一册。
 * 2. 每行进门自动占本册内位次，照先后排定，此后走到哪一步都改不掉。
 * 3. 一行一个落点：待核 / 已挂接 / 挂起；四种不合只停那一行，同册其余各行照旧往下走。
 * 4. 行数越上限整册退回、一行不留。
 * 5. 同册号第二次进门只比对：落点不动、行不增不改；改口先走作废。
 * 6. 本册同码双行合成一条：份数只计一次，位次留靠前那一个。
 * 7. 本期核销以上期挂下数封顶，多出的一截不记本行、不移抵他行。
 * 8. 空册回执只落「空册」，不许记核讫；挂起连跨两期搬往期。
 * 9. 回执三数与逐行落库后点算同源；全辖对账不平当场点出差行。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface ITAdrReportRowService {

    /** 按主键回查单行（屏上翻挂起一笔时，位次直接由行带出） */
    TAdrReportRow selectTAdrReportRowById(Long id);

    /**
     * 一册进门。
     *
     * @param batchNo  下发册号（门口定死，不取行自带）
     * @param periodNo 核对期号（如 2026Q3）
     * @param rowLimit 中心定下的单册行数上限
     * @param rows     原册各行（按送达先后，位次照此排定）
     * @return 首进落账结果；册号已在册时只回比对报告；越上限整册退回
     */
    IntakeResult intake(String batchNo, String periodNo, int rowLimit, List<RowInput> rows);

    /** 作废在册：册头记作废、行软删；作废后同号方可重新进门改口 */
    void voidBill(String batchNo);

    /**
     * 出回执：本册行数 / 挂接 / 挂起，挂接+挂起=本册行数，与逐行点算同源。
     * 查无此册（或已作废）返回 null——绝不替不存在的册记一笔核讫。
     */
    Receipt receipt(String batchNo);

    /** 现行册面各行（含并账从行留痕），按本册位次升序 */
    List<TAdrReportRow> listCurrentRows(String batchNo);

    /** 挂起行（periodNo 给定期号则只看该期；留空看全部现行挂起），按册号、位次升序 */
    List<TAdrReportRow> listHeld(String periodNo);

    /**
     * 跨入新一期时拨一拨挂起账：挂起每跨过一期计一次，连跨两期仍无落点的搬往期。
     *
     * @param newPeriodNo 新期号
     * @return 本期搬到往期的行数
     */
    int rolloverHeld(String newPeriodNo);

    /** 往期层查账（只供事后查账，不牵住新一年开册） */
    List<TAdrReportRow> listArchived(String batchNo);

    /**
     * 全辖对账：逐行同源加总对齐屏上自点总数，不平就逐行点出差在哪几行。
     *
     * @param screenTotal 屏上自己点数报上来的总数
     */
    ReconReport reconcile(long screenTotal);
}

package com.fc.v2.model.custom.adr;

import java.util.ArrayList;
import java.util.List;

/**
 * 二次进门只比对的报告：拿册上每一行去比已落库的存量行，比完即止。
 * 落点不动、行不增不改；要改口先走作废，不许第二遍覆第一遍。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class CompareReport {

    /** 册上行数 */
    private int incomingRows;

    /** 存量行数（在册、未作废、未删除的有效落点行；从行只留痕不参与对账计数） */
    private int storedRows;

    /** 码与份数都一致的行（按位次对齐后逐行比） */
    private List<MatchLine> matched = new ArrayList<>();

    /** 对不上的行（码不同或份数不同），当场点出，不许拿成数糊 */
    private List<MatchLine> mismatched = new ArrayList<>();

    /** 册上有、库里无的位次 */
    private List<Integer> onlyIncoming = new ArrayList<>();

    /** 库里有、册上无的位次 */
    private List<Integer> onlyStored = new ArrayList<>();

    public int getIncomingRows() {
        return incomingRows;
    }

    public void setIncomingRows(int incomingRows) {
        this.incomingRows = incomingRows;
    }

    public int getStoredRows() {
        return storedRows;
    }

    public void setStoredRows(int storedRows) {
        this.storedRows = storedRows;
    }

    public List<MatchLine> getMatched() {
        return matched;
    }

    public void setMatched(List<MatchLine> matched) {
        this.matched = matched;
    }

    public List<MatchLine> getMismatched() {
        return mismatched;
    }

    public void setMismatched(List<MatchLine> mismatched) {
        this.mismatched = mismatched;
    }

    public List<Integer> getOnlyIncoming() {
        return onlyIncoming;
    }

    public void setOnlyIncoming(List<Integer> onlyIncoming) {
        this.onlyIncoming = onlyIncoming;
    }

    public List<Integer> getOnlyStored() {
        return onlyStored;
    }

    public void setOnlyStored(List<Integer> onlyStored) {
        this.onlyStored = onlyStored;
    }

    /**
     * 一行比对明细。按本册位次对齐——位次进门定死，是天然的对齐键。
     */
    public static class MatchLine {
        private Integer rowNo;
        private String incomingCode;
        private String storedCode;
        private String incomingQty;
        private String storedQty;
        /** 对不上的点：CODE 码不同 / QTY 份数不同 */
        private String differAt;

        public MatchLine() {
        }

        public MatchLine(Integer rowNo, String incomingCode, String storedCode,
                         String incomingQty, String storedQty, String differAt) {
            this.rowNo = rowNo;
            this.incomingCode = incomingCode;
            this.incomingQty = incomingQty;
            this.storedCode = storedCode;
            this.storedQty = storedQty;
            this.differAt = differAt;
        }

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getIncomingCode() {
            return incomingCode;
        }

        public void setIncomingCode(String incomingCode) {
            this.incomingCode = incomingCode;
        }

        public String getStoredCode() {
            return storedCode;
        }

        public void setStoredCode(String storedCode) {
            this.storedCode = storedCode;
        }

        public String getIncomingQty() {
            return incomingQty;
        }

        public void setIncomingQty(String incomingQty) {
            this.incomingQty = incomingQty;
        }

        public String getStoredQty() {
            return storedQty;
        }

        public void setStoredQty(String storedQty) {
            this.storedQty = storedQty;
        }

        public String getDifferAt() {
            return differAt;
        }

        public void setDifferAt(String differAt) {
            this.differAt = differAt;
        }
    }
}

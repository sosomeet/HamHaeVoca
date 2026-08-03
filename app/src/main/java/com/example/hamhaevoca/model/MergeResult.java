package com.example.hamhaevoca.model;

public class MergeResult {
    private final int addedCount;
    private final int updatedCount;
    private final int matchedCount;
    private final int keptOldCount;

    public MergeResult(int addedCount, int updatedCount, int matchedCount, int keptOldCount) {
        this.addedCount = addedCount;
        this.updatedCount = updatedCount;
        this.matchedCount = matchedCount;
        this.keptOldCount = keptOldCount;
    }

    public int getAddedCount() { return addedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getMatchedCount() { return matchedCount; }
    public int getKeptOldCount() { return keptOldCount; }

    public String toUserMessage() {
        return "새 단어 " + addedCount + "개 · 내용 갱신 " + updatedCount
                + "개 · 기록 유지 " + matchedCount + "개 · 기존 유지 " + keptOldCount + "개";
    }
}

package com.example.hamhaevoca.model;

public class BookSummary {
    private long id;
    private String name;
    private int totalCount;
    private int memorizedCount;
    private String updatedAt;

    public BookSummary(long id, String name, int totalCount, int memorizedCount, String updatedAt) {
        this.id = id;
        this.name = name;
        this.totalCount = totalCount;
        this.memorizedCount = memorizedCount;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getTotalCount() { return totalCount; }
    public int getMemorizedCount() { return memorizedCount; }
    public int getUnmemorizedCount() { return Math.max(0, totalCount - memorizedCount); }
    public String getUpdatedAt() { return updatedAt; }
}

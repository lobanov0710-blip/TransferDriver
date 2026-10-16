package ru.transferservis.driver.ui.main;

public final class OrderCounts {

    private final boolean exact;

    private final int newCount;
    private final int activeCount;
    private final int historyCount;

    private OrderCounts(
            boolean exact,
            int newCount,
            int activeCount,
            int historyCount
    ) {

        this.exact =
                exact;

        this.newCount =
                newCount;

        this.activeCount =
                activeCount;

        this.historyCount =
                historyCount;
    }

    public static OrderCounts exact(
            int newCount,
            int activeCount,
            int historyCount
    ) {

        return new OrderCounts(
                true,
                Math.max(
                        newCount,
                        0
                ),
                Math.max(
                        activeCount,
                        0
                ),
                Math.max(
                        historyCount,
                        0
                )
        );
    }

    public static OrderCounts unavailable() {

        return new OrderCounts(
                false,
                0,
                0,
                0
        );
    }

    public boolean isExact() {

        return exact;
    }

    public int getNewCount() {

        return newCount;
    }

    public int getActiveCount() {

        return activeCount;
    }

    public int getHistoryCount() {

        return historyCount;
    }
}
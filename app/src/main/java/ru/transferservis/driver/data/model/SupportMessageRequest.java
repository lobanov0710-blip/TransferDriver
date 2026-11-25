package ru.transferservis.driver.data.model;

public final class SupportMessageRequest {

    private final String body;
    private final long expectedRevision;

    public SupportMessageRequest(
            String body,
            long expectedRevision
    ) {
        this.body =
                body;

        this.expectedRevision =
                expectedRevision;
    }

    public String getBody() {
        return body;
    }

    public long getExpectedRevision() {
        return expectedRevision;
    }
}

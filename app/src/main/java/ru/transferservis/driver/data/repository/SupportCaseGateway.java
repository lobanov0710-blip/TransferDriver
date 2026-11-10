package ru.transferservis.driver.data.repository;

public interface SupportCaseGateway {

    void getSupportCase(
            String caseId,
            DriverV1Repository.SupportCaseCallback callback
    );

    void sendSupportMessage(
            String caseId,
            String message,
            long expectedRevision,
            String idempotencyKey,
            DriverV1Repository.SupportCaseCallback callback
    );
}

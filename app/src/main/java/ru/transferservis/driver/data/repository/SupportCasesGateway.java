package ru.transferservis.driver.data.repository;

/** The driver support list/create contract; production delegates to DriverV1Repository. */
public interface SupportCasesGateway {

    void getSupportCases(
            int limit,
            DriverV1Repository.SupportCasesCallback callback
    );

    void createSupportCase(
            String category,
            String orderId,
            String idempotencyKey,
            DriverV1Repository.SupportCaseCallback callback
    );
}

package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.OrderApi;
import ru.transferservis.driver.data.api.StaffAuthInterceptor;
import ru.transferservis.driver.data.model.DriverAssignmentDto;
import ru.transferservis.driver.data.model.DriverAssignmentsResponse;
import ru.transferservis.driver.data.model.OrderStatusRequest;
import ru.transferservis.driver.data.model.OrderTransitionResponse;
import ru.transferservis.driver.data.model.OrdersResponse;

public final class OrderRepository {

    public interface OrdersCallback {

        void onSuccess(
                OrdersResponse response
        );

        void onAuthenticationRequired();

        void onError(
                int httpStatus,
                String message
        );
    }

    public interface TransitionCallback {

        void onSuccess(
                OrderTransitionResponse response
        );

        void onAuthenticationRequired();

        void onConflict();

        void onError(
                int httpStatus,
                String message
        );
    }

    private final OrderApi orderApi;

    public OrderRepository() {

        orderApi =
                ApiClient.getOrderApi();
    }

    public void getOrders(
            String status,
            int limit,
            OrdersCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String normalizedStatus =
                normalizeStatus(
                        status
                );

        if (
                status != null
                        &&
                        !status
                                .trim()
                                .isEmpty()
                        &&
                        normalizedStatus == null
        ) {

            callback.onError(
                    0,
                    "Некорректный статус заявки"
            );

            return;
        }

        int safeLimit =
                Math.max(
                        1,
                        Math.min(
                                limit,
                                200
                        )
                );

        orderApi
                .getAssignments(
                        safeLimit
                )
                .enqueue(
                        new Callback<DriverAssignmentsResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<DriverAssignmentsResponse> call,
                                    @NonNull Response<DriverAssignmentsResponse> response
                            ) {

                                if (
                                        response.code()
                                                == 401
                                ) {

                                    if (
                                            isTemporaryAuthRecoveryFailure(
                                                    response
                                            )
                                    ) {

                                        callback.onError(
                                                0,
                                                "Не удалось обновить сессию. Проверьте подключение"
                                        );

                                    }
                                    else {

                                        callback
                                                .onAuthenticationRequired();
                                    }

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapOrdersHttpError(
                                                    response.code()
                                            )
                                    );

                                    return;
                                }

                                DriverAssignmentsResponse body =
                                        response.body();

                                OrdersResponse adapted =
                                        adaptAssignments(
                                                body,
                                                normalizedStatus,
                                                safeLimit
                                        );

                                if (adapted == null) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                callback.onSuccess(
                                        adapted
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<DriverAssignmentsResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (
                                        call.isCanceled()
                                ) {

                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось загрузить заявки. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    public void changeOrderStatus(
            String orderId,
            String targetStatus,
            long expectedRevision,
            TransitionCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String safeOrderId =
                orderId == null
                        ? ""
                        : orderId.trim();

        if (safeOrderId.isEmpty()) {

            callback.onError(
                    0,
                    "Некорректный ID заявки"
            );

            return;
        }

        String normalizedStatus =
                normalizeStatus(
                        targetStatus
                );

        if (
                normalizedStatus == null
                        ||
                        !"done".equals(
                                normalizedStatus
                        )
        ) {

            callback.onError(
                    0,
                    "Недопустимый переход статуса"
            );

            return;
        }

        if (expectedRevision < 1) {

            callback.onError(
                    0,
                    "Некорректная версия заявки"
            );

            return;
        }

        OrderStatusRequest request =
                new OrderStatusRequest(
                        normalizedStatus,
                        expectedRevision
                );

        String idempotencyKey =
                "android-done-"
                        +
                        UUID.randomUUID();

        orderApi
                .changeOrderStatus(
                        safeOrderId,
                        idempotencyKey,
                        request
                )
                .enqueue(
                        new Callback<OrderTransitionResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<OrderTransitionResponse> call,
                                    @NonNull Response<OrderTransitionResponse> response
                            ) {

                                int status =
                                        response.code();

                                if (status == 401) {

                                    if (
                                            isTemporaryAuthRecoveryFailure(
                                                    response
                                            )
                                    ) {

                                        callback.onError(
                                                0,
                                                "Не удалось обновить сессию. Проверьте подключение"
                                        );

                                    }
                                    else {

                                        callback
                                                .onAuthenticationRequired();
                                    }

                                    return;
                                }

                                if (status == 409) {

                                    callback
                                            .onConflict();

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            status,
                                            mapTransitionHttpError(
                                                    status
                                            )
                                    );

                                    return;
                                }

                                OrderTransitionResponse body =
                                        response.body();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                                ||
                                                body.getOrder()
                                                        == null
                                                ||
                                                isBlank(
                                                        body
                                                                .getOrder()
                                                                .getId()
                                                )
                                ) {

                                    callback.onError(
                                            status,
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                callback.onSuccess(
                                        body
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<OrderTransitionResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (
                                        call.isCanceled()
                                ) {

                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось изменить статус заявки. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    private boolean isTemporaryAuthRecoveryFailure(
            Response<?> response
    ) {

        if (response == null) {
            return false;
        }

        String value =
                response
                        .headers()
                        .get(
                                StaffAuthInterceptor
                                        .AUTH_RECOVERY_HEADER
                        );

        return StaffAuthInterceptor
                .AUTH_RECOVERY_TEMPORARY
                .equals(
                        value
                );
    }

    private String normalizeStatus(
            String status
    ) {

        if (
                status == null
                        ||
                        status
                                .trim()
                                .isEmpty()
        ) {

            return null;
        }

        String normalized =
                status
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                !isAllowedStatus(
                        normalized
                )
        ) {

            return null;
        }

        return normalized;
    }

    private boolean isAllowedStatus(
            String status
    ) {

        if (status == null) {
            return false;
        }

        switch (status) {

            case "new":
            case "taken":
            case "in_progress":
            case "done":
            case "canceled":
                return true;

            default:
                return false;
        }
    }

    private OrdersResponse adaptAssignments(
            DriverAssignmentsResponse response,
            String status,
            int limit
    ) {

        if (
                response == null
                        ||
                        !response.isOk()
                        ||
                        response.getAssignments()
                                == null
        ) {

            return null;
        }

        List<StaffOrderDto> orders =
                new ArrayList<>();

        for (
                DriverAssignmentDto assignment
                :
                response.getAssignments()
        ) {

            if (
                    assignment == null
                            ||
                            assignment.getOrder()
                                    == null
                            ||
                            isBlank(
                                    assignment
                                            .getAssignmentId()
                            )
                            ||
                            assignment.getAssignmentRevision()
                                    == null
                            ||
                            assignment.getAssignmentRevision()
                                    < 1
                            ||
                            isBlank(
                                    assignment
                                            .getVehicleId()
                            )
            ) {

                return null;
            }

            StaffOrderDto order =
                    assignment.getOrder();

            if (
                    isBlank(
                            order.getId()
                    )
                            ||
                            order.getRevision()
                                    == null
                            ||
                            order.getRevision()
                                    < 1
                            ||
                            isBlank(
                                    order.getStatus()
                            )
            ) {

                return null;
            }

            order.bindAssignmentContext(
                    assignment.getAssignmentId(),
                    assignment.getAssignmentRevision(),
                    assignment.getPhase(),
                    assignment.getPhaseUpdatedAt(),
                    assignment.getAssignedAt(),
                    assignment.getVehicleId()
            );

            if (
                    status == null
                            ||
                            status.equals(
                                    order
                                            .getStatus()
                                            .trim()
                            )
            ) {

                orders.add(
                        order
                );
            }
        }

        return OrdersResponse.assignedOnly(
                orders,
                limit,
                status
        );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value
                        .trim()
                        .isEmpty();
    }

    private String mapOrdersHttpError(
            int status
    ) {

        if (status == 400) {

            return "Некорректный запрос";
        }

        if (status == 403) {

            return "Нет доступа к заявкам";
        }

        if (status >= 500) {

            return "Сервер временно недоступен";
        }

        return "Не удалось загрузить заявки";
    }

    private String mapTransitionHttpError(
            int status
    ) {

        if (status == 400) {

            return "Некорректный запрос";
        }

        if (status == 403) {

            return "Этот переход статуса недоступен";
        }

        if (status == 404) {

            return "Заявка больше не существует";
        }

        if (status >= 500) {

            return "Сервер временно недоступен";
        }

        return "Не удалось изменить статус заявки";
    }
}
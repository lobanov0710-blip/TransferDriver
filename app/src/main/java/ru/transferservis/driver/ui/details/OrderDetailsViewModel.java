package ru.transferservis.driver.ui.details;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import ru.transferservis.driver.data.model.OrderTransitionResponse;
import ru.transferservis.driver.data.model.OrdersResponse;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.data.repository.OrderRepository;
import ru.transferservis.driver.ui.main.OrderActionResult;

public final class OrderDetailsViewModel
        extends ViewModel {

    private static final int ORDERS_LIMIT =
            200;

    private final OrderRepository repository;

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(
                    false
            );

    private final MutableLiveData<OrderDetailsState> state =
            new MutableLiveData<>();

    private final MutableLiveData<String> processingTargetStatus =
            new MutableLiveData<>();

    private final MutableLiveData<OrderActionResult> actionResult =
            new MutableLiveData<>();

    private String currentOrderId;

    public OrderDetailsViewModel() {

        repository =
                new OrderRepository();
    }

    public LiveData<Boolean> getLoading() {

        return loading;
    }

    public LiveData<OrderDetailsState> getState() {

        return state;
    }

    public LiveData<String> getProcessingTargetStatus() {

        return processingTargetStatus;
    }

    public LiveData<OrderActionResult> getActionResult() {

        return actionResult;
    }

    public void loadOrder(
            String orderId
    ) {

        String safeOrderId =
                clean(
                        orderId
                );

        if (safeOrderId.isEmpty()) {

            state.setValue(
                    OrderDetailsState.notFound()
            );

            return;
        }

        currentOrderId =
                safeOrderId;

        if (
                Boolean.TRUE.equals(
                        loading.getValue()
                )
        ) {

            return;
        }

        loading.setValue(
                true
        );

        repository.getOrders(
                null,
                ORDERS_LIMIT,

                new OrderRepository.OrdersCallback() {

                    @Override
                    public void onSuccess(
                            OrdersResponse response
                    ) {

                        loading.setValue(
                                false
                        );

                        StaffOrderDto order =
                                findOrder(
                                        response,
                                        safeOrderId
                                );

                        if (order == null) {

                            state.setValue(
                                    OrderDetailsState.notFound()
                            );

                            return;
                        }

                        state.setValue(
                                OrderDetailsState.success(
                                        order
                                )
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        loading.setValue(
                                false
                        );

                        state.setValue(
                                OrderDetailsState
                                        .authenticationRequired()
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        loading.setValue(
                                false
                        );

                        state.setValue(
                                OrderDetailsState.error(
                                        message
                                )
                        );
                    }
                }
        );
    }

    public void transitionOrder(
            String targetStatus
    ) {

        String safeOrderId =
                clean(
                        currentOrderId
                );

        String safeTargetStatus =
                clean(
                        targetStatus
                );

        if (safeOrderId.isEmpty()) {

            actionResult.setValue(
                    OrderActionResult.error(
                            null,
                            safeTargetStatus,
                            "Некорректная заявка"
                    )
            );

            return;
        }

        if (
                !"taken".equals(
                        safeTargetStatus
                )
                        &&
                        !"in_progress".equals(
                                safeTargetStatus
                        )
                        &&
                        !"done".equals(
                                safeTargetStatus
                        )
        ) {

            actionResult.setValue(
                    OrderActionResult.error(
                            safeOrderId,
                            safeTargetStatus,
                            "Недопустимый переход статуса"
                    )
            );

            return;
        }

        String processing =
                processingTargetStatus.getValue();

        if (
                processing != null
                        &&
                        !processing.trim().isEmpty()
        ) {

            return;
        }

        processingTargetStatus.setValue(
                safeTargetStatus
        );

        repository.changeOrderStatus(
                safeOrderId,
                safeTargetStatus,

                new OrderRepository.TransitionCallback() {

                    @Override
                    public void onSuccess(
                            OrderTransitionResponse response
                    ) {

                        processingTargetStatus.setValue(
                                null
                        );

                        actionResult.setValue(
                                OrderActionResult.success(
                                        safeOrderId,
                                        safeTargetStatus
                                )
                        );

                        loadOrder(
                                safeOrderId
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        processingTargetStatus.setValue(
                                null
                        );

                        actionResult.setValue(
                                OrderActionResult
                                        .authenticationRequired(
                                                safeOrderId,
                                                safeTargetStatus
                                        )
                        );
                    }

                    @Override
                    public void onConflict() {

                        processingTargetStatus.setValue(
                                null
                        );

                        actionResult.setValue(
                                OrderActionResult.conflict(
                                        safeOrderId,
                                        safeTargetStatus,
                                        "Состояние заказа уже изменилось"
                                )
                        );

                        loadOrder(
                                safeOrderId
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        processingTargetStatus.setValue(
                                null
                        );

                        actionResult.setValue(
                                OrderActionResult.error(
                                        safeOrderId,
                                        safeTargetStatus,
                                        message
                                )
                        );
                    }
                }
        );
    }

    public void consumeActionResult() {

        actionResult.setValue(
                null
        );
    }

    private StaffOrderDto findOrder(
            OrdersResponse response,
            String orderId
    ) {

        if (
                response == null
                        ||
                        response.getOrders() == null
        ) {

            return null;
        }

        List<StaffOrderDto> orders =
                response.getOrders();

        for (
                StaffOrderDto order
                :
                orders
        ) {

            if (
                    order == null
                            ||
                            order.getId() == null
            ) {

                continue;
            }

            if (
                    orderId.equals(
                            order
                                    .getId()
                                    .trim()
                    )
            ) {

                return order;
            }
        }

        return null;
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
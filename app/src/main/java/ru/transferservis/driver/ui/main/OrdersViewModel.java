package ru.transferservis.driver.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.data.model.OrdersResponse;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.data.repository.OrderRepository;

public final class OrdersViewModel
        extends ViewModel {

    private static final int ORDERS_LIMIT =
            200;

    private final OrderRepository repository;

    private final List<StaffOrderDto> allOrders =
            new ArrayList<>();

    private OrderSection selectedSection =
            OrderSection.NEW;

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(
                    false
            );

    private final MutableLiveData<OrdersResult> result =
            new MutableLiveData<>();

    private final MutableLiveData<OrderSection> section =
            new MutableLiveData<>(
                    OrderSection.NEW
            );

    private final MutableLiveData<OrderCounts> counts =
            new MutableLiveData<>(
                    OrderCounts.unavailable()
            );

    public OrdersViewModel() {

        repository =
                new OrderRepository();
    }

    public LiveData<Boolean> getLoading() {

        return loading;
    }

    public LiveData<OrdersResult> getResult() {

        return result;
    }

    public LiveData<OrderSection> getSection() {

        return section;
    }

    public LiveData<OrderCounts> getCounts() {

        return counts;
    }

    public void setSection(
            OrderSection newSection
    ) {

        if (newSection == null) {
            return;
        }

        if (
                selectedSection
                        == newSection
        ) {

            return;
        }

        selectedSection =
                newSection;

        section.setValue(
                newSection
        );

        publishCurrentSection();
    }

    public boolean loadOrders() {

        if (
                Boolean.TRUE.equals(
                        loading.getValue()
                )
        ) {

            return false;
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

                        List<StaffOrderDto> orders =
                                response.getOrders();

                        allOrders.clear();

                        if (orders != null) {

                            allOrders.addAll(
                                    orders
                            );
                        }

                        updateCounts(
                                response
                        );

                        publishCurrentSection();
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        loading.setValue(
                                false
                        );

                        result.setValue(
                                OrdersResult
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

                        result.setValue(
                                OrdersResult.error(
                                        message
                                )
                        );
                    }
                }
        );

        return true;
    }

    private void updateCounts(
            OrdersResponse response
    ) {

        if (response == null) {

            counts.setValue(
                    OrderCounts.unavailable()
            );

            return;
        }

        /*
         * GET /orders сообщает:
         *
         * total = всё доступное после
         * authorization/status filtering.
         *
         * count = реально возвращённое
         * количество после limit.
         *
         * Если total != count,
         * локально точные счётчики
         * получить нельзя.
         */
        boolean complete =
                response.getTotal()
                        == response.getCount()
                        &&
                        response.getCount()
                                == allOrders.size();

        if (!complete) {

            counts.setValue(
                    OrderCounts.unavailable()
            );

            return;
        }

        int newCount =
                0;

        int activeCount =
                0;

        int historyCount =
                0;

        for (
                StaffOrderDto order
                :
                allOrders
        ) {

            if (
                    order == null
                            ||
                            order.getStatus()
                                    == null
            ) {

                continue;
            }

            String status =
                    order
                            .getStatus()
                            .trim();

            switch (status) {

                case "new":

                    newCount++;

                    break;

                case "taken":
                case "in_progress":

                    activeCount++;

                    break;

                case "done":
                case "canceled":

                    historyCount++;

                    break;

                default:
                    break;
            }
        }

        counts.setValue(
                OrderCounts.exact(
                        newCount,
                        activeCount,
                        historyCount
                )
        );
    }

    private void publishCurrentSection() {

        List<StaffOrderDto> filtered =
                new ArrayList<>();

        for (
                StaffOrderDto order
                :
                allOrders
        ) {

            if (
                    order == null
                            ||
                            order.getStatus()
                                    == null
            ) {

                continue;
            }

            String status =
                    order
                            .getStatus()
                            .trim();

            if (
                    belongsToSection(
                            status,
                            selectedSection
                    )
            ) {

                filtered.add(
                        order
                );
            }
        }

        result.setValue(
                OrdersResult.success(
                        Collections.unmodifiableList(
                                filtered
                        )
                )
        );
    }

    private boolean belongsToSection(
            String status,
            OrderSection section
    ) {

        if (
                status == null
                        ||
                        section == null
        ) {

            return false;
        }

        switch (section) {

            case NEW:

                return "new".equals(
                        status
                );

            case ACTIVE:

                return "taken".equals(
                        status
                )
                        ||
                        "in_progress".equals(
                                status
                        );

            case HISTORY:

                return "done".equals(
                        status
                )
                        ||
                        "canceled".equals(
                                status
                        );

            default:

                return false;
        }
    }
}
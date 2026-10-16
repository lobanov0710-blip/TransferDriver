package ru.transferservis.driver.ui.main;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.ui.details.OrderDetailsActivity;

public final class OrderAdapter
        extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private static final Locale RU_LOCALE =
            new Locale(
                    "ru",
                    "RU"
            );

    private final List<StaffOrderDto> items =
            new ArrayList<>();

    public void submitList(
            List<StaffOrderDto> orders
    ) {

        items.clear();

        if (orders != null) {

            items.addAll(
                    orders
            );
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_order,
                                parent,
                                false
                        );

        return new OrderViewHolder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull OrderViewHolder holder,
            int position
    ) {

        holder.bind(
                items.get(
                        position
                )
        );
    }

    @Override
    public int getItemCount() {

        return items.size();
    }

    final class OrderViewHolder
            extends RecyclerView.ViewHolder {

        private final MaterialCardView cardOrder;

        private final TextView textOrderActiveBadge;
        private final TextView textOrderRoute;
        private final TextView textOrderStatus;
        private final TextView textOrderDate;
        private final TextView textOrderTariff;
        private final TextView textOrderPrice;

        OrderViewHolder(
                @NonNull View itemView
        ) {

            super(
                    itemView
            );

            cardOrder =
                    (MaterialCardView)
                            itemView;

            textOrderActiveBadge =
                    itemView.findViewById(
                            R.id.textOrderActiveBadge
                    );

            textOrderRoute =
                    itemView.findViewById(
                            R.id.textOrderRoute
                    );

            textOrderStatus =
                    itemView.findViewById(
                            R.id.textOrderStatus
                    );

            textOrderDate =
                    itemView.findViewById(
                            R.id.textOrderDate
                    );

            textOrderTariff =
                    itemView.findViewById(
                            R.id.textOrderTariff
                    );

            textOrderPrice =
                    itemView.findViewById(
                            R.id.textOrderPrice
                    );
        }

        void bind(
                StaffOrderDto order
        ) {

            String status =
                    clean(
                            order.getStatus()
                    );

            bindActiveHighlight(
                    status
            );

            textOrderRoute.setText(
                    buildRoute(
                            order
                    )
            );

            textOrderStatus.setText(
                    itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_format,
                                    statusLabel(
                                            status
                                    )
                            )
            );

            bindDate(
                    order.getDate()
            );

            bindTariff(
                    order.getTariff()
            );

            bindPrice(
                    order.getPrice()
            );

            itemView.setOnClickListener(
                    view -> {

                        Intent intent =
                                OrderDetailsActivity
                                        .createIntent(
                                                view.getContext(),
                                                order
                                        );

                        view.getContext()
                                .startActivity(
                                        intent
                                );
                    }
            );
        }

        private void bindActiveHighlight(
                String status
        ) {

            boolean activeTrip =
                    "in_progress".equals(
                            status
                    );

            textOrderActiveBadge.setVisibility(
                    activeTrip
                            ? View.VISIBLE
                            : View.GONE
            );

            cardOrder.setStrokeWidth(
                    dpToPx(
                            activeTrip
                                    ? 3
                                    : 1
                    )
            );

            cardOrder.setCardElevation(
                    dpToPx(
                            activeTrip
                                    ? 6
                                    : 2
                    )
            );
        }

        private int dpToPx(
                int dp
        ) {

            float density =
                    itemView
                            .getResources()
                            .getDisplayMetrics()
                            .density;

            return Math.round(
                    dp
                            * density
            );
        }

        private String buildRoute(
                StaffOrderDto order
        ) {

            String route =
                    clean(
                            order.getRoute()
                    );

            if (!route.isEmpty()) {

                return route;
            }

            String from =
                    clean(
                            order.getFrom()
                    );

            String to =
                    clean(
                            order.getTo()
                    );

            if (
                    !from.isEmpty()
                            &&
                            !to.isEmpty()
            ) {

                return from
                        + " → "
                        + to;
            }

            if (!from.isEmpty()) {

                return from;
            }

            if (!to.isEmpty()) {

                return to;
            }

            return itemView
                    .getContext()
                    .getString(
                            R.string.order_route_unknown
                    );
        }

        private void bindDate(
                String value
        ) {

            String date =
                    clean(
                            value
                    );

            if (date.isEmpty()) {

                textOrderDate.setText(
                        ""
                );

                textOrderDate.setVisibility(
                        View.GONE
                );

                return;
            }

            String formatted =
                    date;

            try {

                LocalDate parsed =
                        LocalDate.parse(
                                date,
                                DateTimeFormatter.ISO_LOCAL_DATE
                        );

                formatted =
                        parsed.format(
                                DateTimeFormatter.ofPattern(
                                        "dd.MM.yyyy",
                                        RU_LOCALE
                                )
                        );

            }
            catch (
                    DateTimeParseException ignored
            ) {
            }

            textOrderDate.setText(
                    itemView
                            .getContext()
                            .getString(
                                    R.string.order_date_format,
                                    formatted
                            )
            );

            textOrderDate.setVisibility(
                    View.VISIBLE
            );
        }

        private void bindTariff(
                String value
        ) {

            String tariff =
                    clean(
                            value
                    );

            if (tariff.isEmpty()) {

                textOrderTariff.setText(
                        ""
                );

                textOrderTariff.setVisibility(
                        View.GONE
                );

                return;
            }

            textOrderTariff.setText(
                    itemView
                            .getContext()
                            .getString(
                                    R.string.order_tariff_format,
                                    tariffLabel(
                                            tariff
                                    )
                            )
            );

            textOrderTariff.setVisibility(
                    View.VISIBLE
            );
        }

        private void bindPrice(
                Long price
        ) {

            if (price == null) {

                textOrderPrice.setText(
                        ""
                );

                textOrderPrice.setVisibility(
                        View.GONE
                );

                return;
            }

            NumberFormat formatter =
                    NumberFormat
                            .getIntegerInstance(
                                    RU_LOCALE
                            );

            textOrderPrice.setText(
                    itemView
                            .getContext()
                            .getString(
                                    R.string.order_price_format,
                                    formatter.format(
                                            price
                                    )
                            )
            );

            textOrderPrice.setVisibility(
                    View.VISIBLE
            );
        }

        private String statusLabel(
                String status
        ) {

            switch (status) {

                case "new":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_new
                            );

                case "taken":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_taken
                            );

                case "in_progress":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_in_progress
                            );

                case "done":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_done
                            );

                case "canceled":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.order_status_canceled
                            );

                default:

                    return status.isEmpty()
                            ? "—"
                            : status;
            }
        }

        private String tariffLabel(
                String tariff
        ) {

            switch (
                    tariff.toLowerCase(
                            Locale.ROOT
                    )
            ) {

                case "comfort":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.tariff_comfort
                            );

                case "business":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.tariff_business
                            );

                case "minivan":

                    return itemView
                            .getContext()
                            .getString(
                                    R.string.tariff_minivan
                            );

                default:

                    return tariff;
            }
        }

        private String clean(
                String value
        ) {

            return value == null
                    ? ""
                    : value.trim();
        }
    }
}
package ru.transferservis.driver.ui.support;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.SupportCaseDto;

public final class SupportCaseAdapter
        extends RecyclerView.Adapter<SupportCaseAdapter.Holder> {

    public interface Listener {
        void onOpen(
                SupportCaseDto supportCase
        );
    }

    private final List<SupportCaseDto> items =
            new ArrayList<>();

    private final Listener listener;

    public SupportCaseAdapter(
            Listener listener
    ) {
        this.listener =
                listener;
    }

    public void submit(
            List<SupportCaseDto> cases
    ) {

        items.clear();

        if (cases != null) {
            items.addAll(
                    cases
            );
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(
                                parent.getContext()
                        )
                        .inflate(
                                R.layout.item_support_case,
                                parent,
                                false
                        );

        return new Holder(
                view
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {

        SupportCaseDto supportCase =
                items.get(
                        position
                );

        holder.category.setText(
                categoryLabel(
                        supportCase.getCategory()
                )
        );

        holder.status.setText(
                statusLabel(
                        supportCase.getStatus()
                )
        );

        Long updatedAt =
                supportCase.getUpdatedAt();

        holder.updated.setText(
                updatedAt == null
                        ||
                        updatedAt <= 0
                        ? ""
                        : holder.itemView
                                .getContext()
                                .getString(
                                        R.string.support_updated_format,
                                        DateFormat
                                                .getMediumDateFormat(
                                                        holder.itemView
                                                                .getContext()
                                                )
                                                .format(
                                                        new Date(
                                                                updatedAt
                                                        )
                                                ),
                                        DateFormat
                                                .getTimeFormat(
                                                        holder.itemView
                                                                .getContext()
                                                )
                                                .format(
                                                        new Date(
                                                                updatedAt
                                                        )
                                                )
                                )
        );

        holder.itemView.setOnClickListener(
                view -> {

                    if (listener != null) {

                        listener.onOpen(
                                supportCase
                        );
                    }
                }
        );
    }

    @Override
    public int getItemCount() {

        return items.size();
    }

    static String categoryLabel(
            String value
    ) {

        String category =
                value == null
                        ? ""
                        : value.trim();

        switch (category) {

            case "passenger_no_show":
                return "Пассажир не вышел";

            case "delay":
                return "Опоздание";

            case "vehicle":
                return "Автомобиль";

            case "route":
                return "Маршрут";

            case "unable_to_complete":
                return "Не могу завершить заказ";

            case "reassignment":
                return "Переназначение";

            case "conflict":
                return "Конфликтная ситуация";

            case "other":
            default:
                return "Другое";
        }
    }

    static String statusLabel(
            String value
    ) {

        String status =
                value == null
                        ? ""
                        : value.trim();

        switch (status) {

            case "open":
                return "Открыто";

            case "in_progress":
                return "В работе";

            case "resolved":
                return "Решено";

            case "closed":
                return "Закрыто";

            default:
                return "Статус неизвестен";
        }
    }

    static final class Holder
            extends RecyclerView.ViewHolder {

        final TextView category;
        final TextView status;
        final TextView updated;

        Holder(
                @NonNull View itemView
        ) {
            super(
                    itemView
            );

            category =
                    itemView.findViewById(
                            R.id.textSupportCaseCategory
                    );

            status =
                    itemView.findViewById(
                            R.id.textSupportCaseStatus
                    );

            updated =
                    itemView.findViewById(
                            R.id.textSupportCaseUpdated
                    );
        }
    }
}

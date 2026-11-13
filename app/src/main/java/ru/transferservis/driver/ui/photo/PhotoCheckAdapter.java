package ru.transferservis.driver.ui.photo;

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
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;

public final class PhotoCheckAdapter
        extends RecyclerView.Adapter<PhotoCheckAdapter.Holder> {

    public interface Listener {
        void onOpen(
                DriverPhotoCheckDto check
        );
    }

    private final List<DriverPhotoCheckDto> items =
            new ArrayList<>();

    private final Listener listener;

    public PhotoCheckAdapter(
            Listener listener
    ) {
        this.listener =
                listener;
    }

    public void submit(
            List<DriverPhotoCheckDto> checks
    ) {

        items.clear();

        if (checks != null) {
            items.addAll(
                    checks
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
                                R.layout.item_photo_check,
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

        DriverPhotoCheckDto check =
                items.get(
                        position
                );

        holder.kind.setText(
                kindLabel(
                        check.getKind()
                )
        );

        holder.reason.setText(
                safe(
                        check.getReason(),
                        holder.itemView
                                .getContext()
                                .getString(
                                        R.string.photo_control_reason_default
                                )
                )
        );

        int uploaded =
                check.getUploadedKindCount()
                        == null
                        ? 0
                        : Math.max(
                                0,
                                check.getUploadedKindCount()
                        );

        int required =
                check.getRequiredKindCount()
                        == null
                        ? 7
                        : Math.max(
                                1,
                                check.getRequiredKindCount()
                        );

        holder.progress.setText(
                holder.itemView
                        .getContext()
                        .getString(
                                R.string.photo_control_progress_format,
                                uploaded,
                                required
                        )
        );

        Long deadline =
                check.getGraceUntil();

        if (
                deadline == null
                        ||
                        deadline <= 0
        ) {

            deadline =
                    check.getDueAt();
        }

        holder.deadline.setText(
                deadline == null
                        ||
                        deadline <= 0
                        ? holder.itemView
                                .getContext()
                                .getString(
                                        R.string.photo_control_deadline_unknown
                                )
                        : holder.itemView
                                .getContext()
                                .getString(
                                        R.string.photo_control_deadline_format,
                                        DateFormat
                                                .getMediumDateFormat(
                                                        holder.itemView
                                                                .getContext()
                                                )
                                                .format(
                                                        new Date(
                                                                deadline
                                                        )
                                                ),
                                        DateFormat
                                                .getTimeFormat(
                                                        holder.itemView
                                                                .getContext()
                                                )
                                                .format(
                                                        new Date(
                                                                deadline
                                                        )
                                                )
                                )
        );

        holder.itemView.setOnClickListener(
                view -> {

                    if (listener != null) {

                        listener.onOpen(
                                check
                        );
                    }
                }
        );
    }

    @Override
    public int getItemCount() {

        return items.size();
    }

    private String kindLabel(
            String value
    ) {

        String kind =
                value == null
                        ? ""
                        : value.trim();

        switch (kind) {

            case "monthly":
                return "Плановый фотоконтроль";

            case "complaint":
                return "Фотоконтроль по обращению";

            case "manual":
                return "Внеплановый фотоконтроль";

            default:
                return "Фотоконтроль";
        }
    }

    private String safe(
            String value,
            String fallback
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return fallback;
        }

        return value.trim();
    }

    static final class Holder
            extends RecyclerView.ViewHolder {

        final TextView kind;
        final TextView reason;
        final TextView progress;
        final TextView deadline;

        Holder(
                @NonNull View itemView
        ) {
            super(
                    itemView
            );

            kind =
                    itemView.findViewById(
                            R.id.textPhotoCheckKind
                    );

            reason =
                    itemView.findViewById(
                            R.id.textPhotoCheckReason
                    );

            progress =
                    itemView.findViewById(
                            R.id.textPhotoCheckProgress
                    );

            deadline =
                    itemView.findViewById(
                            R.id.textPhotoCheckDeadline
                    );
        }
    }
}

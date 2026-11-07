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
import ru.transferservis.driver.data.model.SupportMessageDto;

public final class SupportMessageAdapter
        extends RecyclerView.Adapter<SupportMessageAdapter.Holder> {

    private final List<SupportMessageDto> items =
            new ArrayList<>();

    public void submit(
            List<SupportMessageDto> messages
    ) {

        items.clear();

        if (messages != null) {
            items.addAll(
                    messages
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
                                R.layout.item_support_message,
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

        SupportMessageDto message =
                items.get(
                        position
                );

        String senderKind =
                message.getSenderKind() == null
                        ? ""
                        : message.getSenderKind()
                                .trim();

        holder.sender.setText(
                "driver".equals(
                        senderKind
                )
                        ? R.string.support_sender_driver
                        : R.string.support_sender_admin
        );

        holder.body.setText(
                message.getBody() == null
                        ? ""
                        : message.getBody()
        );

        Long createdAt =
                message.getCreatedAt();

        holder.time.setText(
                createdAt == null
                        ||
                        createdAt <= 0
                        ? ""
                        : DateFormat
                                .getTimeFormat(
                                        holder.itemView
                                                .getContext()
                                )
                                .format(
                                        new Date(
                                                createdAt
                                        )
                                )
        );
    }

    @Override
    public int getItemCount() {

        return items.size();
    }

    static final class Holder
            extends RecyclerView.ViewHolder {

        final TextView sender;
        final TextView body;
        final TextView time;

        Holder(
                @NonNull View itemView
        ) {
            super(
                    itemView
            );

            sender =
                    itemView.findViewById(
                            R.id.textSupportMessageSender
                    );

            body =
                    itemView.findViewById(
                            R.id.textSupportMessageBody
                    );

            time =
                    itemView.findViewById(
                            R.id.textSupportMessageTime
                    );
        }
    }
}

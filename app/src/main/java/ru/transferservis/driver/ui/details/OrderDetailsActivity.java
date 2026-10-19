package ru.transferservis.driver.ui.details;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.ui.login.LoginActivity;
import ru.transferservis.driver.ui.main.OrderActionResult;

public final class OrderDetailsActivity
        extends AppCompatActivity {

    private static final String EXTRA_ORDER_ID =
            "order_id";

    private static final Locale RU_LOCALE =
            new Locale(
                    "ru",
                    "RU"
            );

    private ProgressBar progressDetails;

    private TextView textDetailsMessage;

    private Button buttonDetailsRetry;
    private Button buttonDetailsBack;

    private LinearLayout layoutDetailsContent;

    private TextView textDetailsRoute;
    private TextView textDetailsStatus;
    private TextView textDetailsId;
    private TextView textDetailsFrom;
    private TextView textDetailsTo;
    private TextView textDetailsDate;
    private TextView textDetailsTariff;
    private TextView textDetailsPrice;
    private TextView textDetailsDistance;
    private TextView textDetailsDuration;

    private LinearLayout layoutPassengerDetails;

    private TextView textDetailsCustomer;
    private TextView textDetailsPhone;
    private TextView textDetailsComment;

    private LinearLayout layoutDetailsTools;

    private Button buttonDetailsCall;
    private Button buttonDetailsRoute;
    private Button buttonDetailsAction;

    private OrderDetailsViewModel viewModel;

    private StaffOrderDto currentOrder;

    private String currentTargetStatus;

    private String orderId;

    private boolean firstResume =
            true;

    public static Intent createIntent(
            Context context,
            StaffOrderDto order
    ) {

        String id =
                order == null
                        ? ""
                        : order.getId();

        return createIntent(
                context,
                id
        );
    }

    public static Intent createIntent(
            Context context,
            String orderId
    ) {

        Intent intent =
                new Intent(
                        context,
                        OrderDetailsActivity.class
                );

        String normalizedId =
                orderId == null
                        ? ""
                        : orderId.trim();

        if (!normalizedId.isEmpty()) {

            /*
             * Передаём только идентификатор.
             *
             * PII пассажира через Intent
             * больше не передаётся.
             */
            intent.putExtra(
                    EXTRA_ORDER_ID,
                    normalizedId
            );
        }

        return intent;
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_order_details
        );

        viewModel =
                new ViewModelProvider(
                        this
                )
                        .get(
                                OrderDetailsViewModel.class
                        );

        orderId =
                clean(
                        getIntent()
                                .getStringExtra(
                                        EXTRA_ORDER_ID
                                )
                );

        initViews();

        observeViewModel();

        buttonDetailsBack.setOnClickListener(
                view ->
                        finish()
        );

        buttonDetailsRetry.setOnClickListener(
                view ->
                        loadOrder()
        );

        if (orderId.isEmpty()) {

            showUnavailable();

            return;
        }

        loadOrder();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (firstResume) {

            firstResume =
                    false;

            return;
        }

        /*
         * Возврат из звонилки,
         * Яндекс Карт или из background.
         *
         * Получаем актуальную версию
         * заявки с backend.
         */
        if (!orderId.isEmpty()) {

            loadOrder();
        }
    }

    private void initViews() {

        progressDetails =
                findViewById(
                        R.id.progressDetails
                );

        textDetailsMessage =
                findViewById(
                        R.id.textDetailsMessage
                );

        buttonDetailsRetry =
                findViewById(
                        R.id.buttonDetailsRetry
                );

        buttonDetailsBack =
                findViewById(
                        R.id.buttonDetailsBack
                );

        layoutDetailsContent =
                findViewById(
                        R.id.layoutDetailsContent
                );

        textDetailsRoute =
                findViewById(
                        R.id.textDetailsRoute
                );

        textDetailsStatus =
                findViewById(
                        R.id.textDetailsStatus
                );

        textDetailsId =
                findViewById(
                        R.id.textDetailsId
                );

        textDetailsFrom =
                findViewById(
                        R.id.textDetailsFrom
                );

        textDetailsTo =
                findViewById(
                        R.id.textDetailsTo
                );

        textDetailsDate =
                findViewById(
                        R.id.textDetailsDate
                );

        textDetailsTariff =
                findViewById(
                        R.id.textDetailsTariff
                );

        textDetailsPrice =
                findViewById(
                        R.id.textDetailsPrice
                );

        textDetailsDistance =
                findViewById(
                        R.id.textDetailsDistance
                );

        textDetailsDuration =
                findViewById(
                        R.id.textDetailsDuration
                );

        layoutPassengerDetails =
                findViewById(
                        R.id.layoutPassengerDetails
                );

        textDetailsCustomer =
                findViewById(
                        R.id.textDetailsCustomer
                );

        textDetailsPhone =
                findViewById(
                        R.id.textDetailsPhone
                );

        textDetailsComment =
                findViewById(
                        R.id.textDetailsComment
                );

        layoutDetailsTools =
                findViewById(
                        R.id.layoutDetailsTools
                );

        buttonDetailsCall =
                findViewById(
                        R.id.buttonDetailsCall
                );

        buttonDetailsRoute =
                findViewById(
                        R.id.buttonDetailsRoute
                );

        buttonDetailsAction =
                findViewById(
                        R.id.buttonDetailsAction
                );
    }

    private void observeViewModel() {

        viewModel
                .getLoading()
                .observe(
                        this,
                        loading -> {

                            if (
                                    Boolean.TRUE.equals(
                                            loading
                                    )
                            ) {

                                showLoading();
                            }
                            else {

                                progressDetails.setVisibility(
                                        View.GONE
                                );
                            }
                        }
                );

        viewModel
                .getState()
                .observe(
                        this,
                        state -> {

                            if (state == null) {
                                return;
                            }

                            switch (
                                    state.getType()
                            ) {

                                case SUCCESS:

                                    bindOrder(
                                            state.getOrder()
                                    );

                                    break;

                                case ERROR:

                                    showError(
                                            state.getMessage()
                                    );

                                    break;

                                case NOT_FOUND:

                                    showUnavailable();

                                    break;

                                case AUTHENTICATION_REQUIRED:

                                    clearLocalSession();

                                    openLogin();

                                    break;
                            }
                        }
                );

        viewModel
                .getProcessingTargetStatus()
                .observe(
                        this,
                        targetStatus ->
                                updateProcessingState(
                                        targetStatus
                                )
                );

        viewModel
                .getActionResult()
                .observe(
                        this,
                        result -> {

                            if (result == null) {
                                return;
                            }

                            handleActionResult(
                                    result
                            );

                            viewModel
                                    .consumeActionResult();
                        }
                );
    }

    private void loadOrder() {

        viewModel.loadOrder(
                orderId
        );
    }

    private void bindOrder(
            StaffOrderDto order
    ) {

        if (order == null) {

            showUnavailable();

            return;
        }

        currentOrder =
                order;

        textDetailsMessage.setVisibility(
                View.GONE
        );

        buttonDetailsRetry.setVisibility(
                View.GONE
        );

        progressDetails.setVisibility(
                View.GONE
        );

        layoutDetailsContent.setVisibility(
                View.VISIBLE
        );

        String route =
                buildRoute(
                        order
                );

        textDetailsRoute.setText(
                route
        );

        textDetailsStatus.setText(
                getString(
                        R.string.order_status_format,
                        statusLabel(
                                order.getStatus()
                        )
                )
        );

        bindOptional(
                textDetailsId,
                order.getId(),
                R.string.order_id_format
        );

        bindOptional(
                textDetailsFrom,
                order.getFrom(),
                R.string.order_from_format
        );

        bindOptional(
                textDetailsTo,
                order.getTo(),
                R.string.order_to_format
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

        bindDistance(
                order.getDistance()
        );

        bindDuration(
                order.getDuration()
        );

        bindPassenger(
                order
        );

        bindActions(
                order
        );
    }

    private void bindPassenger(
            StaffOrderDto order
    ) {

        String name =
                clean(
                        order.getName()
                );

        String phone =
                clean(
                        order.getPhone()
                );

        String comment =
                clean(
                        order.getComment()
                );

        if (
                name.isEmpty()
                        &&
                        phone.isEmpty()
                        &&
                        comment.isEmpty()
        ) {

            layoutPassengerDetails.setVisibility(
                    View.GONE
            );

            return;
        }

        layoutPassengerDetails.setVisibility(
                View.VISIBLE
        );

        bindOptional(
                textDetailsCustomer,
                name,
                R.string.order_customer_format
        );

        bindOptional(
                textDetailsPhone,
                phone,
                R.string.order_phone_format
        );

        bindOptional(
                textDetailsComment,
                comment,
                R.string.order_comment_format
        );
    }

    private void bindActions(
            StaffOrderDto order
    ) {

        String status =
                clean(
                        order.getStatus()
                );

        boolean active =
                "taken".equals(
                        status
                )
                        ||
                        "in_progress".equals(
                                status
                        );

        String phone =
                clean(
                        order.getPhone()
                );

        String from =
                clean(
                        order.getFrom()
                );

        String to =
                clean(
                        order.getTo()
                );

        boolean canCall =
                active
                        &&
                        !phone.isEmpty();

        boolean canOpenRoute =
                active
                        &&
                        !from.isEmpty()
                        &&
                        !to.isEmpty();

        if (
                canCall
                        ||
                        canOpenRoute
        ) {

            layoutDetailsTools.setVisibility(
                    View.VISIBLE
            );

        }
        else {

            layoutDetailsTools.setVisibility(
                    View.GONE
            );
        }

        if (canCall) {

            buttonDetailsCall.setVisibility(
                    View.VISIBLE
            );

            buttonDetailsCall.setOnClickListener(
                    view ->
                            callPassenger()
            );

        }
        else {

            buttonDetailsCall.setVisibility(
                    View.GONE
            );

            buttonDetailsCall.setOnClickListener(
                    null
            );
        }

        if (canOpenRoute) {

            buttonDetailsRoute.setVisibility(
                    View.VISIBLE
            );

            buttonDetailsRoute.setOnClickListener(
                    view ->
                            openRoute()
            );

        }
        else {

            buttonDetailsRoute.setVisibility(
                    View.GONE
            );

            buttonDetailsRoute.setOnClickListener(
                    null
            );
        }

        switch (status) {

            case "new":

                currentTargetStatus =
                        "taken";

                buttonDetailsAction.setText(
                        R.string.order_accept
                );

                buttonDetailsAction.setVisibility(
                        View.VISIBLE
                );

                break;

            case "taken":

                currentTargetStatus =
                        "in_progress";

                buttonDetailsAction.setText(
                        R.string.order_start
                );

                buttonDetailsAction.setVisibility(
                        View.VISIBLE
                );

                break;

            case "in_progress":

                currentTargetStatus =
                        "done";

                buttonDetailsAction.setText(
                        R.string.order_finish
                );

                buttonDetailsAction.setVisibility(
                        View.VISIBLE
                );

                break;

            default:

                currentTargetStatus =
                        null;

                buttonDetailsAction.setVisibility(
                        View.GONE
                );

                buttonDetailsAction.setOnClickListener(
                        null
                );

                return;
        }

        buttonDetailsAction.setOnClickListener(
                view ->
                        confirmOrderAction()
        );

        updateProcessingState(
                viewModel
                        .getProcessingTargetStatus()
                        .getValue()
        );
    }

    private void updateProcessingState(
            String processingTarget
    ) {

        if (currentOrder == null) {
            return;
        }

        boolean processing =
                processingTarget != null
                        &&
                        !processingTarget
                                .trim()
                                .isEmpty();

        buttonDetailsCall.setEnabled(
                !processing
        );

        buttonDetailsRoute.setEnabled(
                !processing
        );

        buttonDetailsAction.setEnabled(
                !processing
        );

        if (!processing) {

            String status =
                    clean(
                            currentOrder
                                    .getStatus()
                    );

            switch (status) {

                case "new":

                    buttonDetailsAction.setText(
                            R.string.order_accept
                    );

                    break;

                case "taken":

                    buttonDetailsAction.setText(
                            R.string.order_start
                    );

                    break;

                case "in_progress":

                    buttonDetailsAction.setText(
                            R.string.order_finish
                    );

                    break;

                default:
                    break;
            }

            return;
        }

        switch (
                processingTarget
        ) {

            case "taken":

                buttonDetailsAction.setText(
                        R.string.order_accepting
                );

                break;

            case "in_progress":

                buttonDetailsAction.setText(
                        R.string.order_starting
                );

                break;

            case "done":

                buttonDetailsAction.setText(
                        R.string.order_finishing
                );

                break;

            default:
                break;
        }
    }

    private void confirmOrderAction() {

        if (
                currentOrder == null
                        ||
                        currentTargetStatus == null
        ) {

            return;
        }

        int titleResource;
        int messageResource;
        int positiveResource;

        switch (
                currentTargetStatus
        ) {

            case "taken":

                titleResource =
                        R.string.order_accept_confirm_title;

                messageResource =
                        R.string.order_accept_confirm_message;

                positiveResource =
                        R.string.order_accept;

                break;

            case "in_progress":

                titleResource =
                        R.string.order_start_confirm_title;

                messageResource =
                        R.string.order_start_confirm_message;

                positiveResource =
                        R.string.order_start;

                break;

            case "done":

                titleResource =
                        R.string.order_finish_confirm_title;

                messageResource =
                        R.string.order_finish_confirm_message;

                positiveResource =
                        R.string.order_finish;

                break;

            default:
                return;
        }

        new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        titleResource
                )
                .setMessage(
                        getString(
                                messageResource,
                                buildRoute(
                                        currentOrder
                                )
                        )
                )
                .setNegativeButton(
                        R.string.cancel_button,
                        null
                )
                .setPositiveButton(
                        positiveResource,
                        (
                                dialog,
                                which
                        ) ->
                                viewModel
                                        .transitionOrder(
                                                currentTargetStatus
                                        )
                )
                .show();
    }

    private void handleActionResult(
            OrderActionResult result
    ) {

        switch (
                result.getType()
        ) {

            case SUCCESS:

                Toast.makeText(
                        this,
                        successMessage(
                                result.getTargetStatus()
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                break;

            case CONFLICT:

                Toast.makeText(
                        this,
                        safeMessage(
                                result.getMessage(),
                                getString(
                                        R.string.order_conflict
                                )
                        ),
                        Toast.LENGTH_LONG
                ).show();

                break;

            case ERROR:

                Toast.makeText(
                        this,
                        safeMessage(
                                result.getMessage(),
                                getString(
                                        R.string.order_action_failed
                                )
                        ),
                        Toast.LENGTH_LONG
                ).show();

                break;

            case AUTHENTICATION_REQUIRED:

                clearLocalSession();

                openLogin();

                break;
        }
    }

    private void callPassenger() {

        if (
                currentOrder == null
                        ||
                        !isActiveOrder(
                                currentOrder
                        )
        ) {

            return;
        }

        String phone =
                clean(
                        currentOrder
                                .getPhone()
                );

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    R.string.order_phone_unavailable,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        Intent.ACTION_DIAL,
                        Uri.fromParts(
                                "tel",
                                phone,
                                null
                        )
                );

        try {

            startActivity(
                    intent
            );

        }
        catch (
                ActivityNotFoundException exception
        ) {

            Toast.makeText(
                    this,
                    R.string.dialer_unavailable,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openRoute() {

        if (
                currentOrder == null
                        ||
                        !isActiveOrder(
                                currentOrder
                        )
        ) {

            return;
        }

        String from =
                clean(
                        currentOrder
                                .getFrom()
                );

        String to =
                clean(
                        currentOrder
                                .getTo()
                );

        if (
                from.isEmpty()
                        ||
                        to.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    R.string.order_route_unavailable,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String url =
                "https://yandex.ru/maps/"
                        +
                        "?mode=routes"
                        +
                        "&rtext="
                        +
                        Uri.encode(
                                from
                        )
                        +
                        "~"
                        +
                        Uri.encode(
                                to
                        )
                        +
                        "&rtt=auto";

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                url
                        )
                );

        try {

            startActivity(
                    intent
            );

        }
        catch (
                ActivityNotFoundException exception
        ) {

            Toast.makeText(
                    this,
                    R.string.maps_unavailable,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean isActiveOrder(
            StaffOrderDto order
    ) {

        String status =
                order == null
                        ? ""
                        : clean(
                        order.getStatus()
                );

        return "taken".equals(
                status
        )
                ||
                "in_progress".equals(
                        status
                );
    }

    private void showLoading() {

        progressDetails.setVisibility(
                View.VISIBLE
        );

        textDetailsMessage.setVisibility(
                View.GONE
        );

        buttonDetailsRetry.setVisibility(
                View.GONE
        );

        /*
         * Если данные уже были показаны,
         * при тихом refresh экран не прячем.
         */
        if (currentOrder == null) {

            layoutDetailsContent.setVisibility(
                    View.GONE
            );
        }
    }

    private void showError(
            String message
    ) {

        progressDetails.setVisibility(
                View.GONE
        );

        layoutDetailsContent.setVisibility(
                View.GONE
        );

        currentOrder =
                null;

        textDetailsMessage.setText(
                safeMessage(
                        message,
                        getString(
                                R.string.order_details_load_failed
                        )
                )
        );

        textDetailsMessage.setVisibility(
                View.VISIBLE
        );

        buttonDetailsRetry.setVisibility(
                View.VISIBLE
        );
    }

    private void showUnavailable() {

        progressDetails.setVisibility(
                View.GONE
        );

        layoutDetailsContent.setVisibility(
                View.GONE
        );

        buttonDetailsRetry.setVisibility(
                View.GONE
        );

        currentOrder =
                null;

        textDetailsMessage.setText(
                R.string.order_details_unavailable
        );

        textDetailsMessage.setVisibility(
                View.VISIBLE
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

            textDetailsDate.setVisibility(
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

        textDetailsDate.setText(
                getString(
                        R.string.order_date_format,
                        formatted
                )
        );

        textDetailsDate.setVisibility(
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

            textDetailsTariff.setVisibility(
                    View.GONE
            );

            return;
        }

        textDetailsTariff.setText(
                getString(
                        R.string.order_tariff_format,
                        tariffLabel(
                                tariff
                        )
                )
        );

        textDetailsTariff.setVisibility(
                View.VISIBLE
        );
    }

    private void bindPrice(
            Long price
    ) {

        if (price == null) {

            textDetailsPrice.setVisibility(
                    View.GONE
            );

            return;
        }

        NumberFormat formatter =
                NumberFormat
                        .getIntegerInstance(
                                RU_LOCALE
                        );

        textDetailsPrice.setText(
                getString(
                        R.string.order_price_format,
                        formatter.format(
                                price
                        )
                )
        );

        textDetailsPrice.setVisibility(
                View.VISIBLE
        );
    }

    private void bindDistance(
            Double distance
    ) {

        if (distance == null) {

            textDetailsDistance.setVisibility(
                    View.GONE
            );

            return;
        }

        textDetailsDistance.setText(
                getString(
                        R.string.order_distance_format,
                        String.format(
                                RU_LOCALE,
                                "%.1f",
                                distance
                        )
                )
        );

        textDetailsDistance.setVisibility(
                View.VISIBLE
        );
    }

    private void bindDuration(
            Integer duration
    ) {

        if (duration == null) {

            textDetailsDuration.setVisibility(
                    View.GONE
            );

            return;
        }

        int total =
                Math.max(
                        duration,
                        0
                );

        int hours =
                total / 60;

        int minutes =
                total % 60;

        String formatted =
                hours > 0
                        ? hours
                        + " ч "
                        + minutes
                        + " мин"
                        : minutes
                        + " мин";

        textDetailsDuration.setText(
                getString(
                        R.string.order_duration_format,
                        formatted
                )
        );

        textDetailsDuration.setVisibility(
                View.VISIBLE
        );
    }

    private void bindOptional(
            TextView view,
            String value,
            int stringResource
    ) {

        String cleanValue =
                clean(
                        value
                );

        if (cleanValue.isEmpty()) {

            view.setText(
                    ""
            );

            view.setVisibility(
                    View.GONE
            );

            return;
        }

        view.setText(
                getString(
                        stringResource,
                        cleanValue
                )
        );

        view.setVisibility(
                View.VISIBLE
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

        return getString(
                R.string.order_route_unknown
        );
    }

    private String statusLabel(
            String value
    ) {

        String status =
                clean(
                        value
                );

        switch (status) {

            case "new":
                return getString(
                        R.string.order_status_new
                );

            case "taken":
                return getString(
                        R.string.order_status_taken
                );

            case "in_progress":
                return getString(
                        R.string.order_status_in_progress
                );

            case "done":
                return getString(
                        R.string.order_status_done
                );

            case "canceled":
                return getString(
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
                return getString(
                        R.string.tariff_comfort
                );

            case "business":
                return getString(
                        R.string.tariff_business
                );

            case "minivan":
                return getString(
                        R.string.tariff_minivan
                );

            default:
                return tariff;
        }
    }

    private String successMessage(
            String targetStatus
    ) {

        if (
                "taken".equals(
                        targetStatus
                )
        ) {

            return getString(
                    R.string.order_accepted
            );
        }

        if (
                "in_progress".equals(
                        targetStatus
                )
        ) {

            return getString(
                    R.string.order_started
            );
        }

        if (
                "done".equals(
                        targetStatus
                )
        ) {

            return getString(
                    R.string.order_finished
            );
        }

        return getString(
                R.string.order_action_success
        );
    }

    private void clearLocalSession() {

        new SecureSessionStore(
                this
        )
                .clear();
    }

    private void openLogin() {

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private String safeMessage(
            String message,
            String fallback
    ) {

        return message == null
                ||
                message.trim().isEmpty()
                ? fallback
                : message.trim();
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
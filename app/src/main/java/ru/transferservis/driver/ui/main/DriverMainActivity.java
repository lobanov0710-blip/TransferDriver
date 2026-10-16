package ru.transferservis.driver.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.DriverSessionManager;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.ui.login.LoginActivity;

public class DriverMainActivity
        extends AppCompatActivity {

    private TextView textDriverTitle;
    private TextView textDriverStatus;
    private TextView textOrdersEmpty;

    private ProgressBar progressSession;

    private Button buttonRetrySession;
    private Button buttonLogout;

    private RecyclerView recyclerOrders;

    private SwipeRefreshLayout swipeRefreshOrders;

    private TabLayout tabOrders;

    private DriverSessionManager sessionManager;

    private OrdersViewModel ordersViewModel;

    private OrderAdapter orderAdapter;

    private boolean refreshAfterResume =
            false;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_driver_main
        );

        sessionManager =
                new DriverSessionManager(
                        this
                );

        ordersViewModel =
                new ViewModelProvider(
                        this
                )
                        .get(
                                OrdersViewModel.class
                        );

        initViews();

        setupOrdersList();

        setupSwipeRefresh();

        setupTabs();

        observeOrders();

        buttonRetrySession.setOnClickListener(
                view ->
                        ensureSessionAndLoadOrders(
                                true
                        )
        );

        buttonLogout.setOnClickListener(
                view ->
                        logout()
        );

        ensureSessionAndLoadOrders(
                true
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (
                refreshAfterResume
        ) {

            refreshAfterResume =
                    false;

            ensureSessionAndLoadOrders(
                    false
            );
        }
    }

    @Override
    protected void onStop() {

        super.onStop();

        if (
                !isFinishing()
                        &&
                        !isChangingConfigurations()
        ) {

            refreshAfterResume =
                    true;
        }
    }

    private void initViews() {

        textDriverTitle =
                findViewById(
                        R.id.textDriverTitle
                );

        textDriverStatus =
                findViewById(
                        R.id.textDriverStatus
                );

        textOrdersEmpty =
                findViewById(
                        R.id.textOrdersEmpty
                );

        progressSession =
                findViewById(
                        R.id.progressSession
                );

        buttonRetrySession =
                findViewById(
                        R.id.buttonRetrySession
                );

        buttonLogout =
                findViewById(
                        R.id.buttonLogout
                );

        recyclerOrders =
                findViewById(
                        R.id.recyclerOrders
                );

        swipeRefreshOrders =
                findViewById(
                        R.id.swipeRefreshOrders
                );

        tabOrders =
                findViewById(
                        R.id.tabOrders
                );
    }

    private void setupOrdersList() {

        orderAdapter =
                new OrderAdapter();

        recyclerOrders.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recyclerOrders.setAdapter(
                orderAdapter
        );
    }

    private void setupSwipeRefresh() {

        swipeRefreshOrders.setOnRefreshListener(
                () ->
                        ensureSessionAndLoadOrders(
                                false
                        )
        );

        swipeRefreshOrders
                .setOnChildScrollUpCallback(
                        (
                                parent,
                                child
                        ) ->
                                recyclerOrders
                                        .canScrollVertically(
                                                -1
                                        )
                );
    }

    private void setupTabs() {

        tabOrders.removeAllTabs();

        tabOrders.addTab(
                tabOrders
                        .newTab()
                        .setText(
                                R.string.orders_tab_new
                        )
        );

        tabOrders.addTab(
                tabOrders
                        .newTab()
                        .setText(
                                R.string.orders_tab_active
                        )
        );

        tabOrders.addTab(
                tabOrders
                        .newTab()
                        .setText(
                                R.string.orders_tab_history
                        )
        );

        tabOrders.addOnTabSelectedListener(
                new TabLayout
                        .OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab
                    ) {

                        if (tab == null) {
                            return;
                        }

                        switch (
                                tab.getPosition()
                        ) {

                            case 0:

                                ordersViewModel
                                        .setSection(
                                                OrderSection.NEW
                                        );

                                break;

                            case 1:

                                ordersViewModel
                                        .setSection(
                                                OrderSection.ACTIVE
                                        );

                                break;

                            case 2:

                                ordersViewModel
                                        .setSection(
                                                OrderSection.HISTORY
                                        );

                                break;

                            default:
                                break;
                        }
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab
                    ) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab
                    ) {
                    }
                }
        );
    }

    private void observeOrders() {

        ordersViewModel
                .getLoading()
                .observe(
                        this,
                        loading -> {

                            if (
                                    Boolean.TRUE.equals(
                                            loading
                                    )
                            ) {

                                showOrdersLoading();
                            }
                            else {

                                swipeRefreshOrders
                                        .setRefreshing(
                                                false
                                        );
                            }
                        }
                );

        ordersViewModel
                .getSection()
                .observe(
                        this,
                        section -> {

                            syncSelectedTab(
                                    section
                            );

                            updateEmptyStateText(
                                    section
                            );
                        }
                );

        ordersViewModel
                .getCounts()
                .observe(
                        this,
                        this::updateTabCounts
                );

        ordersViewModel
                .getResult()
                .observe(
                        this,
                        result -> {

                            if (result == null) {
                                return;
                            }

                            switch (
                                    result.getType()
                            ) {

                                case SUCCESS:

                                    showOrders(
                                            result.getOrders()
                                    );

                                    break;

                                case ERROR:

                                    showOrdersError(
                                            result.getMessage()
                                    );

                                    break;

                                case AUTHENTICATION_REQUIRED:

                                    clearLocalSession();

                                    openLogin();

                                    break;
                            }
                        }
                );
    }

    private void updateTabCounts(
            OrderCounts counts
    ) {

        if (
                tabOrders.getTabCount()
                        < 3
        ) {

            return;
        }

        TabLayout.Tab newTab =
                tabOrders.getTabAt(
                        0
                );

        TabLayout.Tab activeTab =
                tabOrders.getTabAt(
                        1
                );

        TabLayout.Tab historyTab =
                tabOrders.getTabAt(
                        2
                );

        if (
                newTab == null
                        ||
                        activeTab == null
                        ||
                        historyTab == null
        ) {

            return;
        }

        if (
                counts == null
                        ||
                        !counts.isExact()
        ) {

            newTab.setText(
                    R.string.orders_tab_new
            );

            activeTab.setText(
                    R.string.orders_tab_active
            );

            historyTab.setText(
                    R.string.orders_tab_history
            );

            return;
        }

        newTab.setText(
                getString(
                        R.string.orders_tab_new_count,
                        counts.getNewCount()
                )
        );

        activeTab.setText(
                getString(
                        R.string.orders_tab_active_count,
                        counts.getActiveCount()
                )
        );

        historyTab.setText(
                getString(
                        R.string.orders_tab_history_count,
                        counts.getHistoryCount()
                )
        );
    }

    private void updateEmptyStateText(
            OrderSection section
    ) {

        if (section == null) {

            textOrdersEmpty.setText(
                    R.string.orders_empty_new
            );

            return;
        }

        switch (section) {

            case ACTIVE:

                textOrdersEmpty.setText(
                        R.string.orders_empty_active
                );

                break;

            case HISTORY:

                textOrdersEmpty.setText(
                        R.string.orders_empty_history
                );

                break;

            case NEW:
            default:

                textOrdersEmpty.setText(
                        R.string.orders_empty_new
                );

                break;
        }
    }

    private void syncSelectedTab(
            OrderSection section
    ) {

        if (
                section == null
                        ||
                        tabOrders.getTabCount()
                                < 3
        ) {

            return;
        }

        int position;

        switch (section) {

            case ACTIVE:

                position =
                        1;

                break;

            case HISTORY:

                position =
                        2;

                break;

            case NEW:
            default:

                position =
                        0;

                break;
        }

        TabLayout.Tab tab =
                tabOrders.getTabAt(
                        position
                );

        if (
                tab != null
                        &&
                        !tab.isSelected()
        ) {

            tab.select();
        }
    }

    private void ensureSessionAndLoadOrders(
            boolean fullScreenLoading
    ) {

        if (fullScreenLoading) {

            showSessionLoading();

        }
        else {

            progressSession.setVisibility(
                    View.GONE
            );

            buttonRetrySession.setVisibility(
                    View.GONE
            );

            swipeRefreshOrders.setVisibility(
                    View.VISIBLE
            );

            swipeRefreshOrders.setRefreshing(
                    true
            );

            textDriverStatus.setText(
                    R.string.orders_loading
            );
        }

        sessionManager.ensureValidSession(
                new DriverSessionManager.SessionCallback() {

                    @Override
                    public void onReady(
                            DriverSession session
                    ) {

                        runOnUiThread(
                                () -> {

                                    showAuthenticated(
                                            session
                                    );

                                    boolean started =
                                            ordersViewModel
                                                    .loadOrders();

                                    if (!started) {

                                        swipeRefreshOrders
                                                .setRefreshing(
                                                        false
                                                );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () -> {

                                    swipeRefreshOrders
                                            .setRefreshing(
                                                    false
                                            );

                                    clearLocalSession();

                                    openLogin();
                                }
                        );
                    }

                    @Override
                    public void onTemporaryError(
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    if (fullScreenLoading) {

                                        showSessionError(
                                                message
                                        );

                                    }
                                    else {

                                        showRefreshError(
                                                message
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private void logout() {

        swipeRefreshOrders.setRefreshing(
                false
        );

        progressSession.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                false
        );

        buttonRetrySession.setVisibility(
                View.GONE
        );

        textDriverStatus.setText(
                R.string.logout_in_progress
        );

        sessionManager.logout(
                new DriverSessionManager.LogoutCallback() {

                    @Override
                    public void onLoggedOut() {

                        runOnUiThread(
                                () ->
                                        openLogin()
                        );
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    progressSession.setVisibility(
                                            View.GONE
                                    );

                                    buttonLogout.setEnabled(
                                            true
                                    );

                                    textDriverStatus.setText(
                                            safeMessage(
                                                    message,
                                                    getString(
                                                            R.string.logout_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void showSessionLoading() {

        swipeRefreshOrders.setRefreshing(
                false
        );

        swipeRefreshOrders.setEnabled(
                false
        );

        progressSession.setVisibility(
                View.VISIBLE
        );

        buttonRetrySession.setVisibility(
                View.GONE
        );

        buttonLogout.setVisibility(
                View.GONE
        );

        tabOrders.setVisibility(
                View.GONE
        );

        swipeRefreshOrders.setVisibility(
                View.GONE
        );

        textDriverStatus.setText(
                R.string.driver_session_checking
        );
    }

    private void showAuthenticated(
            DriverSession session
    ) {

        buttonLogout.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                true
        );

        tabOrders.setVisibility(
                View.VISIBLE
        );

        swipeRefreshOrders.setEnabled(
                true
        );

        String displayName =
                session.getDisplayName();

        if (
                displayName != null
                        &&
                        !displayName
                                .trim()
                                .isEmpty()
        ) {

            textDriverTitle.setText(
                    displayName.trim()
            );

        }
        else {

            textDriverTitle.setText(
                    R.string.driver_screen_title
            );
        }
    }

    private void showOrdersLoading() {

        textDriverStatus.setText(
                R.string.orders_loading
        );

        buttonRetrySession.setVisibility(
                View.GONE
        );

        if (
                swipeRefreshOrders
                        .isRefreshing()
        ) {

            progressSession.setVisibility(
                    View.GONE
            );

            return;
        }

        progressSession.setVisibility(
                View.VISIBLE
        );

        swipeRefreshOrders.setVisibility(
                View.GONE
        );
    }

    private void showOrders(
            List<StaffOrderDto> orders
    ) {

        swipeRefreshOrders.setRefreshing(
                false
        );

        progressSession.setVisibility(
                View.GONE
        );

        buttonRetrySession.setVisibility(
                View.GONE
        );

        buttonLogout.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                true
        );

        tabOrders.setVisibility(
                View.VISIBLE
        );

        swipeRefreshOrders.setVisibility(
                View.VISIBLE
        );

        orderAdapter.submitList(
                orders
        );

        int count =
                orders == null
                        ? 0
                        : orders.size();

        textDriverStatus.setText(
                getString(
                        R.string.orders_loaded_count,
                        count
                )
        );

        updateEmptyStateText(
                ordersViewModel
                        .getSection()
                        .getValue()
        );

        if (count == 0) {

            recyclerOrders.setVisibility(
                    View.GONE
            );

            textOrdersEmpty.setVisibility(
                    View.VISIBLE
            );

        }
        else {

            textOrdersEmpty.setVisibility(
                    View.GONE
            );

            recyclerOrders.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void showOrdersError(
            String message
    ) {

        swipeRefreshOrders.setRefreshing(
                false
        );

        progressSession.setVisibility(
                View.GONE
        );

        buttonRetrySession.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                true
        );

        tabOrders.setVisibility(
                View.VISIBLE
        );

        swipeRefreshOrders.setVisibility(
                View.VISIBLE
        );

        textDriverStatus.setText(
                safeMessage(
                        message,
                        getString(
                                R.string.orders_load_failed
                        )
                )
        );
    }

    private void showRefreshError(
            String message
    ) {

        swipeRefreshOrders.setRefreshing(
                false
        );

        progressSession.setVisibility(
                View.GONE
        );

        buttonRetrySession.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                true
        );

        tabOrders.setVisibility(
                View.VISIBLE
        );

        swipeRefreshOrders.setVisibility(
                View.VISIBLE
        );

        textDriverStatus.setText(
                safeMessage(
                        message,
                        getString(
                                R.string.orders_load_failed
                        )
                )
        );
    }

    private void showSessionError(
            String message
    ) {

        swipeRefreshOrders.setRefreshing(
                false
        );

        progressSession.setVisibility(
                View.GONE
        );

        buttonRetrySession.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setVisibility(
                View.VISIBLE
        );

        buttonLogout.setEnabled(
                true
        );

        tabOrders.setVisibility(
                View.GONE
        );

        swipeRefreshOrders.setVisibility(
                View.GONE
        );

        textDriverStatus.setText(
                safeMessage(
                        message,
                        getString(
                                R.string.session_refresh_failed
                        )
                )
        );
    }

    private void clearLocalSession() {

        new SecureSessionStore(
                this
        )
                .clear();
    }

    private String safeMessage(
            String message,
            String fallback
    ) {

        if (
                message == null
                        ||
                        message.trim().isEmpty()
        ) {

            return fallback;
        }

        return message.trim();
    }

    private void openLogin() {

        Intent intent =
                new Intent(
                        DriverMainActivity.this,
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
}
package ru.transferservis.driver.ui.main;

import android.content.res.Configuration;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keep the bottom availability control outside system bars/IME in both native themes. */
public final class MainWindow {
    private MainWindow() { }
    public static void configure(AppCompatActivity activity, View root) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        boolean light = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(light); bars.setAppearanceLightNavigationBars(light);
        float density = activity.getResources().getDisplayMetrics().density;
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int bottom = Math.max(system.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            view.setPadding(Math.round(16 * density) + system.left, Math.round(20 * density) + system.top,
                    Math.round(16 * density) + system.right, Math.round(16 * density) + bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}

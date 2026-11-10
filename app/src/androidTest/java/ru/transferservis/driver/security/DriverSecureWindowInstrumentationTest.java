package ru.transferservis.driver.security;

import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.transferservis.driver.ui.details.OrderDetailsActivity;
import ru.transferservis.driver.ui.login.LoginActivity;
import ru.transferservis.driver.ui.photo.PhotoCheckActivity;
import ru.transferservis.driver.ui.photo.PhotoChecksActivity;
import ru.transferservis.driver.ui.support.SupportCaseActivity;
import ru.transferservis.driver.ui.support.SupportCasesActivity;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class DriverSecureWindowInstrumentationTest {

    @Test
    public void loginWindowKeepsSecureFlagAfterActivityRecreation() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> assertTrue(
                    "Login window must prevent screenshots",
                    (activity.getWindow().getAttributes().flags
                            & WindowManager.LayoutParams.FLAG_SECURE) != 0));

            scenario.recreate();

            scenario.onActivity(activity -> assertTrue(
                    "Recreated login window must prevent screenshots",
                    (activity.getWindow().getAttributes().flags
                            & WindowManager.LayoutParams.FLAG_SECURE) != 0));
        }
    }

    @Test
    public void sensitiveDriverScreensUseSecureActivity() {
        Class<?>[] activities = {
                OrderDetailsActivity.class,
                PhotoCheckActivity.class,
                PhotoChecksActivity.class,
                SupportCaseActivity.class,
                SupportCasesActivity.class
        };
        for (Class<?> activity : activities) {
            assertTrue(activity.getSimpleName() + " must inherit SecureActivity",
                    SecureActivity.class.isAssignableFrom(activity));
        }
    }
}

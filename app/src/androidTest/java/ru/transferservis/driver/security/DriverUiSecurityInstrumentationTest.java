package ru.transferservis.driver.security;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import ru.transferservis.driver.ui.photo.PhotoCheckActivity;
import ru.transferservis.driver.ui.photo.PhotoChecksActivity;
import ru.transferservis.driver.ui.photo.PhotoSanitizer;
import ru.transferservis.driver.ui.support.SupportCaseActivity;
import ru.transferservis.driver.ui.support.SupportCasesActivity;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class DriverUiSecurityInstrumentationTest {

    @Test
    public void photoSanitizerRemovesGpsExifMetadata()
            throws Exception {

        Context context =
                InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext();

        File directory =
                new File(
                        context.getCacheDir(),
                        "photo-control-test"
                );

        assertTrue(
                directory.exists()
                        ||
                        directory.mkdirs()
        );

        File source =
                new File(
                        directory,
                        "source.jpg"
                );

        File destination =
                new File(
                        directory,
                        "sanitized.jpg"
                );

        Bitmap bitmap =
                Bitmap.createBitmap(
                        96,
                        64,
                        Bitmap.Config.ARGB_8888
                );

        try (
                FileOutputStream stream =
                        new FileOutputStream(
                                source,
                                false
                        )
        ) {

            assertTrue(
                    bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            90,
                            stream
                    )
            );
        }
        finally {

            bitmap.recycle();
        }

        ExifInterface sourceExif =
                new ExifInterface(
                        source.getAbsolutePath()
                );

        sourceExif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE,
                "40/1,0/1,0/1000"
        );

        sourceExif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                "N"
        );

        sourceExif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE,
                "74/1,0/1,0/1000"
        );

        sourceExif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                "W"
        );

        sourceExif.saveAttributes();

        PhotoSanitizer.sanitizeJpeg(
                source,
                destination
        );

        assertTrue(
                destination.isFile()
        );

        assertTrue(
                destination.length() > 0
        );

        ExifInterface sanitizedExif =
                new ExifInterface(
                        destination.getAbsolutePath()
                );

        assertNull(
                sanitizedExif.getAttribute(
                        ExifInterface.TAG_GPS_LATITUDE
                )
        );

        assertNull(
                sanitizedExif.getAttribute(
                        ExifInterface.TAG_GPS_LONGITUDE
                )
        );

        Bitmap decoded =
                BitmapFactory.decodeFile(
                        destination.getAbsolutePath()
                );

        assertNotNull(
                decoded
        );

        decoded.recycle();

        source.delete();
        destination.delete();
        directory.delete();
    }


    @Test
    public void fileProviderExposesOnlyPhotoControlCache()
            throws Exception {

        Context context =
                InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext();

        String authority =
                context.getPackageName()
                        +
                        ".fileprovider";

        ProviderInfo provider =
                context
                        .getPackageManager()
                        .resolveContentProvider(
                                authority,
                                PackageManager.GET_META_DATA
                        );

        assertNotNull(
                provider
        );

        assertFalse(
                provider.exported
        );

        assertTrue(
                provider.grantUriPermissions
        );

        File allowedDirectory =
                new File(
                        context.getCacheDir(),
                        "photo-control"
                );

        assertTrue(
                allowedDirectory.exists()
                        ||
                        allowedDirectory.mkdirs()
        );

        File allowed =
                new File(
                        allowedDirectory,
                        "provider-test.jpg"
                );

        assertTrue(
                allowed.createNewFile()
                        ||
                        allowed.isFile()
        );

        Uri uri =
                FileProvider.getUriForFile(
                        context,
                        authority,
                        allowed
                );

        assertEquals(
                "content",
                uri.getScheme()
        );

        File denied =
                new File(
                        context.getCacheDir(),
                        "not-photo-control.txt"
                );

        assertTrue(
                denied.createNewFile()
                        ||
                        denied.isFile()
        );

        try {

            FileProvider.getUriForFile(
                    context,
                    authority,
                    denied
            );

            fail(
                    "FileProvider exposed cache outside photo-control path"
            );

        }
        catch (
                IllegalArgumentException expected
        ) {
            // Expected: path is outside configured FileProvider root.
        }

        allowed.delete();
        denied.delete();
    }


    @Test
    public void driverPhotoAndSupportActivitiesAreNotExported()
            throws Exception {

        Context context =
                InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext();

        Class<?>[] activities = {
                PhotoChecksActivity.class,
                PhotoCheckActivity.class,
                SupportCasesActivity.class,
                SupportCaseActivity.class
        };

        for (Class<?> activity : activities) {

            ActivityInfo info =
                    context
                            .getPackageManager()
                            .getActivityInfo(
                                    new ComponentName(
                                            context,
                                            activity
                                    ),
                                    0
                            );

            assertFalse(
                    activity.getSimpleName()
                            +
                            " must not be exported",
                    info.exported
            );
        }
    }
}

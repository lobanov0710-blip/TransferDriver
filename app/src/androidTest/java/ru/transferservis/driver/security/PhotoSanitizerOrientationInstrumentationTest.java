package ru.transferservis.driver.security;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import ru.transferservis.driver.ui.photo.PhotoSanitizer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class PhotoSanitizerOrientationInstrumentationTest {

    @Test
    public void horizontalMirrorIsAppliedAndExifNotCarriedOver() throws Exception {
        withPhoto(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, (file, result) -> {
            Bitmap bitmap = BitmapFactory.decodeFile(result.getAbsolutePath());
            assertNotNull(bitmap);
            try {
                assertEquals(120, bitmap.getWidth());
                assertEquals(80, bitmap.getHeight());
                assertColor(bitmap.getPixel(10, 10), Color.BLUE);
                assertColor(bitmap.getPixel(110, 10), Color.RED);
                assertColor(bitmap.getPixel(10, 70), Color.YELLOW);
                assertColor(bitmap.getPixel(110, 70), Color.GREEN);
            } finally {
                bitmap.recycle();
            }
            ExifInterface exif = new ExifInterface(result.getAbsolutePath());
            int normalizedOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            assertTrue("Output must not require EXIF transform",
                    normalizedOrientation == ExifInterface.ORIENTATION_NORMAL
                            || normalizedOrientation == ExifInterface.ORIENTATION_UNDEFINED);
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        });
    }

    @Test
    public void clockwiseRotationSwapsDimensionsAndPosition() throws Exception {
        withPhoto(ExifInterface.ORIENTATION_ROTATE_90, (file, result) -> {
            Bitmap bitmap = BitmapFactory.decodeFile(result.getAbsolutePath());
            assertNotNull(bitmap);
            try {
                assertEquals(80, bitmap.getWidth());
                assertEquals(120, bitmap.getHeight());
                assertColor(bitmap.getPixel(10, 10), Color.GREEN);
                assertColor(bitmap.getPixel(70, 10), Color.RED);
                assertColor(bitmap.getPixel(10, 110), Color.YELLOW);
                assertColor(bitmap.getPixel(70, 110), Color.BLUE);
            } finally {
                bitmap.recycle();
            }
        });
    }

    @Test
    public void transposeIsNotMistakenForPlainRotation() throws Exception {
        withPhoto(ExifInterface.ORIENTATION_TRANSPOSE, (file, result) -> {
            Bitmap bitmap = BitmapFactory.decodeFile(result.getAbsolutePath());
            assertNotNull(bitmap);
            try {
                assertEquals(80, bitmap.getWidth());
                assertEquals(120, bitmap.getHeight());
                assertColor(bitmap.getPixel(10, 10), Color.RED);
                assertColor(bitmap.getPixel(70, 10), Color.GREEN);
                assertColor(bitmap.getPixel(10, 110), Color.BLUE);
                assertColor(bitmap.getPixel(70, 110), Color.YELLOW);
            } finally {
                bitmap.recycle();
            }
        });
    }

    @Test
    public void emptyInputIsRejectedWithoutCreatingOutput() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory = new File(context.getCacheDir(), "d3-invalid-" + UUID.randomUUID());
        assertTrue(directory.mkdirs());
        File input = new File(directory, "empty.jpg");
        File output = new File(directory, "output.jpg");
        try {
            assertTrue(input.createNewFile());
            try {
                PhotoSanitizer.sanitizeJpeg(input, output);
                fail("Empty JPEG must not pass the upload sanitizer");
            } catch (IOException expected) {
                assertFalse(output.exists());
            }
        } finally {
            input.delete();
            output.delete();
            directory.delete();
        }
    }

    private interface Verification {
        void verify(File input, File output) throws Exception;
    }

    private static void withPhoto(int orientation, Verification verification)
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory = new File(context.getCacheDir(), "d3-exif-" + UUID.randomUUID());
        assertTrue(directory.mkdirs());
        File source = new File(directory, "source.jpg");
        File result = new File(directory, "result.jpg");
        Bitmap original = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
        try {
            for (int x = 0; x < 120; x++) {
                for (int y = 0; y < 80; y++) {
                    int color;
                    if (x < 60) {
                        color = y < 40 ? Color.RED : Color.GREEN;
                    } else {
                        color = y < 40 ? Color.BLUE : Color.YELLOW;
                    }
                    original.setPixel(x, y, color);
                }
            }
            try (FileOutputStream stream = new FileOutputStream(source)) {
                assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 96, stream));
            }
            ExifInterface sourceExif = new ExifInterface(source.getAbsolutePath());
            sourceExif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    Integer.toString(orientation));
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                    "40/1,0/1,0/1000");
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            sourceExif.saveAttributes();

            PhotoSanitizer.sanitizeJpeg(source, result);
            assertTrue(result.isFile());
            assertTrue(result.length() > 0);
            verification.verify(source, result);
        } finally {
            original.recycle();
            source.delete();
            result.delete();
            directory.delete();
        }
    }

    private static void assertColor(int pixel, int expected) {
        assertTrue("Wrong R channel",
                Math.abs(Color.red(pixel) - Color.red(expected)) < 60);
        assertTrue("Wrong G channel",
                Math.abs(Color.green(pixel) - Color.green(expected)) < 60);
        assertTrue("Wrong B channel",
                Math.abs(Color.blue(pixel) - Color.blue(expected)) < 60);
    }
}

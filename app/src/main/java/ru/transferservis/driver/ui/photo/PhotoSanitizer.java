package ru.transferservis.driver.ui.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class PhotoSanitizer {

    private static final int MAX_DIMENSION =
            2048;

    private static final int JPEG_QUALITY =
            88;

    private PhotoSanitizer() {
    }

    public static void sanitizeJpeg(
            File source,
            File destination
    ) throws IOException {

        if (
                source == null
                        ||
                        destination == null
                        ||
                        !source.isFile()
                        ||
                        source.length() <= 0
        ) {

            throw new IOException(
                    "Invalid source image"
            );
        }

        BitmapFactory.Options bounds =
                new BitmapFactory.Options();

        bounds.inJustDecodeBounds =
                true;

        BitmapFactory.decodeFile(
                source.getAbsolutePath(),
                bounds
        );

        if (
                bounds.outWidth <= 0
                        ||
                        bounds.outHeight <= 0
        ) {

            throw new IOException(
                    "Invalid image bounds"
            );
        }

        int sample =
                1;

        while (
                bounds.outWidth / sample
                        > MAX_DIMENSION
                        ||
                        bounds.outHeight / sample
                                > MAX_DIMENSION
        ) {

            sample *=
                    2;
        }

        BitmapFactory.Options options =
                new BitmapFactory.Options();

        options.inSampleSize =
                sample;

        Bitmap decoded =
                BitmapFactory.decodeFile(
                        source.getAbsolutePath(),
                        options
                );

        if (decoded == null) {

            throw new IOException(
                    "Unable to decode image"
            );
        }

        Bitmap output =
                decoded;

        try {

            ExifInterface exif =
                    new ExifInterface(
                            source.getAbsolutePath()
                    );

            int orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    );

            int rotation =
                    rotationDegrees(
                            orientation
                    );

            if (rotation != 0) {

                Matrix matrix =
                        new Matrix();

                matrix.postRotate(
                        rotation
                );

                output =
                        Bitmap.createBitmap(
                                decoded,
                                0,
                                0,
                                decoded.getWidth(),
                                decoded.getHeight(),
                                matrix,
                                true
                        );
            }

            try (
                    FileOutputStream stream =
                            new FileOutputStream(
                                    destination,
                                    false
                            )
            ) {

                boolean compressed =
                        output.compress(
                                Bitmap.CompressFormat.JPEG,
                                JPEG_QUALITY,
                                stream
                        );

                stream.flush();

                if (!compressed) {

                    throw new IOException(
                            "Unable to encode image"
                    );
                }
            }

            if (
                    !destination.isFile()
                            ||
                            destination.length() <= 0
            ) {

                throw new IOException(
                        "Sanitized image is empty"
                );
            }

        }
        finally {

            if (
                    output != decoded
                            &&
                            !output.isRecycled()
            ) {

                output.recycle();
            }

            if (!decoded.isRecycled()) {

                decoded.recycle();
            }
        }
    }

    private static int rotationDegrees(
            int orientation
    ) {

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;

            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;

            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;

            default:
                return 0;
        }
    }
}

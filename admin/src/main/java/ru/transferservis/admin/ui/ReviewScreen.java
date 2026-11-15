package ru.transferservis.admin.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import ru.transferservis.admin.data.RegistrationReview;
import ru.transferservis.admin.data.ReviewAction;

/** Foreground memory only; never placed in saved state or a disk image cache. */
public final class ReviewScreen {
    public final RegistrationReview registration;
    public final Bitmap preview;
    public final String previewId;
    public final ReviewAction uncertain;
    public ReviewScreen(RegistrationReview registration,Bitmap preview,String previewId,ReviewAction uncertain) {
        this.registration=registration;this.preview=preview;this.previewId=previewId;this.uncertain=uncertain;
    }
    public static Bitmap decode(byte[] bytes) throws IOException {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>20000||bounds.outHeight>20000
                ||(long)bounds.outWidth*bounds.outHeight>40_000_000)throw new IOException("Invalid image");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(bitmap==null)throw new IOException("Invalid image");return bitmap;
    }
}

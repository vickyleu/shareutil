package me.shaohui.shareutil.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import me.shaohui.shareutil.ShareLogger;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;

import static me.shaohui.shareutil.ShareLogger.INFO;

import androidx.core.content.FileProvider;

/**
 * Created by shaohui on 2016/11/19.
 */

public class ImageDecoder {

    private static final String FILE_NAME = "/shareData/test.png";

    public static String decode(Context context, ShareImageObject imageObject) throws Exception {
        File resultFile = cacheFile(context);

        if (!TextUtils.isEmpty(imageObject.getPathOrUrl())) {
            return decode(context, imageObject.getPathOrUrl());
        } else if (imageObject.getBitmap() != null) {
            // save bitmap to file
            FileOutputStream outputStream = new FileOutputStream(resultFile);
            imageObject.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            return resultFile.getAbsolutePath();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static byte[] bitmap2Bytes(final Bitmap bitmap, final Bitmap.CompressFormat format) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, 100, baos);
        return baos.toByteArray();
    }

    private static String decode(Context context, String pathOrUrl) throws Exception {
        File resultFile = cacheFile(context);

        if (new File(pathOrUrl).exists()) {
            // copy file
            return decodeFile(context,new File(pathOrUrl), resultFile);
        } else if (HttpUrl.parse(pathOrUrl) != null) {
            // download image
            return downloadImageToUri(pathOrUrl, resultFile);
        } else {
            throw new IllegalArgumentException("Please input a file path or http url");
        }
    }

    private static String downloadImageToUri(String url, File resultFile) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        BufferedSink sink = Okio.buffer(Okio.sink(resultFile));
        sink.writeAll(response.body().source());

        sink.close();
        response.close();

        return resultFile.getAbsolutePath();
    }

    private static File cacheFile(Context context) throws Exception {
        String state = Environment.getExternalStorageState();
        if (state != null && state.equals(Environment.MEDIA_MOUNTED)) {
            return new File(context.getExternalFilesDir(null), FILE_NAME);
        } else {
            throw new Exception(INFO.SD_CARD_NOT_AVAILABLE);
        }
    }

    private static void copyFile(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[4096];
        while (-1 != inputStream.read(buffer)) {
            outputStream.write(buffer);
        }

        outputStream.flush();
        inputStream.close();
        outputStream.close();
    }

    private static String decodeFile(Context context, File origin, File result) throws IOException {
        copyFile(new FileInputStream(origin), new FileOutputStream(result, false));
        Uri contentUri = FileProvider.getUriForFile(context,context.getPackageName() + ".fileprovider",  // 要与`AndroidManifest.xml`里配置的`authorities`一致，假设你的应用包名为com.example.app
                result);
        // 授权给微信访问路径
        context.grantUriPermission("com.tencent.mm",  // 这里填微信包名
                contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return contentUri.toString();
//        return result.getAbsolutePath();
    }

    public static byte[] compress2Byte(String imagePath, int size, int length) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int outH = options.outHeight;
        int outW = options.outWidth;
        int inSampleSize = 1;

        while (outH / inSampleSize > size || outW / inSampleSize > size) {
            inSampleSize *= 2;
        }

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, result);
        if (result.size() > length) {
            result.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, result);
        }

        bitmap.recycle();
        return result.toByteArray();
    }
}

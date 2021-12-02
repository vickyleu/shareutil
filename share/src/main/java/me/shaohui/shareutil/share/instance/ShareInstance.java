package me.shaohui.shareutil.share.instance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import me.shaohui.shareutil.share.ShareImageObject;
import me.shaohui.shareutil.share.ShareListener;

/**
 * Created by shaohui on 2016/11/18.
 */

public interface ShareInstance {

    void shareText(int platform, String text, Activity activity, ShareListener listener);

    void shareMedia(int platform, String title, String targetUrl, String summary,
            ShareImageObject shareImageObject, Activity activity, ShareListener listener);

    void shareImage(int platform, ShareImageObject shareImageObject, Activity activity,
            ShareListener listener);

    void shareMiniProgram(int platform,String webpageUrl, String originId, String urlPath, String title,
                          String description, ShareImageObject thumbData,Activity activity,ShareListener listener);

    void handleResult(Intent data);

    boolean isInstall(Context context);

    void recycle();
}

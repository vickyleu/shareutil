package me.shaohui.shareutil.share.instance;

import static me.shaohui.shareutil.ShareLogger.INFO;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzonePublish;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.Tencent;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.shaohui.shareutil.ShareUtil;
import me.shaohui.shareutil.share.ImageDecoder;
import me.shaohui.shareutil.share.ShareImageObject;
import me.shaohui.shareutil.share.ShareListener;
import me.shaohui.shareutil.share.SharePlatform;

/**
 * Created by shaohui on 2016/11/18.
 */

public class QQShareInstance implements ShareInstance {

    private Tencent mTencent;

    public QQShareInstance(Context context, String app_id) {
        mTencent = Tencent.createInstance(app_id, context.getApplicationContext(),context.getPackageName() + ".shareprovider");
    }

    @Override
    public void shareText(int platform, String text, Activity activity, ShareListener listener) {
        if (platform == SharePlatform.QZONE) {
            shareToQZoneForText(text, activity, listener);
        } else {
            activity.finish();
            listener.shareFailure(new Exception(INFO.QQ_NOT_SUPPORT_SHARE_TXT));
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void shareMedia(final int platform, final String title, final String targetUrl,
                           final String summary, final ShareImageObject shareImageObject, final Activity activity,
                           final ShareListener listener) {
        Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            try {
                emitter.onNext(ImageDecoder.decode(activity, shareImageObject, platform,true));
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> listener.shareRequest())
                .subscribe(imageUri -> {
                    if (platform == SharePlatform.QZONE) {
                        shareToQZoneForMedia(title, targetUrl, summary, imageUri, activity,
                                listener);
                    } else {
                        shareToQQForMedia(title, summary, targetUrl, imageUri, activity, listener);
                    }
                }, throwable -> {
                    activity.finish();
                    listener.shareFailure(new Exception(throwable));
                });
    }

    @SuppressLint("CheckResult")
    @Override
    public void shareImage(final int platform, final ShareImageObject shareImageObject,
                           final Activity activity, final ShareListener listener) {
        Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            try {
                emitter.onNext(ImageDecoder.decode(activity, shareImageObject, platform,true));
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> listener.shareRequest())
                .subscribe(localPath -> {
                    if (platform == SharePlatform.QZONE) {
                        shareToQzoneForImage(localPath, activity, listener);
                    } else {
                        shareToQQForImage(localPath, activity, listener);
                    }
                }, throwable -> {
                    activity.finish();
                    listener.shareFailure(new Exception(throwable));
                });
    }

    @Override
    public void shareMiniProgram(int platform, String webpageUrl, String originId, String urlPath, String title, String description, ShareImageObject thumbData, Activity activity, ShareListener listener) {

    }

    @Override
    public void handleResult(Intent data) {
        Tencent.handleResultData(data, ShareUtil.mShareListener);
    }

    @Override
    public boolean isInstall(Context context) {
        try {
            return mTencent.isQQInstalled(context);
        }catch (Exception e){
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                return false;
            }

            List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
            for (PackageInfo info : packageInfos) {
                if (TextUtils.equals(info.packageName.toLowerCase(), "com.tencent.mobileqq")) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void recycle() {
        if (mTencent != null) {
//            mTencent.releaseResource();
            mTencent = null;
        }
    }

    private void shareToQQForMedia(String title, String summary, String targetUrl, String thumbUrl,
                                   Activity activity, ShareListener listener) {
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        params.putString(QQShare.SHARE_TO_QQ_TITLE, title);
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, targetUrl);
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, thumbUrl);
//        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, thumbUrl);
        mTencent.shareToQQ(activity, params, listener);
    }

    private void shareToQQForImage(String localUrl, Activity activity, ShareListener listener) {
        Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, localUrl);
        mTencent.shareToQQ(activity, params, listener);
    }

    private void shareToQZoneForText(String text, Activity activity, ShareListener listener) {
        final Bundle params = new Bundle();
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,
                QzonePublish.PUBLISH_TO_QZONE_TYPE_PUBLISHMOOD);
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, text);
        mTencent.publishToQzone(activity, params, listener);
    }

    private void shareToQZoneForMedia(String title, String targetUrl, String summary,
                                      String imageUrl, Activity activity, ShareListener listener) {
        final Bundle params = new Bundle();
//        final ArrayList<String> image = new ArrayList<>();
//        image.add(imageUrl);
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,
                QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, summary);
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, targetUrl);
//        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, image);
        params.putString(QzoneShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, imageUrl);
        mTencent.shareToQzone(activity, params, listener);
    }

    private void shareToQzoneForImage(String imagePath, Activity activity, ShareListener listener) {
        final Bundle params = new Bundle();
//        final ArrayList<String> image = new ArrayList<>();
//        image.add(imagePath);
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,
                QzonePublish.PUBLISH_TO_QZONE_TYPE_PUBLISHMOOD);
//        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, image);
        params.putString(QzoneShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, imagePath);
        mTencent.publishToQzone(activity, params, listener);
    }
}

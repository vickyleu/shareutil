package me.shaohui.shareutil.share.instance;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.common.UiError;
import com.sina.weibo.sdk.openapi.IWBAPI;
import com.sina.weibo.sdk.openapi.WBAPIFactory;
import com.sina.weibo.sdk.share.WbShareCallback;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.shaohui.shareutil.ShareManager;
import me.shaohui.shareutil.ShareUtil;
import me.shaohui.shareutil.share.ShareImageObject;
import me.shaohui.shareutil.share.ShareListener;

/**
 * Created by shaohui on 2016/11/18.
 */

public class WeiboShareInstance implements ShareInstance {
    /**
     * 微博分享限制thumb image必须小于2097152，否则点击分享会没有反应
     */

    private IWBAPI mWeiboShareAPI;

    private static final int TARGET_SIZE = 1024;

    private static final int TARGET_LENGTH = 2097152;

    public WeiboShareInstance(Context context, String appId) {
        AuthInfo authInfo = new AuthInfo(context, appId,//ShareManager.CONFIG.getWeiboId(),
                ShareManager.CONFIG.getWeiboRedirectUrl(), ShareManager.CONFIG.getWeiboScope());
        mWeiboShareAPI = WBAPIFactory.createWBAPI(context);
        mWeiboShareAPI.registerApp(context, authInfo);
    }

    @Override
    public void shareText(int platform, String text, Activity activity, ShareListener listener) {
        TextObject textObject = new TextObject();
        textObject.text = text;
        WeiboMultiMessage message = new WeiboMultiMessage();
        message.textObject = textObject;
        sendRequest(message);
    }

    @Override
    public void shareMedia(int platform, final String title, final String targetUrl, String summary,
                           ShareImageObject shareImageObject, final Activity activity,
                           final ShareListener listener) {
        String content = String.format("%s %s", title, targetUrl);
        shareTextOrImage(shareImageObject, content, activity, listener);
    }

    @Override
    public void shareImage(int platform, ShareImageObject shareImageObject, Activity activity,
                           ShareListener listener) {
        shareTextOrImage(shareImageObject, null, activity, listener);
    }

    @Override
    public void shareMiniProgram(int platform, String webpageUrl, String originId, String urlPath, String title, String description, ShareImageObject thumbData, Activity activity, ShareListener listener) {

    }

    @Override
    public void handleResult(Intent data) {
        mWeiboShareAPI.doResultIntent(data, new WbShareCallback() {
            @Override
            public void onComplete() {
                ShareUtil.mShareListener.shareSuccess();
            }

            @Override
            public void onError(UiError uiError) {
                ShareUtil.mShareListener.shareFailure(new Exception(uiError.errorMessage));
            }

            @Override
            public void onCancel() {
                ShareUtil.mShareListener.shareCancel();
            }
        });
    }

    @Override
    public boolean isInstall(Context context) {
        return mWeiboShareAPI.isWBAppInstalled();
    }

    @Override
    public void recycle() {
        mWeiboShareAPI = null;
    }

    @SuppressLint("CheckResult")
    private void shareTextOrImage(final ShareImageObject shareImageObject, final String text,
                                  final Activity activity, final ShareListener listener) {

        Flowable.create((FlowableOnSubscribe<Pair<String, byte[]>>) e -> {
        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscription -> listener.shareRequest())
                .subscribe(pair -> {
                    ImageObject imageObject = new ImageObject();
                    imageObject.imageData = pair.second;
                    imageObject.imagePath = pair.first;

                    WeiboMultiMessage message = new WeiboMultiMessage();
                    message.imageObject = imageObject;
                    if (!TextUtils.isEmpty(text)) {
                        TextObject textObject = new TextObject();
                        textObject.text = text;
                        message.textObject = textObject;
                    }
                    sendRequest(message);
                }, throwable -> {
                    activity.finish();
                    listener.shareFailure(new Exception(throwable));
                });
    }

    private void sendRequest(WeiboMultiMessage message) {
        mWeiboShareAPI.shareMessage(message, true);
//        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
//        request.transaction = String.valueOf(System.currentTimeMillis());
//        request.multiMessage = message;
//        mWeiboShareAPI.sendRequest(activity, request);
    }
}

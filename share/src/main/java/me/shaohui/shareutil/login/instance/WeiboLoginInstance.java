package me.shaohui.shareutil.login.instance;

import static com.umeng.socialize.PlatformConfig.configs;
import static me.shaohui.shareutil.ShareLogger.INFO;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.handler.SinaSsoHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.shaohui.shareutil.ShareLogger;
import me.shaohui.shareutil.login.LoginListener;
import me.shaohui.shareutil.login.LoginPlatform;
import me.shaohui.shareutil.login.LoginResult;
import me.shaohui.shareutil.login.result.BaseToken;
import me.shaohui.shareutil.login.result.WeiboToken;
import me.shaohui.shareutil.login.result.WeiboUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by shaohui on 2016/12/1.
 */

public class WeiboLoginInstance extends LoginInstance {

    private static final String USER_INFO = "https://api.weibo.com/2/users/show.json";

    private SinaSsoHandler mSsoHandler;

    private LoginListener mLoginListener;

    public WeiboLoginInstance(Activity activity, LoginListener listener, boolean fetchUserInfo) {
        super(activity, listener, fetchUserInfo);
//        AuthInfo authInfo = new AuthInfo(activity, ShareManager.CONFIG.getWeiboId(),
//                ShareManager.CONFIG.getWeiboRedirectUrl(), ShareManager.CONFIG.getWeiboScope());
        mSsoHandler = new SinaSsoHandler();
        mSsoHandler.onCreate(activity, configs.get(SHARE_MEDIA.SINA));
        mLoginListener = listener;
    }

    @Override
    public void doLogin(Activity activity, final LoginListener listener,
                        final boolean fetchUserInfo) {
        mSsoHandler.authorize(new UMAuthListener() {
            @Override
            public void onStart(SHARE_MEDIA share_media) {

            }

            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                WeiboToken weiboToken = WeiboToken.parse(map);
                if (fetchUserInfo) {
                    listener.beforeFetchUserInfo(weiboToken);
                    fetchUserInfo(weiboToken);
                } else {
                    listener.loginSuccess(new LoginResult(LoginPlatform.WEIBO, weiboToken));
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                ShareLogger.i(INFO.WEIBO_AUTH_ERROR);
                listener.loginFailure(new Exception(throwable.getMessage()));
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
                ShareLogger.i(INFO.AUTH_CANCEL);
                listener.loginCancel();
            }
        });
    }

    @SuppressLint("CheckResult")
    @Override
    public void fetchUserInfo(final BaseToken token) {
        Flowable.create((FlowableOnSubscribe<WeiboUser>) weiboUserEmitter -> {
            OkHttpClient client = new OkHttpClient();
            Request request =
                    new Request.Builder().url(buildUserInfoUrl(token, USER_INFO)).build();
            try {
                Response response = client.newCall(request).execute();
                JSONObject jsonObject = new JSONObject(response.body().string());
                WeiboUser user = WeiboUser.parse(jsonObject);
                weiboUserEmitter.onNext(user);
            } catch (IOException | JSONException e) {
                ShareLogger.e(INFO.FETCH_USER_INOF_ERROR);
                weiboUserEmitter.onError(e);
            }
        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(weiboUser -> mLoginListener.loginSuccess(
                        new LoginResult(LoginPlatform.WEIBO, token, weiboUser)), throwable ->
                        mLoginListener.loginFailure(new Exception(throwable)));
    }

    private String buildUserInfoUrl(BaseToken token, String baseUrl) {
        return baseUrl + "?access_token=" + token.getAccessToken() + "&uid=" + token.getOpenid();
    }

    @Override
    public void handleResult(int requestCode, int resultCode, Intent data) {
        mSsoHandler.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean isInstall(Context context) {
        return mSsoHandler.isInstall();
    }

    @Override
    public void recycle() {
        mSsoHandler = null;
        mLoginListener = null;
    }
}

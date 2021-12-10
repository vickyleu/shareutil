package me.shaohui.shareutil.login.instance;

import static me.shaohui.shareutil.ShareLogger.INFO;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.tencent.connect.UnionInfo;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.shaohui.shareutil.ShareLogger;
import me.shaohui.shareutil.ShareManager;
import me.shaohui.shareutil.login.LoginListener;
import me.shaohui.shareutil.login.LoginPlatform;
import me.shaohui.shareutil.login.LoginResult;
import me.shaohui.shareutil.login.result.BaseToken;
import me.shaohui.shareutil.login.result.QQToken;
import me.shaohui.shareutil.login.result.QQUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by shaohui on 2016/12/1.
 */

public class QQLoginInstance extends LoginInstance {

    private static final String SCOPE = "all";

    private static final String URL = "https://graph.qq.com/user/get_user_info";

    private Tencent mTencent;

    private IUiListener mIUiListener;

    private LoginListener mLoginListener;

    private Activity mActivity;

    public QQLoginInstance(final Activity activity, final LoginListener listener,
                           final boolean fetchUserInfo) {
        super(activity, listener, fetchUserInfo);
        mActivity = activity;
        mTencent = Tencent.createInstance(ShareManager.CONFIG.getQqId(), activity,activity.getPackageName() + ".shareprovider");
        mLoginListener = listener;
        mIUiListener = new IUiListener() {
            @Override
            public void onComplete(Object o) {
                ShareLogger.i(INFO.QQ_AUTH_SUCCESS);
                try {
                    QQToken token = QQToken.parse((JSONObject) o);
                    initOpenidAndToken((JSONObject) o);
                    if (fetchUserInfo) {
                        listener.beforeFetchUserInfo(token);
                        fetchUserInfo(token);
                    } else {
                        listener.loginSuccess(new LoginResult(LoginPlatform.QQ, token));
                    }

                } catch (JSONException e) {
                    ShareLogger.i(INFO.ILLEGAL_TOKEN);
                    mLoginListener.loginFailure(e);
                }
            }

            @Override
            public void onError(UiError uiError) {
                ShareLogger.i(INFO.QQ_LOGIN_ERROR);
                listener.loginFailure(
                        new Exception("QQError: " + uiError.errorCode + uiError.errorDetail));
            }

            @Override
            public void onCancel() {
                ShareLogger.i(INFO.AUTH_CANCEL);
                listener.loginCancel();
            }

            @Override
            public void onWarning(int i) {

            }
        };
    }

    private void getUnionId(Context context, final BaseToken token, final Runnable r) {
        if (mTencent != null && mTencent.isSessionValid()) {
            IUiListener listener = new IUiListener() {
                @Override
                public void onError(UiError e) {
                }

                @Override
                public void onComplete(final Object response) {
                    if (response != null) {
                        JSONObject jsonObject = (JSONObject) response;
                        try {
                            String unionid = jsonObject.getString("unionid");
                            token.setUnionid(unionid);
                            r.run();
                        } catch (Exception ignored) {
                        }
                    }
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onWarning(int i) {

                }
            };
            UnionInfo unionInfo = new UnionInfo(context, mTencent.getQQToken());
            unionInfo.getUnionId(listener);
        }
    }

    @Override
    public void doLogin(Activity activity, final LoginListener listener, boolean fetchUserInfo) {
        mTencent.login(activity, SCOPE, mIUiListener);
    }

    @SuppressLint("CheckResult")
    @Override
    public void fetchUserInfo(final BaseToken token) {
        Flowable.create((FlowableOnSubscribe<QQUser>) qqUserEmitter -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(buildUserInfoUrl(token, URL)).build();

            try {
                Response response = client.newCall(request).execute();
                JSONObject jsonObject = new JSONObject(response.body().string());
                QQUser user = QQUser.parse(token.getOpenid(), jsonObject);
                qqUserEmitter.onNext(user);
            } catch (IOException | JSONException e) {
                ShareLogger.e(INFO.FETCH_USER_INOF_ERROR);
                qqUserEmitter.onError(e);
            }
        }, BackpressureStrategy.DROP)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(qqUser -> {
                    //登陆qq完成之后获取UnionId
                    getUnionId(mActivity, token, () -> mLoginListener.loginSuccess(new LoginResult(LoginPlatform.QQ, token, qqUser)));
                }, throwable -> mLoginListener.loginFailure(new Exception(throwable)));
    }

    private void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch (Exception ignored) {
        }
    }

    private String buildUserInfoUrl(BaseToken token, String base) {
        return base
                + "?access_token="
                + token.getAccessToken()
                + "&oauth_consumer_key="
                + ShareManager.CONFIG.getQqId()
                + "&openid="
                + token.getOpenid();
    }

    @Override
    public void handleResult(int requestCode, int resultCode, Intent data) {
        Tencent.handleResultData(data, mIUiListener);
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
//        mTencent.releaseResource();
        mIUiListener = null;
        mLoginListener = null;
        mTencent = null;
        mActivity = null;
    }
}

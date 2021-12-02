package me.shaohui.shareutil.share;

import static me.shaohui.shareutil.ShareLogger.INFO;

import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

/**
 * Created by shaohui on 2016/11/18.
 */

public abstract class ShareListener implements IUiListener {
    @Override
    public final void onComplete(Object o) {
        shareSuccess();
    }

    @Override
    public final void onError(UiError uiError) {
        shareFailure(
                new Exception(uiError == null ? INFO.DEFAULT_QQ_SHARE_ERROR : uiError.errorDetail));
    }

    @Override
    public final void onCancel() {
        shareCancel();
    }


    public abstract void shareSuccess();

    public abstract void shareFailure(Exception e);

    public abstract void shareCancel();

    // 用于缓解用户焦虑
    public void shareRequest() {
    }
}

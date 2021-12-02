package me.shaohui.shareutil.login.result;

/**
 * Created by shaohui on 2016/12/3.
 */

public class BaseToken {

    private String access_token;

    private String openid;

    private String unionid;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }
}

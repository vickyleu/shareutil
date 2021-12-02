package me.shaohui.shareutil.login.result;


import java.util.Map;

/**
 * Created by shaohui on 2016/12/3.
 */

public class WeiboToken extends BaseToken {

    private String refreshToken;

    private String userName;

    public static WeiboToken parse(Map<String, String> map) {
        WeiboToken target = new WeiboToken();
        target.setOpenid(map.get("uid"));
        target.setAccessToken(map.get("access_token"));
        target.setRefreshToken(map.get("refresh_token"));
        target.setUserName(map.get("userName"));
        return target;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String phoneNum) {
        this.userName = phoneNum;
    }
}

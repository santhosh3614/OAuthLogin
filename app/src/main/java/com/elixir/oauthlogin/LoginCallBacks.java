package com.elixir.oauthlogin;

/**
 * Created by santhoshkumar on 29/4/15.
 */
public interface LoginCallBacks {
    void onLoginSuccess(LoginType loginType, String name, String email, String phno, String thumburl);
    void onLoginFailed(LoginType loginType);
}

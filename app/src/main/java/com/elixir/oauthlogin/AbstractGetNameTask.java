package com.elixir.oauthlogin;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
* Created by santhoshkumar on 29/4/15.
*/
public abstract class AbstractGetNameTask extends AsyncTask<Void, Void, Void>{
    private static final String TAG = "TokenInfoTask";
    protected LoginCallBacks mActivity;
    protected String mScope;
    protected String mEmail;
    protected int mRequestCode;
    private LoginType loginType;

    AbstractGetNameTask(MainActivity activity, String email, String scope,LoginType loginType) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = email;
        this.loginType=loginType;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            fetchNameFromProfileServer();
        } catch (IOException ex) {
            onError("Following Error occured, please try again. "
                    + ex.getMessage(), ex);
        } catch (JSONException e) {
            onError("Bad response: " + e.getMessage(), e);
        }
        return null;
    }

    protected void onError(String msg, Exception e) {
        if (e != null) {
            mActivity.onLoginFailed(loginType);
            e.printStackTrace();
        }
    }

    /**
     * Get a authentication token if one is not available. If the error is not
     * recoverable then it displays the error message on parent activity.
     */
    protected abstract String fetchToken() throws IOException;

    /**
     * Contacts the user info server to get the profile of the user and extracts
     * the first name of the user from the profile. In order to authenticate
     * with the user info server the method first fetches an access token from
     * Google Play services.
     * @return
     * @return
     *
     * @throws java.io.IOException
     *             if communication with user info server failed.
     * @throws org.json.JSONException
     *             if the response from the server could not be parsed.
     */
    private void fetchNameFromProfileServer() throws IOException, JSONException {
        String token = fetchToken();
        URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token="+ token);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int sc = con.getResponseCode();
        if (sc == 200) {
            InputStream is = con.getInputStream();
            String GOOGLE_USER_DATA = readResponse(is);
            is.close();
            JSONObject profileData = new JSONObject(GOOGLE_USER_DATA);
            String name=null;
            String gender=null;
            final String phno=null;
            String pic=null;
            if (profileData.has("picture")) {
                pic = profileData.getString("picture");
            }
            if (profileData.has("name")) {
                name = profileData.getString("name");
            }
            if (profileData.has("gender")) {
                gender = profileData.getString("gender");
            }
            final String finalName = name;
            final String finalPic = pic;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mActivity.onLoginSuccess(loginType, finalName, mEmail, phno, finalPic);
                }
            });
            return;
        } else if (sc == 401) {
            GoogleAuthUtil.invalidateToken((Activity)mActivity, token);
            onError("Server auth error, please try again.", null);
            return;
        } else {
            onError("Server returned the following error code: " + sc, null);
            return;
        }
    }

    /**
     * Reads the response from the input stream and returns it as a string.
     */
    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }


}

package com.elixir.oauthlogin;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends Activity implements LoginCallBacks {

    private static final String PREF_KEY_TWITTER_LOGIN = "is_twitter_logged_in";
    private static final int WEBVIEW_REQUEST_CODE = 123;
    private static final String PREF_KEY_OAUTH_TOKEN = "key_oauth";
    private static final String PREF_KEY_OAUTH_SECRET = "key_oath_secret";
    private static final String PREF_USER_NAME = "user_name";
    String token;
    int serverCode;
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
    @InjectView(R.id.main_lay)
    RelativeLayout mainRelativeLayout;
    @InjectView(R.id.intr_rel)
    RelativeLayout poorIntrReltaRelativeLayout;
    @InjectView(R.id.gmail_login_btn)
    Button gmailLoginBtn;
    @InjectView(R.id.fb_login_btn)
    Button fbLoginBtn;
    @InjectView(R.id.twitter_login_btn)
    Button twitterLoginBtn;
    @InjectView(R.id.profile_name_txtview)
    TextView profileTextview;
    @InjectView(R.id.profile_pic_imageview)
    ImageView profilePicImageView;
    private AccountManager mAccountManager;
    private String LOGIN_INFO = "login_info";
    private SharedPreferences mSharedPreferences;
    private LoginType loginType;
    private ProgressDialog progressDialog;
    private Twitter twitter;
    private RequestToken requestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        mSharedPreferences = getSharedPreferences(LOGIN_INFO, MODE_PRIVATE);
        ButterKnife.inject(this);
        init();

    }

    private void init() {
        if (!Utils.isNetworkAvailable(this)) {
            poorIntrReltaRelativeLayout.setVisibility(View.VISIBLE);
            mainRelativeLayout.setVisibility(View.INVISIBLE);
            return;
        }
        int logInType = mSharedPreferences.getInt("loginType", -1);
        if (logInType != -1) {
            loginType = LoginType.values()[logInType];
            initLoginBtn(loginType);
            initProfileData();
            if (loginType == LoginType.TWITTER) {
                twitterLoginBtn.setText(getString(R.string.twitter_logout));
            } else if (loginType == LoginType.GMAIL) {
                gmailLoginBtn.setText(getString(R.string.gmail_logout));
            } else {
                fbLoginBtn.setText(getString(R.string.fb_logout));
            }
        }
    }

    void initLoginBtn(LoginType loginType) {
        fbLoginBtn.setVisibility(loginType == LoginType.FACEBOOK ? View.VISIBLE : View.INVISIBLE);
        gmailLoginBtn.setVisibility(loginType == LoginType.GMAIL ? View.VISIBLE : View.INVISIBLE);
        twitterLoginBtn.setVisibility(loginType == LoginType.TWITTER ? View.VISIBLE : View.INVISIBLE);
    }

    private void initProfileData() {
        profileTextview.setText(mSharedPreferences.getString("name", null));
        new GetImageFromUrl(profilePicImageView).execute(mSharedPreferences.getString("thumbnail", null));
    }

    private void pushGmailsDialog() {
        final String[] gmails = Utils.getAccountNames(this);
        if (gmails.length == 0) {
            Toast.makeText(MainActivity.this, "No sync gmails !",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Gmail").setSingleChoiceItems(gmails, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.show();
                syncGoogleAccount(gmails[which]);
                dialog.dismiss();
            }
        }).show();
    }

    public void syncGoogleAccount(String gmail) {
        if (Utils.isNetworkAvailable(this)) {
            new GetNameInForgroundTask(MainActivity.this, gmail, SCOPE, LoginType.GMAIL).execute();
        } else {
            Toast.makeText(MainActivity.this, "No Network Service!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick({R.id.gmail_login_btn, R.id.fb_login_btn, R.id.twitter_login_btn})
    void loginLogoutClickListner(View view) {
        Button txtview = (Button) view;
        if (view.getId() == R.id.gmail_login_btn) {
            if (txtview.getText().equals(getString(R.string.gmail_login))) {
                pushGmailsDialog();
            } else {
                mSharedPreferences.edit().clear().commit();
                finish();
                Toast.makeText(this, "logout succesful.", Toast.LENGTH_LONG).show();
            }
        } else if (view.getId() == R.id.twitter_login_btn) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    loginToTwitter();
                    return null;
                }
            }.execute();
        }
    }

    private void loginToTwitter() {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(getString(R.string.twitter_consumer_key));
            builder.setOAuthConsumerSecret(getString(R.string.twitter_consumer_secret));
            Configuration configuration = builder.build();
            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();
            try {
                requestToken = twitter
                        .getOAuthRequestToken(getString(R.string.twitter_callback));
                final Intent intent = new Intent(this, WebActivity.class);
                intent.putExtra(WebActivity.EXTRA_URL, requestToken.getAuthenticationURL());
                startActivityForResult(intent, WEBVIEW_REQUEST_CODE);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {
            // user already logged into twitter
            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            String verifier = data.getExtras().getString("oauth_verifier");
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

                long userID = accessToken.getUserId();
                final User user = twitter.showUser(userID);
                String username = user.getName();

                onLoginSuccess(LoginType.TWITTER,username,null,null,user.getProfileImageUrlHttps().toString());

            } catch (Exception e) {
                Log.e("Twitter Login Failed", e.getMessage());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Saving user information, after user is authenticated for the first time.
     * You don't need to show user to login, until user has a valid access toen
     */
    private void saveTwitterInfo(AccessToken accessToken) {
        long userID = accessToken.getUserId();
        User user;
        try {
            user = twitter.showUser(userID);
            String username = user.getName();
			/* Storing oAuth tokens to shared preferences */
            SharedPreferences.Editor e = mSharedPreferences.edit();
            e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
            e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
            e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
            e.putString(PREF_USER_NAME, username);
            e.commit();

        } catch (TwitterException e1) {
            e1.printStackTrace();
        }
    }

    private void setLogout() {
        fbLoginBtn.setVisibility(View.VISIBLE);
        gmailLoginBtn.setVisibility(View.VISIBLE);
        twitterLoginBtn.setVisibility(View.VISIBLE);
        fbLoginBtn.setText(getString(R.string.fb_login));
        gmailLoginBtn.setText(getString(R.string.gmail_login));
        twitterLoginBtn.setText(getString(R.string.twitter_login));
    }


    @Override
    public void onLoginSuccess(LoginType loginType, String name, String email, String phno, String thumburl) {
        progressDialog.dismiss();
        System.out.println("pref:" + mSharedPreferences + " " + loginType);
        mSharedPreferences.edit().putString("name", name).putString("email", email)
                .putString("phno", phno).putString("thumbnail", thumburl).putInt("loginType", loginType.ordinal()).commit();
        initProfileData();
        initLoginBtn(loginType);
        if (loginType == LoginType.TWITTER) {
            twitterLoginBtn.setText(getString(R.string.twitter_logout));
        } else if (loginType == LoginType.GMAIL) {
            gmailLoginBtn.setText(getString(R.string.gmail_logout));
        }
    }

    @Override
    public void onLoginFailed(LoginType loginType) {
        Toast.makeText(this, "Login failed .Plz try again", Toast.LENGTH_LONG).show();
        setLogout();
    }

    public boolean isTwitterLoggedInAlready() {
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }
}
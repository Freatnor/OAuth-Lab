package com.example.freatnor.oauth_lab;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    WebView mWebView;

    private SharedPreferences mPreferences;

    private final String YOUR_AUTHORIZATION_URL = "https://api.twitter.com/oauth/request_token"; //ENTER YOUR AUTHORIZATION URL HERE

    private final String USER_LOGIN_URL = "https://api.twitter.com/oauth/authenticate?oauth_token=";
    private final String CALLBACK_STRING = "oauth_callback_confirmed=";
    private final String VERIFIER_STRING = "oauth_verifier=";
    private final String OAUTH_STRING = "oauth_token=";
    private final String OAUTH_SECRET_STRING = "oauth_token_secret=";

    private static final String TAG = "LoginActivity";

    private String mAuthorizationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mWebView = (WebView) findViewById(R.id.webview);

        mPreferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);

        if(!mPreferences.contains(TwitterAppData.TRUE_OAUTH_TOKEN) || !mPreferences.contains(TwitterAppData.TRUE_OAUTH_TOKEN_SECRET)) {
            getOAuthTokens();
        }



    }

    //change for the initial oauth request
    private void getOAuthTokens() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(TwitterAppData.REQUEST_TOKEN_URL)
                .addHeader("authorization", TwitterAppData.buildAuthorizationHeader())
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .post(null)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: Failed Token request", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Bad or empty response. Unexpected Code " + response);
                }
                String body = response.body().string();
                Log.i(TAG, "onResponse: " + body);
                String token = "";
                String secret = "";
                if(body.contains(OAUTH_STRING)&&body.contains(OAUTH_SECRET_STRING)){
                    //get the new token and secret
                    int tokenIndex = body.indexOf(OAUTH_STRING);
                    int secretIndex = body.indexOf(OAUTH_SECRET_STRING);
                    TwitterAppData.OAUTH_TOKEN = body.substring(tokenIndex+OAUTH_STRING.length(), secretIndex - 1);
                    TwitterAppData.OAUTH_TOKEN_SECRET = body.substring(secretIndex + OAUTH_SECRET_STRING.length(), CALLBACK_STRING.length() - 1);

                    mWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            if(url.contains(VERIFIER_STRING)){ //CHECKING TO SEE IF THE URL WE HAVE IS THE ONE WE WANT
                                Log.i(TAG, "shouldOverrideUrlLoading: " + url);

                                int index = url.indexOf(VERIFIER_STRING);
                                Log.i(TAG, url.substring(index+1+VERIFIER_STRING.length()));

                                //STRIPPING AWAY THE URL AND ONLY KEEPING THE CODE
                                String code = url.substring(index+VERIFIER_STRING.length());
                                getAccessToken(code);
                                return true;
                            }
                            else {
                                return false;
                            }
                        }

                    });
                    mWebView.loadUrl(USER_LOGIN_URL + TwitterAppData.OAUTH_TOKEN); //WHAT DO YOU THINK THE URL SHOULD BE?

                }
                else{
                    Log.e(TAG, "onResponse: bad response, no token or secret");
                }
            }
        });

    }

    /**
     * @param verifier THE OAUTH_VERIFIER WE RETRIEVED FROM THE REDIRECT URL
     */
    private void getAccessToken(String verifier){
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("oauth_verifier", verifier)
                .build();
        Request request = new Request.Builder()
                .url("https://api.twitter.com/oauth/access_token")
                .addHeader("authorization", TwitterAppData.buildAuthorizationHeader())
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("content-length", "" + ("oauth_verifier".length() + verifier.length() + 1))
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: Failed Token request", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Bad or empty response. Unexpected Code " + response);
                }
                String body = response.body().string();
                Log.i(TAG, "onResponse: " + body);
                String token = "";
                String secret = "";
                if(body.contains(OAUTH_STRING)&&body.contains(OAUTH_SECRET_STRING)){
                    //get the new token and secret
                    int tokenIndex = body.indexOf(OAUTH_STRING);
                    int secretIndex = body.indexOf(OAUTH_SECRET_STRING);
                    TwitterAppData.TRUE_OAUTH_TOKEN = body.substring(tokenIndex+OAUTH_STRING.length(), secretIndex - 1);
                    TwitterAppData.TRUE_OAUTH_TOKEN_SECRET = body.substring(secretIndex + OAUTH_SECRET_STRING.length());

                }
                else{
                    Log.e(TAG, "onResponse: bad response, no token or secret");
                }

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });

    }


}
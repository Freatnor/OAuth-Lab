package com.example.freatnor.oauth_lab;

import android.content.Intent;
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

    private final String YOUR_AUTHORIZATION_URL = "https://api.twitter.com/oauth/request_token"; //ENTER YOUR AUTHORIZATION URL HERE

    private final String USER_LOGIN_URL = "https://api.twitter.com/oauth/authenticate?oauth_token=";
    private final String VERIFIER_STRING = "oauth_verifier=";

    private static final String TAG = "LoginActivity";

    private String mAuthorizationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mWebView = (WebView) findViewById(R.id.webview);
        
        getOAuthTokens():

        mWebView.setWebViewClient(new WebViewClient() {

            /*
                YOU CAN READ THE DOCUMENTATION ON THIS METHOD TO FIND OUT WHAT IT DOES

                IN SHORT, IT WAITS FOR A URL REQUEST TO HAPPEN AND THEN TRIES TO INTERCEPT IT TO DO
                SOMETHING ELSE OTHER THAN LOADING A WEB PAGE.

                WE NEED THIS SO WE CAN HANDLE THE REDIRECT URL WHEN IT COMES THROUGH.
            */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.contains(VERIFIER_STRING)){ //CHECKING TO SEE IF THE URL WE HAVE IS THE ONE WE WANT
                    Log.i(TAG, "shouldOverrideUrlLoading: " + url);

                    int index = url.indexOf(VERIFIER_STRING);
                    Log.i(TAG, url.substring(index+1+VERIFIER_STRING.length()));

                    //STRIPPING AWAY THE URL AND ONLY KEEPING THE CODE
                    String code = url.substring(index+1+VERIFIER_STRING.length());
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

    private void getOAuthTokens() {
    }

    /**
     * WE WILL WORK ON THIS TOGETHER
     * @param verifier THE OAUTH_VERIFIER WE RETRIEVED FROM THE REDIRECT URL
     */
    private void getAccessToken(String verifier){
        //WE'LL WORK ON THIS TOGETHER
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
                try {
                    JSONObject obj = new JSONObject(body);
                    token = obj.getString("access_token");
                    Log.i(TAG, "onResponse: parsed token - "+token);
                } catch (JSONException e){
                    Log.e(TAG, "onResponse: Bad JSON " + response + " " + body, e);
                }

                startActivity(new Intent(LoginActivity.this, MainActivity.class).putExtra("Authorization", token));
            }
        });

    }


}
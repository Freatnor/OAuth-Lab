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

public class LoginActivity extends AppCompatActivity {
    WebView mWebView;

    private final String YOUR_AUTHORIZATION_URL = "https://api.twitter.com/oauth/request_token"; //ENTER YOUR AUTHORIZATION URL HERE
    private final String AUTHORIZATION_STRING = "OAuth ";

    private static final String TAG = "LoginActivity";

    private String mAuthorizationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mWebView = (WebView) findViewById(R.id.webview);

        mWebView.setWebViewClient(new WebViewClient() {

            /*
                YOU CAN READ THE DOCUMENTATION ON THIS METHOD TO FIND OUT WHAT IT DOES

                IN SHORT, IT WAITS FOR A URL REQUEST TO HAPPEN AND THEN TRIES TO INTERCEPT IT TO DO
                SOMETHING ELSE OTHER THAN LOADING A WEB PAGE.

                WE NEED THIS SO WE CAN HANDLE THE REDIRECT URL WHEN IT COMES THROUGH.
            */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.contains("code=")){ //CHECKING TO SEE IF THE URL WE HAVE IS THE ONE WE WANT
                    Log.i(TAG, "shouldOverrideUrlLoading: " + url);

                    int index = url.indexOf("=");
                    Log.i(TAG, url.substring(index+1));

                    //STRIPPING AWAY THE URL AND ONLY KEEPING THE CODE
                    String code = url.substring(index+1);
                    getAccessToken(code);
                    return true;
                }
                else {
                    return false;
                }
            }

        });
        mWebView.loadUrl(YOUR_AUTHORIZATION_URL); //WHAT DO YOU THINK THE URL SHOULD BE?

    }

    /**
     * WE WILL WORK ON THIS TOGETHER
     * @param code THE CODE WE RETRIEVED FROM THE REDIRECT URL
     */
    private void getAccessToken(String code){
        //WE'LL WORK ON THIS TOGETHER
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("client_id", InstagramAppData.CLIENT_ID)
                .add("client_secret", InstagramAppData.CLIENT_SECRET)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", InstagramAppData.CALLBACK_URL)
                .add("code", code)
                .build();
        Request request = new Request.Builder()
                .url("https://api.instagram.com/oauth/access_token")
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
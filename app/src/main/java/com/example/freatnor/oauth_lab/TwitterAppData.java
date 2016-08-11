package com.example.freatnor.oauth_lab;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Jonathan Taylor on 8/4/16.
 */
public class TwitterAppData {

    private static final String TAG = "TwitterAppData";

    public static final String CLIENT_ID = "PVdY7UijGBN389sDyAFVLqaHy";
    public static final String CLIENT_SECRET = "WzgC7asTfwCUIGW4yQSF1gauQwB56eOD1ooLRZXKg8AYQpqjAe";
    public static final String CALLBACK_URL = "https://callback";

    public static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/access_token";

    public static final String AUTHORIZATION_HEADER_PREFIX = "OAuth ";

    public static final String OAUTH_VERSION = "1.0";
    public static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";

    public static final String TRUE_OAUTH_TOKEN = "oauth_token";
    public static final String TRUE_OAUTH_TOKEN_SECRET = "oauth_token_secret";

    private static SharedPreferences mPreferences;



    //method for building the authorization header, needs a sorted map of all the normal parameters and body
    //for the signature creation
    public static String buildAuthorizationHeader(boolean isInitialRequest, TreeMap<String, String> initialParameters,
                                                  String method, String url, Context context){

        mPreferences = context.getSharedPreferences(context.getString(R.string.shared_preferences),
                Context.MODE_PRIVATE);

        String finalHeader = AUTHORIZATION_HEADER_PREFIX;

        //setup a sorted map to hold our parameters for the Authorization Header
        TreeMap<String, String> parameters = initialParameters;
        parameters.put("method", method);
        parameters.put("url", url);

        //parameters for the signature...
        parameters.put("oauth_consumer_key", CLIENT_ID);
        parameters.put("oauth_signature_method", OAUTH_SIGNATURE_METHOD);
        parameters.put("oauth_version", OAUTH_VERSION);

        //create the timestamp and later use it to create the nonce
        long timestamp = System.currentTimeMillis();
        parameters.put("oauth_timestamp", "" + timestamp);

        //if it's not the initial request put the oauth token, otherwise do the
        if(isInitialRequest){
            parameters.put("oauth_callback", CALLBACK_URL);
        } else {
            parameters.put("oauth_token", mPreferences.getString(TRUE_OAUTH_TOKEN, null));
        }

        //start of try block for all the encodings....
        try{
            //loop through the map as an iterator and encode all the current strings
            Iterator<String> iter = parameters.keySet().iterator();
            while(iter.hasNext()){
                String key = iter.next();
                parameters.put(key, URLEncoder.encode(parameters.get(key), "UTF-8"));
            }

            //create the encoded nonce
            parameters.put("oauth_nonce", createNonce(timestamp));

            //create the encoded signing key
            parameters.put("signing_key", createSigningKey());

            //create the signature
             createSignature(parameters);


        } catch(UnsupportedEncodingException e){
            Log.e(TAG, "buildAuthorizationHeader: something happened while encoding the parameters", e);
        }

        Iterator<String> iter = parameters.keySet().iterator();
        while(iter.hasNext()){
            String key = iter.next();
            //if the key is for oauth
            if(key.contains("oauth")){
                finalHeader += (key + "=" + parameters.get(key) + ", ");
            }
        }


    }

    private static void createSignature(TreeMap<String, String> parameters) throws UnsupportedEncodingException {
        String baseSignature = parameters.get("method") + "&" + parameters.get("url") + "&";
        String signature;

        //loop through and concatenate
        Iterator<String> iter = parameters.keySet().iterator();
        while(iter.hasNext()){
            String key = iter.next();
            if(!key.equals("method") && !key.equals("url") && !key.equals("signing_key")){
                baseSignature += (key + "=" + parameters.get(key) + "&";
            }
        }
        baseSignature = baseSignature.substring(0, baseSignature.length() -2);
        Log.d(TAG, "createSignature: created base signature - " + baseSignature);

        //HmacUtils.hmacSha1Hex(key, string_to_sign);
        try {
            SecretKeySpec secretKey = new SecretKeySpec(parameters.get("signing_key").getBytes("UTF-8"), "HmacSHA1");
            Log.d(TAG, "createSignature: secret key - " + secretKey.toString());
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKey);
            byte[] hmacData = mac.doFinal(baseSignature.getBytes("UTF-8"));
            parameters.put("oauth_signature", Base64.encodeToString(hmacData, Base64.NO_WRAP));
        } catch (NoSuchAlgorithmException e){
            Log.e(TAG, "createSignature: bad algorithm", e);
        } catch (InvalidKeyException e){
            Log.e(TAG, "createSignature: invalid key", e);
        }

        parameters.put("oauth_signature", signature);
    }

    private static String createNonce(long time){
        String nonce = Base64.encodeToString(ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(time).array(),Base64.NO_WRAP);
        Log.d(TAG, "createNonce: " + nonce);
        return nonce;
    }

    private static String createSigningKey() throws UnsupportedEncodingException{
        String result = "";
        result += URLEncoder.encode(CLIENT_SECRET, "UTF-8") + "&";
        if(mPreferences.contains(TRUE_OAUTH_TOKEN_SECRET)){
            result += URLEncoder.encode(mPreferences.getString(TRUE_OAUTH_TOKEN_SECRET, null), "UTF-8");
        }
        Log.d(TAG, "createSigningKey: " + result);
        return result;
    }


}

package com.douyin.streaming.douyin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.douyin.streaming.network.DouyinAPI;
import com.douyin.streaming.network.DouyinAPIResponse;
import com.douyin.streaming.utils.EncryptionUtils;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DouyinSDKManager {
    private static final String TAG = "DouyinSDKManager";
    private static final String PREF_NAME = "douyin_sdk_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_STREAM_KEY = "stream_key";
    private static final String KEY_RTMP_URL = "rtmp_url";
    private static final String KEY_USER_ID = "user_id";
    
    private Context context;
    private SharedPreferences prefs;
    private DouyinAPI douyinAPI;
    private String appId;
    private String appSecret;
    
    public interface LoginCallback {
        void onSuccess(String accessToken, String refreshToken);
        void onFailure(String error);
    }
    
    public interface StreamKeyCallback {
        void onSuccess(String streamKey, String rtmpUrl);
        void onFailure(String error);
    }
    
    public DouyinSDKManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.douyinAPI = DouyinAPI.create();
        
        // 从配置中获取应用ID和密钥
        this.appId = getAppId();
        this.appSecret = getAppSecret();
    }
    
    private String getAppId() {
        try {
            return context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0)
                    .metaData.getString("DOUYIN_APP_ID");
        } catch (Exception e) {
            Log.e(TAG, "获取应用ID失败", e);
            return "default_app_id";
        }
    }
    
    private String getAppSecret() {
        try {
            return context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), 0)
                    .metaData.getString("DOUYIN_APP_SECRET");
        } catch (Exception e) {
            Log.e(TAG, "获取应用密钥失败", e);
            return "default_app_secret";
        }
    }
    
    public boolean isLoggedIn() {
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        return accessToken != null && !accessToken.isEmpty();
    }
    
    public boolean hasStreamKey() {
        String streamKey = prefs.getString(KEY_STREAM_KEY, null);
        return streamKey != null && !streamKey.isEmpty();
    }
    
    public void login(LoginCallback callback) {
        if (isLoggedIn()) {
            callback.onSuccess(
                prefs.getString(KEY_ACCESS_TOKEN, ""),
                prefs.getString(KEY_REFRESH_TOKEN, "")
            );
            return;
        }
        
        // 模拟登录过程（实际应用中需要调用抖音OAuth接口）
        performLogin(callback);
    }
    
    private void performLogin(LoginCallback callback) {
        try {
            // 构建登录请求参数
            JSONObject loginParams = new JSONObject();
            loginParams.put("app_id", appId);
            loginParams.put("app_secret", appSecret);
            loginParams.put("grant_type", "client_credentials");
            loginParams.put("scope", "live_stream");
            
            // 调用抖音登录API
            douyinAPI.login(loginParams.toString()).enqueue(new Callback<DouyinAPIResponse>() {
                @Override
                public void onResponse(Call<DouyinAPIResponse> call, Response<DouyinAPIResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DouyinAPIResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // 解析登录响应
                            try {
                                JSONObject data = new JSONObject(apiResponse.getData());
                                String accessToken = data.getString("access_token");
                                String refreshToken = data.getString("refresh_token");
                                String userId = data.getString("user_id");
                                
                                // 保存登录信息
                                saveLoginInfo(accessToken, refreshToken, userId);
                                
                                callback.onSuccess(accessToken, refreshToken);
                            } catch (Exception e) {
                                Log.e(TAG, "解析登录响应失败", e);
                                callback.onFailure("解析登录响应失败");
                            }
                        } else {
                            callback.onFailure(apiResponse.getMessage());
                        }
                    } else {
                        callback.onFailure("网络请求失败");
                    }
                }
                
                @Override
                public void onFailure(Call<DouyinAPIResponse> call, Throwable t) {
                    Log.e(TAG, "登录请求失败", t);
                    callback.onFailure("网络连接失败: " + t.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "登录过程出错", e);
            callback.onFailure("登录过程出错: " + e.getMessage());
        }
    }
    
    public void getStreamKey(StreamKeyCallback callback) {
        if (!isLoggedIn()) {
            callback.onFailure("请先登录");
            return;
        }
        
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, "");
        if (accessToken.isEmpty()) {
            callback.onFailure("访问令牌无效");
            return;
        }
        
        // 调用获取推流码API
        douyinAPI.getStreamKey("Bearer " + accessToken).enqueue(new Callback<DouyinAPIResponse>() {
            @Override
            public void onResponse(Call<DouyinAPIResponse> call, Response<DouyinAPIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DouyinAPIResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            JSONObject data = new JSONObject(apiResponse.getData());
                            String streamKey = data.getString("stream_key");
                            String rtmpUrl = data.getString("rtmp_url");
                            
                            // 保存推流信息
                            saveStreamInfo(streamKey, rtmpUrl);
                            
                            callback.onSuccess(streamKey, rtmpUrl);
                        } catch (Exception e) {
                            Log.e(TAG, "解析推流码响应失败", e);
                            callback.onFailure("解析推流码响应失败");
                        }
                    } else {
                        callback.onFailure(apiResponse.getMessage());
                    }
                } else {
                    callback.onFailure("获取推流码失败");
                }
            }
            
            @Override
            public void onFailure(Call<DouyinAPIResponse> call, Throwable t) {
                Log.e(TAG, "获取推流码请求失败", t);
                callback.onFailure("网络连接失败: " + t.getMessage());
            }
        });
    }
    
    public void refreshToken(LoginCallback callback) {
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "");
        if (refreshToken.isEmpty()) {
            callback.onFailure("刷新令牌无效");
            return;
        }
        
        // 调用刷新令牌API
        douyinAPI.refreshToken(refreshToken).enqueue(new Callback<DouyinAPIResponse>() {
            @Override
            public void onResponse(Call<DouyinAPIResponse> call, Response<DouyinAPIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DouyinAPIResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            JSONObject data = new JSONObject(apiResponse.getData());
                            String newAccessToken = data.getString("access_token");
                            String newRefreshToken = data.getString("refresh_token");
                            
                            // 更新令牌信息
                            saveLoginInfo(newAccessToken, newRefreshToken, 
                                        prefs.getString(KEY_USER_ID, ""));
                            
                            callback.onSuccess(newAccessToken, newRefreshToken);
                        } catch (Exception e) {
                            Log.e(TAG, "解析刷新令牌响应失败", e);
                            callback.onFailure("解析刷新令牌响应失败");
                        }
                    } else {
                        callback.onFailure(apiResponse.getMessage());
                    }
                } else {
                    callback.onFailure("刷新令牌失败");
                }
            }
            
            @Override
            public void onFailure(Call<DouyinAPIResponse> call, Throwable t) {
                Log.e(TAG, "刷新令牌请求失败", t);
                callback.onFailure("网络连接失败: " + t.getMessage());
            }
        });
    }
    
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_STREAM_KEY);
        editor.remove(KEY_RTMP_URL);
        editor.remove(KEY_USER_ID);
        editor.apply();
        
        Log.d(TAG, "用户已登出");
    }
    
    private void saveLoginInfo(String accessToken, String refreshToken, String userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
        
        Log.d(TAG, "登录信息已保存");
    }
    
    private void saveStreamInfo(String streamKey, String rtmpUrl) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_STREAM_KEY, streamKey);
        editor.putString(KEY_RTMP_URL, rtmpUrl);
        editor.apply();
        
        Log.d(TAG, "推流信息已保存");
    }
    
    public String getCurrentStreamKey() {
        return prefs.getString(KEY_STREAM_KEY, "");
    }
    
    public String getCurrentRtmpUrl() {
        return prefs.getString(KEY_RTMP_URL, "");
    }
    
    public String getCurrentAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }
}


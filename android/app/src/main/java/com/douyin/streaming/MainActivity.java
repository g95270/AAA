package com.douyin.streaming;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.douyin.streaming.douyin.DouyinSDKManager;
import com.douyin.streaming.service.StreamingService;
import com.douyin.streaming.streaming.StreamingManager;
import com.douyin.streaming.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView tvStatus;
    private Button btnLogin;
    private Button btnGetStreamKey;
    private Button btnStartStream;
    private Button btnStopStream;
    private Button btnSettings;
    
    private DouyinSDKManager douyinSDKManager;
    private StreamingManager streamingManager;
    private boolean isStreaming = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initManagers();
        checkPermissions();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        btnLogin = findViewById(R.id.btn_login);
        btnGetStreamKey = findViewById(R.id.btn_get_stream_key);
        btnStartStream = findViewById(R.id.btn_start_stream);
        btnStopStream = findViewById(R.id.btn_stop_stream);
        btnSettings = findViewById(R.id.btn_settings);
        
        btnLogin.setOnClickListener(v -> handleLogin());
        btnGetStreamKey.setOnClickListener(v -> getStreamKey());
        btnStartStream.setOnClickListener(v -> startStreaming());
        btnStopStream.setOnClickListener(v -> stopStreaming());
        btnSettings.setOnClickListener(v -> openSettings());
        
        updateUI();
    }
    
    private void initManagers() {
        douyinSDKManager = new DouyinSDKManager(this);
        streamingManager = new StreamingManager(this);
        
        // 设置状态监听器
        streamingManager.setStatusListener(new StreamingManager.StatusListener() {
            @Override
            public void onStatusChanged(String status) {
                runOnUiThread(() -> {
                    tvStatus.setText(status);
                    Log.d(TAG, "推流状态: " + status);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "推流错误: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "推流错误: " + error);
                });
            }
        });
    }
    
    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissions.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
        }
    }
    
    private void handleLogin() {
        if (douyinSDKManager.isLoggedIn()) {
            Toast.makeText(this, "已经登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");
        
        douyinSDKManager.login(new DouyinSDKManager.LoginCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("已登录");
                    Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    updateUI();
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                    Toast.makeText(MainActivity.this, "登录失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void getStreamKey() {
        if (!douyinSDKManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnGetStreamKey.setEnabled(false);
        btnGetStreamKey.setText("获取中...");
        
        douyinSDKManager.getStreamKey(new DouyinSDKManager.StreamKeyCallback() {
            @Override
            public void onSuccess(String streamKey, String rtmpUrl) {
                runOnUiThread(() -> {
                    btnGetStreamKey.setEnabled(true);
                    btnGetStreamKey.setText("已获取推流码");
                    Toast.makeText(MainActivity.this, "推流码获取成功", Toast.LENGTH_SHORT).show();
                    updateUI();
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    btnGetStreamKey.setEnabled(true);
                    btnGetStreamKey.setText("获取推流码");
                    Toast.makeText(MainActivity.this, "获取推流码失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void startStreaming() {
        if (!douyinSDKManager.hasStreamKey()) {
            Toast.makeText(this, "请先获取推流码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnStartStream.setEnabled(false);
        btnStartStream.setText("推流中...");
        
        // 启动推流服务
        Intent serviceIntent = new Intent(this, StreamingService.class);
        serviceIntent.setAction(StreamingService.ACTION_START_STREAM);
        startForegroundService(serviceIntent);
        
        isStreaming = true;
        updateUI();
        
        Toast.makeText(this, "推流已开始", Toast.LENGTH_SHORT).show();
    }
    
    private void stopStreaming() {
        btnStopStream.setEnabled(false);
        btnStopStream.setText("停止中...");
        
        // 停止推流服务
        Intent serviceIntent = new Intent(this, StreamingService.class);
        serviceIntent.setAction(StreamingService.ACTION_STOP_STREAM);
        startService(serviceIntent);
        
        isStreaming = false;
        updateUI();
        
        Toast.makeText(this, "推流已停止", Toast.LENGTH_SHORT).show();
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void updateUI() {
        boolean isLoggedIn = douyinSDKManager.isLoggedIn();
        boolean hasStreamKey = douyinSDKManager.hasStreamKey();
        
        btnLogin.setText(isLoggedIn ? "已登录" : "登录");
        btnGetStreamKey.setEnabled(isLoggedIn);
        btnGetStreamKey.setText(hasStreamKey ? "已获取推流码" : "获取推流码");
        btnStartStream.setEnabled(hasStreamKey && !isStreaming);
        btnStopStream.setEnabled(isStreaming);
        
        if (isStreaming) {
            tvStatus.setText("推流中...");
        } else if (hasStreamKey) {
            tvStatus.setText("已获取推流码，可以开始推流");
        } else if (isLoggedIn) {
            tvStatus.setText("已登录，请获取推流码");
        } else {
            tvStatus.setText("请先登录抖音账号");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "部分权限被拒绝，应用可能无法正常工作", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isStreaming) {
            stopStreaming();
        }
    }
}


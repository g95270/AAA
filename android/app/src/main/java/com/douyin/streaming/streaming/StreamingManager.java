package com.douyin.streaming.streaming;

import android.content.Context;
import android.util.Log;

import com.douyin.streaming.streaming.protocols.OKBStreamingProtocol;
import com.douyin.streaming.streaming.protocols.VRStreamingProtocol;
import com.douyin.streaming.streaming.protocols.ATFStreamingProtocol;
import com.douyin.streaming.streaming.protocols.ATSStreamingProtocol;
import com.douyin.streaming.streaming.protocols.StreamingProtocol;
import com.douyin.streaming.utils.StreamingConfig;

import java.util.HashMap;
import java.util.Map;

public class StreamingManager {
    private static final String TAG = "StreamingManager";
    
    public enum StreamingType {
        OKB("OKB推流"),
        VR("VR推流"),
        ATF("ATF推流"),
        ATS("ATS推流");
        
        private final String displayName;
        
        StreamingType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public interface StatusListener {
        void onStatusChanged(String status);
        void onError(String error);
    }
    
    private Context context;
    private StreamingConfig config;
    private Map<StreamingType, StreamingProtocol> protocols;
    private StreamingProtocol currentProtocol;
    private StreamingType currentType;
    private StatusListener statusListener;
    private boolean isStreaming = false;
    
    public StreamingManager(Context context) {
        this.context = context;
        this.config = new StreamingConfig();
        initProtocols();
    }
    
    private void initProtocols() {
        protocols = new HashMap<>();
        protocols.put(StreamingType.OKB, new OKBStreamingProtocol(context, config));
        protocols.put(StreamingType.VR, new VRStreamingProtocol(context, config));
        protocols.put(StreamingType.ATF, new ATFStreamingProtocol(context, config));
        protocols.put(StreamingType.ATS, new ATSStreamingProtocol(context, config));
        
        // 设置默认推流方式
        currentType = StreamingType.OKB;
        currentProtocol = protocols.get(currentType);
    }
    
    public void setStreamingType(StreamingType type) {
        if (isStreaming) {
            Log.w(TAG, "推流进行中，无法切换推流方式");
            return;
        }
        
        if (protocols.containsKey(type)) {
            currentType = type;
            currentProtocol = protocols.get(type);
            Log.d(TAG, "推流方式已切换为: " + type.getDisplayName());
            
            if (statusListener != null) {
                statusListener.onStatusChanged("推流方式: " + type.getDisplayName());
            }
        } else {
            Log.e(TAG, "不支持的推流方式: " + type);
        }
    }
    
    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
    }
    
    public void startStreaming(String streamKey, String rtmpUrl) {
        if (isStreaming) {
            Log.w(TAG, "推流已在进行中");
            return;
        }
        
        if (currentProtocol == null) {
            String error = "推流协议未初始化";
            Log.e(TAG, error);
            if (statusListener != null) {
                statusListener.onError(error);
            }
            return;
        }
        
        try {
            Log.d(TAG, "开始推流，方式: " + currentType.getDisplayName());
            if (statusListener != null) {
                statusListener.onStatusChanged("正在启动推流...");
            }
            
            // 配置推流参数
            config.setStreamKey(streamKey);
            config.setRtmpUrl(rtmpUrl);
            config.setStreamingType(currentType);
            
            // 启动推流
            currentProtocol.startStreaming(config, new StreamingProtocol.StreamingCallback() {
                @Override
                public void onStarted() {
                    isStreaming = true;
                    Log.d(TAG, "推流已启动");
                    if (statusListener != null) {
                        statusListener.onStatusChanged("推流已启动 - " + currentType.getDisplayName());
                    }
                }
                
                @Override
                public void onStopped() {
                    isStreaming = false;
                    Log.d(TAG, "推流已停止");
                    if (statusListener != null) {
                        statusListener.onStatusChanged("推流已停止");
                    }
                }
                
                @Override
                public void onError(String error) {
                    isStreaming = false;
                    Log.e(TAG, "推流错误: " + error);
                    if (statusListener != null) {
                        statusListener.onError(error);
                    }
                }
                
                @Override
                public void onStatusUpdate(String status) {
                    if (statusListener != null) {
                        statusListener.onStatusChanged(status);
                    }
                }
            });
            
        } catch (Exception e) {
            String error = "启动推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (statusListener != null) {
                statusListener.onError(error);
            }
        }
    }
    
    public void stopStreaming() {
        if (!isStreaming || currentProtocol == null) {
            Log.w(TAG, "推流未在进行中");
            return;
        }
        
        try {
            Log.d(TAG, "停止推流");
            if (statusListener != null) {
                statusListener.onStatusChanged("正在停止推流...");
            }
            
            currentProtocol.stopStreaming();
            
        } catch (Exception e) {
            String error = "停止推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (statusListener != null) {
                statusListener.onError(error);
            }
        }
    }
    
    public void pauseStreaming() {
        if (!isStreaming || currentProtocol == null) {
            Log.w(TAG, "推流未在进行中");
            return;
        }
        
        try {
            Log.d(TAG, "暂停推流");
            currentProtocol.pauseStreaming();
            
            if (statusListener != null) {
                statusListener.onStatusChanged("推流已暂停");
            }
            
        } catch (Exception e) {
            String error = "暂停推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (statusListener != null) {
                statusListener.onError(error);
            }
        }
    }
    
    public void resumeStreaming() {
        if (!isStreaming || currentProtocol == null) {
            Log.w(TAG, "推流未在进行中");
            return;
        }
        
        try {
            Log.d(TAG, "恢复推流");
            currentProtocol.resumeStreaming();
            
            if (statusListener != null) {
                statusListener.onStatusChanged("推流已恢复");
            }
            
        } catch (Exception e) {
            String error = "恢复推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (statusListener != null) {
                statusListener.onError(error);
            }
        }
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    public StreamingType getCurrentStreamingType() {
        return currentType;
    }
    
    public StreamingConfig getConfig() {
        return config;
    }
    
    public void updateConfig(StreamingConfig newConfig) {
        this.config = newConfig;
        Log.d(TAG, "推流配置已更新");
    }
    
    public void setVideoQuality(int width, int height, int bitrate, int fps) {
        config.setVideoWidth(width);
        config.setVideoHeight(height);
        config.setVideoBitrate(bitrate);
        config.setVideoFps(fps);
        
        Log.d(TAG, String.format("视频质量设置: %dx%d, %dkbps, %dfps", 
                width, height, bitrate, fps));
    }
    
    public void setAudioQuality(int sampleRate, int channels, int bitrate) {
        config.setAudioSampleRate(sampleRate);
        config.setAudioChannels(channels);
        config.setAudioBitrate(bitrate);
        
        Log.d(TAG, String.format("音频质量设置: %dHz, %d声道, %dkbps", 
                sampleRate, channels, bitrate));
    }
    
    public void setNetworkConfig(int timeout, int retryCount, boolean enableAdaptive) {
        config.setNetworkTimeout(timeout);
        config.setRetryCount(retryCount);
        config.setEnableAdaptiveBitrate(enableAdaptive);
        
        Log.d(TAG, String.format("网络配置: 超时%dms, 重试%d次, 自适应比特率%s", 
                timeout, retryCount, enableAdaptive ? "开启" : "关闭"));
    }
    
    public void release() {
        if (isStreaming) {
            stopStreaming();
        }
        
        for (StreamingProtocol protocol : protocols.values()) {
            try {
                protocol.release();
            } catch (Exception e) {
                Log.e(TAG, "释放推流协议失败", e);
            }
        }
        
        protocols.clear();
        currentProtocol = null;
        Log.d(TAG, "推流管理器已释放");
    }
}


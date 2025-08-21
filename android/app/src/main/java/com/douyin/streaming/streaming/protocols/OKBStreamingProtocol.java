package com.douyin.streaming.streaming.protocols;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.douyin.streaming.utils.StreamingConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OKB推流协议实现
 * 使用FFmpeg进行RTMP推流
 */
public class OKBStreamingProtocol implements StreamingProtocol {
    private static final String TAG = "OKBStreamingProtocol";
    
    private Context context;
    private StreamingConfig config;
    private StreamingCallback callback;
    private StreamingStatus status = StreamingStatus.IDLE;
    private StreamingStats stats;
    private FFmpegSession ffmpegSession;
    private ScheduledExecutorService statsExecutor;
    private String rtmpUrl;
    private boolean isPaused = false;
    
    public OKBStreamingProtocol(Context context, StreamingConfig config) {
        this.context = context;
        this.config = config;
        this.stats = new StreamingStats();
    }
    
    @Override
    public void startStreaming(StreamingConfig config, StreamingCallback callback) {
        this.config = config;
        this.callback = callback;
        
        if (status == StreamingStatus.STREAMING || status == StreamingStatus.CONNECTING) {
            Log.w(TAG, "推流已在进行中");
            return;
        }
        
        try {
            updateStatus(StreamingStatus.CONNECTING);
            if (callback != null) {
                callback.onStatusUpdate("正在连接推流服务器...");
            }
            
            // 构建推流URL
            rtmpUrl = config.getRtmpUrl() + config.getStreamKey();
            Log.d(TAG, "推流URL: " + rtmpUrl);
            
            // 构建FFmpeg命令
            String ffmpegCommand = buildFFmpegCommand();
            Log.d(TAG, "FFmpeg命令: " + ffmpegCommand);
            
            // 执行FFmpeg推流
            ffmpegSession = FFmpegKit.executeAsync(ffmpegCommand, 
                    session -> handleFFmpegResult(session),
                    log -> handleFFmpegLog(log),
                    statistics -> handleFFmpegStatistics(statistics));
            
            // 启动统计信息收集
            startStatsCollection();
            
        } catch (Exception e) {
            String error = "启动OKB推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            updateStatus(StreamingStatus.ERROR);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public void stopStreaming() {
        if (status == StreamingStatus.IDLE || status == StreamingStatus.DISCONNECTED) {
            Log.w(TAG, "推流未在进行中");
            return;
        }
        
        try {
            Log.d(TAG, "停止OKB推流");
            
            // 停止FFmpeg会话
            if (ffmpegSession != null) {
                FFmpegKit.cancel(ffmpegSession.getSessionId());
                ffmpegSession = null;
            }
            
            // 停止统计信息收集
            stopStatsCollection();
            
            // 更新状态
            updateStatus(StreamingStatus.DISCONNECTED);
            
            if (callback != null) {
                callback.onStopped();
            }
            
        } catch (Exception e) {
            String error = "停止OKB推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public void pauseStreaming() {
        if (status != StreamingStatus.STREAMING) {
            Log.w(TAG, "推流未在进行中，无法暂停");
            return;
        }
        
        try {
            Log.d(TAG, "暂停OKB推流");
            isPaused = true;
            updateStatus(StreamingStatus.PAUSED);
            
            if (callback != null) {
                callback.onStatusUpdate("推流已暂停");
            }
            
        } catch (Exception e) {
            String error = "暂停OKB推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public void resumeStreaming() {
        if (status != StreamingStatus.PAUSED) {
            Log.w(TAG, "推流未暂停，无法恢复");
            return;
        }
        
        try {
            Log.d(TAG, "恢复OKB推流");
            isPaused = false;
            updateStatus(StreamingStatus.STREAMING);
            
            if (callback != null) {
                callback.onStatusUpdate("推流已恢复");
            }
            
        } catch (Exception e) {
            String error = "恢复OKB推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public StreamingStatus getStatus() {
        return status;
    }
    
    @Override
    public StreamingStats getStats() {
        return stats;
    }
    
    @Override
    public void setConfig(StreamingConfig config) {
        this.config = config;
    }
    
    @Override
    public void release() {
        stopStreaming();
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.shutdown();
        }
    }
    
    /**
     * 构建FFmpeg推流命令
     */
    private String buildFFmpegCommand() {
        StringBuilder command = new StringBuilder();
        
        // 输入源（摄像头和麦克风）
        command.append("-f android_camera -i 0 "); // 前置摄像头
        command.append("-f android_microphone -i 1 "); // 麦克风
        
        // 视频编码设置
        command.append("-c:v libx264 "); // H.264编码
        command.append("-preset ultrafast "); // 最快编码速度
        command.append("-tune zerolatency "); // 零延迟调优
        command.append("-profile:v baseline "); // 基线配置
        command.append("-level 3.0 "); // H.264级别
        
        // 视频参数
        command.append("-s ").append(config.getVideoWidth()).append("x").append(config.getVideoHeight()).append(" ");
        command.append("-r ").append(config.getVideoFps()).append(" ");
        command.append("-b:v ").append(config.getVideoBitrate()).append("k ");
        command.append("-maxrate ").append(config.getVideoBitrate()).append("k ");
        command.append("-bufsize ").append(config.getVideoBitrate() * 2).append("k ");
        
        // 音频编码设置
        command.append("-c:a aac "); // AAC音频编码
        command.append("-b:a ").append(config.getAudioBitrate()).append("k ");
        command.append("-ar ").append(config.getAudioSampleRate()).append(" ");
        command.append("-ac ").append(config.getAudioChannels()).append(" ");
        
        // 网络设置
        command.append("-f flv "); // 输出格式为FLV
        command.append("-rtmp_live live "); // 直播模式
        command.append("-rtmp_buffer 5000 "); // 缓冲区大小
        
        // 输出URL
        command.append(rtmpUrl);
        
        return command.toString();
    }
    
    /**
     * 处理FFmpeg执行结果
     */
    private void handleFFmpegResult(FFmpegSession session) {
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            Log.d(TAG, "OKB推流成功完成");
            updateStatus(StreamingStatus.DISCONNECTED);
            if (callback != null) {
                callback.onStopped();
            }
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            Log.d(TAG, "OKB推流被取消");
            updateStatus(StreamingStatus.DISCONNECTED);
            if (callback != null) {
                callback.onStopped();
            }
        } else {
            String error = "OKB推流失败: " + session.getFailStackTrace();
            Log.e(TAG, error);
            updateStatus(StreamingStatus.ERROR);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    /**
     * 处理FFmpeg日志
     */
    private void handleFFmpegLog(com.arthenica.ffmpegkit.Log log) {
        String message = log.getMessage();
        Log.d(TAG, "FFmpeg日志: " + message);
        
        // 检测连接状态
        if (message.contains("Connection established")) {
            updateStatus(StreamingStatus.CONNECTED);
            if (callback != null) {
                callback.onStatusUpdate("已连接到推流服务器");
            }
        } else if (message.contains("Streaming started")) {
            updateStatus(StreamingStatus.STREAMING);
            if (callback != null) {
                callback.onStarted();
            }
        } else if (message.contains("Connection lost") || message.contains("Connection failed")) {
            updateStatus(StreamingStatus.ERROR);
            if (callback != null) {
                callback.onError("推流连接丢失");
            }
        }
    }
    
    /**
     * 处理FFmpeg统计信息
     */
    private void handleFFmpegStatistics(com.arthenica.ffmpegkit.Statistics statistics) {
        if (statistics != null) {
            stats.setBytesSent(statistics.getSize());
            stats.setFramesSent(statistics.getFrameNumber());
            stats.setVideoFramesSent(statistics.getVideoFrameNumber());
            stats.setAudioFramesSent(statistics.getAudioFrameNumber());
            stats.setDuration(statistics.getTime());
            
            // 计算实时比特率和帧率
            if (statistics.getTime() > 0) {
                double currentBitrate = (statistics.getSize() * 8.0 / 1000) / (statistics.getTime() / 1000.0);
                stats.setBitrate(currentBitrate);
                
                if (statistics.getTime() > 1000) { // 1秒后开始计算帧率
                    double currentFps = (statistics.getFrameNumber() * 1000.0) / statistics.getTime();
                    stats.setFps(currentFps);
                }
            }
        }
    }
    
    /**
     * 启动统计信息收集
     */
    private void startStatsCollection() {
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.shutdown();
        }
        
        statsExecutor = Executors.newSingleThreadScheduledExecutor();
        statsExecutor.scheduleAtFixedRate(() -> {
            if (status == StreamingStatus.STREAMING && !isPaused) {
                stats.setRunningTime(getStats().getRunningTime());
                
                if (callback != null) {
                    String statusText = String.format("推流中 - 比特率: %.1fkbps, 帧率: %.1ffps, 时长: %ds",
                            stats.getBitrate(), stats.getFps(), stats.getRunningTime());
                    callback.onStatusUpdate(statusText);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        stats.setStartTime(System.currentTimeMillis());
    }
    
    /**
     * 停止统计信息收集
     */
    private void stopStatsCollection() {
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.shutdown();
            statsExecutor = null;
        }
    }
    
    /**
     * 更新推流状态
     */
    private void updateStatus(StreamingStatus newStatus) {
        StreamingStatus oldStatus = this.status;
        this.status = newStatus;
        
        Log.d(TAG, String.format("OKB推流状态变化: %s -> %s", 
                oldStatus.getDescription(), newStatus.getDescription()));
    }
}


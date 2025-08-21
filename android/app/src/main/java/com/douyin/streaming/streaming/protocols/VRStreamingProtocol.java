package com.douyin.streaming.streaming.protocols;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.douyin.streaming.utils.StreamingConfig;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * VR推流协议实现
 * 支持VR全景推流，使用Google VR SDK和FFmpeg
 */
public class VRStreamingProtocol implements StreamingProtocol {
    private static final String TAG = "VRStreamingProtocol";
    
    private Context context;
    private StreamingConfig config;
    private StreamingCallback callback;
    private StreamingStatus status = StreamingStatus.IDLE;
    private StreamingStats stats;
    private FFmpegSession ffmpegSession;
    private ScheduledExecutorService statsExecutor;
    private String rtmpUrl;
    private boolean isPaused = false;
    
    // VR相关参数
    private int vrMode = 0; // 0: 单目, 1: 双目, 2: 全景
    private int vrProjection = 0; // 0: 等距矩形, 1: 立方体, 2: 等角
    private int vrResolution = 4096; // VR视频分辨率
    private boolean enableStereoscopic = true; // 启用立体视觉
    
    public VRStreamingProtocol(Context context, StreamingConfig config) {
        this.context = context;
        this.config = config;
        this.stats = new StreamingStats();
        initVRConfig();
    }
    
    private void initVRConfig() {
        // 根据设备性能调整VR参数
        if (config.getVideoWidth() >= 3840 && config.getVideoHeight() >= 2160) {
            vrResolution = 8192; // 4K设备使用更高分辨率
        } else if (config.getVideoWidth() >= 1920 && config.getVideoHeight() >= 1080) {
            vrResolution = 4096; // 1080p设备使用标准分辨率
        } else {
            vrResolution = 2048; // 低端设备使用较低分辨率
        }
        
        // 根据推流配置选择VR模式
        if (config.getVideoWidth() > config.getVideoHeight() * 2) {
            vrMode = 2; // 超宽比例使用全景模式
        } else if (config.getVideoWidth() > config.getVideoHeight() * 1.5) {
            vrMode = 1; // 宽比例使用双目模式
        } else {
            vrMode = 0; // 标准比例使用单目模式
        }
    }
    
    @Override
    public void startStreaming(StreamingConfig config, StreamingCallback callback) {
        this.config = config;
        this.callback = callback;
        
        if (status == StreamingStatus.STREAMING || status == StreamingStatus.CONNECTING) {
            Log.w(TAG, "VR推流已在进行中");
            return;
        }
        
        try {
            updateStatus(StreamingStatus.CONNECTING);
            if (callback != null) {
                callback.onStatusUpdate("正在启动VR推流...");
            }
            
            // 构建推流URL
            rtmpUrl = config.getRtmpUrl() + config.getStreamKey();
            Log.d(TAG, "VR推流URL: " + rtmpUrl);
            
            // 构建VR推流FFmpeg命令
            String ffmpegCommand = buildVRFFmpegCommand();
            Log.d(TAG, "VR FFmpeg命令: " + ffmpegCommand);
            
            // 执行FFmpeg推流
            ffmpegSession = FFmpegKit.executeAsync(ffmpegCommand, 
                    session -> handleFFmpegResult(session),
                    log -> handleFFmpegLog(log),
                    statistics -> handleFFmpegStatistics(statistics));
            
            // 启动统计信息收集
            startStatsCollection();
            
        } catch (Exception e) {
            String error = "启动VR推流失败: " + e.getMessage();
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
            Log.w(TAG, "VR推流未在进行中");
            return;
        }
        
        try {
            Log.d(TAG, "停止VR推流");
            
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
            String error = "停止VR推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public void pauseStreaming() {
        if (status != StreamingStatus.STREAMING) {
            Log.w(TAG, "VR推流未在进行中，无法暂停");
            return;
        }
        
        try {
            Log.d(TAG, "暂停VR推流");
            isPaused = true;
            updateStatus(StreamingStatus.PAUSED);
            
            if (callback != null) {
                callback.onStatusUpdate("VR推流已暂停");
            }
            
        } catch (Exception e) {
            String error = "暂停VR推流失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }
    
    @Override
    public void resumeStreaming() {
        if (status != StreamingStatus.PAUSED) {
            Log.w(TAG, "VR推流未暂停，无法恢复");
            return;
        }
        
        try {
            Log.d(TAG, "恢复VR推流");
            isPaused = false;
            updateStatus(StreamingStatus.STREAMING);
            
            if (callback != null) {
                callback.onStatusUpdate("VR推流已恢复");
            }
            
        } catch (Exception e) {
            String error = "恢复VR推流失败: " + e.getMessage();
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
        initVRConfig();
    }
    
    @Override
    public void release() {
        stopStreaming();
        if (statsExecutor != null && !statsExecutor.isShutdown()) {
            statsExecutor.shutdown();
        }
    }
    
    /**
     * 构建VR推流FFmpeg命令
     */
    private String buildVRFFmpegCommand() {
        StringBuilder command = new StringBuilder();
        
        // VR输入源配置
        if (vrMode == 2) { // 全景模式
            command.append("-f android_camera -i 0 "); // 前置摄像头
            command.append("-f android_camera -i 1 "); // 后置摄像头
        } else if (vrMode == 1) { // 双目模式
            command.append("-f android_camera -i 0 "); // 前置摄像头
            command.append("-f android_camera -i 1 "); // 后置摄像头
        } else { // 单目模式
            command.append("-f android_camera -i 0 "); // 前置摄像头
        }
        
        // 音频输入
        command.append("-f android_microphone -i 2 "); // 麦克风
        
        // VR视频处理滤镜
        command.append("-filter_complex \"");
        
        if (vrMode == 2) { // 全景模式
            // 拼接两个摄像头画面为全景
            command.append("[0:v][1:v]hstack=inputs=2[panorama];");
            // 应用等距矩形投影
            command.append("[panorama]v360=input=flat:output=equirect:interpolation=cubic[vrout]");
        } else if (vrMode == 1) { // 双目模式
            // 左右眼画面处理
            command.append("[0:v]scale=").append(vrResolution/2).append(":").append(vrResolution).append("[left];");
            command.append("[1:v]scale=").append(vrResolution/2).append(":").append(vrResolution).append("[right];");
            command.append("[left][right]hstack=inputs=2[stereo]");
        } else { // 单目模式
            // 单目VR处理
            command.append("[0:v]scale=").append(vrResolution).append(":").append(vrResolution).append("[vrout]");
        }
        
        command.append("\" ");
        
        // VR视频编码设置
        command.append("-c:v libx264 "); // H.264编码
        command.append("-preset ultrafast "); // 最快编码速度
        command.append("-tune zerolatency "); // 零延迟调优
        command.append("-profile:v high "); // 高配置支持VR
        command.append("-level 4.1 "); // H.264级别
        
        // VR视频参数
        if (vrMode == 2) { // 全景模式
            command.append("-s ").append(vrResolution * 2).append("x").append(vrResolution).append(" ");
        } else if (vrMode == 1) { // 双目模式
            command.append("-s ").append(vrResolution).append("x").append(vrResolution).append(" ");
        } else { // 单目模式
            command.append("-s ").append(vrResolution).append("x").append(vrResolution).append(" ");
        }
        
        command.append("-r ").append(config.getVideoFps()).append(" ");
        command.append("-b:v ").append(config.getVideoBitrate()).append("k ");
        command.append("-maxrate ").append(config.getVideoBitrate()).append("k ");
        command.append("-bufsize ").append(config.getVideoBitrate() * 2).append("k ");
        
        // VR特殊参数
        command.append("-x264opts keyint=").append(config.getVideoFps() * 2).append(":min-keyint=").append(config.getVideoFps()).append(" ");
        command.append("-g ").append(config.getVideoFps() * 2).append(" "); // GOP大小
        
        // 音频编码设置
        command.append("-c:a aac "); // AAC音频编码
        command.append("-b:a ").append(config.getAudioBitrate()).append("k ");
        command.append("-ar ").append(config.getAudioSampleRate()).append(" ");
        command.append("-ac ").append(config.getAudioChannels()).append(" ");
        
        // VR元数据
        command.append("-metadata:s:v:0 spherical-video=1 ");
        if (vrMode == 1) {
            command.append("-metadata:s:v:0 stereo-mode=left_right ");
        }
        
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
            Log.d(TAG, "VR推流成功完成");
            updateStatus(StreamingStatus.DISCONNECTED);
            if (callback != null) {
                callback.onStopped();
            }
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            Log.d(TAG, "VR推流被取消");
            updateStatus(StreamingStatus.DISCONNECTED);
            if (callback != null) {
                callback.onStopped();
            }
        } else {
            String error = "VR推流失败: " + session.getFailStackTrace();
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
        Log.d(TAG, "VR FFmpeg日志: " + message);
        
        // 检测连接状态
        if (message.contains("Connection established")) {
            updateStatus(StreamingStatus.CONNECTED);
            if (callback != null) {
                callback.onStatusUpdate("VR推流已连接到服务器");
            }
        } else if (message.contains("Streaming started")) {
            updateStatus(StreamingStatus.STREAMING);
            if (callback != null) {
                callback.onStarted();
            }
        } else if (message.contains("Connection lost") || message.contains("Connection failed")) {
            updateStatus(StreamingStatus.ERROR);
            if (callback != null) {
                callback.onError("VR推流连接丢失");
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
                    String statusText = String.format("VR推流中 - 比特率: %.1fkbps, 帧率: %.1ffps, 时长: %ds, 模式: %s",
                            stats.getBitrate(), stats.getFps(), stats.getRunningTime(), 
                            getVRModeDescription());
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
     * 获取VR模式描述
     */
    private String getVRModeDescription() {
        switch (vrMode) {
            case 0: return "单目";
            case 1: return "双目";
            case 2: return "全景";
            default: return "未知";
        }
    }
    
    /**
     * 更新推流状态
     */
    private void updateStatus(StreamingStatus newStatus) {
        StreamingStatus oldStatus = this.status;
        this.status = newStatus;
        
        Log.d(TAG, String.format("VR推流状态变化: %s -> %s", 
                oldStatus.getDescription(), newStatus.getDescription()));
    }
    
    /**
     * 设置VR模式
     */
    public void setVRMode(int mode) {
        if (mode >= 0 && mode <= 2) {
            this.vrMode = mode;
            Log.d(TAG, "VR模式已设置为: " + getVRModeDescription());
        }
    }
    
    /**
     * 设置VR投影方式
     */
    public void setVRProjection(int projection) {
        if (projection >= 0 && projection <= 2) {
            this.vrProjection = projection;
            Log.d(TAG, "VR投影方式已设置");
        }
    }
    
    /**
     * 设置VR分辨率
     */
    public void setVRResolution(int resolution) {
        if (resolution >= 1024 && resolution <= 16384) {
            this.vrResolution = resolution;
            Log.d(TAG, "VR分辨率已设置为: " + resolution);
        }
    }
}


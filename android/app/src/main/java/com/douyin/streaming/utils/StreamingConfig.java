package com.douyin.streaming.utils;

import com.douyin.streaming.streaming.StreamingManager;

/**
 * 推流配置类
 * 包含所有推流相关的参数设置
 */
public class StreamingConfig {
    
    // 推流基本信息
    private String streamKey = "";
    private String rtmpUrl = "";
    private StreamingManager.StreamingType streamingType = StreamingManager.StreamingType.OKB;
    
    // 视频参数
    private int videoWidth = 1920;
    private int videoHeight = 1080;
    private int videoBitrate = 2500; // kbps
    private int videoFps = 30;
    private String videoCodec = "libx264";
    private String videoPreset = "ultrafast";
    private String videoProfile = "baseline";
    private int videoLevel = 30;
    private int videoGop = 60;
    
    // 音频参数
    private int audioSampleRate = 44100;
    private int audioChannels = 2;
    private int audioBitrate = 128; // kbps
    private String audioCodec = "aac";
    private int audioProfile = 1; // AAC-LC
    
    // 网络参数
    private int networkTimeout = 10000; // ms
    private int retryCount = 3;
    private boolean enableAdaptiveBitrate = true;
    private int bufferSize = 5000; // ms
    private boolean enableLowLatency = true;
    
    // 高级参数
    private boolean enableHardwareAcceleration = true;
    private boolean enableAudioFilter = false;
    private boolean enableVideoFilter = false;
    private String customFFmpegOptions = "";
    
    // 质量预设
    public enum QualityPreset {
        ULTRA_LOW("超低质量", 640, 480, 500, 15),
        LOW("低质量", 854, 480, 800, 24),
        MEDIUM("中等质量", 1280, 720, 1500, 30),
        HIGH("高质量", 1920, 1080, 2500, 30),
        ULTRA_HIGH("超高质量", 2560, 1440, 4000, 30),
        CUSTOM("自定义", 0, 0, 0, 0);
        
        private final String displayName;
        private final int width;
        private final int height;
        private final int bitrate;
        private final int fps;
        
        QualityPreset(String displayName, int width, int height, int bitrate, int fps) {
            this.displayName = displayName;
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
            this.fps = fps;
        }
        
        public String getDisplayName() { return displayName; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getBitrate() { return bitrate; }
        public int getFps() { return fps; }
    }
    
    // 构造函数
    public StreamingConfig() {
        // 使用默认配置
    }
    
    public StreamingConfig(QualityPreset preset) {
        if (preset != QualityPreset.CUSTOM) {
            this.videoWidth = preset.getWidth();
            this.videoHeight = preset.getHeight();
            this.videoBitrate = preset.getBitrate();
            this.videoFps = preset.getFps();
        }
    }
    
    // Getters and Setters
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
    
    public String getRtmpUrl() { return rtmpUrl; }
    public void setRtmpUrl(String rtmpUrl) { this.rtmpUrl = rtmpUrl; }
    
    public StreamingManager.StreamingType getStreamingType() { return streamingType; }
    public void setStreamingType(StreamingManager.StreamingType streamingType) { this.streamingType = streamingType; }
    
    public int getVideoWidth() { return videoWidth; }
    public void setVideoWidth(int videoWidth) { this.videoWidth = videoWidth; }
    
    public int getVideoHeight() { return videoHeight; }
    public void setVideoHeight(int videoHeight) { this.videoHeight = videoHeight; }
    
    public int getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(int videoBitrate) { this.videoBitrate = videoBitrate; }
    
    public int getVideoFps() { return videoFps; }
    public void setVideoFps(int videoFps) { this.videoFps = videoFps; }
    
    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
    
    public String getVideoPreset() { return videoPreset; }
    public void setVideoPreset(String videoPreset) { this.videoPreset = videoPreset; }
    
    public String getVideoProfile() { return videoProfile; }
    public void setVideoProfile(String videoProfile) { this.videoProfile = videoProfile; }
    
    public int getVideoLevel() { return videoLevel; }
    public void setVideoLevel(int videoLevel) { this.videoLevel = videoLevel; }
    
    public int getVideoGop() { return videoGop; }
    public void setVideoGop(int videoGop) { this.videoGop = videoGop; }
    
    public int getAudioSampleRate() { return audioSampleRate; }
    public void setAudioSampleRate(int audioSampleRate) { this.audioSampleRate = audioSampleRate; }
    
    public int getAudioChannels() { return audioChannels; }
    public void setAudioChannels(int audioChannels) { this.audioChannels = audioChannels; }
    
    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int audioBitrate) { this.audioBitrate = audioBitrate; }
    
    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
    
    public int getAudioProfile() { return audioProfile; }
    public void setAudioProfile(int audioProfile) { this.audioProfile = audioProfile; }
    
    public int getNetworkTimeout() { return networkTimeout; }
    public void setNetworkTimeout(int networkTimeout) { this.networkTimeout = networkTimeout; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public boolean isEnableAdaptiveBitrate() { return enableAdaptiveBitrate; }
    public void setEnableAdaptiveBitrate(boolean enableAdaptiveBitrate) { this.enableAdaptiveBitrate = enableAdaptiveBitrate; }
    
    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    
    public boolean isEnableLowLatency() { return enableLowLatency; }
    public void setEnableLowLatency(boolean enableLowLatency) { this.enableLowLatency = enableLowLatency; }
    
    public boolean isEnableHardwareAcceleration() { return enableHardwareAcceleration; }
    public void setEnableHardwareAcceleration(boolean enableHardwareAcceleration) { this.enableHardwareAcceleration = enableHardwareAcceleration; }
    
    public boolean isEnableAudioFilter() { return enableAudioFilter; }
    public void setEnableAudioFilter(boolean enableAudioFilter) { this.enableAudioFilter = enableAudioFilter; }
    
    public boolean isEnableVideoFilter() { return enableVideoFilter; }
    public void setEnableVideoFilter(boolean enableVideoFilter) { this.enableVideoFilter = enableVideoFilter; }
    
    public String getCustomFFmpegOptions() { return customFFmpegOptions; }
    public void setCustomFFmpegOptions(String customFFmpegOptions) { this.customFFmpegOptions = customFFmpegOptions; }
    
    /**
     * 应用质量预设
     */
    public void applyQualityPreset(QualityPreset preset) {
        if (preset != QualityPreset.CUSTOM) {
            this.videoWidth = preset.getWidth();
            this.videoHeight = preset.getHeight();
            this.videoBitrate = preset.getBitrate();
            this.videoFps = preset.getFps();
            
            // 根据质量调整其他参数
            switch (preset) {
                case ULTRA_LOW:
                case LOW:
                    this.videoPreset = "ultrafast";
                    this.videoProfile = "baseline";
                    this.videoLevel = 30;
                    this.audioBitrate = 64;
                    this.enableHardwareAcceleration = false;
                    break;
                case MEDIUM:
                    this.videoPreset = "veryfast";
                    this.videoProfile = "main";
                    this.videoLevel = 31;
                    this.audioBitrate = 96;
                    this.enableHardwareAcceleration = true;
                    break;
                case HIGH:
                case ULTRA_HIGH:
                    this.videoPreset = "fast";
                    this.videoProfile = "high";
                    this.videoLevel = 41;
                    this.audioBitrate = 128;
                    this.enableHardwareAcceleration = true;
                    break;
            }
        }
    }
    
    /**
     * 根据网络状况自动调整参数
     */
    public void adjustForNetworkCondition(int networkSpeed) {
        if (!enableAdaptiveBitrate) return;
        
        // 网络速度单位：kbps
        if (networkSpeed < 1000) { // 1Mbps以下
            this.videoBitrate = Math.min(this.videoBitrate, 800);
            this.videoFps = Math.min(this.videoFps, 24);
            this.videoHeight = Math.min(this.videoHeight, 720);
        } else if (networkSpeed < 3000) { // 3Mbps以下
            this.videoBitrate = Math.min(this.videoBitrate, 1500);
            this.videoFps = Math.min(this.videoFps, 30);
            this.videoHeight = Math.min(this.videoHeight, 1080);
        } else if (networkSpeed < 10000) { // 10Mbps以下
            this.videoBitrate = Math.min(this.videoBitrate, 3000);
            this.videoFps = Math.min(this.videoFps, 30);
            this.videoHeight = Math.min(this.videoHeight, 1080);
        } else { // 10Mbps以上
            this.videoBitrate = Math.min(this.videoBitrate, 5000);
            this.videoFps = Math.min(this.videoFps, 60);
            this.videoHeight = Math.min(this.videoHeight, 1440);
        }
        
        // 调整音频比特率
        this.audioBitrate = Math.min(this.audioBitrate, this.videoBitrate / 20);
    }
    
    /**
     * 获取视频宽高比
     */
    public float getAspectRatio() {
        if (videoHeight > 0) {
            return (float) videoWidth / videoHeight;
        }
        return 16.0f / 9.0f; // 默认16:9
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValid() {
        return streamKey != null && !streamKey.isEmpty() &&
               rtmpUrl != null && !rtmpUrl.isEmpty() &&
               videoWidth > 0 && videoHeight > 0 &&
               videoBitrate > 0 && videoFps > 0 &&
               audioSampleRate > 0 && audioChannels > 0 &&
               audioBitrate > 0;
    }
    
    /**
     * 获取配置摘要
     */
    public String getConfigSummary() {
        return String.format("视频: %dx%d, %dkbps, %dfps | 音频: %dHz, %d声道, %dkbps | 推流方式: %s",
                videoWidth, videoHeight, videoBitrate, videoFps,
                audioSampleRate, audioChannels, audioBitrate,
                streamingType.getDisplayName());
    }
    
    /**
     * 复制配置
     */
    public StreamingConfig copy() {
        StreamingConfig copy = new StreamingConfig();
        copy.streamKey = this.streamKey;
        copy.rtmpUrl = this.rtmpUrl;
        copy.streamingType = this.streamingType;
        copy.videoWidth = this.videoWidth;
        copy.videoHeight = this.videoHeight;
        copy.videoBitrate = this.videoBitrate;
        copy.videoFps = this.videoFps;
        copy.videoCodec = this.videoCodec;
        copy.videoPreset = this.videoPreset;
        copy.videoProfile = this.videoProfile;
        copy.videoLevel = this.videoLevel;
        copy.videoGop = this.videoGop;
        copy.audioSampleRate = this.audioSampleRate;
        copy.audioChannels = this.audioChannels;
        copy.audioBitrate = this.audioBitrate;
        copy.audioCodec = this.audioCodec;
        copy.audioProfile = this.audioProfile;
        copy.networkTimeout = this.networkTimeout;
        copy.retryCount = this.retryCount;
        copy.enableAdaptiveBitrate = this.enableAdaptiveBitrate;
        copy.bufferSize = this.bufferSize;
        copy.enableLowLatency = this.enableLowLatency;
        copy.enableHardwareAcceleration = this.enableHardwareAcceleration;
        copy.enableAudioFilter = this.enableAudioFilter;
        copy.enableVideoFilter = this.enableVideoFilter;
        copy.customFFmpegOptions = this.customFFmpegOptions;
        return copy;
    }
}


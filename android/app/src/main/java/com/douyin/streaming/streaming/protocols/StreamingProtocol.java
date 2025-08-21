package com.douyin.streaming.streaming.protocols;

import com.douyin.streaming.utils.StreamingConfig;

/**
 * 推流协议接口
 * 定义所有推流方式需要实现的基本方法
 */
public interface StreamingProtocol {
    
    /**
     * 推流回调接口
     */
    interface StreamingCallback {
        /**
         * 推流启动成功
         */
        void onStarted();
        
        /**
         * 推流停止
         */
        void onStopped();
        
        /**
         * 推流出错
         */
        void onError(String error);
        
        /**
         * 状态更新
         */
        void onStatusUpdate(String status);
    }
    
    /**
     * 启动推流
     * @param config 推流配置
     * @param callback 推流回调
     */
    void startStreaming(StreamingConfig config, StreamingCallback callback);
    
    /**
     * 停止推流
     */
    void stopStreaming();
    
    /**
     * 暂停推流
     */
    void pauseStreaming();
    
    /**
     * 恢复推流
     */
    void resumeStreaming();
    
    /**
     * 获取推流状态
     * @return 推流状态
     */
    StreamingStatus getStatus();
    
    /**
     * 获取推流统计信息
     * @return 推流统计
     */
    StreamingStats getStats();
    
    /**
     * 设置推流参数
     * @param config 推流配置
     */
    void setConfig(StreamingConfig config);
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 推流状态枚举
     */
    enum StreamingStatus {
        IDLE("空闲"),
        CONNECTING("连接中"),
        CONNECTED("已连接"),
        STREAMING("推流中"),
        PAUSED("已暂停"),
        ERROR("错误"),
        DISCONNECTED("已断开");
        
        private final String description;
        
        StreamingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 推流统计信息
     */
    class StreamingStats {
        private long bytesSent;
        private long framesSent;
        private long audioFramesSent;
        private long videoFramesSent;
        private long droppedFrames;
        private long retryCount;
        private long startTime;
        private long duration;
        private double bitrate;
        private double fps;
        private double audioBitrate;
        private double videoBitrate;
        private int networkQuality;
        
        // Getters and Setters
        public long getBytesSent() { return bytesSent; }
        public void setBytesSent(long bytesSent) { this.bytesSent = bytesSent; }
        
        public long getFramesSent() { return framesSent; }
        public void setFramesSent(long framesSent) { this.framesSent = framesSent; }
        
        public long getAudioFramesSent() { return audioFramesSent; }
        public void setAudioFramesSent(long audioFramesSent) { this.audioFramesSent = audioFramesSent; }
        
        public long getVideoFramesSent() { return videoFramesSent; }
        public void setVideoFramesSent(long videoFramesSent) { this.videoFramesSent = videoFramesSent; }
        
        public long getDroppedFrames() { return droppedFrames; }
        public void setDroppedFrames(long droppedFrames) { this.droppedFrames = droppedFrames; }
        
        public long getRetryCount() { return retryCount; }
        public void setRetryCount(long retryCount) { this.retryCount = retryCount; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public double getBitrate() { return bitrate; }
        public void setBitrate(double bitrate) { this.bitrate = bitrate; }
        
        public double getFps() { return fps; }
        public void setFps(double fps) { this.fps = fps; }
        
        public double getAudioBitrate() { return audioBitrate; }
        public void setAudioBitrate(double audioBitrate) { this.audioBitrate = audioBitrate; }
        
        public double getVideoBitrate() { return videoBitrate; }
        public void setVideoBitrate(double videoBitrate) { this.videoBitrate = videoBitrate; }
        
        public int getNetworkQuality() { return networkQuality; }
        public void setNetworkQuality(int networkQuality) { this.networkQuality = networkQuality; }
        
        /**
         * 获取平均比特率 (kbps)
         */
        public double getAverageBitrate() {
            if (duration > 0) {
                return (bytesSent * 8.0 / 1000) / (duration / 1000.0);
            }
            return 0.0;
        }
        
        /**
         * 获取丢帧率
         */
        public double getDropRate() {
            if (framesSent > 0) {
                return (double) droppedFrames / framesSent * 100;
            }
            return 0.0;
        }
        
        /**
         * 获取运行时长 (秒)
         */
        public long getRunningTime() {
            if (startTime > 0) {
                return (System.currentTimeMillis() - startTime) / 1000;
            }
            return 0;
        }
    }
}


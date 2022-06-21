/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

/**
 * Created by heavyrainlee on 24/02/2018.
 */

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.util.LinkedList;

import cn.tojoy.chat.kit.R;

public class AsyncPlayer {
    private static final int PLAY = 1;
    private static final int STOP = 2;
    private static final boolean mDebug = false;

    private static final class Command {
        int code;
        Context context;
        int resId;
        boolean looping;
        int stream;
        long requestTime;

        public String toString() {
            return "{ code=" + code + " looping=" + looping + " stream=" + stream + " resId=" + resId + " }";
        }
    }

    private LinkedList mCmdQueue = new LinkedList();

    private void startSound(Command cmd) {
        if (mSoundPool != null) {
            mSoundPool.stop(streamID);
            mSoundPool.release();
        }
        //sdk版本21是SoundPool 的一个分水岭
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入最多播放音频数量,
            builder.setMaxStreams(1);
            //AudioAttributes是一个封装音频各种属性的方法
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适的属性
            attrBuilder.setLegacyStreamType(cmd.stream);
            //加载一个AudioAttributes
            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
        } else {
            /**
             * 第一个参数：int maxStreams：SoundPool对象的最大并发流数
             * 第二个参数：int streamType：AudioManager中描述的音频流类型
             *第三个参数：int srcQuality：采样率转换器的质量。 目前没有效果。 使用0作为默认值。
             */
            mSoundPool = new SoundPool(1, cmd.stream, 0);
        }
        int soundID = 0;
        try {
            soundID = mSoundPool.load(cmd.context, cmd.resId, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //异步需要等待加载完成，音频才能播放成功
        int finalSoundID = soundID;
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (mSoundPool == null || status != 0) {
                    return;
                }
                //第一个参数soundID
                //第二个参数leftVolume为左侧音量值（范围= 0.0到1.0）
                //第三个参数rightVolume为右的音量值（范围= 0.0到1.0）
                //第四个参数priority 为流的优先级，值越大优先级高，影响当同时播放数量超出了最大支持数时SoundPool对该流的处理
                //第五个参数loop 为音频重复播放次数，0为值播放一次，-1为无限循环，其他值为播放loop+1次
                //第六个参数 rate为播放的速率，范围0.5-2.0(0.5为一半速率，1.0为正常速率，2.0为两倍速率)
                streamID = mSoundPool.play(finalSoundID, 1, 1, 0, cmd.looping ? -1 : 0, 1);
            }
        });

    }

    private final class Thread extends java.lang.Thread {
        Thread() {
            super("AsyncPlayer-" + mTag);
        }

        public void run() {
            while (true) {
                Command cmd = null;

                synchronized (mCmdQueue) {

                    cmd = (Command) mCmdQueue.removeFirst();
                }

                switch (cmd.code) {
                    case PLAY:
                        startSound(cmd);
                        break;
                    case STOP:

                        Log.e(mTag, "STOP CMD");
                        if (mSoundPool != null) {
                            Log.e("AsyncPlayer", "mSoundPool stop & release");
                            mSoundPool.stop(streamID);
                            mSoundPool.release();
                            mSoundPool = null;
                        } else {
                            Log.w(mTag, "STOP command without a player");
                        }
                        break;
                }

                synchronized (mCmdQueue) {
                    if (mCmdQueue.size() == 0) {

                        mThread = null;
                        releaseWakeLock();
                        return;
                    }
                }
            }
        }
    }

    private String mTag;
    private Thread mThread;
    private SoundPool mSoundPool = null;
    private int streamID;
    private PowerManager.WakeLock mWakeLock;

    private int mState = STOP;

    public AsyncPlayer(String tag) {
        if (tag != null) {
            mTag = tag;
        } else {
            mTag = "AsyncPlayer";
        }
    }

    public void play(Context context, int resId, boolean looping, int stream) {
        Command cmd = new Command();
        cmd.requestTime = SystemClock.uptimeMillis();
        cmd.code = PLAY;
        cmd.context = context;
        cmd.resId = resId;
        cmd.looping = looping;
        cmd.stream = stream;
        synchronized (mCmdQueue) {
            enqueueLocked(cmd);
            mState = PLAY;
        }
    }

    public void stop() {
        Log.e(mTag, "stop");
        synchronized (mCmdQueue) {
            if (mState != STOP) {
                Command cmd = new Command();
                cmd.requestTime = SystemClock.uptimeMillis();
                cmd.code = STOP;
                enqueueLocked(cmd);
                mState = STOP;
            }
        }
    }

    private void enqueueLocked(Command cmd) {
        mCmdQueue.add(cmd);
        if (mThread == null) {
            acquireWakeLock();
            mThread = new Thread();
            mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        if (mWakeLock != null || mThread != null) {
            throw new RuntimeException("assertion failed mWakeLock=" + mWakeLock + " mThread=" + mThread);
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mTag);
    }

    private void acquireWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }
}

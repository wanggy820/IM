/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.tencent.rtmp.ui.TXCloudVideoView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import java.util.List;
import cn.tojoy.chat.kit.BuildConfig;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.sdk.ParticipantProfile;
import cn.tojoy.chat.kit.sdk.TJAVCallMessageCallBack;
import cn.tojoy.chat.kit.sdk.TJCallEndReason;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class VoipCallService extends Service implements TJAVCallMessageCallBack {
    private static final int NOTIFICATION_ID = 1;

    private WindowManager wm;
    private View view;
    private WindowManager.LayoutParams params;
    private Intent resumeActivityIntent;
    private boolean initialized = false;
    private boolean showFloatingWindow = false;

    private String focusTargetId;

    private Handler handler = new Handler();
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 可以多次调用
    public static void start(Context context, boolean showFloatingView) {
        Intent intent = new Intent(context, VoipCallService.class);
        intent.putExtra("showFloatingView", showFloatingView);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, VoipCallService.class);
        context.stopService(intent);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean screenShare = intent.getBooleanExtra("screenShare", false);
        if (screenShare) {
            Activity activity = TJIMSDK.getSDK().getCurrentActivity();
            if (activity instanceof  VoipBaseActivity) {
                ((VoipBaseActivity) activity).startScreenShare();
            }
            return START_NOT_STICKY;
        }

        focusTargetId = intent.getStringExtra("focusTargetId");
        if (TJCallState.Idle == TJIMSDK.getSDK().state) {
            stopSelf();
        } else {
            if (!initialized) {
                initialized = true;
                startForeground(NOTIFICATION_ID, buildNotification());
                checkCallState();
            }
            showFloatingWindow = intent.getBooleanExtra("showFloatingView", false);
            if (showFloatingWindow) {
                rendererInitialized = false;
                lastState = null;
                try {
                    showFloatingWindow();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TJIMSDK.setCallBack(this);
            } else {
                hideFloatBox();
            }
        }
        return START_NOT_STICKY;
    }

    private void checkCallState() {
        if (TJCallState.Idle == TJIMSDK.getSDK().state) {
            stopSelf();
        } else {
            updateNotification();
            if (showFloatingWindow && TJIMSDK.getSDK().state == TJCallState.Connected) {
                if (TJIMSDK.getSDK().isScreenSharing) {
                    showScreenSharingView();
                } else if (TJIMSDK.getSDK().content.isAudioOnly()) {
                    showAudioView();
                } else {
                    showVideoView();
                }
            }
            handler.postDelayed(this::checkCallState, 1000);
        }
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        resumeActivityIntent = new Intent(this, VoipDummyActivity.class);
        resumeActivityIntent.putExtra(SingleCallActivity.EXTRA_FROM_FLOATING_VIEW, true);
        resumeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resumeActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channelId = BuildConfig.LIBRARY_PACKAGE_NAME + ".voip";
            String channelName = "voip";
            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        String title;
        switch (TJIMSDK.getSDK().state) {
            case Outgoing:
                title = "等待对方接听...";
                break;
            case Incoming:
                title = "邀请您进行通话...";
                break;
            case Connecting:
                title = "接听中...";
                break;
            default:
                title = "通话中...";
                break;
        }
        return builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null && view != null) {
            wm.removeView(view);
        }
    }

    private void showFloatingWindow() {
        if (wm != null) {
            return;
        }
        view = LayoutInflater.from(this).inflate(R.layout.av_voip_float_view, null);
        view.setOnTouchListener(onTouchListener);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.type = type;
        params.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        params.format = PixelFormat.TRANSLUCENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        params.x = getResources().getDisplayMetrics().widthPixels;
        params.y = 0;

        wm.addView(view, params);
        didChangeState(TJIMSDK.getSDK().state);
    }

    public void hideFloatBox() {
        if (wm != null && view != null) {
            wm.removeView(view);
            wm = null;
            view = null;
        }
    }

    private void showUnConnectedCallInfo() {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
        }

        view.findViewById(R.id.audioLinearLayout).setVisibility(View.VISIBLE);
        TextView timeView = view.findViewById(R.id.durationTextView);
        ImageView mediaIconV = view.findViewById(R.id.av_media_type);
        mediaIconV.setImageResource(R.drawable.av_float_audio);

        String title = "";
        switch (TJIMSDK.getSDK().state) {
            case Outgoing:
                title = "等待接听";
                break;
            case Incoming:
                title = "等待接听";
                break;
            case Connecting:
                title = "接听中";
                break;
        }
        timeView.setText(title);
    }

    private void showScreenSharingView() {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
        }
        view.findViewById(R.id.screenSharingTextView).setVisibility(View.VISIBLE);
        view.findViewById(R.id.durationTextView).setVisibility(View.GONE);
        view.findViewById(R.id.av_media_type).setVisibility(View.GONE);
    }

    private void showAudioView() {
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        if (remoteVideoFrameLayout.getVisibility() == View.VISIBLE) {
            remoteVideoFrameLayout.setVisibility(View.GONE);
            wm.removeView(view);
            wm.addView(view, params);
        }

        view.findViewById(R.id.audioLinearLayout).setVisibility(View.VISIBLE);
        TextView timeView = view.findViewById(R.id.durationTextView);
        ImageView mediaIconV = view.findViewById(R.id.av_media_type);
        mediaIconV.setImageResource(R.drawable.av_float_audio);

        long duration = (System.currentTimeMillis() - TJIMSDK.getSDK().content.getConnectTime()) / 1000;
        if (duration >= 3600) {
            timeView.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
        } else {
            timeView.setText(String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60)));
        }
    }

    private boolean rendererInitialized = false;
    private TJCallState lastState = null;
    private void showVideoView() {
        view.findViewById(R.id.audioLinearLayout).setVisibility(View.GONE);
        FrameLayout remoteVideoFrameLayout = view.findViewById(R.id.remoteVideoFrameLayout);
        remoteVideoFrameLayout.setVisibility(View.VISIBLE);

        if (!rendererInitialized || lastState != TJIMSDK.getSDK().state) {
            rendererInitialized = true;
            lastState = TJIMSDK.getSDK().state;
            TXCloudVideoView videoView = new TXCloudVideoView(this);
            remoteVideoFrameLayout.addView(videoView);

            String targetId = null;
            if (TJIMSDK.getSDK().isConference()) {
                List<ParticipantProfile> participants = TJIMSDK.getSDK().getParticipants();
                if (!participants.isEmpty()) {
                    for (ParticipantProfile profile : participants) {
                        if (!profile.audience) {
                            targetId = profile.userId;
                            break;
                        }
                    }
                }

            } else {
                targetId = TJIMSDK.getSDK().content.getTargetIds().get(0);
                if (!TextUtils.isEmpty(focusTargetId) && (TJIMSDK.getSDK().content.getTargetIds().contains(focusTargetId) || ChatManager.Instance().getUserId().equals(focusTargetId))) {
                    targetId = focusTargetId;
                } else if (TJIMSDK.getSDK().message.conversation.type == Conversation.ConversationType.Group) {
                    for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
                        if (profile.state == TJCallState.Connected) {
                            targetId = profile.userId;
                            break;
                        }
                    }
                }
            }

            if (targetId != null && TJIMSDK.getSDK().state == TJCallState.Connected && !targetId.equals(ChatManager.Instance().getUserId())) {
                TJIMSDK.startRemoteView(targetId, videoView);
            } else {
                TJIMSDK.startLocalPreview(videoView);
            }
        }
    }

    private void clickToResume() {
        if (!TJIMSDK.getSDK().content.isAudioOnly()) {
            if (focusTargetId != null && !focusTargetId.equals(ChatManager.Instance().getUserId())) {
                TJIMSDK.stopRemoteView(focusTargetId);
            } else  {
                TJIMSDK.stopLocalPreview();
            }
        }
        showFloatingWindow = false;
        startActivity(resumeActivityIntent);
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        float lastX, lastY;
        int oldOffsetX, oldOffsetY;
        int tag = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            if (tag == 0) {
                oldOffsetX = params.x;
                oldOffsetY = params.y;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                lastX = x;
                lastY = y;
            } else if (action == MotionEvent.ACTION_MOVE) {
                // 减小偏移量,防止过度抖动
                params.x += (int) (x - lastX) / 3;
                params.y += (int) (y - lastY) / 3;
                tag = 1;
                wm.updateViewLayout(v, params);
            } else if (action == MotionEvent.ACTION_UP) {
                int newOffsetX = params.x;
                int newOffsetY = params.y;
                if (Math.abs(oldOffsetX - newOffsetX) <= 20 && Math.abs(oldOffsetY - newOffsetY) <= 20) {
                    clickToResume();
                } else {
                    tag = 0;
                }
            }
            return true;
        }
    };

    @Override
    public void didChangeState(TJCallState state) {
        Log.v("VOIP", "TJCallState:"+state);
        if (state == TJCallState.Idle) {
            stopSelf();
            return;
        }
        showFloatingWindow();
        if (TJIMSDK.getSDK().content.isAudioOnly()) {
            if (focusTargetId != null && !focusTargetId.equals(ChatManager.Instance().getUserId())) {
                TJIMSDK.stopRemoteView(focusTargetId);
            } else  {
                TJIMSDK.stopLocalPreview();
            }
        }

        if (TJIMSDK.getSDK().state != TJCallState.Connected) {
            showUnConnectedCallInfo();
        } else {
            if (TJIMSDK.getSDK().isScreenSharing) {
                showScreenSharingView();
            } else if (TJIMSDK.getSDK().content.isAudioOnly()) {
                showAudioView();
            } else {
                showVideoView();
            }
        }
    }

    @Override
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom) {
        if (TJIMSDK.getSDK().getParticipants().size() == 0) {
            TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
            stopSelf();
        }
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {

    }
}

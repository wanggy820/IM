/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.tencent.rtmp.ui.TXCloudVideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.tojoy.chat.kit.GlideApp;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.sdk.TJAVCallMessageCallBack;
import cn.tojoy.chat.kit.sdk.TJCallEndReason;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;

public class SingleVideoFragment extends Fragment implements TJAVCallMessageCallBack {

    @BindView(R2.id.pip_video_view)
    FrameLayout pipRenderer;
    @BindView(R2.id.fullscreen_video_view)
    FrameLayout fullscreenRenderer;
    @BindView(R2.id.outgoingActionContainer)
    ViewGroup outgoingActionContainer;
    @BindView(R2.id.incomingActionContainer)
    ViewGroup incomingActionContainer;
    @BindView(R2.id.connectedActionContainer)
    ViewGroup connectedActionContainer;
    @BindView(R2.id.inviteeInfoContainer)
    ViewGroup inviteeInfoContainer;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.shareScreenTextView)
    TextView shareScreenTextView;

    TXCloudVideoView localVideoView;
    TXCloudVideoView remoteVideoView;

    // True if local view is in the fullscreen renderer.
    private String targetId;

    private boolean callControlVisible = true;

    private static final String TAG = "VideoFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_p2p_video_layout, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        targetId = TJIMSDK.getSDK().message.conversation.target;
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(targetId, false);
        if (userInfo == null) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        GlideApp.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        nameTextView.setText(userViewModel.getUserDisplayName(userInfo));

        TJIMSDK.setCallBack(this);
        updateCallDuration();
        didChangeState(TJIMSDK.getSDK().state);
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            long s = System.currentTimeMillis() - TJIMSDK.getSDK().content.getConnectTime();
            s = s / 1000;
            String text;
            if (s > 3600) {
                text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            } else {
                text = String.format("%02d:%02d", s / 60, (s % 60));
            }
            durationTextView.setText(text);
        }
        if (TJIMSDK.getSDK().state != TJCallState.Idle) {
            handler.postDelayed(this::updateCallDuration, 1000);
        }
    }

    @Override
    public void didChangeState(TJCallState state) {
        if (TJCallState.Idle == TJIMSDK.getSDK().state) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else if (TJCallState.Connected == TJIMSDK.getSDK().state) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);

            if (TJIMSDK.getSDK().isScreenSharing) {
                shareScreenTextView.setText("结束屏幕共享");
            } else {
                shareScreenTextView.setText("开始屏幕共享");
            }
            didCreateLocalVideoTrack();
            didReceiveRemoteVideoTrack(targetId);
        } else if (TJIMSDK.getSDK().state == TJCallState.Connecting) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);
            durationTextView.setText(R.string.connecting);
        } else if (TJIMSDK.getSDK().state == TJCallState.Outgoing) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.VISIBLE);
            connectedActionContainer.setVisibility(View.GONE);
            descTextView.setText(R.string.av_waiting);
            didCreateLocalVideoTrack();
        } else {
            incomingActionContainer.setVisibility(View.VISIBLE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.GONE);
            descTextView.setText(R.string.av_video_invite);
        }

        if (TJIMSDK.getSDK().content.isAudioOnly()) {
            audioAccept();
        }
    }

    @Override
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom) {

    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {

    }

    public void didCreateLocalVideoTrack() {
        if (localVideoView == null) {
            localVideoView = new TXCloudVideoView(getActivity());
            if (TJIMSDK.getSDK().state == TJCallState.Outgoing) {
                fullscreenRenderer.addView(localVideoView);
            } else {
                pipRenderer.addView(localVideoView);
            }
        }
        TJIMSDK.startLocalPreview(localVideoView);
    }

    public void didReceiveRemoteVideoTrack(String userId) {
        pipRenderer.setVisibility(View.VISIBLE);
        if (localVideoView != null) {
            ((ViewGroup) localVideoView.getParent()).removeView(localVideoView);
            pipRenderer.addView(localVideoView);
        }

        if (remoteVideoView == null) {
            remoteVideoView = new TXCloudVideoView(getActivity());
            fullscreenRenderer.removeAllViews();
            fullscreenRenderer.addView(remoteVideoView);
        }
        TJIMSDK.startRemoteView(userId, remoteVideoView);
    }


    @OnClick(R2.id.acceptImageView)
    public void accept() {
        if (TJIMSDK.getSDK().state == TJCallState.Incoming) {
            TJIMSDK.answerCall();
        }
    }

    @OnClick({R2.id.incomingAudioOnlyImageView})
    public void audioAccept() {
        ((SingleCallActivity) getActivity()).audioAccept();
    }

    @OnClick({R2.id.outgoingAudioOnlyImageView, R2.id.connectedAudioOnlyImageView})
    public void audioCall() {
        ((SingleCallActivity) getActivity()).audioCall();
    }

    // callFragment.OnCallEvents interface implementation.
    @OnClick({R2.id.connectedHangupImageView,
        R2.id.outgoingHangupImageView,
        R2.id.incomingHangupImageView})
    public void hangUp() {
        TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
    }

    @OnClick(R2.id.switchCameraImageView)
    public void switchCamera() {
        TJIMSDK.switchCamera();
    }

    @OnClick(R2.id.shareScreenImageView)
    void shareScreen() {
        if (TJIMSDK.getSDK().state != TJCallState.Connected) {
            return;
        }
        if (!TJIMSDK.getSDK().isScreenSharing) {
            shareScreenTextView.setText("结束屏幕共享");
            ((VoipBaseActivity) getActivity()).startScreenShare();
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
            shareScreenTextView.setText("开始屏幕共享");
        }
    }

    @OnClick(R2.id.fullscreen_video_view)
    void toggleCallControlVisibility() {
        if (TJIMSDK.getSDK().state != TJCallState.Connected) {
            return;
        }
        callControlVisible = !callControlVisible;
        if (callControlVisible) {
            connectedActionContainer.setVisibility(View.VISIBLE);
        } else {
            connectedActionContainer.setVisibility(View.GONE);
        }
        // TODO animation
    }

    @OnClick(R2.id.minimizeImageView)
    public void minimize() {
        TJIMSDK.stopLocalPreview();
        TJIMSDK.stopRemoteView(targetId);
        ((SingleCallActivity) getActivity()).showFloatingView(targetId);
    }

    @OnClick(R2.id.pip_video_view)
    void setSwappedFeeds() {
        Log.v(TAG, "setSwappedFeeds");
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.stopLocalPreview();
            TJIMSDK.stopRemoteView(targetId);
            TXCloudVideoView tmp = localVideoView;
            localVideoView = remoteVideoView;
            remoteVideoView = tmp;
            Log.v(TAG, "setSwappedFeeds" + localVideoView);
            TJIMSDK.startRemoteView(targetId, remoteVideoView);
            TJIMSDK.startLocalPreview(localVideoView);
        }
    }
}

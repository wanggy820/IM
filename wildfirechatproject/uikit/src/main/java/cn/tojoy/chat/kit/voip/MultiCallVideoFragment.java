/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.tencent.rtmp.ui.TXCloudVideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.tojoy.chat.kit.GlideApp;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.sdk.ParticipantProfile;
import cn.tojoy.chat.kit.sdk.TJAVCallMessageCallBack;
import cn.tojoy.chat.kit.sdk.TJCallEndReason;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallVideoFragment extends Fragment implements TJAVCallMessageCallBack {
    @BindView(R2.id.rootView)
    RelativeLayout rootRelativeLayout;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.videoContainerGridLayout)
    GridLayout participantGridView;
    @BindView(R2.id.focusVideoContainer)
    RelativeLayout focusVideoContainer;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;
    @BindView(R2.id.videoImageView)
    ImageView videoImageView;
    @BindView(R2.id.switchCameraImageView)
    ImageView switchCameraImageView;

    private List<String> participantIds;
    private UserInfo me;
    private UserViewModel userViewModel;

    private MultiCallItem focusMultiCallItem;

    public static final String TAG = "MultiCallVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_video_outgoing_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        initParticipantsView();

        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
                if (profile.state == TJCallState.Connected) {
                    didReceiveRemoteVideoTrack(profile.userId);
                }
            }
        }
        didCreateLocalVideoTrack();
        updateCallDuration();
        updateParticipantStatus();

        TJIMSDK.setCallBack(this);
    }

    private void initParticipantsView() {
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        participantIds = new ArrayList<>();
        for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
            participantIds.add(profile.userId);
        }
        participantIds.add(ChatManager.Instance().getUserId());

        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participantIds);

        for (UserInfo userInfo : participantUserInfos) {
            if (userInfo.uid.equals(me.uid)) {
                continue;
            }
            MultiCallItem multiCallItem = new MultiCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);

            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(width / 3, width / 3));
            multiCallItem.getStatusTextView().setText(R.string.connecting);
            multiCallItem.setOnClickListener(clickListener);
            GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
            participantGridView.addView(multiCallItem);
        }

        MultiCallItem multiCallItem = new MultiCallItem(getActivity());
        multiCallItem.setTag(me.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        multiCallItem.getStatusTextView().setText(me.displayName);
        multiCallItem.setOnClickListener(clickListener);
        GlideApp.with(multiCallItem).load(me.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());

        Log.v(TAG, "width:"+multiCallItem.getWidth() + ", height:"+multiCallItem.getHeight());

        Log.v(TAG, "设备 width:"+width + ", height:"+height);
        focusVideoContainer.setLayoutParams(new RelativeLayout.LayoutParams(width, height));

        focusVideoContainer.addView(multiCallItem);
        focusMultiCallItem = multiCallItem;
        ((VoipBaseActivity) getActivity()).setFocusVideoUserId(me.uid);

        muteImageView.setSelected(TJIMSDK.getSDK().audioMuted);
        switchCameraImageView.setSelected(!TJIMSDK.getSDK().frontCamera);
        videoImageView.setSelected(TJIMSDK.getSDK().videoMuted);
    }

    private void updateParticipantStatus() {
        int count = participantGridView.getChildCount();
        String meUid = userViewModel.getUserId();
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            String userId = (String) view.getTag();
            if (meUid.equals(userId)) {
                ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
                    if (profile.userId.equals(userId)) {
                        if (profile.state == TJCallState.Connected) {
                            ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
                        } else if (profile.videoMuted) {
                            ((MultiCallItem) view).getStatusTextView().setText("关闭摄像头");
                            ((MultiCallItem) view).getStatusTextView().setVisibility(View.VISIBLE);
                        }
                        break;
                    }
                }
            }
        }
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
            TJIMSDK.stopRemoteView(profile.userId);
        }
        TJIMSDK.stopLocalPreview();
        ((MultiCallActivity) getActivity()).showFloatingView(((VoipBaseActivity) getActivity()).getFocusVideoUserId());
    }

    @OnClick(R2.id.addParticipantImageView)
    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant(TJIMSDK.TJ_MAX_PARTICIPANT_COUNT - participantIds.size() - 1);
    }

    @OnClick(R2.id.muteImageView)
    void audioMute() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.setAudioMuted(!TJIMSDK.getSDK().audioMuted);
            muteImageView.setSelected(TJIMSDK.getSDK().audioMuted);
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        TJIMSDK.switchCamera();
        switchCameraImageView.setSelected(!TJIMSDK.getSDK().frontCamera);
    }

    @OnClick(R2.id.videoImageView)
    void videoMute() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.setVideoMuted(!TJIMSDK.getSDK().videoMuted);
            videoImageView.setSelected(TJIMSDK.getSDK().videoMuted);
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
    }

    @OnClick(R2.id.shareScreenImageView)
    void shareScreen() {
        if (!TJIMSDK.getSDK().isScreenSharing) {
            ((VoipBaseActivity) getActivity()).startScreenShare();
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
        }
    }

    public void didCreateLocalVideoTrack() {
        MultiCallItem item = rootRelativeLayout.findViewWithTag(me.uid);
        TXCloudVideoView videoView = item.findViewWithTag("v_" + me.uid);

        if (videoView == null) {
            videoView = new TXCloudVideoView(getActivity());
            videoView.setTag("v_" + me.uid);
            item.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        TJIMSDK.startLocalPreview(videoView);
    }

    public void didReceiveRemoteVideoTrack(String userId) {
        MultiCallItem item = rootRelativeLayout.findViewWithTag(userId);
        if (item == null) {
            return;
        }

        TXCloudVideoView videoView = new TXCloudVideoView(getActivity());
        if (videoView != null) {
            item.addView(videoView);
            videoView.setTag("v_" + userId);
            TJIMSDK.startRemoteView(userId, videoView);
        }
    }

    public void didRemoveRemoteVideoTrack(String userId) {
        MultiCallItem item = rootRelativeLayout.findViewWithTag(userId);
        if (item != null) {
            View view = item.findViewWithTag("v_" + userId);
            if (view != null) {
                item.removeView(view);
            }

            item.getStatusTextView().setText("关闭摄像头");
            item.getStatusTextView().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void didChangeState(TJCallState callState) {
        if (callState == TJCallState.Connected) {
            updateParticipantStatus();
        } else if (callState == TJCallState.Idle) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom) {
        Log.v(TAG, "用户:" + userId + (isEnterRoom ? "进入房间" : "离开房间"));
        Log.v(TAG, "participantIds:" + participantIds + ", size:" + (participantIds == null ? 0:participantIds.size()));
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        if (participantIds.contains(userId) && participantIds.size() <= 2 && !isEnterRoom) {
            TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
            return;
        }

        if (isEnterRoom) {
            didReceiveRemoteVideoTrack(userId);
            if (participantIds.contains(userId)) {
                return;
            }
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int with = dm.widthPixels;

            participantGridView.getLayoutParams().height = with;

            UserInfo userInfo = userViewModel.getUserInfo(userId, false);
            MultiCallItem multiCallItem = new MultiCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);
            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
            multiCallItem.getStatusTextView().setText(userInfo.displayName);
            multiCallItem.setOnClickListener(clickListener);
            GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
            participantGridView.addView(multiCallItem);
            participantIds.add(userId);
        } else  {
            didRemoveRemoteVideoTrack(userId);
            View view = participantGridView.findViewWithTag(userId);
            if (view != null) {
                participantGridView.removeView(view);
            }
            participantIds.remove(userId);

            if (userId.equals(((VoipBaseActivity) getActivity()).getFocusVideoUserId())) {
                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(null);
                focusVideoContainer.removeView(focusMultiCallItem);
                focusMultiCallItem = null;
            }

            Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        MultiCallItem multiCallItem = participantGridView.findViewWithTag(userId);
        if (multiCallItem != null) {
            if (volume > 1000) {
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            } else {
                multiCallItem.getStatusTextView().setVisibility(View.GONE);
                multiCallItem.getStatusTextView().setText("");
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (TJIMSDK.getSDK().state != TJCallState.Connected ) {
                return;
            }
            String userId = (String) v.getTag();
            if (!userId.equals(((VoipBaseActivity) getActivity()).getFocusVideoUserId())) {
                MultiCallItem clickedMultiCallItem = (MultiCallItem) v;
                int clickedIndex = participantGridView.indexOfChild(v);
                participantGridView.removeView(clickedMultiCallItem);
                participantGridView.endViewTransition(clickedMultiCallItem);

                if (focusMultiCallItem != null) {
                    focusVideoContainer.removeView(focusMultiCallItem);
                    focusVideoContainer.endViewTransition(focusMultiCallItem);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int with = dm.widthPixels;
                    participantGridView.addView(focusMultiCallItem, clickedIndex, new RelativeLayout.LayoutParams(with / 3, with / 3));
                }
                focusVideoContainer.addView(clickedMultiCallItem, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                focusMultiCallItem = clickedMultiCallItem;
                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(userId);

            }
        }
    };


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
}

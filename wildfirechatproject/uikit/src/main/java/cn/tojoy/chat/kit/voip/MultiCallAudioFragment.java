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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

public class MultiCallAudioFragment extends Fragment implements TJAVCallMessageCallBack {
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.audioContainerGridLayout)
    GridLayout audioContainerGridLayout;
    @BindView(R2.id.speakerImageView)
    ImageView speakerImageView;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;

    //包含自己
    private List<String> participantIds;
    private UserViewModel userViewModel;

    public static final String TAG = "MultiCallVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_audio_outgoing_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            getActivity().finish();
            return;
        }


        muteImageView.setSelected(TJIMSDK.getSDK().audioMuted);

        initParticipantsView();
        updateParticipantStatus();
        updateCallDuration();

        speakerImageView.setSelected(TJIMSDK.getSDK().handsFreeOn);
        TJIMSDK.setCallBack(this);
    }

    private void initParticipantsView() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        audioContainerGridLayout.getLayoutParams().height = with;
        audioContainerGridLayout.removeAllViews();

        // session里面的participants包含除自己外的所有人
        participantIds = new ArrayList<>();
        for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
            participantIds.add(profile.userId);
        }
        participantIds.add(ChatManager.Instance().getUserId());
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participantIds);
        int size = with / Math.max((int) Math.ceil(Math.sqrt(participantUserInfos.size())), 3);
        for (UserInfo userInfo : participantUserInfos) {
            MultiCallItem multiCallItem = new MultiCallItem(getActivity());
            multiCallItem.setTag(userInfo.uid);

            multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            multiCallItem.getStatusTextView().setText(R.string.connecting);
            GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
            audioContainerGridLayout.addView(multiCallItem);
        }
    }

    private void updateParticipantStatus() {
        int count = audioContainerGridLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = audioContainerGridLayout.getChildAt(i);
            String userId = (String) view.getTag();
            if (ChatManager.Instance().getUserId().equals(userId)) {
                ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
                    if (profile.userId.equals(userId)) {
                        if (profile.state == TJCallState.Connected) {
                            ((MultiCallItem) view).getStatusTextView().setVisibility(View.GONE);
                        }
                    }
                    break;
                }
            }
        }
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        ((MultiCallActivity) getActivity()).showFloatingView(null);
    }

    @OnClick(R2.id.addParticipantImageView)
    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant(TJIMSDK.TJ_MAX_PARTICIPANT_COUNT - participantIds.size() - 1);
    }

    @OnClick(R2.id.muteImageView)
    void mute() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.setAudioMuted(!TJIMSDK.getSDK().audioMuted);
            muteImageView.setSelected(TJIMSDK.getSDK().audioMuted);
        }
    }

    @OnClick(R2.id.speakerImageView)
    void speaker() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            TJIMSDK.setHandsFreeOn(!TJIMSDK.isHandsFreeOn());
            speakerImageView.setSelected(TJIMSDK.isHandsFreeOn());
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
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
    public void didChangeState(TJCallState callState) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (callState == TJCallState.Connected) {
            updateParticipantStatus();
        } else if (callState == TJCallState.Idle) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
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
        if (!isEnterRoom) {
            View view = audioContainerGridLayout.findViewWithTag(userId);
            if (view != null) {
                audioContainerGridLayout.removeView(view);
            }
            participantIds.remove(userId);

            Toast.makeText(getActivity(), "用户" + ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();
            return;
        }

        if (participantIds.contains(userId)) {
            return;
        }
        int count = audioContainerGridLayout.getChildCount();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;
        for (int i = 0; i < count; i++) {
            View view = audioContainerGridLayout.getChildAt(i);
            // 将自己放到最后
            if (ChatManager.Instance().getUserId().equals(view.getTag())) {

                UserInfo info = userViewModel.getUserInfo(userId, false);
                MultiCallItem multiCallItem = new MultiCallItem(getActivity());
                multiCallItem.setTag(info.uid);

                multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));

                multiCallItem.getStatusTextView().setText(R.string.connecting);
                GlideApp.with(multiCallItem).load(info.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
                audioContainerGridLayout.addView(multiCallItem, i);
                break;
            }
        }
        participantIds.add(userId);
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        MultiCallItem multiCallItem = audioContainerGridLayout.findViewWithTag(userId);
        if (multiCallItem != null) {
            if (volume > 10) {
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            } else {
                multiCallItem.getStatusTextView().setVisibility(View.GONE);
                multiCallItem.getStatusTextView().setText("");
            }
        }
    }
}

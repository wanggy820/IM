/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class SingleAudioFragment extends Fragment implements TJAVCallMessageCallBack {
    private boolean audioEnable = true;

    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;
    @BindView(R2.id.speakerImageView)
    ImageView spearImageView;
    @BindView(R2.id.incomingActionContainer)
    ViewGroup incomingActionContainer;
    @BindView(R2.id.outgoingActionContainer)
    ViewGroup outgoingActionContainer;
    @BindView(R2.id.descTextView)
    TextView descTextView;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;

    private static final String TAG = "AudioFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_p2p_audio_layout, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }


    //监听蓝牙设备/耳机连接
//    @Override
//    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
//        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
//        if(audioManager.isSpeakerphoneOn()) {
//            spearImageView.setSelected(true);
//        } else {
//            spearImageView.setSelected(false);
//        }
//
//        if(device == AVAudioManager.AudioDevice.WIRED_HEADSET || device == AVAudioManager.AudioDevice.BLUETOOTH) {
//            spearImageView.setEnabled(false);
//        } else {
//            spearImageView.setEnabled(true);
//        }
//    }

    @OnClick(R2.id.muteImageView)//设置静音
    public void mute() {
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            audioEnable = !audioEnable;
            TJIMSDK.setAudioMuted(!audioEnable);
            muteImageView.setSelected(!audioEnable);
        }
    }

    @OnClick({R2.id.incomingHangupImageView, R2.id.outgoingHangupImageView})
    public void hangup() {
        TJIMSDK.endCallWithReason(TJCallEndReason.RemoteHangup);
    }

    @OnClick(R2.id.acceptImageView)
    public void onCallConnect() {
        if (TJIMSDK.getSDK().state == TJCallState.Incoming) {
            TJIMSDK.answerCall();
        }
    }

    @OnClick(R2.id.minimizeImageView)
    public void minimize() {
        ((SingleCallActivity) getActivity()).showFloatingView(null);
    }

    @OnClick(R2.id.speakerImageView)
    public void speakerClick() {
        if (TJIMSDK.getSDK().state != TJCallState.Connected && TJIMSDK.getSDK().state != TJCallState.Outgoing) {
            return;
        }

        TJIMSDK.setHandsFreeOn(!TJIMSDK.isHandsFreeOn());
        spearImageView.setSelected(!TJIMSDK.isHandsFreeOn());
    }

    private void init() {
        if (TJIMSDK.getSDK().state == TJCallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        if (TJIMSDK.getSDK().state == TJCallState.Connected) {
            descTextView.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.VISIBLE);
            durationTextView.setVisibility(View.VISIBLE);
        } else {
            if (TJIMSDK.getSDK().state == TJCallState.Outgoing) {
                descTextView.setText(R.string.av_waiting);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                incomingActionContainer.setVisibility(View.GONE);
            } else {
                descTextView.setText(R.string.av_audio_invite);
                outgoingActionContainer.setVisibility(View.GONE);
                incomingActionContainer.setVisibility(View.VISIBLE);
            }
        }
        String targetId = TJIMSDK.getSDK().message.sender;
        UserInfo userInfo = ChatManager.Instance().getUserInfo(targetId, false);
        GlideApp.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        nameTextView.setText(userViewModel.getUserDisplayName(userInfo));
        audioEnable = !TJIMSDK.getSDK().audioMuted;
        muteImageView.setSelected(!audioEnable);
        updateCallDuration();

        spearImageView.setSelected(!TJIMSDK.isHandsFreeOn());
        TJIMSDK.setCallBack(this);
    }

    private void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
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
        runOnUiThread(() -> {
            if (state == TJCallState.Connected) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                descTextView.setVisibility(View.GONE);
                durationTextView.setVisibility(View.VISIBLE);
            } else if (state == TJCallState.Idle) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().finish();
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    @Override
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom) {

    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {

    }
}

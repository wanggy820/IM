/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.tojoy.chat.kit.GlideApp;
import cn.tojoy.chat.kit.sdk.ParticipantProfile;
import cn.tojoy.chat.kit.sdk.TJAVCallMessageCallBack;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.tojoy.chat.kit.user.UserViewModel;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.R2;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallIncomingFragment extends Fragment implements TJAVCallMessageCallBack {

    @BindView(R2.id.invitorImageView)
    ImageView invitorImageView;
    @BindView(R2.id.invitorTextView)
    TextView invitorTextView;
    @BindView(R2.id.participantGridView)
    RecyclerView participantRecyclerView;

    @BindView(R2.id.acceptImageView)
    ImageView acceptImageView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_incoming, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        if(TJIMSDK.getSDK().content.isAudioOnly()) {
            acceptImageView.setImageResource(R.drawable.av_voice_answer_selector);
        }
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo invitor = userViewModel.getUserInfo(TJIMSDK.getSDK().message.sender, false);
        invitorTextView.setText(invitor.displayName);
        GlideApp.with(this).load(invitor.portrait).placeholder(R.mipmap.avatar_def).into(invitorImageView);

        List<String> participants = new ArrayList<>();
        for (ParticipantProfile profile : TJIMSDK.getSDK().getParticipants()) {
            if (!profile.userId.equals(TJIMSDK.getSDK().message.sender)) {
                participants.add(profile.userId);
            }
        }

        //把自己也加入到用户列表中
        participants.add(ChatManager.Instance().getUserId());
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);

        FlexboxLayoutManager manager = new FlexboxLayoutManager(getActivity(), FlexDirection.ROW);
        manager.setJustifyContent(JustifyContent.CENTER);

        MultiCallParticipantAdapter adapter = new MultiCallParticipantAdapter();
        adapter.setParticipants(participantUserInfos);
        participantRecyclerView.setLayoutManager(manager);
        participantRecyclerView.setAdapter(adapter);
        TJIMSDK.setCallBack(this);
    }


    @OnClick(R2.id.hangupImageView)
    void hangup() {
        ((MultiCallActivity) getActivity()).hangup();
    }

    @OnClick(R2.id.acceptImageView)
    void accept() {
        ((MultiCallActivity) getActivity()).accept();
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        ((MultiCallActivity) getActivity()).showFloatingView(null);
    }

    @Override
    public void didChangeState(TJCallState state) {

    }

    @Override
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom) {
        List<UserInfo> participants = ((MultiCallParticipantAdapter)participantRecyclerView.getAdapter()).getParticipants();
        if (isEnterRoom) {
            boolean exist = false;
            for (UserInfo user : participants) {
                if (user.uid.equals(userId)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
                participants.add(userViewModel.getUserInfo(userId, false));
                participantRecyclerView.getAdapter().notifyDataSetChanged();
            }
        } else  {
            for (UserInfo user : participants) {
                if (user.uid.equals(userId)) {
                    participants.remove(user);
                    participantRecyclerView.getAdapter().notifyDataSetChanged();
                    break;
                }
            }
            if (TJIMSDK.getSDK().message.conversation.target == null) {
                invitorTextView.setText("");
                invitorImageView.setImageBitmap(null);
            }
        }
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {

    }
}

/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.WfcUIKit;
import cn.tojoy.chat.kit.annotation.EnableContextMenu;
import cn.tojoy.chat.kit.annotation.MessageContentType;
import cn.tojoy.chat.kit.conversation.ConversationFragment;
import cn.tojoy.chat.kit.conversation.message.model.UiMessage;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.sdk.TJCallEndReason;
import cn.tojoy.chat.kit.sdk.TJCallState;
import cn.tojoy.chat.kit.sdk.TJIMSDK;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.model.Conversation;

@MessageContentType(CallStartMessageContent.class)
@EnableContextMenu
public class VoipMessageViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.contentTextView)
    TextView textView;

    @BindView(R2.id.callTypeImageView)
    ImageView callTypeImageView;

    public VoipMessageViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        CallStartMessageContent content = (CallStartMessageContent) message.message.content;
        if (content.getConnectTime() > 0 && content.getEndTime() > 0) {
            String text;
            long duration = (content.getEndTime() - content.getConnectTime()) / 1000;
            if (duration > 3600) {
                text = String.format("通话时长 %d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
            } else {
                text = String.format("通话时长 %02d:%02d", duration / 60, (duration % 60));
            }
            textView.setText(text);
        } else if (TJIMSDK.getSDK().state == TJCallState.Connected && TJIMSDK.getSDK().message != null && message.message.messageId == TJIMSDK.getSDK().message.messageId) {
            textView.setText("对话中");
        } else {
            String text = "未接通";
            if(message.message.content instanceof CallStartMessageContent) {
                CallStartMessageContent startMessageContent = (CallStartMessageContent)message.message.content;
                TJCallEndReason reason = TJCallEndReason.reason(startMessageContent.getStatus());
                if(reason == TJCallEndReason.UnKnown) {
                    text = "未接通";
                } else if(reason == TJCallEndReason.Busy) {
                    text = "线路忙";
                } else if(reason == TJCallEndReason.SignalError) {
                    text = "网络错误";
                } else if(reason == TJCallEndReason.Hangup) {
                    text = "已取消";
                } else if(reason == TJCallEndReason.MediaError) {
                    text = "网络错误";
                } else if(reason == TJCallEndReason.RemoteHangup) {
                    text = "对方已取消";
                } else if(reason == TJCallEndReason.OpenCameraFailure) {
                    text = "网络错误";
                } else if(reason == TJCallEndReason.Timeout) {
                    text = "未接听";
                } else if(reason == TJCallEndReason.AcceptByOtherClient) {
                    text = "已在其他端接听";
                } else if(reason == TJCallEndReason.AllLeft) {
                    text = "通话已结束";
                } else if(reason == TJCallEndReason.RemoteBusy) {
                    text = "对方已取消";
                } else if(reason == TJCallEndReason.RemoteTimeout) {
                    text = "对方未接听";
                } else if(reason == TJCallEndReason.RemoteNetworkError) {
                    text = "对方网络错误";
                } else if(reason == TJCallEndReason.RoomDestroyed) {
                    text = "通话已结束";
                } else if(reason == TJCallEndReason.RoomNotExist) {
                    text = "通话已结束";
                } else if(reason == TJCallEndReason.RoomParticipantsFull) {
                    text = "已达到最大通话人数";
                }
            }
            textView.setText(text);
        }

        if(content.isAudioOnly()) {
            callTypeImageView.setImageResource(R.mipmap.ic_msg_cell_voice_call);
        } else {
            callTypeImageView.setImageResource(R.mipmap.ic_msg_cell_video_call);
        }
    }

    @OnClick(R2.id.contentTextView)
    public void call(View view) {
        if (((CallStartMessageContent) message.message.content).getStatus() == 1) {
            return;
        }
        CallStartMessageContent callStartMessageContent = (CallStartMessageContent) message.message.content;
        if (message.message.conversation.type == Conversation.ConversationType.Single) {
            WfcUIKit.singleCall(fragment.getContext(), message.message.conversation.target, callStartMessageContent.isAudioOnly());
        } else {
            fragment.pickGroupMemberToVoipChat(callStartMessageContent.isAudioOnly());
        }
    }
    @OnClick(R2.id.callTypeImageView)
    public void onclick(View view) {
        call(view);
    }
}

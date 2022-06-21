/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.annotation.EnableContextMenu;
import cn.tojoy.chat.kit.annotation.MessageContentType;
import cn.tojoy.chat.kit.conversation.ConversationFragment;
import cn.tojoy.chat.kit.conversation.message.CompositeMessageContentActivity;
import cn.tojoy.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.CompositeMessageContent;


@MessageContentType(value = {
    CompositeMessageContent.class,

})
@EnableContextMenu
public class CompositeMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.contentTextView)
    TextView contentTextView;

    public CompositeMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        CompositeMessageContent content = (CompositeMessageContent) message.message.content;
        titleTextView.setText(content.getTitle());
        contentTextView.setText(content.compositeDigest());
    }

    @OnClick(R2.id.compositeContentLayout)
    public void onClick(View view) {
        Intent intent = new Intent(fragment.getContext(), CompositeMessageContentActivity.class);
        intent.putExtra("message", message.message);
        fragment.startActivity(intent);
    }

}

/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.group.manage;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.tojoy.chat.kit.R;
import cn.tojoy.chat.kit.contact.UserListAdapter;
import cn.tojoy.chat.kit.contact.model.HeaderValue;
import cn.tojoy.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfirechat.model.GroupInfo;

public class MuteGroupMemberViewHolder extends HeaderViewHolder<HeaderValue> {
    TextView titleTextView;

    public MuteGroupMemberViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        titleTextView = itemView.findViewById(R.id.nameTextView);
    }

    @Override
    public void onBind(HeaderValue headerValue) {
        GroupInfo groupInfo = (GroupInfo) headerValue.getValue();
        titleTextView.setText(headerValue.isBoolValue() ? "发言白名单" : "群成员禁言");
    }
}

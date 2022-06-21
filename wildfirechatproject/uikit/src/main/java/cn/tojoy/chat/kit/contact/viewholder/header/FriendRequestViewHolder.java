/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.contact.viewholder.header;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.tojoy.chat.kit.contact.UserListAdapter;
import cn.tojoy.chat.kit.contact.model.FriendRequestValue;
import cn.tojoy.chat.kit.R2;
import cn.wildfirechat.remote.ChatManager;

@SuppressWarnings("unused")
public class FriendRequestViewHolder extends HeaderViewHolder<FriendRequestValue> {
    @BindView(R2.id.unreadFriendRequestCountTextView)
    TextView unreadRequestCountTextView;
    private FriendRequestValue value;


    public FriendRequestViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    public void onBind(FriendRequestValue value) {
        this.value = value;
        int count = ChatManager.Instance().getUnreadFriendRequestStatus();
        if (count > 0) {
            unreadRequestCountTextView.setVisibility(View.VISIBLE);
            unreadRequestCountTextView.setText("" + count);
        } else {
            unreadRequestCountTextView.setVisibility(View.GONE);
        }
    }
}

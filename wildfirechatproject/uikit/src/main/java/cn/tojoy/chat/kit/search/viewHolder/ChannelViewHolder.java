/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.search.viewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.tojoy.chat.kit.R2;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.channelNameTextView)
    TextView channelNameTextView;

    public ChannelViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(ChannelInfo channelInfo) {
        channelNameTextView.setText(channelInfo.name);
        Glide.with(itemView.getContext()).load(channelInfo.portrait).into(portraitImageView);
    }
}

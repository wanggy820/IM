/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import butterknife.BindView;
import butterknife.OnClick;
import cn.tojoy.chat.kit.R2;
import cn.tojoy.chat.kit.WfcWebViewActivity;
import cn.tojoy.chat.kit.annotation.EnableContextMenu;
import cn.tojoy.chat.kit.annotation.MessageContentType;
import cn.tojoy.chat.kit.annotation.MessageContextMenuItem;
import cn.tojoy.chat.kit.conversation.ConversationFragment;
import cn.tojoy.chat.kit.conversation.message.model.UiMessage;
import cn.tojoy.chat.kit.mm.MMPreviewActivity;
import cn.tojoy.chat.kit.widget.LinkClickListener;
import cn.tojoy.chat.kit.widget.LinkTextViewMovementMethod;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.remote.ChatManager;


@MessageContentType(value = {
    TextMessageContent.class,
    PTextMessageContent.class

})
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.contentTextView)
    TextView contentTextView;
    @BindView(R2.id.refTextView)
    TextView refTextView;

    private QuoteInfo quoteInfo;

    public TextMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        TextMessageContent textMessageContent = (TextMessageContent) message.message.content;
        String content = textMessageContent.getContent();
        if (content.startsWith("<") && content.endsWith(">")) {
            contentTextView.setText(Html.fromHtml(content));
        } else {
            MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
        }
        contentTextView.setMovementMethod(new LinkTextViewMovementMethod(new LinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                WfcWebViewActivity.loadUrl(fragment.getContext(), "", link);
                return true;
            }
        }));

        quoteInfo = textMessageContent.getQuoteInfo();
        if (quoteInfo != null && quoteInfo.getMessageUid() > 0) {
            refTextView.setVisibility(View.VISIBLE);
            refTextView.setText(quoteInfo.getUserDisplayName() + ": " + quoteInfo.getMessageDigest());
        } else {
            refTextView.setVisibility(View.GONE);
        }
    }

    @OnClick(R2.id.contentTextView)
    public void onClick(View view) {
        String content = ((TextMessageContent) message.message.content).getContent();
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "消息内容", content);
    }

    @OnClick(R2.id.refTextView)
    public void onRefClick(View view) {
        Message message = ChatManager.Instance().getMessageByUid(quoteInfo.getMessageUid());
        if (message != null) {
            // TODO previewMessageActivity
            MessageContent messageContent = message.content;
            if (messageContent instanceof TextMessageContent) {
                WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "消息内容", ((TextMessageContent) messageContent).getContent());
            } else {
                if (messageContent instanceof VideoMessageContent) {
                    MMPreviewActivity.previewVideo(fragment.getActivity(), (VideoMessageContent) messageContent);
                } else if (messageContent instanceof ImageMessageContent) {
                    MMPreviewActivity.previewImage(fragment.getActivity(), (ImageMessageContent) messageContent);
                }
            }
        }
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        TextMessageContent content = (TextMessageContent) message.message.content;
        ClipData clipData = ClipData.newPlainText("messageContent", content.getContent());
        clipboardManager.setPrimaryClip(clipData);
    }


    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CLIP.equals(tag)) {
            return "复制";
        }
        return super.contextMenuTitle(context, tag);
    }
}

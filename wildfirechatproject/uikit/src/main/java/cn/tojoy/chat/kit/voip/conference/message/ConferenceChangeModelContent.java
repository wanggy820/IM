/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.tojoy.chat.kit.voip.conference.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Conference_Change_Model;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Conference_Change_Model, flag = PersistFlag.Transparent)
public class ConferenceChangeModelContent extends MessageContent {
    private String callId;
    private boolean audience;


    public ConferenceChangeModelContent() {
    }

    public ConferenceChangeModelContent(String callId, boolean audience) {
        this.callId = callId;
        this.audience = audience;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public boolean isAudience() {
        return audience;
    }

    public void setAudience(boolean audience) {
        this.audience = audience;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;

        try {
            JSONObject objWrite = new JSONObject();
            if (audience) {
                objWrite.put("a", audience);
            }

            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        callId = payload.content;

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                audience = jsonObject.optBoolean("a", false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[网络电话]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.callId);
        dest.writeInt(this.audience ? 1 : 0);
    }

    protected ConferenceChangeModelContent(Parcel in) {
        super(in);
        this.callId = in.readString();
        this.audience = in.readInt() == 1;
    }

    public static final Creator<ConferenceChangeModelContent> CREATOR = new Creator<ConferenceChangeModelContent>() {
        @Override
        public ConferenceChangeModelContent createFromParcel(Parcel source) {
            return new ConferenceChangeModelContent(source);
        }

        @Override
        public ConferenceChangeModelContent[] newArray(int size) {
            return new ConferenceChangeModelContent[size];
        }
    };
}

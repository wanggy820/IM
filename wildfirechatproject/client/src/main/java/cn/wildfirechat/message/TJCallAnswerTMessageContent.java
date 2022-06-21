package cn.wildfirechat.message;

import android.os.Parcel;
import org.json.JSONException;
import org.json.JSONObject;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_Accept_T;

@ContentTag(type = ContentType_Call_Accept_T, flag = PersistFlag.Transparent)
public class TJCallAnswerTMessageContent extends MessageContent {
    private int roomId;
    private long inviteMsgUid;
    private boolean audioOnly;

    public TJCallAnswerTMessageContent() {
    }

    public TJCallAnswerTMessageContent(int roomId, long inviteMsgUid, boolean audioOnly) {
        this.roomId = roomId;
        this.inviteMsgUid = inviteMsgUid;
        this.audioOnly = audioOnly;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public long getInviteMsgUid() {
        return inviteMsgUid;
    }

    public void setInviteMsgUid(long inviteMsgUid) {
        this.inviteMsgUid = inviteMsgUid;
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }


    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = String.valueOf(roomId);

        try {
            JSONObject objWrite = new JSONObject();
            if (inviteMsgUid > 0) {
                objWrite.put("i", inviteMsgUid);
            }
            objWrite.put("a", audioOnly ? 1 : 0);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        roomId = Integer.valueOf(payload.content);

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                inviteMsgUid = jsonObject.optLong("i", 0);
                audioOnly = jsonObject.optInt("a") > 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[通话中...]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.roomId);
        dest.writeLong(this.inviteMsgUid);
        dest.writeByte(this.audioOnly ? (byte) 1 : (byte) 0);
    }

    protected TJCallAnswerTMessageContent(Parcel in) {
        super(in);
        this.roomId = in.readInt();
        this.inviteMsgUid = in.readLong();
        this.audioOnly = in.readByte() != 0;
    }

    public static final Creator<TJCallAnswerTMessageContent> CREATOR = new Creator<TJCallAnswerTMessageContent>() {
        @Override
        public TJCallAnswerTMessageContent createFromParcel(Parcel source) {
            return new TJCallAnswerTMessageContent(source);
        }

        @Override
        public TJCallAnswerTMessageContent[] newArray(int size) {
            return new TJCallAnswerTMessageContent[size];
        }
    };
}

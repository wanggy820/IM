package cn.wildfirechat.message;

import android.os.Parcel;
import org.json.JSONException;
import org.json.JSONObject;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Call_End;

@ContentTag(type = ContentType_Call_End, flag = PersistFlag.No_Persist)
public class TJCallByeMessageContent  extends MessageContent {
    private int roomId;
    private long inviteMsgUid;
    private boolean audioOnly;
    /**
     * 0, UnKnown,
     * 1, Busy,
     * 2, SignalError,
     * 3, Hangup,
     * 4, MediaError,
     * 5, RemoteHangup,
     * 6, OpenCameraFailure,
     * 7, Timeout,
     * 8, AcceptByOtherClient
     */
    private int endReason;

    public TJCallByeMessageContent() {
    }

    public TJCallByeMessageContent(int roomId, long inviteMsgUid, boolean audioOnly, int endReason) {
        this.roomId = roomId;
        this.inviteMsgUid = inviteMsgUid;
        this.audioOnly = audioOnly;
        this.endReason = endReason;
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

    public int getEndReason() {
        return endReason;
    }

    public void setEndReason(int endReason) {
        this.endReason = endReason;
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
            if (endReason > 0) {
                objWrite.put("e", endReason);
            }
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
                endReason = jsonObject.optInt("e", 0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        if (this.audioOnly) {
            return "[语音对话]";
        }
        return "[视频对话]";
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
        dest.writeInt(this.endReason);
    }

    protected TJCallByeMessageContent(Parcel in) {
        super(in);
        this.roomId = in.readInt();
        this.inviteMsgUid = in.readLong();
        this.audioOnly = in.readByte() != 0;
        this.endReason = in.readInt();
    }

    public static final Creator<TJCallByeMessageContent> CREATOR = new Creator<TJCallByeMessageContent>() {
        @Override
        public TJCallByeMessageContent createFromParcel(Parcel source) {
            return new TJCallByeMessageContent(source);
        }

        @Override
        public TJCallByeMessageContent[] newArray(int size) {
            return new TJCallByeMessageContent[size];
        }
    };
}

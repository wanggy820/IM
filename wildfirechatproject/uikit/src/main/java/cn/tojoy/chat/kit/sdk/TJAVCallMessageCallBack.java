package cn.tojoy.chat.kit.sdk;

public interface TJAVCallMessageCallBack {
    public void didChangeState(TJCallState state);
    public void didReceiveParticipantProfile(String userId, boolean isEnterRoom);
    public void didReportAudioVolume(String userId, int volume);
}

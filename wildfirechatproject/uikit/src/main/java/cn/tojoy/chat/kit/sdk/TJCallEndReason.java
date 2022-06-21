package cn.tojoy.chat.kit.sdk;


public enum TJCallEndReason {
    UnKnown,
    Busy,
    SignalError,
    Hangup,
    MediaError,
    RemoteHangup,
    OpenCameraFailure,
    Timeout,
    AcceptByOtherClient,
    AllLeft,
    RemoteBusy,
    RemoteTimeout,
    RemoteNetworkError,
    RoomDestroyed,
    RoomNotExist,
    RoomParticipantsFull;

    public static TJCallEndReason reason(int reason) {
        return reason >= 0 && reason < values().length ? values()[reason] : UnKnown;
    }
}

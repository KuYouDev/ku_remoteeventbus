package kuyou.common.ipc;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import kuyou.common.ipc.basic.IRemoteEventFrameStatusListener;

/**
 * action :IPC框架内部事件处理器
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-21 <br/>
 * </p>
 */
public class FrameEventHandler extends RemoteEventHandler {

    private static final String TAG = "kuyou.common.ipc > RemoteFrameEventHandler";

    private IRemoteEventFrameStatusListener mRemoteEventFrameStatusListener;

    private volatile static FrameEventHandler sInstance;

    private FrameEventHandler() {
        List<Integer> list = new ArrayList<>();
        list.add(Code.BIND_IPC_SERVICE_SUCCESS);
        list.add(Code.BIND_IPC_SERVICE_TIME_OUT);
        list.add(Code.UNBIND_IPC_SERVICE);
        list.add(Code.CLIENT_REGISTER_SUCCESS);
        list.add(Code.CLIENT_REGISTER_FAIL);
        setEventDispatchList(list);
    }

    public static FrameEventHandler getInstance() {
        if (sInstance == null) {
            synchronized (FrameEventHandler.class) {
                if (sInstance == null) {
                    sInstance = new FrameEventHandler();
                }
            }
        }
        return sInstance;
    }

    public static boolean isFrameEvent(int eventCode) {
        return Code.FRAME_FLAG >= eventCode;
    }

    public static boolean isFrameEvent(RemoteEvent event) {
        return Code.FRAME_FLAG >= event.getCode();
    }

    public void setRemoteEventFrameStatusListener(IRemoteEventFrameStatusListener remoteEventFrameStatusListener) {
        mRemoteEventFrameStatusListener = remoteEventFrameStatusListener;
    }

    public void dispatchFrameStatus(int code) {
        if (null == mRemoteEventFrameStatusListener)
            return;
        mRemoteEventFrameStatusListener.onStatus(code);
    }

    @Override
    protected int remoteEventFilterPolicy(Bundle data) {
        //非框架事件视为无效
        if (!isFrameEvent(RemoteEvent.getCodeByData(data))) {
            return -1;
        }
        return super.remoteEventFilterPolicy(data);
    }

    @Override
    public void remoteEvent2LocalEvent(Bundle data) {
        final int code = RemoteEvent.getCodeByData(data);
        switch (code) {
            case Code.CLIENT_REGISTER_SUCCESS:

                break;
            default:
                break;
        }
    }
}

package kuyou.common.ipc;

import android.os.Bundle;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kuyou.common.assist.IAssistHandler;
import kuyou.common.ipc.basic.IEventBusDispatchCallback;
import kuyou.common.ipc.basic.IEventDispatcher;
import kuyou.common.ipc.event.RemoteEvent;

/**
 * action :事件分发器[本地转远程，远程转本地][抽象]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-24 <br/>
 * </p>
 */
public class EventDispatcherImpl implements IEventDispatcher<EventDispatcherImpl> {

    private volatile static EventDispatcherImpl sInstance;

    public static EventDispatcherImpl getInstance() {
        if (sInstance == null) {
            synchronized (EventDispatcherImpl.class) {
                if (sInstance == null) {
                    sInstance = new EventDispatcherImpl();
                }
            }
        }
        return sInstance;
    }

    private EventDispatcherImpl() {
    }

    protected final static int MSG_RECEIVE_EVENT = 0;
    protected String mTagLog = "kuyou.common.ipc > EventDispatcherImpl";
    private String mLocalModulePackageName;
    private List<Integer> mEventReceiveList = null;
    private Queue<RemoteEvent> mEventsToBeSendQueue = new LinkedList<>();
    private IEventBusDispatchCallback mEventBusDispatchCallbackRemote,
            mEventBusDispatchCallbackAssistHandler;

    @Override
    public EventDispatcherImpl setEventReceiveList(List<Integer> list) {
        if (null == list) {
            return EventDispatcherImpl.this;
        }
        if (null == mEventReceiveList) {
            mEventReceiveList = new ArrayList<>();
        }
        Log.d(mTagLog, "setEventReceiveList > ");
        for (Integer code : list) {
            if (-1 != mEventReceiveList.indexOf(code))
                continue;
            mEventReceiveList.add(code);
            Log.d(mTagLog, "setEventReceiveList > code = " + code);
        }
        return EventDispatcherImpl.this;
    }

    @Override
    public void setRemoteCallback(IEventBusDispatchCallback val) {
        mEventBusDispatchCallbackRemote = val;
        performEventsToBeSend();
    }

    protected IEventBusDispatchCallback getRemoteCallback() {
        return mEventBusDispatchCallbackRemote;
    }

    @Override
    public EventDispatcherImpl setAssistHandlerConfig(IAssistHandler val) {
        if (null != val) {
            setEventReceiveList(val.getEventReceiveList());
            mEventBusDispatchCallbackAssistHandler = val.getEventDispatchCallback();
        }
        return EventDispatcherImpl.this;
    }

    protected IEventBusDispatchCallback getAssistHandlerCallback() {
        return mEventBusDispatchCallbackAssistHandler;
    }

    public EventDispatcherImpl setLocalModulePackageName(String val) {
        if (null != val) {
            mTagLog = new StringBuilder(mTagLog).append(" > ").append(val).toString();
            Log.d(mTagLog, "setLocalModulePackageName > ");
            mLocalModulePackageName = val;
        }
        return EventDispatcherImpl.this;
    }

    protected String getLocalModulePackageName() {
        return mLocalModulePackageName;
    }

    @Override
    public void dispatchEventRemote2Local(Bundle data) {
        int eventCode = receiveEventFilterPolicy(data);
        if (-1 == eventCode) {
            return;
        }
        Log.d(mTagLog, "dispatchEventRemote2Local > eventCode = " + eventCode);
        final RemoteEvent event = new RemoteEvent() {
            @Override
            public int getCode() {
                return eventCode;
            }
        }.setData(data);

        //使用协处理器分发
        if (null != getAssistHandlerCallback()
                && getAssistHandlerCallback().dispatchEvent(event)) {
            return;
        }
        //用默认分发
        EventBus.getDefault().post(event);
    }

    @Override
    public final boolean dispatch(RemoteEvent event) {
        //远程分发
        if (event.isRemote()) {
            boolean result = false;
            if (null == getRemoteCallback()) {
                if (mEventsToBeSendQueue.size() < 128) {
                    mEventsToBeSendQueue.offer(event);
                } else {
                    Log.w(mTagLog, "dispatch > process fail : mEventsToBeSendQueue is full ");
                }
            } else {
                event.setStartPackageName(getLocalModulePackageName());
                event.setStartProcessID(android.os.Process.myPid());
                result = getRemoteCallback().dispatchEvent(event);
                if (!result) {
                    Log.w(mTagLog, "dispatch > process fail : send event code = " + event.getCode());
                }
            }
            if (!event.isDispatch2Myself()) {
                return result;
            }
        }
        //使用协处理器分发
        if (null != getAssistHandlerCallback()
                && getAssistHandlerCallback().dispatchEvent(event)) {
            return true;
        }
        //用默认分发
        EventBus.getDefault().post(event);
        return true;
    }

    protected List<Integer> getEventReceiveList() {
        return mEventReceiveList;
    }

    protected int receiveEventFilterPolicy(Bundle data) {
        if (null == mLocalModulePackageName) {
            throw new NullPointerException("LocalModulePackageName is null,\nplease perform method \"setLocalModulePackageName(String val)\"");
        }
        if (mLocalModulePackageName.equals(RemoteEvent.getStartPackageNameByData(data))) {
            Log.d(mTagLog, "receiveEventFilterPolicy > give up event start package name = " + mLocalModulePackageName);
            return -1;
        }
        int eventCode = RemoteEvent.getCodeByData(data);
        //不设置 mEventReceiveList，就视为远程事件全部接收
        if (null != getEventReceiveList() && -1 == getEventReceiveList().indexOf(eventCode)) {
            //Log.d(mTagLog, "receiveEventFilterPolicy > give up event = " + eventCode);
            return -1;
        }
        return eventCode;
    }

    protected void performEventsToBeSend() {
        if (null == getRemoteCallback()) {
            Log.w(mTagLog, "performEventsToBeSend > process fail : dispatchEventCallBack is null");
            return;
        }
        synchronized (mEventsToBeSendQueue) {
            if (mEventsToBeSendQueue.size() == 0) {
                return;
            }
            Log.d(mTagLog, "performEventsToBeSend > start cleaning up the event to send events ");
            RemoteEvent event;
            while (true) {
                event = mEventsToBeSendQueue.poll();
                if (null == event) {
                    break;
                }
                dispatch(event);
            }
        }
    }
}

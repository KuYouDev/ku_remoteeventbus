package kuyou.common.ipc;

import android.os.Bundle;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kuyou.common.assist.AssistHandlerManager;
import kuyou.common.assist.IEventBusDispatchCallback;
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
    private IEventBusDispatchCallback mEventBusDispatchRemoteCallback;

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

    public IEventBusDispatchCallback getEventBusDispatchRemoteCallback() {
        return mEventBusDispatchRemoteCallback;
    }

    public EventDispatcherImpl setEventBusDispatchRemoteCallback(IEventBusDispatchCallback val) {
        mEventBusDispatchRemoteCallback = val;
        performEventsToBeSend();
        return EventDispatcherImpl.this;
    }

    public EventDispatcherImpl setLocalModulePackageName(String val) {
        mTagLog = new StringBuilder(mTagLog).append(" > ").append(val).toString();
        Log.d(mTagLog, "setLocalModulePackageName > ");
        mLocalModulePackageName = val;
        return EventDispatcherImpl.this;
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
        //协处理器处理
        if (AssistHandlerManager.getInstance().dispatchReceiveEventNotice(event)) {
            return;
        }
        EventBus.getDefault().post(event);
    }

    @Override
    public final boolean dispatch(RemoteEvent event) {
        if (null == getEventBusDispatchRemoteCallback()) {
            if (mEventsToBeSendQueue.size() < 128) {
                mEventsToBeSendQueue.offer(event);
            } else {
                Log.w(mTagLog, "dispatch > process fail : mEventsToBeSendQueue is full ");
            }
            return false;
        }
        boolean result = true;
        if (event.isRemote()) {
            result = getEventBusDispatchRemoteCallback().dispatchEvent(event);
            if (!result) {
                Log.w(mTagLog, "dispatch > process fail : send event code = " + event.getCode());
            }
            if (!event.isDispatch2Myself()) {
                return result;
            }
        }
        //协处理器处理
        if (AssistHandlerManager.getInstance().dispatchReceiveEventNotice(event)) {
            return result;
        }
        EventBus.getDefault().post(event);
        return result;
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
        if (null == getEventBusDispatchRemoteCallback()) {
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

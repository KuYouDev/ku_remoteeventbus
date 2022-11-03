package kuyou.common.ipc;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import kuyou.common.ipc.basic.IRemoteEventHandler;

/**
 * action :远程事件接收，本地分发器[抽象]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-24 <br/>
 * </p>
 */
public class RemoteEventHandler implements IRemoteEventHandler {
    protected final static int MSG_RECEIVE_EVENT = 0;

    protected String mTagLog = "kuyou.common.ipc > RemoteEventHandler";
    protected String mLocalModulePackageName;

    private List<Integer> mEventDispatchList = new ArrayList<>();
    private HandlerThread mHandlerThreadEvent;
    private Handler mHandlerEvent;

    private List<Bundle> mRemoteEventDataCache = new ArrayList<>();

    private volatile static RemoteEventHandler sInstance;

    public RemoteEventHandler() {
        super();
        mHandlerThreadEvent = new HandlerThread("RemoteEvent.sub.thread.dispatch");
        mHandlerThreadEvent.start();
        mHandlerEvent = new Handler(mHandlerThreadEvent.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                RemoteEventHandler.this.handleMessage(msg);
            }
        };
    }

    public static RemoteEventHandler getInstance() {
        if (sInstance == null) {
            synchronized (RemoteEventHandler.class) {
                if (sInstance == null) {
                    sInstance = new RemoteEventHandler();
                }
            }
        }
        return sInstance;
    }

    protected List<Integer> getEventDispatchList() {
        return mEventDispatchList;
    }

    public RemoteEventHandler setEventDispatchList(List<Integer> list) {
        if (null == list) {
            Log.w(mTagLog, "setEventDispatchList > process fail : list is null");
            return RemoteEventHandler.this;
        }
        for (Integer code : list) {
            if (-1 != mEventDispatchList.indexOf(code))
                continue;
            mEventDispatchList.add(code);
            Log.d(mTagLog, "setEventDispatchList > code = " + code);
        }
        Log.d(mTagLog, "setEventDispatchList > ");
        return RemoteEventHandler.this;
    }

    @Override
    public String getLocalModulePackageName() {
        return mLocalModulePackageName;
    }

    public RemoteEventHandler setLocalModulePackageName(String val) {
        mTagLog = new StringBuilder(mTagLog).append(" > ").append(val).toString();
        Log.d(mTagLog, "setLocalModulePackageName > ");
        mLocalModulePackageName = val;
        return RemoteEventHandler.this;
    }

    protected int remoteEventFilterPolicy(Bundle data) {
        if (null == mLocalModulePackageName) {
            throw new NullPointerException("LocalModulePackageName is null,\nplease perform method \"setLocalModulePackageName(String val)\"");
        }
        if (null == mEventDispatchList) {
            throw new NullPointerException("EventDispatchList is null,\nplease perform method \"setEventDispatchList(List<Integer> list)\"");
        }
        int eventCode = RemoteEvent.getCodeByData(data);
        if (-1 == getEventDispatchList().indexOf(eventCode)) {
            Log.d(mTagLog, "remoteEventFilterPolicy > give up event = " + eventCode);
            return -1;
        }
        if (mLocalModulePackageName.equals(RemoteEvent.getStartPackageNameByData(data))) {
            Log.d(mTagLog, "remoteEventFilterPolicy > give up event start package name = " + mLocalModulePackageName);
            return -1;
        }
        return eventCode;
    }

    @Override
    public void remoteEvent2LocalEvent(Bundle data) {
        int eventCode = remoteEventFilterPolicy(data);
        if (-1 == eventCode) {
            return;
        }
        EventBus.getDefault().post(new RemoteEvent() {
            @Override
            public int getCode() {
                return eventCode;
            }
        }.setData(data));
    }

    private void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_RECEIVE_EVENT:
                synchronized (mRemoteEventDataCache) {
                    for (Bundle data : mRemoteEventDataCache) {
                        int eventCode = remoteEventFilterPolicy(data);
                        if (-1 == eventCode) {
                            return;
                        }
                        EventBus.getDefault().post(new RemoteEvent() {
                            @Override
                            public int getCode() {
                                return eventCode;
                            }
                        }.setData(data));
                    }
                    mRemoteEventDataCache.clear();
                }
                Log.d(mTagLog, "RemoteEvent.dispatch.sub.thread > handleMessage : MSG_RECEIVE_EVENT");
                break;
            default:
                break;
        }
    }
}

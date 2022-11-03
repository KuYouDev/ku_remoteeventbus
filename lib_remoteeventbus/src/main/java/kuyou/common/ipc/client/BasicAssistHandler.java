package kuyou.common.ipc.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kuyou.common.ipc.RemoteEvent;
import kuyou.common.status.StatusProcessBusImpl;
import kuyou.common.status.basic.IStatusProcessBus;

/**
 * action :业务处理器[抽象]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-23 <br/>
 * </p>
 */
public abstract class BasicAssistHandler {

    protected static String TAG = "kuyou.common.ipc.client > BasicAssistHandler";

    private Context mContext;

    private boolean isAmIDead = false;

    private boolean isReady = false;
    private List<RemoteEvent> mEventsToBeSendList = new ArrayList<RemoteEvent>();

    protected Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    protected Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        TAG = new StringBuilder()
                .append(context.getPackageName())
                .append(" > ")
                .append(this.getClass().getSimpleName())
                .toString();
        mContext = context;
    }

    public boolean isEnable() {
        return true;
    }

    //协处理器 相关
    public void start() {

    }

    public void stop() {

    }

    public boolean isAmIDead() {
        return isAmIDead;
    }

    public void setAmIDead(boolean flag) {
        isAmIDead = flag;
    }

    public void setReady(boolean ready) {
        isReady = ready;
        performEventsToBeSend();
    }

    protected boolean isReady() {
        return isReady;
    }

    protected void performEventsToBeSend() {
        synchronized (mEventsToBeSendList) {
            if (mEventsToBeSendList.size() == 0) {
                return;
            }
            if (!isReady()) {
                Log.w(TAG, "performEventsToBeSend > process fail : is not ready");
                return;
            }
            if (null == getDispatchEventCallBack()) {
                Log.w(TAG, "performEventsToBeSend > process fail : dispatchEventCallBack is null");
                return;
            }
            Log.d(TAG, "performEventsToBeSend > start cleaning up the event to send events ");
            for (RemoteEvent event : mEventsToBeSendList) {
                getDispatchEventCallBack().dispatchEvent(event);
            }
            mEventsToBeSendList.clear();
        }
    }

    /**
     * action: 嵌套的协处理器列表 <br/>
     * remarks: 协处理里面有嵌套的协处理器时请重载此方法，已保证正常初始化
     */
    public List<BasicAssistHandler> getSubEventHandlers() {
        return null;
    }

    // StatusProcessBus 相关

    private IStatusProcessBus mStatusProcessBus;

    public IStatusProcessBus getStatusProcessBus() {
        initStatusProcessBus();
        return mStatusProcessBus;
    }

    final public void initStatusProcessBus() {
        if (null != mStatusProcessBus) {
            return;
        }
        mStatusProcessBus = new StatusProcessBusImpl() {
            @Override
            protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {
                BasicAssistHandler.this.onReceiveProcessStatusNotice(statusCode, isRemove);
            }
        };
        initReceiveProcessStatusNotices();
    }

    protected void initReceiveProcessStatusNotices() {
    }

    protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {

    }

    //RemoteEventBus 相关

    private IEventBusDispatchCallback mEventBusDispatchCallBack;
    private List<Integer> mHandleLocalEventCodeList = null,
            mHandleRemoteEventCodeList = null;

    public boolean onReceiveEventNotice(RemoteEvent event) {
        return false;
    }

    protected void initReceiveEventNotices() {
    }

    protected BasicAssistHandler registerHandleEvent(int eventCode) {
        registerHandleEvent(eventCode, true);
        return BasicAssistHandler.this;
    }

    protected BasicAssistHandler registerHandleEvent(int eventCode, boolean isRemote) {
        if (null == mHandleLocalEventCodeList) {
            mHandleLocalEventCodeList = new ArrayList<>();
            mHandleRemoteEventCodeList = new ArrayList<>();
        }
        if (isRemote) {
            mHandleRemoteEventCodeList.add(eventCode);
        } else {
            mHandleLocalEventCodeList.add(eventCode);
        }
        return BasicAssistHandler.this;
    }

    protected boolean unRegisterHandleEvent(int eventCode) {
        if (-1 != getHandleLocalEventCodeList().indexOf(eventCode)) {
            getHandleLocalEventCodeList().remove(Integer.valueOf(eventCode));
            return true;
        }
        if (-1 != getHandleRemoteEventCodeList().indexOf(eventCode)) {
            getHandleRemoteEventCodeList().remove(Integer.valueOf(eventCode));
            return true;
        }
        return false;
    }

    public List<Integer> getHandleLocalEventCodeList() {
        if (null == mHandleLocalEventCodeList) {
            mHandleLocalEventCodeList = new ArrayList<>();
            initReceiveEventNotices();
        }
        return mHandleLocalEventCodeList;
    }

    public List<Integer> getHandleRemoteEventCodeList() {
        if (null == mHandleRemoteEventCodeList) {
            mHandleRemoteEventCodeList = new ArrayList<>();
            initReceiveEventNotices();
        }
        return mHandleRemoteEventCodeList;
    }

    public BasicAssistHandler setDispatchEventCallBack(IEventBusDispatchCallback dispatchEventCallBack) {
        mEventBusDispatchCallBack = dispatchEventCallBack;
        performEventsToBeSend();
        return BasicAssistHandler.this;
    }

    protected IEventBusDispatchCallback getDispatchEventCallBack() {
        return mEventBusDispatchCallBack;
    }

    protected boolean dispatchEvent(RemoteEvent event) {
        if (null == event) {
            Log.e(TAG, "dispatchEvent > process fail : event is null");
            return false;
        }
        if (!isReady() || null == mEventBusDispatchCallBack) {
            synchronized (mEventsToBeSendList) {
                mEventsToBeSendList.add(event);
            }
            Log.e(TAG, "dispatchEvent > process fail : mDispatchEventCallBack is not ready ,eventCode = " + event.getCode());
            return false;
        }

        return mEventBusDispatchCallBack.dispatchEvent(event);
    }

    protected boolean dispatchEvent(final int eventCode) {
        return dispatchEvent(new RemoteEvent() {
            @Override
            public int getCode() {
                return eventCode;
            }
        });
    }

    protected void runOnUiThread(Runnable runnable) {
        mMainThreadHandler.post(runnable);
    }

    protected final <T extends BasicAssistHandler> T findHandlerByFlag(Class<?> flag) {
        if (null != getHandlerGroupManager() && getHandlerGroupManager().getHandlerGroup().containsKey(flag)) {
            return (T) getHandlerGroupManager().getHandlerGroup().get(flag);
        }
        Log.e(TAG, "findHandlerByFlag > process fail : flag = " + flag);
        return null;
    }

    private IHandlerGroupManager<Class<?>, BasicAssistHandler> mHandlerGroupManager;

    protected IHandlerGroupManager<Class<?>, BasicAssistHandler> getHandlerGroupManager() {
        return mHandlerGroupManager;
    }

    public BasicAssistHandler setHandlerGroupManager(IHandlerGroupManager<Class<?>, BasicAssistHandler> manager) {
        mHandlerGroupManager = manager;
        return BasicAssistHandler.this;
    }
}

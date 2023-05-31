package kuyou.common.assist;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import kuyou.common.ipc.RemoteEventBus;
import kuyou.common.ipc.basic.IEventBusDispatchCallback;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.status.StatusProcessBusImpl;
import kuyou.common.status.basic.IStatusProcessBus;
import kuyou.common.status.basic.IStatusProcessBusCallback;

/**
 * action :业务处理器[抽象]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-23 <br/>
 * </p>
 */
public abstract class BasicAssistHandler implements RemoteEventBus.ILiveListener {
    protected static String TAG = "kuyou.common.ipc.client > BasicAssistHandler";

    protected Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Queue<RemoteEvent> mEventsToBeSendQueue = new LinkedList<>();

    private IHandlerFinder<BasicAssistHandler, Class<?>> mHandlerFinder;
    private IEventBusDispatchCallback mEventBusReceiveCallBack;
    private List<Integer> mHandleRegisterEventCodeList = new ArrayList<>(),
            mHandleRegisterRemoteEventCodeList = new ArrayList<>();
    private IStatusProcessBus mStatusProcessBus;
    private Context mContext;
    private boolean isReady = false;

    public BasicAssistHandler() {
        initStatusProcessBus();
        initReceiveProcessStatusNotices();
        initReceiveEventNotices();
    }

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

    /**
     * action: 是否开启允许被自动加载  <br/>
     *
     * @return boolean 默认允许 <br/>
     */
    public boolean isEnable() {
        return true;
    }

    //协处理器 相关
    public void start() {
        //EventBus.getDefault().register(BasicAssistHandler.this);
    }

    public void stop() {
        EventBus.getDefault().unregister(BasicAssistHandler.this);
    }

    @Override
    public void onEventDispatchServiceConnectChange(boolean isConnect) {
        setReady(isConnect);
    }

    public void setReady(boolean ready) {
        isReady = ready;
        performEventsToBeSend();
    }

    protected boolean isReady() {
        return isReady;
    }

    protected void performEventsToBeSend() {
        if (!isReady()) {
            Log.w(TAG, "performEventsToBeSend > process fail : is not ready");
            return;
        }
        synchronized (mEventsToBeSendQueue) {
            if (mEventsToBeSendQueue.size() == 0) {
                return;
            }
            Log.d(TAG, "performEventsToBeSend > start cleaning up the event to send events ");
            RemoteEvent event;
            RemoteEventBus eventBus = RemoteEventBus.getInstance();
            while (true) {
                event = mEventsToBeSendQueue.poll();
                if (null == event) {
                    break;
                }
                eventBus.post(event);
            }
        }
    }

    public IStatusProcessBus getStatusProcessBus() {
        return mStatusProcessBus;
    }

    final public void initStatusProcessBus() {
        if (null != mStatusProcessBus) {
            return;
        }
        mStatusProcessBus = new StatusProcessBusImpl() {
            //callback 主要为获取数据
            @Override
            protected void onReceiveProcessStatusNotice(int statusCode, Bundle data, boolean isRemove) {
                BasicAssistHandler.this.onReceiveProcessStatusNotice(statusCode, data, isRemove);
            }
        };
    }

    protected void initReceiveProcessStatusNotices() {
    }

    protected void onReceiveProcessStatusNotice(int statusCode, Bundle data, boolean isRemove) {
        onReceiveProcessStatusNotice(statusCode, isRemove);
    }

    protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {

    }

    @Subscribe
    public void onReceiveEvent(RemoteEvent event) {
        onReceiveEventNotice(event);
    }

    protected boolean onReceiveEventNotice(RemoteEvent event) {
        return false;
    }

    protected void initReceiveEventNotices() {
    }

    protected BasicAssistHandler registerHandleEvent(int eventCode) {
        registerHandleEvent(eventCode, true);
        return BasicAssistHandler.this;
    }

    protected BasicAssistHandler registerHandleEvent(int eventCode, boolean isRemote) {
        if (isRemote) {
            mHandleRegisterRemoteEventCodeList.add(eventCode);
        }
        mHandleRegisterEventCodeList.add(eventCode);
        return BasicAssistHandler.this;
    }

    public List<Integer> getHandleRegisterEventCodeList() {
        return mHandleRegisterEventCodeList;
    }

    public List<Integer> getHandleRegisterRemoteEventCodeList() {
        return mHandleRegisterRemoteEventCodeList;
    }

    public List<Integer> getEventReceiveList() {
        return getHandleRegisterEventCodeList();
    }

    protected boolean dispatchEvent(RemoteEvent event) {
        if (null == event) {
            Log.e(TAG, "dispatchEvent > process fail : event is null");
            return false;
        }
        if (!isReady() && event.isSticky()) {
            mEventsToBeSendQueue.offer(event);
            Log.w(TAG, "dispatchEvent > process fail : is not ready ,eventCode = " + event.getCode());
            return false;
        }
        return RemoteEventBus.getInstance().post(event);
    }

    protected boolean dispatchEvent(final int eventCode) {
        return dispatchEvent(new RemoteEvent() {
            @Override
            public int getCode() {
                return eventCode;
            }
        }.setRemote(false));
    }

    protected void runOnUiThread(Runnable runnable) {
        mMainThreadHandler.post(runnable);
    }

    protected final <T extends BasicAssistHandler> T findHandlerByFlag(Class<?> flag) {
        if (null == mHandlerFinder) {
            Log.w(TAG, "findHandlerByFlag > process fail : mHandlerFinder is null");
            return null;
        }
        return mHandlerFinder.findHandlerByFlag(flag);
    }

    public BasicAssistHandler setHandlerFinder(IHandlerFinder<BasicAssistHandler, Class<?>> val) {
        mHandlerFinder = val;
        return BasicAssistHandler.this;
    }
}

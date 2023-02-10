package kuyou.common.ipc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kuyou.common.assist.IAssistHandler;
import kuyou.common.ipc.basic.IEventBusDispatchCallback;
import kuyou.common.ipc.basic.IEventDispatcher;
import kuyou.common.ipc.basic.IRemoteConfig;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.status.StatusProcessBusCallbackImpl;
import kuyou.common.status.StatusProcessBusImpl;
import kuyou.common.status.basic.IStatusProcessBus;
import kuyou.common.status.basic.IStatusProcessBusCallback;

/**
 * action :事件远程分发器
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-27 <br/>
 * </p>
 */
public class RemoteEventBus implements IRemoteConfig {

    private volatile static RemoteEventBus sInstance;

    private RemoteEventBus() {

    }

    public static RemoteEventBus getInstance() {
        if (sInstance == null) {
            synchronized (RemoteEventBus.class) {
                if (sInstance == null) {
                    sInstance = new RemoteEventBus();
                }
            }
        }
//        else if (null == sInstance.getRegisterConfig()) {
//            throw new RuntimeException("RemoteEventBus is not register");
//        }
        return sInstance;
    }

    //服务端重启启动后，通知客户端连接
    public static final String ACTION_IPC_BOOT = "action.kuyou.ipc.boot";
    protected final String IPC_FRAME_PACKAGE_NAME_NORMAL = "com.kuyou.ipc";
    protected final static int PS_NOTICE_IPC_PROCESS_AWAKEN = 0;
    protected final static int PS_BIND_FRAME_SERVICE = 1;
    protected final static int PS_BIND_FRAME_SERVICE_TIME_OUT = 2;

    protected String mTagLog = "kuyou.common.ipc > RemoteEventBus";

    private Context mContext = null;
    private IRemoteBinderConfig mRegisterConfig = null;
    private IEventDispatcher mEventDispatcher = null;
    private IStatusProcessBus mStatusProcessBus = null;
    private IRemoteService mEventDispatchService = null;
    private ServiceConnection mEventDispatchServiceConnection = null;
    private List<ILiveListener> mLiveListenerList = new ArrayList<>();
    private BroadcastReceiver mBroadcastReceiverAutoReConnect;

    public boolean register(Object instance, Integer... event_codes) {
        if (null == instance) {
            Log.e(mTagLog, "register > process fail :instance is null");
            return false;
        }
        //连接远程服务
        if (instance instanceof IRemoteBinderConfig) {
            setRegisterConfig((IRemoteBinderConfig) instance);
            mTagLog = new StringBuilder(mTagLog).append(" > ").append(
                            getRegisterConfig().getContext().getPackageName()).
                    toString();
            Log.d(mTagLog, "binder > ");
            getStatusProcessBus().start(PS_NOTICE_IPC_PROCESS_AWAKEN);
            mEventDispatcher = EventDispatcherImpl.getInstance()
                    .setLocalModulePackageName(getRegisterConfig().getContext().getPackageName());
            return true;
        }

        boolean result = true;

        //服务注册监听器
        if (instance instanceof ILiveListener) {
            setLiveListener((ILiveListener) instance);
        }

        //事件过滤
        if (null != event_codes && event_codes.length > 0) {
            if (null == mEventDispatcher) {
                Log.e(mTagLog, "register > process fail : eventDispatcher is null");
                result = false;
            } else {
                ArrayList<Integer> eventCodeList = new ArrayList<Integer>(event_codes.length);
                Collections.addAll(eventCodeList, event_codes);
                mEventDispatcher.setEventReceiveList(eventCodeList);
            }
        }

        //事件分发
        if (instance instanceof IAssistHandler) {
            if (null == mEventDispatcher) {
                Log.e(mTagLog, "register > process fail : eventDispatcher is null");
                result = false;
            } else {
                mEventDispatcher.setAssistHandlerConfig((IAssistHandler) instance);
            }
        } else {
            EventBus.getDefault().register(instance);
        }
        return result;
    }

    public void unregister(Object instance) {
        if (instance instanceof IAssistHandler) {
            mEventDispatcher.setAssistHandlerConfig(null);
        } else {
            EventBus.getDefault().unregister(instance);
        }
    }

    /**
     * @param event 可远程事件 <br/>
     * @return 发送结果  <br/>
     * <p>
     * remarks: 1.1 <br/>
     * </p>
     * @action 发送本地或远程事件 <br/>
     */
    public boolean post(RemoteEvent event) {
        if (null == mEventDispatcher) {
            Log.w(mTagLog, "post > process warn : instance is not register , event_code = " + event.getCode());
            return false;
        }
        return mEventDispatcher.dispatch(event);
    }

    public RemoteEventBus setLiveListener(ILiveListener val) {
        if (null == val) {
            return RemoteEventBus.this;
        }
        Log.d(mTagLog, "setLiveListener > val = " + val);
        mLiveListenerList.add(val);
        return RemoteEventBus.this;
    }

    protected void dispatchEventDispatchServiceConnectChange(final boolean isConnect) {
        post(new RemoteEvent() {
            @Override
            public int getCode() {
                return isConnect ? Code.REF_DISPATCH_SERVICE_BINDER_SUCCESS : Code.REF_DISPATCH_SERVICE_UNBIND;
            }
        }.setRemote(false));
        if (0 == mLiveListenerList.size()) {
            return;
        }
        for (ILiveListener listener : mLiveListenerList) {
            listener.onEventDispatchServiceConnectChange(isConnect);
        }
    }

    protected RemoteEventBus setRegisterConfig(IRemoteBinderConfig val) {
        mRegisterConfig = val;
        return RemoteEventBus.this;
    }

    protected IRemoteBinderConfig getRegisterConfig() {
        return mRegisterConfig;
    }

    protected IStatusProcessBus getStatusProcessBus() {
        initStatusProcessBus();
        return mStatusProcessBus;
    }

    final protected void initStatusProcessBus() {
        if (null != mStatusProcessBus) {
            return;
        }
        mStatusProcessBus = new StatusProcessBusImpl() {
            @Override
            protected void onReceiveProcessStatusNotice(int statusCode,Bundle data, boolean isRemove) {
                RemoteEventBus.this.onReceiveProcessStatusNotice(statusCode,data, isRemove);
            }
        };
        initReceiveProcessStatusNotices();
    }

    protected void initReceiveProcessStatusNotices() {
        getStatusProcessBus().registerStatusNoticeCallback(PS_NOTICE_IPC_PROCESS_AWAKEN,
                new StatusProcessBusCallbackImpl()
                        .setNoticeHandleLooperPolicy(IStatusProcessBusCallback.LOOPER_POLICY_BACKGROUND));

        getStatusProcessBus().registerStatusNoticeCallback(PS_BIND_FRAME_SERVICE,
                new StatusProcessBusCallbackImpl()
                        .setNoticeHandleLooperPolicy(IStatusProcessBusCallback.LOOPER_POLICY_BACKGROUND));

        getStatusProcessBus().registerStatusNoticeCallback(PS_BIND_FRAME_SERVICE_TIME_OUT,
                new StatusProcessBusCallbackImpl()
                        .setNoticeHandleLooperPolicy(IStatusProcessBusCallback.LOOPER_POLICY_BACKGROUND));
    }

    protected void onReceiveProcessStatusNotice(int statusCode,Bundle data, boolean isRemove) {
        switch (statusCode) {
            case PS_NOTICE_IPC_PROCESS_AWAKEN:
                Log.w(mTagLog, "onReceiveProcessStatusNotice:PS_NOTICE_IPC_PROCESS_AWAKEN");

                Intent intent = new Intent();
                intent.setAction(ACTION_FLAG_FRAME_EVENT);
                intent.setPackage(getIpcFlag());
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                getRegisterConfig().getContext().sendBroadcast(intent);

                getStatusProcessBus().start(PS_BIND_FRAME_SERVICE, 500);
                break;
            case PS_BIND_FRAME_SERVICE:
                Log.w(mTagLog, "onReceiveProcessStatusNotice:PS_BIND_FRAME_SERVICE");
                getStatusProcessBus().start(PS_BIND_FRAME_SERVICE_TIME_OUT, 2 * 1000);
                bindIPCService(getRegisterConfig().getContext());
                break;
            case PS_BIND_FRAME_SERVICE_TIME_OUT:
                Log.w(mTagLog, "onReceiveProcessStatusNotice:PS_BIND_FRAME_SERVICE_TIME_OUT");
                post(new RemoteEvent() {
                    @Override
                    public int getCode() {
                        return Code.REF_DISPATCH_SERVICE_BIND_TIME_OUT;
                    }
                });
                break;
            default:
                break;
        }
    }

    protected String getIpcFlag() {
        if (null == getRegisterConfig()
                || TextUtils.isEmpty(getRegisterConfig().getIpcFlag())) {
            return IPC_FRAME_PACKAGE_NAME_NORMAL;
        }
        return getRegisterConfig().getIpcFlag();
    }

    private void bindIPCService(Context context) {
        mEventDispatchServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(RemoteEventBus.this.mTagLog, "onServiceConnected: ");
                RemoteEventBus.this.getStatusProcessBus().stop(PS_BIND_FRAME_SERVICE_TIME_OUT);
                RemoteEventBus.this.mEventDispatchService = IRemoteService.Stub.asInterface(service);
                RemoteEventBus.this.mEventDispatcher.setRemoteCallback(new IEventBusDispatchCallback() {
                    //发送远程事件到远程分发服务
                    @Override
                    public boolean dispatchEvent(RemoteEvent event) {
                        if (null == mEventDispatchService) {
                            Log.e(mTagLog, "dispatchEvent > process fail :  ipc bind is null , event_code = " + event.getCode());
                        } else {
                            try {
                                //Log.d(mTagLog, "mEventDispatcher > dispatchEvent > event " + event.getCode());
                                mEventDispatchService.sendEvent(event.getData());
                                return true;
                            } catch (Exception e) {
                                //Log.e(mTagLog, "mEventDispatcher > dispatchEvent > process fail : event = " + event.getCode());
                            }
                        }
                        return false;
                    }
                });
                RemoteEventBus.this.dispatchEventDispatchServiceConnectChange(true);
                try {
                    RemoteEventBus.this.mEventDispatchService.registerCallback(
                            RemoteEventBus.this.getRegisterConfig().getContext().getApplicationContext().getPackageName(),
                            new IRemoteServiceCallBack.Stub() {
                                @Override
                                public void onReceiveEvent(Bundle data) {
                                    RemoteEventBus.this.mEventDispatcher.dispatchEventRemote2Local(data);
                                }

                                @Override
                                public List<String> getReceiveEventFlag() {
                                    return null;
                                }
                            });
                } catch (Exception e) {
                    Log.e(RemoteEventBus.this.mTagLog, Log.getStackTraceString(e));
                    onServiceDisconnected(null);
                }
                RemoteEventBus.this.registerAutoReConnect();
            }

            public void onServiceDisconnected(ComponentName className) {
                RemoteEventBus.this.mEventDispatchService = null;
                RemoteEventBus.this.mEventDispatcher.setRemoteCallback(null);
                RemoteEventBus.this.dispatchEventDispatchServiceConnectChange(false);
                Log.d(mTagLog, "onServiceDisconnected: ");
            }
        };

        Log.d(mTagLog, "bindIPCService > packageName = " + getIpcFlag());
        //Log.d(mTagLog, "bindIPCService > getName = " + RemoteEventDispatchService.class.getName());
        Intent intent = new Intent();
        intent.setPackage(getIpcFlag());
        intent.setAction("kuyou.common.ipc");
        intent.setComponent(new ComponentName(getIpcFlag(), "kuyou.common.ipc.RemoteEventDispatchService"));
        context.bindService(intent, mEventDispatchServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerAutoReConnect(){
        if(null != mBroadcastReceiverAutoReConnect){
            return;
        }
        if(null == getIpcFlag()){
            //Log.d(mTagLog, "registerAutoReConnect > cancel option , getIpcFlag is null");
            return;
        }
        if(getIpcFlag().equals(getRegisterConfig().getContext().getPackageName())){
            //Log.d(mTagLog, "registerAutoReConnect > cancel option");
            return;
        }
        //Log.d(mTagLog, "registerAutoReConnect > option ");
        mBroadcastReceiverAutoReConnect= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(null!=action && action.equals(ACTION_IPC_BOOT)){
                    if(null != RemoteEventBus.this.mEventDispatchService){
                        //Log.d(mTagLog, "registerAutoReConnect > onReceive > cancel start reconnect ipc");
                        return;
                    }
                    Log.i(mTagLog, "registerAutoReConnect > onReceive > start reconnect ipc");
                   RemoteEventBus.this.getStatusProcessBus().start(PS_BIND_FRAME_SERVICE);
                }
            }
        };
        try{
            getRegisterConfig().getContext().registerReceiver(mBroadcastReceiverAutoReConnect,new IntentFilter(ACTION_IPC_BOOT));
        }catch(Exception e){
            Log.e(mTagLog, Log.getStackTraceString(e));
        }
    }

    public static interface IRemoteBinderConfig {

        public Context getContext();

        /**
         * action:指定IPC框架服务包名
         */
        public String getIpcFlag();
    }

    public static class RemoteBuilder implements IRemoteBinderConfig {
        private Context mContext = null;
        private String mIpcFlag = null;

        public RemoteBuilder setContext(Context val) {
            mContext = val;
            return RemoteBuilder.this;
        }

        public RemoteBuilder setIpcFlag(String val) {
            mIpcFlag = val;
            return RemoteBuilder.this;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public String getIpcFlag() {
            return mIpcFlag;
        }
    }

    public static interface ILiveListener {
        public void onEventDispatchServiceConnectChange(boolean isConnect);
    }
}

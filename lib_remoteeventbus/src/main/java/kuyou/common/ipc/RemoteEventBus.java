package kuyou.common.ipc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kuyou.common.assist.IEventBusDispatchCallback;
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

    protected final String IPC_FRAME_PACKAGE_NAME_NORMAL = "com.kuyou.ipc";
    protected final static int PS_NOTICE_IPC_PROCESS_AWAKEN = 0;
    protected final static int PS_BIND_FRAME_SERVICE = 1;
    protected final static int PS_BIND_FRAME_SERVICE_TIME_OUT = 2;

    protected String mTagLog = "kuyou.common.ipc > RemoteEventBus";

    private Context mContext = null;
    private IRegisterConfig mRegisterConfig = null;
    private IEventDispatcher mEventDispatcher = null;
    private IStatusProcessBus mStatusProcessBus = null;
    private IFrameLiveListener mFrameLiveListener = null;
    private IRemoteService mEventDispatchService = null;
    private ServiceConnection mEventDispatchServiceConnection = null;

    public void binder(IRegisterConfig config) {
        Log.d(mTagLog, "binder > ");
        mRegisterConfig = config;
        getStatusProcessBus().start(PS_NOTICE_IPC_PROCESS_AWAKEN);
        mEventDispatcher = EventDispatcherImpl.getInstance()
                .setLocalModulePackageName(getRegisterConfig().getContext().getPackageName())
                .setEventReceiveList(getRegisterConfig().getEventDispatchList())
                .setEventBusDispatchRemoteCallback(new IEventBusDispatchCallback() {
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

        mTagLog = new StringBuilder(mTagLog).append(" > ").append(
                        getRegisterConfig().getContext().getPackageName()).
                toString();

        setFrameLiveListener(getRegisterConfig().getFrameLiveListener());
    }

    public boolean register(Object instance,Integer ... event_codes) {
        if(null == mEventDispatcher){
            Log.e(mTagLog, "register > process fail : ");
            return false;
        }
        EventBus.getDefault().register(instance);
        if(null != event_codes && event_codes.length>0){
            ArrayList<Integer> eventCodeList = new ArrayList<Integer>(event_codes.length);
            Collections.addAll(eventCodeList, event_codes);
            mEventDispatcher.setEventReceiveList(eventCodeList);
        }
        return true;
    }

    public void unregister(Object instance) {
        EventBus.getDefault().unregister(instance);
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

    public RemoteEventBus setFrameLiveListener(IFrameLiveListener frameLiveListener) {
        mFrameLiveListener = frameLiveListener;
        return RemoteEventBus.this;
    }

    protected IRegisterConfig getRegisterConfig() {
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
            protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {
                RemoteEventBus.this.onReceiveProcessStatusNotice(statusCode, isRemove);
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

    protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {
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
                Log.d(mTagLog, "onServiceConnected: ");
                getStatusProcessBus().stop(PS_BIND_FRAME_SERVICE_TIME_OUT);
                mEventDispatchService = IRemoteService.Stub.asInterface(service);
                post(new RemoteEvent() {
                    @Override
                    public int getCode() {
                        return IRemoteConfig.Code.REF_DISPATCH_SERVICE_BINDER_SUCCESS;
                    }
                }.setRemote(false));
                try {
                    mEventDispatchService.registerCallback(getRegisterConfig().getContext().getApplicationContext().getPackageName(),
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
                    Log.e(mTagLog, Log.getStackTraceString(e));
                    RemoteEventBus.this.onDisconnected();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                mEventDispatchService = null;
                post(new RemoteEvent() {
                    @Override
                    public int getCode() {
                        return IRemoteConfig.Code.REF_DISPATCH_SERVICE_UNBIND;
                    }
                }.setRemote(false));
                RemoteEventBus.this.onDisconnected();
                Log.d(mTagLog, "onServiceDisconnected: ");
            }
        };

        Log.d(mTagLog, "bindIPCService > packageName = " + getIpcFlag());
        //Log.d(mTagLog, "bindIPCService > getName = " + RemoteEventDispatchService.class.getName());
        Intent intent = new Intent();
        intent.setPackage(getIpcFlag());
        intent.setAction("kuyou.common.ipc");
        context.bindService(intent, mEventDispatchServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void onDisconnected() {
        if (null == mFrameLiveListener) {
            //Log.d(mTagLog, "onDisconnected > mFrameLiveListener is null");
            return;
        }
        post(new RemoteEvent() {
            @Override
            public int getCode() {
                return Code.REF_CLIENT_UNREGISTER;
            }
        }.setPolicyDispatch2Myself(true)
                .setRemote(true));
        mFrameLiveListener.onIpcFrameUnResister();
    }

    public static interface IRegisterConfig {

        public Context getContext();

        /**
         * action:指定IPC框架服务包名
         */
        public String getIpcFlag();

        /**
         * action:指定IPC框架状态监听器
         */
        public IFrameLiveListener getFrameLiveListener();

        /**
         * action:指定想要接收的远程事件ID列表
         */
        public List<Integer> getEventDispatchList();
    }

    public static class Builder implements IRegisterConfig {
        private Context mContext = null;
        private String mIpcFlag = null;
        private IFrameLiveListener mFrameLiveListener = null;
        private List<Integer> mEventReceiveList = null;

        public Builder setContext(Context val) {
            mContext = val;
            return Builder.this;
        }

        public Builder setIpcFlag(String val) {
            mIpcFlag = val;
            return Builder.this;
        }

        public Builder setFrameLiveListener(IFrameLiveListener val) {
            mFrameLiveListener = val;
            return Builder.this;
        }

        public Builder setEventReceiveList(List<Integer> val) {
            mEventReceiveList = val;
            return Builder.this;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public String getIpcFlag() {
            return mIpcFlag;
        }

        @Override
        public IFrameLiveListener getFrameLiveListener() {
            return mFrameLiveListener;
        }

        @Override
        public List<Integer> getEventDispatchList() {
            return mEventReceiveList;
        }
    }

    public static interface IFrameLiveListener {
        public void onIpcFrameResisterSuccess();

        public void onIpcFrameUnResister();
    }
}

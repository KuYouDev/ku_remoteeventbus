package kuyou.common.ipc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import kuyou.common.ipc.basic.IRemoteConfig;
import kuyou.common.ipc.basic.IRemoteEventHandler;

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

    public static RemoteEventBus getInstance(Context context) {
        if (sInstance == null) {
            synchronized (RemoteEventBus.class) {
                if (sInstance == null) {
                    sInstance = new RemoteEventBus(context);
                }
            }
        }
        return sInstance;
    }

    public static RemoteEventBus getInstance() {
        if (null == sInstance) {
            throw new NullPointerException("RemoteEventBus is null\nplease perform method \"RemoteEventBus.getInstance(Context context)\"");
        }
        if (null == sInstance.mContext) {
            throw new NullPointerException("context is null");
        }
        return sInstance;
    }

    private final String IPC_FRAME_PACKAGE_NAME_NORMAL = "com.kuyou.ipc";

    protected String mTagLog = "kuyou.common.ipc > RemoteEventBus";

    private Context mContext;

    private IRemoteEventHandler mRemoteEventHandler;
    private FrameEventHandler mFrameEventHandler;
    private IFrameLiveListener mFrameLiveListener = null;
    private IRemoteService mEventDispatchService = null;
    private ServiceConnection mEventDispatchServiceConnection = null;

    private RemoteEventBus(Context context) {
        mContext = context.getApplicationContext();
        mFrameEventHandler = FrameEventHandler.getInstance();
    }

    protected Context getContext() {
        return mContext;
    }

    public RemoteEventBus register(Object instance) {
        if (!(instance instanceof IRegisterConfig)) {
            EventBus.getDefault().register(instance);
            return RemoteEventBus.this;
        }
        IRegisterConfig config = (IRegisterConfig) instance;
        startIPCDaemon(getContext());

        EventBus.getDefault().register(config.getLocalEventDispatchHandler());

        mRemoteEventHandler = RemoteEventHandler.getInstance()
                .setLocalModulePackageName(getContext().getPackageName())
                .setEventDispatchList(config.getEventDispatchList());

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bindIPCService(mContext);
            }
        }, 1000);

        if (null != mRemoteEventHandler.getLocalModulePackageName()) {
            mTagLog = new StringBuilder(mTagLog).append(" > ").append(
                    mRemoteEventHandler.getLocalModulePackageName()).
                    toString();
        }

        setFrameLiveListener(config.getFrameLiveListener());

        Log.d(mTagLog, "register > ");

        return RemoteEventBus.this;
    }

    public void unregister(Object instance) {
        if (instance instanceof IRegisterConfig) {
            return;
        }
        EventBus.getDefault().unregister(instance);
    }

    protected void dispatchRemoteEventFrameStatus(int code) {
        mFrameEventHandler.dispatchFrameStatus(code);
    }

    protected void startIPCDaemon(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_FLAG_FRAME_EVENT);
        intent.setPackage(getFramePackageName());
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(intent);
    }

    private String mFramePackageName;

    protected String getFramePackageName() {
        return null == mFramePackageName ? IPC_FRAME_PACKAGE_NAME_NORMAL : mFramePackageName;
    }

    public RemoteEventBus setFramePackageName(String packageName) {
        Log.d(mTagLog, "setFramePackageName > packageName = "+packageName);
        mFramePackageName = packageName;
        return RemoteEventBus.this;
    }

    private void bindIPCService(Context context) {
        mEventDispatchServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(mTagLog, "onServiceConnected: ");
                dispatchRemoteEventFrameStatus(Code.BIND_IPC_SERVICE_SUCCESS);
                mEventDispatchService = IRemoteService.Stub.asInterface(service);

                try {
                    mEventDispatchService.registerCallback(mContext.getApplicationContext().getPackageName(),
                            new IRemoteServiceCallBack.Stub() {
                                @Override
                                public void onReceiveEvent(Bundle data) {
                                    RemoteEventBus.this.mRemoteEventHandler.remoteEvent2LocalEvent(data);
                                }

                                @Override
                                public List<String> getReceiveEventFlag() {
                                    return null;
                                }
                            });
                    RemoteEventBus.this.onResisterSuccess();
                } catch (Exception e) {
                    Log.e(mTagLog, Log.getStackTraceString(e));
                    RemoteEventBus.this.onUnResister();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                dispatchRemoteEventFrameStatus(Code.UNBIND_IPC_SERVICE);
                Log.d(mTagLog, "onServiceDisconnected: ");
                mEventDispatchService = null;
                RemoteEventBus.this.onUnResister();
            }
        };
        
        Log.d(mTagLog, "bindIPCService > getFramePackageName = "+getFramePackageName());
        Intent intent = new Intent();
        intent.setPackage(getFramePackageName());
        intent.setAction("kuyou.common.ipc.FrameRemoteService");
        context.bindService(intent, mEventDispatchServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isRegister(String packageName) {
        if (null != mEventDispatchService) {
            try {
                return mEventDispatchService.getRegisterModules().contains(packageName);
            } catch (Exception e) {
                Log.e(mTagLog, Log.getStackTraceString(e));
            }
        }
        return true;
    }

    public boolean dispatch(RemoteEvent event) {
        boolean result = true;
        if (event.isRemote()) {
            if (null == mEventDispatchService) {
                Log.e(mTagLog, "dispatch > IPC service is not bind");
                result = false;
            } else {
                try {
                    //Log.d(mTagLog, "dispatch > event " + event.getCode());
                    mEventDispatchService.sendEvent(event.getData());
                } catch (Exception e) {
                    Log.e(mTagLog, "dispatch > process fail : event = " + event.getCode());
                    result = false;
                }
            }
            if (!event.isDispatch2Myself()) {
                return result;
            }
        }
        EventBus.getDefault().post(event);
        return result;
    }

    public RemoteEventBus setFrameLiveListener(IFrameLiveListener frameLiveListener) {
        mFrameLiveListener = frameLiveListener;
        return RemoteEventBus.this;
    }

    protected void onResisterSuccess() {
        if (null == mFrameLiveListener) {
            //Log.d(mTagLog, "onResisterSuccess > mFrameLiveListener is null");
            return;
        }
        mFrameLiveListener.onIpcFrameResisterSuccess();
    }

    protected void onUnResister() {
        if (null == mFrameLiveListener) {
            //Log.d(mTagLog, "onUnResister > mFrameLiveListener is null");
            return;
        }
        mFrameLiveListener.onIpcFrameUnResister();
    }

    public static interface IFrameLiveListener {
        public void onIpcFrameResisterSuccess();

        public void onIpcFrameUnResister();
    }

    public static interface IRegisterConfig {
        /**
         * action:指定IPC框架状态监听器
         */
        public IFrameLiveListener getFrameLiveListener();

        /**
         * action:指定想要接收的远程事件ID列表
         */
        public List<Integer> getEventDispatchList();

        /**
         * action:指定远程事件转发到本地后的处理器
         */
        public Object getLocalEventDispatchHandler();
    }
}

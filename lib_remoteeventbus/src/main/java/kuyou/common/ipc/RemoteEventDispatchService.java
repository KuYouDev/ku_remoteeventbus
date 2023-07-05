package kuyou.common.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import kuyou.common.assist.BasicAssistHandler;
import kuyou.common.ipc.basic.IRemoteConfig;
import kuyou.common.ipc.event.EventFrame;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.status.StatusProcessBusCallbackImpl;
import kuyou.common.status.StatusProcessBusImpl;
import kuyou.common.status.basic.IStatusProcessBus;
import kuyou.common.status.basic.IStatusProcessBusCallback;

/**
 * action :IPC框架服务
 * <p>
 * author: wuguoxian <br/>
 * date: 21-7-22 <br/>
 * remarks:  <br/>
 * 未实现：模块注册状态回调</p>
 * 未实现：事件Code注册回调</p>
 */
public class RemoteEventDispatchService extends Service {
    private static final String TAG = "kuyou.common.ipc > RemoteEventDispatchService";

    protected final static int PS_DISPATCH = 0;

    //private Map<String, IRemoteServiceCallBack> mModuleCallbackList = new HashMap<>();
    private Map<IRemoteServiceCallBack, String> mModuleCallbackList = new HashMap<>();
    private ArrayList<String> mRegisterModuleList = new ArrayList<>();
    private ArrayList<String> mRegisterClientList = new ArrayList<>();
    private final Queue<Bundle> mDispatchCache = new LinkedList<>();
    private Object mDispatchLock = new Object();
    private IStatusProcessBus mStatusProcessBus = new StatusProcessBusImpl() {
        //callback 主要为获取数据
        @Override
        protected void onReceiveProcessStatusNotice(int statusCode, Bundle data, boolean isRemove) {
            RemoteEventDispatchService.this.onReceiveProcessStatusNotice(statusCode, data, isRemove);
        }
    };

    private RemoteCallbackList<IRemoteServiceCallBack> mCallbackList = new RemoteCallbackList<IRemoteServiceCallBack>() {
        @Override
        public void onCallbackDied(IRemoteServiceCallBack callback) {
            super.onCallbackDied(callback);
            synchronized (RemoteEventDispatchService.this.mModuleCallbackList) {
                if (mModuleCallbackList.containsKey(callback)) {
                    String packageName = mModuleCallbackList.get(callback);
                    mRegisterModuleList.remove(packageName);
                    mRegisterClientList.remove(packageName);
                }
            }
        }
    };

    protected void onReceiveProcessStatusNotice(int statusCode, Bundle data, boolean isRemove) {
        onReceiveProcessStatusNotice(statusCode, isRemove);
    }

    protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {
        switch (statusCode) {
            case PS_DISPATCH:
                synchronized (mDispatchLock){
                    Bundle data = null;
                    synchronized (mDispatchCache) {
                        data = mDispatchCache.poll();
                    }
                    dispatch2Callback(data);
                    if (null != data)
                        getStatusProcessBus().start(PS_DISPATCH);
                }
                break;
            default:
                break;
        }
    }

    private void dispatch2Callback( Bundle data){
        if (null == mCallbackList) {
            mCallbackList = new RemoteCallbackList<IRemoteServiceCallBack>();
            Log.e(TAG, "dispatch2Callback > mCallbackList is null");
            return ;
        }
        if (mCallbackList.getRegisteredCallbackCount() == 0) {
            return;
        }
        final int N = mCallbackList.beginBroadcast();
        if (N < 0) {
            Log.e(TAG, "dispatch2Callback > mCallbackList register info is null");
            return;
        }
        for (int i = 0; i < N; i++) {
            IRemoteServiceCallBack callBack = null;
            try {
                callBack = mCallbackList.getBroadcastItem(i);
                callBack.onReceiveEvent(data);
            } catch (Exception e) {
                if (e instanceof NullPointerException) {
                    continue;
                }
                Log.e(TAG, new StringBuilder("dispatch2Callback > process fail : ")
                        .append("eventCode = ").append(RemoteEvent.getCodeByData(data))
                        .append("\n").append(Log.getStackTraceString(e))
                        .toString());
            }
        }
        try {
            mCallbackList.finishBroadcast();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private IRemoteService.Stub mBinder = new IRemoteService.Stub() {

        @Override
        public void sendEvent(Bundle data) throws RemoteException {
            if (data == null) {
                return;
            }
            if (!mDispatchCache.offer(data)) {
                Log.e(TAG, "sendEvent: > process fail : add to dispatch queue ");
            } else if (!getStatusProcessBus().isStart(PS_DISPATCH)) {
                getStatusProcessBus().start(PS_DISPATCH);
            }
        }

        @Override
        public void registerCallback(String packageName, IRemoteServiceCallBack cb) throws RemoteException {
            if (packageName == null) {
                Log.e(TAG, "registerCallback > process fail : packageName is null");
                return;
            }
            if (cb == null) {
                Log.e(TAG, "registerCallback > process fail : IRemoteServiceCallBack is null");
                return;
            }
            Log.d(TAG, "registerCallback > packageName = " + packageName);
            mCallbackList.register(cb);
            mModuleCallbackList.put(cb, packageName);
            mRegisterModuleList.add(packageName);
            if (!RemoteEventDispatchService.this.getApplicationContext()
                    .getPackageName()
                    .equals(packageName)) {
                mRegisterClientList.add(packageName);
            }

            RemoteEventBus.getInstance().post(new EventFrame() {
                @Override
                public int getCode() {
                    return IRemoteConfig.Code.REF_CLIENT_REGISTER_SUCCESS;
                }
            }.setTag(packageName)
                    .setRegisterClientList(mRegisterModuleList)
                    .setPolicyDispatch2Myself(true)
                    .setRemote(true));
        }

        @Override
        public void unregisterCallback(String packageName, IRemoteServiceCallBack cb) throws RemoteException {
            if (packageName == null) {
                Log.e(TAG, "unregisterCallback > process fail : packageName is null");
                return;
            }
            if (cb == null) {
                Log.e(TAG, "unregisterCallback > process fail : IRemoteServiceCallBack is null");
                return;
            }
            Log.d(TAG, "unregisterCallback > packageName = " + packageName);
            mCallbackList.unregister(cb);
            mModuleCallbackList.remove(cb);
            mRegisterModuleList.remove(packageName);
            mRegisterClientList.remove(packageName);
        }

        @Override
        public List<String> getRegisterModules() throws RemoteException {
            return mRegisterModuleList;
        }

    };

    private int beginBroadcastCallback(RemoteCallbackList callback) {
        if (callback.getRegisteredCallbackCount() == 0) {
            return -1;
        }
        int N = -1;
        try {
            N = callback.beginBroadcast();
        } catch (Exception e) {
            callback.finishBroadcast();
            N = callback.beginBroadcast();
        }
        if (N == 0) {
            callback.finishBroadcast();
            return -1;
        }
        return N;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dispatchIpcServerBootNotice();

        getStatusProcessBus().registerStatusNoticeCallback(new StatusProcessBusCallbackImpl()
                .setStatusCode(PS_DISPATCH)
                .setThreadCode(IRemoteConfig.ThreadCode.DISPATCH, false)
        );
    }

    protected IStatusProcessBus getStatusProcessBus() {
        return mStatusProcessBus;
    }

    private void dispatchIpcServerBootNotice() {
        Log.d(TAG, "dispatchIpcServerBootNotice > registerAutoReConnect");
        Intent notice = new Intent(RemoteEventBus.ACTION_IPC_BOOT);
        notice.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        sendBroadcast(notice);
    }
}
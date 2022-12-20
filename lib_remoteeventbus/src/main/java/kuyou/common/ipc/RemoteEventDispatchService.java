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
import java.util.List;
import java.util.Map;

import kuyou.common.ipc.basic.IRemoteConfig;
import kuyou.common.ipc.event.EventFrame;
import kuyou.common.ipc.event.RemoteEvent;

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

    //private Map<String, IRemoteServiceCallBack> mModuleCallbackList = new HashMap<>();
    private Map<IRemoteServiceCallBack, String> mModuleCallbackList = new HashMap<>();
    private ArrayList<String> mRegisterModuleList = new ArrayList<>();
    private ArrayList<String> mRegisterClientList = new ArrayList<>();

    private IRemoteService.Stub mBinder = new IRemoteService.Stub() {

        @Override
        public void sendEvent(Bundle data) throws RemoteException {
            if (data == null) {
                return;
            }
            if (null == mCallbackList) {
                mCallbackList = new RemoteCallbackList<IRemoteServiceCallBack>();
                Log.e(TAG, "sendEvent > mCallbackList is null");
                return;
            }
            //Log.d(TAG, "registerCallback > sendEvent = " + RemoteEvent.getCodeByData(data));
            int N = beginBroadcastCallback(mCallbackList);
            for (int i = 0; i < N; i++) {
                IRemoteServiceCallBack callBack = null;
                try {
                    callBack = mCallbackList.getBroadcastItem(i);
                    callBack.onReceiveEvent(data);
                } catch (Exception e) {
                    Log.e(TAG, new StringBuilder("sendEvent > process fail : ")
                            .append("eventCode = ").append(RemoteEvent.getCodeByData(data))
                            .append("\n modulePackageName = ").append(mModuleCallbackList.get(callBack))
                            //.append("\n").append(Log.getStackTraceString(e))
                            .toString());
                }
            }
            mCallbackList.finishBroadcast();
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
            if (!RemoteEventDispatchService.this.getApplicationContext().getPackageName().equals(packageName)) {
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
    }

    private void dispatchIpcServerBootNotice(){
        Log.d(TAG, "dispatchIpcServerBootNotice > registerAutoReConnect");
        Intent notice = new Intent(RemoteEventBus.ACTION_IPC_BOOT);
        notice.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        sendBroadcast(notice);
    }
}
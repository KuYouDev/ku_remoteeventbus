package kuyou.common.status;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import kuyou.common.status.basic.IStatusProcessBus;
import kuyou.common.status.basic.IStatusProcessBusCallback;

/**
 * action :提供状态的物流服务[将调度ID代理成用户指定状态ID]
 * <p>
 * author: wuguoxian <br/>
 * date: 21-08-24 <br/>
 * <p>
 */
public abstract class StatusProcessBusImpl implements IStatusProcessBus {
    protected final String TAG = "kuyou.common.status > StatusProcessBusImpl";

    private IStatusProcessBus mStatusProcessBusFrame;
    private Map<Integer, Integer> mStatusProcessBusProxyStatusCodeList;
    private Map<Integer, Integer> mStatusProcessBusProxyFlagList;
    private Map<Integer, IStatusProcessBusCallback> mStatusProcessBusCallbackList;
    private Object mLock = new Object();

    public StatusProcessBusImpl() {
        mStatusProcessBusFrame = StatusProcessBusFrame.getInstance();
        mStatusProcessBusProxyStatusCodeList = new HashMap<>();
        mStatusProcessBusProxyFlagList = new HashMap<>();
        mStatusProcessBusCallbackList = new HashMap<>();
    }

    //callback 主要为了传递数据
    protected abstract void onReceiveProcessStatusNotice(int statusCode,Bundle data, boolean isRemove);

    @Override
    public void registerStatusNoticeCallback(int statusCode, IStatusProcessBusCallback callback) {
        synchronized (mLock) {
            if (mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
                Log.w(TAG, "registerStatusNoticeCallback > process fail : statusCode is registered");
                return;
            }
            //Log.d(TAG, "registerStatusNoticeCallback > statusCode = " + statusCode);
            StatusProcessBusCallbackImpl callbackProxy = new StatusProcessBusCallbackImpl(callback) {
                @Override
                public void onReceiveProcessStatusNotice(boolean isRemove) {
                    
                    StatusProcessBusImpl.this.onReceiveProcessStatusNotice(
                            mStatusProcessBusProxyFlagList.get(Integer.valueOf(getStatusProcessFlag())),
                            mStatusProcessBusCallbackList.get(Integer.valueOf(getStatusProcessFlag())).getData(true), 
                            isRemove);
                }
            };
            //PSB内部真实的statusCode
            final int processFlag = mStatusProcessBusFrame.registerStatusNoticeCallback(callbackProxy);
            callbackProxy.setStatusProcessFlag(processFlag);

            mStatusProcessBusProxyStatusCodeList.put(statusCode, processFlag);
            mStatusProcessBusProxyFlagList.put(processFlag, statusCode);
            mStatusProcessBusCallbackList.put(processFlag, callback);
        }
    }

    @Override
    public void unRegisterStatus(int statusCode) {
        synchronized (mLock) {
            final int flag = Integer.valueOf(statusCode);
            if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
                return;
            }
            //PSB内部真实的statusCode
            Integer processFlag = mStatusProcessBusProxyStatusCodeList.get(flag);
            mStatusProcessBusProxyStatusCodeList.remove(flag);
            mStatusProcessBusProxyFlagList.remove(processFlag);
            mStatusProcessBusFrame.unRegisterStatus(processFlag);
        }
    }

    @Override
    public int registerStatusNoticeCallback(IStatusProcessBusCallback callback) {
        final int statusCode = callback.getStatusCode();
        if (-1 == statusCode) {
            Log.e(TAG, "registerStatusNoticeCallback > process fail : statusCode is invalid");
            return -1;
        }
        registerStatusNoticeCallback(statusCode, callback);
        return 0;
    }

    @Override
    public void start(int statusCode) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(flag));
    }

    @Override
    public void start(int statusCode, Bundle data) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        mStatusProcessBusCallbackList.get(mStatusProcessBusProxyStatusCodeList.get(flag)).setData(data);
        mStatusProcessBusFrame.start(mStatusProcessBusProxyStatusCodeList.get(flag),data);
    }

    @Override
    public void start(int statusCode, long delayed) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(flag), delayed);
    }

    @Override
    public void start(int statusCode, long delayed, Bundle data) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusCallbackList.get(mStatusProcessBusProxyStatusCodeList.get(flag)).setData(data);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(flag), delayed, data);
    }

    @Override
    public void stop(int statusCode) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "stop > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        mStatusProcessBusCallbackList.get(mStatusProcessBusProxyStatusCodeList.get(flag)).getData(true);
        //Log.d(TAG, "stop > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .stop(mStatusProcessBusProxyStatusCodeList.get(flag));
    }

    @Override
    public boolean isStart(int statusCode) {
        final Integer flag = Integer.valueOf(statusCode);
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(flag)) {
            Log.w(TAG, "isStart > process fail : statusCode is not registered = " + statusCode);
            return false;
        }
        return mStatusProcessBusFrame
                .isStart(mStatusProcessBusProxyStatusCodeList.get(flag));
    }
}

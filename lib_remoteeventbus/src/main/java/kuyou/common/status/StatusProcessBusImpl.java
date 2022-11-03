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

    public StatusProcessBusImpl() {
        mStatusProcessBusFrame = StatusProcessBusFrame.getInstance();
        mStatusProcessBusProxyStatusCodeList = new HashMap<Integer, Integer>();
        mStatusProcessBusProxyFlagList = new HashMap<Integer, Integer>();
    }

    protected abstract void onReceiveProcessStatusNotice(int statusCode, boolean isRemove);

    @Override
    public void registerStatusNoticeCallback(int statusCode, IStatusProcessBusCallback callback) {
        if (mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "registerStatusNoticeCallback > process fail : statusCode is registered");
            return;
        }
        //Log.d(TAG, "registerStatusNoticeCallback > statusCode = " + statusCode);
        StatusProcessBusCallbackImpl callbackProxy = new StatusProcessBusCallbackImpl(callback) {
            @Override
            public void onReceiveProcessStatusNotice(boolean isRemove) {
                StatusProcessBusImpl.this.onReceiveProcessStatusNotice(
                        mStatusProcessBusProxyFlagList.get(Integer.valueOf(getStatusProcessFlag())), isRemove);
            }
        };
        final int flag = mStatusProcessBusFrame.registerStatusNoticeCallback(callbackProxy);
        callbackProxy.setStatusProcessFlag(flag);

        mStatusProcessBusProxyStatusCodeList.put(statusCode, flag);
        mStatusProcessBusProxyFlagList.put(flag, statusCode);
    }

    @Override
    public int registerStatusNoticeCallback(IStatusProcessBusCallback callback) {
        throw new RuntimeException("this api is disable");
    }

    @Override
    public void start(int statusCode) {
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(Integer.valueOf(statusCode)));
    }

    @Override
    public void start(int statusCode, long delayed) {
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(Integer.valueOf(statusCode)), delayed);
    }

    @Override
    public void start(int statusCode, long delayed, Bundle data) {
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "start > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "start > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .start(mStatusProcessBusProxyStatusCodeList.get(Integer.valueOf(statusCode)), delayed, data);
    }

    @Override
    public void stop(int statusCode) {
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "stop > process fail : statusCode is not registered = " + statusCode);
            return;
        }
        //Log.d(TAG, "stop > statusCode = " + statusCode);
        mStatusProcessBusFrame
                .stop(mStatusProcessBusProxyStatusCodeList.get(Integer.valueOf(statusCode)));
    }

    @Override
    public boolean isStart(int statusCode) {
        if (!mStatusProcessBusProxyStatusCodeList.containsKey(Integer.valueOf(statusCode))) {
            Log.w(TAG, "isStart > process fail : statusCode is not registered = " + statusCode);
            return false;
        }
        return mStatusProcessBusFrame
                .isStart(mStatusProcessBusProxyStatusCodeList.get(Integer.valueOf(statusCode)));
    }
}

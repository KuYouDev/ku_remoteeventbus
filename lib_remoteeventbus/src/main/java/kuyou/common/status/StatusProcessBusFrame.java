package kuyou.common.status;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import kuyou.common.status.basic.IStatusProcessBus;
import kuyou.common.status.basic.IStatusProcessBusCallback;

/**
 * action :提供状态的物流服务[调度实现]
 * <p>
 * author: wuguoxian <br/>
 * date: 21-08-24 <br/>
 * <p>
 */
public class StatusProcessBusFrame extends Handler implements IStatusProcessBus {

    protected static final String TAG = "ckuyou.common.status > StatusProcessBusFrame";

    private volatile static StatusProcessBusFrame instance;

    public static StatusProcessBusFrame getInstance() {
        if (instance == null) {
            synchronized (StatusProcessBusFrame.class) {
                if (instance == null) {
                    HandlerThread handlerThreadStatusProcessBus = new HandlerThread(".HandlerThread.StatusProcessBus");
                    handlerThreadStatusProcessBus.start();
                    instance = new StatusProcessBusFrame(handlerThreadStatusProcessBus.getLooper());
                }
            }
        }
        return instance;
    }

    private StatusProcessBusFrame(Looper looper) {
        super(looper);
    }

    private int mProcessFlag = 0;

    private Looper mLooper;

    protected Map<Integer, IStatusProcessBusCallback> mStatusProcessBusCallbackList = new HashMap<Integer, IStatusProcessBusCallback>();
    protected Map<Integer, Handler> mStatusProcessBusCallbackHandlerList = new HashMap<Integer, Handler>();
    protected Map<Integer, Runnable> mStatusProcessBusCallbackRunnableList = new HashMap<Integer, Runnable>();

    protected int applyProcessFlag() {
        //Log.d(TAG, "applyProcessFlag > basic flag = " + mProcessFlag);
        return mProcessFlag++;
    }

    protected Looper getBackgroundHandleLooper() {
        if (null == mLooper) {
            HandlerThread handlerThreadBackgroundHandle = new HandlerThread(".HandlerThread.BackgroundHandle");
            handlerThreadBackgroundHandle.start();
            mLooper = handlerThreadBackgroundHandle.getLooper();
        }
        return mLooper;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        int flag = msg.what;
        removeMessages(flag);
        if (!mStatusProcessBusCallbackList.containsKey(flag)) {
            Log.e(TAG, "handleMessage > process fail : mStatusProcessBusCallbackList not contains msg = " + flag);
            return;
        }
        IStatusProcessBusCallback config = mStatusProcessBusCallbackList.get(flag);
        if (config.isAutoNoticeReceiveCycle()) {
            sendEmptyMessageDelayed(flag, config.getNoticeReceiveFreq());
        }
        mStatusProcessBusCallbackHandlerList.get(flag).post(mStatusProcessBusCallbackRunnableList.get(flag));
    }

    @Override
    public void registerStatusNoticeCallback(int statusCode, IStatusProcessBusCallback callback) {
        throw new RuntimeException("this api is not realized");
    }

    @Override
    public void unRegisterStatus(int processFlag) {
        stop(processFlag);
        mStatusProcessBusCallbackList.remove(processFlag);
        mStatusProcessBusCallbackRunnableList.remove(processFlag);
        mStatusProcessBusCallbackHandlerList.remove(processFlag);
    }

    @Override
    public int registerStatusNoticeCallback(final IStatusProcessBusCallback callback) {
        final int looperPolicy = callback.getNoticeHandleLooperPolicy();
        Looper looper = null;
        switch (looperPolicy) {
            case IStatusProcessBusCallback.LOOPER_POLICY_BACKGROUND:
                looper = getBackgroundHandleLooper();
                break;
            case IStatusProcessBusCallback.LOOPER_POLICY_MAIN:
                looper = Looper.getMainLooper();
                break;
            default:
                looper = callback.getNoticeHandleLooper();
                break;
        }
        if (null == looper) {
            Log.e(TAG, "registerStatusNoticeCallback > process fail :getNoticeHandleLooper is null ");
            return -1;
        }
        final int flag = applyProcessFlag();
        mStatusProcessBusCallbackList.put(flag, callback);
        mStatusProcessBusCallbackRunnableList.put(flag, new Runnable() {
            @Override
            public void run() {
                callback.onReceiveProcessStatusNotice(false);
            }
        });
        mStatusProcessBusCallbackHandlerList.put(flag, new Handler(looper));
        return flag;
    }

    @Override
    public void start(int processFlag) {
        if (!mStatusProcessBusCallbackList.containsKey(processFlag)) {
            Log.e(TAG, "start > process fail : mStatusProcessBusCallbackList not contains msg = " + processFlag);
            return;
        }
        removeMessages(processFlag);
        long delayed = mStatusProcessBusCallbackList.get(processFlag).getNoticeReceiveFreq();
        if (0 < delayed) {
            sendEmptyMessageDelayed(processFlag, delayed);
            return;
        }
        sendEmptyMessage(processFlag);
    }

    @Override
    public void start(int processFlag, long delayed) {
        if (!mStatusProcessBusCallbackList.containsKey(processFlag)) {
            Log.e(TAG, "start > process fail : mStatusProcessBusCallbackList not contains msg = " + processFlag);
            return;
        }
        removeMessages(processFlag);
        if (delayed > 0) {
            sendEmptyMessageDelayed(processFlag, delayed);
            return;
        }
        sendEmptyMessage(processFlag);
    }

    @Override
    public void start(int msgCode, long delayed, Bundle data) {
        if (!mStatusProcessBusCallbackList.containsKey(msgCode)) {
            Log.e(TAG, "start > process fail : mStatusProcessBusCallbackList not contains msg = " + msgCode);
            return;
        }
        removeMessages(msgCode);
        Message msg = new Message();
        msg.what = msgCode;
        msg.setData(data);
        if (delayed > 0) {
            sendMessageDelayed(msg, delayed);
            return;
        }
        sendMessage(msg);
    }

    @Override
    public void stop(int processFlag) {
        if (!mStatusProcessBusCallbackList.containsKey(processFlag)) {
            Log.e(TAG, "stop > process fail : mStatusProcessBusCallbackList not contains msg = " + processFlag);
            return;
        }
        if (hasMessages(processFlag)) {
            removeMessages(processFlag);
        }
        if (!mStatusProcessBusCallbackList.get(processFlag).isEnableReceiveRemoveNotice()) {
            return;
        }
        mStatusProcessBusCallbackList.get(processFlag).onReceiveProcessStatusNotice(true);
    }

    @Override
    public boolean isStart(int processFlag) {
        return hasMessages(processFlag);
    }
}

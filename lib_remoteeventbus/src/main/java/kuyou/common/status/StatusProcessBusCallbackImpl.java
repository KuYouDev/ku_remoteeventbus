package kuyou.common.status;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import kuyou.common.status.basic.IStatusProcessBusCallback;

public class StatusProcessBusCallbackImpl implements IStatusProcessBusCallback<StatusProcessBusCallbackImpl> {
    protected static final String TAG = "kuyou.common.status > StatusProcessBusCallbackImpl ";

    private int mStatusProcessFlag = -1;
    private boolean isAutoNoticeReceiveCycle = false;
    private boolean isEnableReceiveRemoveNotice = false;
    private long mNoticeReceiveFreq = 0;

    private Looper mNoticeHandleLooper = null;
    private Bundle[] mDataCache = new Bundle[1];
    private int mNoticeHandleLooperPolicy = -1;
    private int mStatusCode = -1;
    private int mThreadCode = -1;

    public StatusProcessBusCallbackImpl() {
    }

    /**
     * action:自动循环收到消息
     *
     * @param val1 ，为true表示开启，自动循环收到消息
     * @param val2 ，val1为true时，自动循环收到消息的周期
     */
    public StatusProcessBusCallbackImpl(boolean val1, long val2) {
        isAutoNoticeReceiveCycle = val1;
        mNoticeReceiveFreq = val2;
    }

    public StatusProcessBusCallbackImpl(IStatusProcessBusCallback callback) {
        isAutoNoticeReceiveCycle = callback.isAutoNoticeReceiveCycle();
        isEnableReceiveRemoveNotice = callback.isEnableReceiveRemoveNotice();
        mNoticeReceiveFreq = callback.getNoticeReceiveFreq();
        mNoticeHandleLooper = callback.getNoticeHandleLooper();
        mNoticeHandleLooperPolicy = callback.getNoticeHandleLooperPolicy();
        mThreadCode = callback.getThreadCode();
        mDataCache[0] = callback.getData(false);
    }

    public StatusProcessBusCallbackImpl setAutoNoticeReceiveCycle(boolean autoNoticeReceiveCycle) {
        isAutoNoticeReceiveCycle = autoNoticeReceiveCycle;
        return StatusProcessBusCallbackImpl.this;
    }

    public StatusProcessBusCallbackImpl setNoticeReceiveFreq(long noticeReceiveFreq) {
        mNoticeReceiveFreq = noticeReceiveFreq;
        return StatusProcessBusCallbackImpl.this;
    }

    @Override
    public void onReceiveProcessStatusNotice(boolean isRemove) {

    }

    @Override
    public boolean isAutoNoticeReceiveCycle() {
        return isAutoNoticeReceiveCycle;
    }

    @Override
    public long getNoticeReceiveFreq() {
        return mNoticeReceiveFreq;
    }

    @Override
    public Looper getNoticeHandleLooper() {
        return mNoticeHandleLooper;
    }

    @Override
    public int getNoticeHandleLooperPolicy() {
        return mNoticeHandleLooperPolicy;
    }

    @Override
    public boolean isEnableReceiveRemoveNotice() {
        return isEnableReceiveRemoveNotice;
    }

    @Override
    public int getStatusProcessFlag() {
        return mStatusProcessFlag;
    }

    @Override
    public StatusProcessBusCallbackImpl setStatusCode(int statusCode) {
        mStatusCode = statusCode;
        return StatusProcessBusCallbackImpl.this;
    }

    @Override
    public int getStatusCode() {
        return mStatusCode;
    }

    @Override
    public int getThreadCode() {
        return mThreadCode;
    }

    @Override
    public StatusProcessBusCallbackImpl setThreadCode(int val) {
       setNoticeHandleLooperPolicy(IStatusProcessBusCallback.LOOPER_POLICY_POOL);
        mThreadCode = val;
        return StatusProcessBusCallbackImpl.this;
    }

    public StatusProcessBusCallbackImpl setStatusProcessFlag(int statusProcessFlag) {
        mStatusProcessFlag = statusProcessFlag;
        return StatusProcessBusCallbackImpl.this;
    }

    /**
     * action:是否接收状态移除通知
     *
     * @param val ，为true表示已主动移除
     */
    public StatusProcessBusCallbackImpl setEnableReceiveRemoveNotice(boolean val) {
        isEnableReceiveRemoveNotice = val;
        return StatusProcessBusCallbackImpl.this;
    }

    public StatusProcessBusCallbackImpl setNoticeHandleLooper(Looper looper) {
        mNoticeHandleLooper = looper;
        return StatusProcessBusCallbackImpl.this;
    }

    public StatusProcessBusCallbackImpl setNoticeHandleLooperPolicy(int policy) {
        if (LOOPER_POLICY_MAIN != policy 
                && LOOPER_POLICY_BACKGROUND != policy
                && LOOPER_POLICY_POOL != policy) {
            Log.e(TAG, "setNoticeHandleLooperPolicy > process fail : policy is invalid");
        }
        mNoticeHandleLooperPolicy = policy;
        return StatusProcessBusCallbackImpl.this;
    }

    @Override
    public Bundle getData(boolean isClean) {
        Bundle data = mDataCache[0];
        if(isClean){
            mDataCache[0] = null;
        }
        return data;
    }

    @Override
    public StatusProcessBusCallbackImpl setData(Bundle val) {
        mDataCache[0] = val;
        return StatusProcessBusCallbackImpl.this;
    }
}

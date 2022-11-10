package com.kuyou.ssm.demo;

import android.util.Log;

import kuyou.common.assist.BasicAssistHandler;
import kuyou.common.ipc.basic.IRemoteConfig;
import kuyou.common.ipc.event.EventFrame;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.status.StatusProcessBusCallbackImpl;
import kuyou.common.status.basic.IStatusProcessBusCallback;
import kuyou.sdk.ssm.event.EventCommon;
import kuyou.sdk.ssm.event.IEventDefinitionFrame;
import kuyou.sdk.ssm.info.CscStatusInfo;
import kuyou.sdk.ssm.protocol.ICscStatus;

/**
 * action :SDK初始化结果监听器
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 2022/10/25 <br/>
 * </p>
 */
public class SdkInitListenHandler extends BasicAssistHandler {

    protected static final String TAG = "com.kuyou.ssm.demo > SdkInitListenHandler";
    protected final static int PS_INIT_TIME_OUT = 0;

    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    protected void initReceiveEventNotices() {
        registerHandleEvent(IRemoteConfig.Code.REF_DISPATCH_SERVICE_BIND_TIME_OUT, true);
        registerHandleEvent(IRemoteConfig.Code.REF_CLIENT_REGISTER_SUCCESS, true);
    }

    @Override
    public boolean onReceiveEventNotice(RemoteEvent event) {
        switch (event.getCode()) {
            case IRemoteConfig.Code.REF_DISPATCH_SERVICE_BINDER_SUCCESS:
                getStatusProcessBus().stop(PS_INIT_TIME_OUT);
                Log.d(TAG, "onReceiveProcessStatusNotice:REF_DISPATCH_SERVICE_BINDER_SUCCESS > ");
                return true;
            case IRemoteConfig.Code.REF_DISPATCH_SERVICE_BIND_TIME_OUT:
                getStatusProcessBus().stop(PS_INIT_TIME_OUT);
                Log.d(TAG, "onReceiveProcessStatusNotice:REF_DISPATCH_SERVICE_BIND_TIME_OUT > ");
                noticeCscStatus(ICscStatus.TIMEOUT);
                return true;
            case IRemoteConfig.Code.REF_CLIENT_REGISTER_SUCCESS:
                final String flag = EventFrame.getTag(event);
                if (null != flag && flag.equals(getContext().getPackageName())) {
                    getStatusProcessBus().stop(PS_INIT_TIME_OUT);
                    Log.d(TAG, "onReceiveProcessStatusNotice:REF_CLIENT_REGISTER_SUCCESS > ");
                    noticeCscStatus(ICscStatus.SUCCESS);
                } else {
                    Log.d(TAG, "onReceiveProcessStatusNotice:REF_CLIENT_REGISTER_SUCCESS > process fail : not this app");
                }
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    protected void initReceiveProcessStatusNotices() {
        getStatusProcessBus().registerStatusNoticeCallback(PS_INIT_TIME_OUT,
                new StatusProcessBusCallbackImpl().setNoticeReceiveFreq(1000 * 3)
                        .setNoticeHandleLooperPolicy(IStatusProcessBusCallback.LOOPER_POLICY_BACKGROUND));
    }

    @Override
    protected void onReceiveProcessStatusNotice(int statusCode, boolean isRemove) {
        if (PS_INIT_TIME_OUT == statusCode) {
            Log.d(TAG, "handleMessage:MSG_INIT_TIME_OUT > ");
            noticeCscStatus(ICscStatus.TIMEOUT);
        }
    }

    @Override
    public void start() {
        getStatusProcessBus().start(PS_INIT_TIME_OUT);
    }

    private void noticeCscStatus(int status) {
        Log.d(TAG, "noticeCscStatus > status = " + status);
        dispatchEvent(EventCommon
                .getInstance(IEventDefinitionFrame.CODE_FRAME_RESULT_CSC_STATUS)
                .setInfo(new CscStatusInfo().setPackageName(getContext().getPackageName()).setStatus(status))
                .setRemote(false));
    }
}

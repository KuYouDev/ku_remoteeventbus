package com.kuyou.ssm.demo;

import android.app.Application;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.ipc.RemoteEventBus;
import kuyou.sdk.ssm.event.EventCommon;
import kuyou.sdk.ssm.event.IEventDefinitionFrame;
import kuyou.sdk.ssm.info.CscStatusInfo;
import kuyou.sdk.ssm.protocol.ICscStatus;

/**
 * action :SSM_SDK初始化
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 2022/10/13 <br/>
 * </p>
 */
public class DemoApplication extends Application {
    private static final String TAG = "com.kuyou.ssm.demo > DemoApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        SdkInitListenHandler sdkListener = new SdkInitListenHandler();
        sdkListener.setContext(getApplicationContext());
        sdkListener.start();

        RemoteEventBus.getInstance().register(new RemoteEventBus.RemoteBuilder()
                .setIpcFlag("com.kuyou.ssm")
                .setContext(getApplicationContext()));
        RemoteEventBus.getInstance().register(sdkListener);
        RemoteEventBus.getInstance().register(DemoApplication.this,
                IEventDefinitionFrame.CODE_FRAME_RESULT_CSC_STATUS);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onReceiveEventNotice(RemoteEvent event) {
        switch (event.getCode()){
            case IEventDefinitionFrame.CODE_FRAME_RESULT_CSC_STATUS:
                CscStatusInfo info = EventCommon.getInfo(event);
                if (null == info) {
                    Log.e(TAG, "onReceiveEventNotice:CODE_FRAME_RESULT_CSC_STATUS > process fail : CscStatusInfo is null ");
                    return;
                }
                String initresult = "初始化失败";
                switch (info.getStatus()) {
                    case ICscStatus.SUCCESS:
                        initresult = "初始化成功";
                        break;
                    case ICscStatus.TIMEOUT:
                        initresult = "初始化超时,请确认SSM服务是否安装";
                        break;
                    case ICscStatus.INIT_FAIL:
                    default:
                        break;
                }
                Log.d(TAG, "onReceiveEventNotice > SDK初始化结果:" + initresult);
                break;
        }
    }

}

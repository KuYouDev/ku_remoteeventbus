package com.kuyou.ssm.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import kuyou.common.ipc.RemoteEventBus;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.sdk.ssm.event.EventCommon;
import kuyou.sdk.ssm.event.IEventDefinitionClient;
import kuyou.sdk.ssm.info.BeamInfo;
import kuyou.sdk.ssm.info.BeiDouSimInfo;
import kuyou.sdk.ssm.info.FeedBackInfo;
import kuyou.sdk.ssm.info.MessageInfo;
import kuyou.sdk.ssm.protocol.IMessageContentType;
import kuyou.sdk.ssm.protocol.IMessageStatus;
import kuyou.sdk.ssm.protocol.IMessageType;

/**
 * action :基础SDK接口演示
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 2022/10/12 <br/>
 * </p>
 */
public class MainActivity extends Activity {
    private static final String TAG = "com.kuyou.ssm.nct.demo > MainActivity";

    protected final static int MSG_SHOW_RESULT = 0;
    protected final static int MSG_SHOW_RESULT_READ_CARD = 1;
    protected final static int MSG_SHOW_BEAM_INFO = 2;

    private Button mBtnSend, mBtnReadCard, mBtnClearResult;
    private EditText mEtSendCardNumber, mEtSendMsgContent, mEtBeamInfo, mEtResult;
    private Handler mHandlerMain;

    private BeiDouSimInfo mBeiDouSimInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init views
        setContentView(R.layout.activity_main);
        mBtnSend = findViewById(R.id.btn_send_msg);
        mBtnReadCard = findViewById(R.id.btn_read_card);
        mBtnClearResult = findViewById(R.id.btn_clear_result);
        mEtSendCardNumber = findViewById(R.id.et_msg_id);
        mEtSendMsgContent = findViewById(R.id.et_msg_content);
        mEtResult = findViewById(R.id.et_result);
        mEtBeamInfo = findViewById(R.id.et_beam_info);
        mHandlerMain = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                MainActivity.this.handleMessage(msg);
                super.handleMessage(msg);
            }
        };
        RemoteEventBus.getInstance().register(MainActivity.this,
                IEventDefinitionClient.CODE_SERVER_RESULT_READ_CARD,
                IEventDefinitionClient.CODE_CLIENT_RESULT_BEAM,
                IEventDefinitionClient.CODE_CLIENT_RESULT_SEND_MESSAGE,
                IEventDefinitionClient.CODE_SERVER_NOTICE_RECEIVE_MESSAGE);
    }

    @Override
    protected void onDestroy() {
        RemoteEventBus.getInstance().unregister(MainActivity.this);
        super.onDestroy();
    }

    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_msg:
                sendMsg();
                break;
            case R.id.btn_read_card:
                readCard();
                break;
            case R.id.btn_clear_result:
                mEtResult.getText().clear();
                mEtBeamInfo.getText().clear();
                break;
            default:
                break;
        }
    }

    /**
     * action :接收注册的事件通知
     * <p>
     * remarks:Demo默认使用前台线程分发<br/>
     * remarks:实际项目建议使用 @Subscribe(threadMode = ThreadMode.BACKGROUND)，改成后台线程分发事件,显示到UI时请使用runOnUiThread等接口切换线程  <br/>
     * remarks:实际项目中onReceiveEventNotice方法名可以可以自由修改，保留注解@Subscribe即可
     * author: wuguoxian <br/>
     * date: 2022/08/22 <br/>
     * </p>
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEventNotice(RemoteEvent event) {
        switch (event.getCode()) {
            //读卡申请处理结果
            case IEventDefinitionClient.CODE_SERVER_RESULT_READ_CARD:
                List<BeiDouSimInfo> mSimListNow = new ArrayList<>();
                mSimListNow = EventCommon.getInfoList(event);
                if (null == mSimListNow) {
                    Log.d(TAG, "onReceiveEventNotice:CODE_CLIENT_RESULT_READ_CARD > 读取失败,返回结果为空");
                    break;
                }
                Log.d(TAG, "onReceiveEventNotice:CODE_CLIENT_RESULT_READ_CARD >");
                for (BeiDouSimInfo info : mSimListNow) {
                    //演示使用，默认只初始化一次，实际项目请按需选择卡
                    if (null == mBeiDouSimInfo && null != info) {
                        mBeiDouSimInfo = info;
                        Log.d(TAG, "onReceiveEventNotice:info = " + info.toString());
                    }
                    runOnUiThread(() -> showResult(info.toString()));
                }
                break;

            //波束[短报文信号]信息 , S1有10信道,S2c有21信道
            case IEventDefinitionClient.CODE_CLIENT_RESULT_BEAM:
                final BeamInfo beamInfo = EventCommon.getInfo(event);
                final int singleLevel = beamInfo.getValidChannelCount();
                Log.d(TAG, "onReceiveEventNotice > singleLevel = " + singleLevel);

                showBeam(beamInfo.toString());
                break;

            //发送短报文处理结果，操作成功不能保证对方可以接收到
            case IEventDefinitionClient.CODE_CLIENT_RESULT_SEND_MESSAGE:
                final FeedBackInfo result = EventCommon.getInfo(event);
                if (null == result) {
                    Log.w(TAG, "onSendResult > process fail : FeedBackInfo is null");
                    break;
                }
                Log.d(TAG, "dispatchResult > result = " + result.toString());
                showResult(result.toString());
                break;

            //收到短报文
            case IEventDefinitionClient.CODE_SERVER_NOTICE_RECEIVE_MESSAGE:
                MessageInfo messageInfoReceive = EventCommon.getInfo(event);
                if (null == messageInfoReceive) {
                    Log.w(TAG, "onReceiveEventNotice:CODE_CLIENT_RESULT_RECEIVE_MESSAGE > process fail : MessageInfo is null");
                    break;
                }

                Log.d(TAG, "onReceiveEventNotice:CODE_CLIENT_RESULT_RECEIVE_MESSAGE > msg = " + messageInfoReceive);
                showResult(messageInfoReceive.toString());
                break;

            default:
                break;
        }
    }

    //UI显示用
    private void handleMessage(Message msg) {
        mHandlerMain.removeMessages(msg.what);
        switch (msg.what) {
            case MSG_SHOW_RESULT:
                String result = null != msg.obj && (msg.obj instanceof String) ? (String) msg.obj : null;
                showResult(result);
                break;
            case MSG_SHOW_RESULT_READ_CARD:
                showResult(mBeiDouSimInfo.toString());
                mEtSendCardNumber.setText(mBeiDouSimInfo.getCardNumber());
                break;
            case MSG_SHOW_BEAM_INFO:
                String beamInfo = null != msg.obj && (msg.obj instanceof String) ? (String) msg.obj : null;
                showBeam(beamInfo);
                break;
            default:
                break;
        }
    }

    //显示服务返回信息
    private void showResult(String info) {
        if (null == info) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Message msg = new Message();
            msg.what = MSG_SHOW_RESULT;
            msg.obj = info;
            mHandlerMain.sendMessage(msg);
            return;
        }
        mEtResult.getEditableText().append("\n\n");
        mEtResult.getEditableText().append(info);
        mEtResult.setSelection(mEtResult.getText().length());
    }

    //显示服务返回的波束[信号]信息
    private void showBeam(String info) {
        if (null == info) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Message msg = new Message();
            msg.what = MSG_SHOW_BEAM_INFO;
            msg.obj = info;
            mHandlerMain.sendMessage(msg);
            return;
        }
        mEtBeamInfo.setText(info);
    }

    //发送报文
    private void sendMsg() {
        if (null == mBeiDouSimInfo) {
            final String result = "sendMsg > process fail : 未读到北斗卡";
            Log.e(TAG, result);
            showResult(result);
            return;
        }
        if (null == mEtSendCardNumber.getText() || 6 > mEtSendCardNumber.getText().length()) {
            final String result = "sendMsg > process fail : 发送号码无效";
            Log.e(TAG, result);
            showResult(result);
            return;
        }
        if (null == mEtSendMsgContent.getText() || 0 == mEtSendMsgContent.getText().length()) {
            final String result = "sendMsg > process fail : 发送内容为空";
            Log.e(TAG, result);
            showResult(result);
            return;
        }
        MessageInfo infoSend = new MessageInfo()
                .setFlagCreateTimeStamp(System.currentTimeMillis())//设定发送时间戳，也是流水号
                .setType(IMessageType.SEND)
                .setContentType(IMessageContentType.TEXT)
                .setStatus(IMessageStatus.SEND_HANDLE_ING)//默认状态
                .setContent(mEtSendMsgContent.getText().toString())
                //.setCardNumberLocal(mBeiDouSimInfo.getCardNumber())//本机号码
                .setCardNumber(mEtSendCardNumber.getText().toString());//发送号码
        //result只是客户端到终端短报文服务的执行结果,不是终端短报文发送结果
        //终端短报文发送结果请监听事件:IEventDefinitionClient.CODE_CLIENT_RESULT_SEND_MESSAGE
        final boolean result = RemoteEventBus.getInstance().post(EventCommon
                .getInstance(IEventDefinitionClient.CODE_CLIENT_REQUEST_SEND_MESSAGE)
                .setInfo(infoSend)
                .setRemote(true));
    }

    private void readCard() {
        final boolean result = RemoteEventBus.getInstance().post(EventCommon
                .getInstance(IEventDefinitionClient.CODE_CLIENT_REQUEST_READ_CARD)
                .setRemote(true));
    }
}

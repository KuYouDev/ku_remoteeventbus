package kuyou.common.status.basic;

import android.os.Bundle;
import android.os.Looper;

import kuyou.common.status.StatusProcessBusCallbackImpl;

public interface IStatusProcessBusCallback <Impl> {

    /**
     * action:设定状态通知处理线程,使用主线程
     */
    public final static int LOOPER_POLICY_MAIN = 0;

    /**
     * action:设定状态通知处理线程,使用后台线程
     */
    public final static int LOOPER_POLICY_BACKGROUND = 1;

    /**
     * action:设定状态通知处理线程,使用线程池
     */
    public final static int LOOPER_POLICY_POOL = 2;

    /**
     * action:收到状态通知 <br/>
     * remarks:  isEnableReceiveRemoveNotice 返回false时isRemove为空 <br/>
     *
     * @param isRemove ，为true表示状态已被主动移除 <br/>
     */
    public void onReceiveProcessStatusNotice(boolean isRemove);

    /**
     * action:设定是否开启状态通知自动循环
     */
    public boolean isAutoNoticeReceiveCycle();

    /**
     * action:设定状态通知接收频度
     */
    public long getNoticeReceiveFreq();

    /**
     * action:设定状态通知处理线程
     */
    public Looper getNoticeHandleLooper();

    /**
     * action:设定状态通知处理线程配置策略
     */
    public int getNoticeHandleLooperPolicy();

    /**
     * action:是否接收状态移除通知
     *
     * @return 为true表示接收
     */
    public boolean isEnableReceiveRemoveNotice();

    /**
     * action:状态通知ID
     *
     * @return 为true表示接收
     */
    public int getStatusProcessFlag();

    /**
     * action:设定状态ID
     */
    public Impl setStatusCode(int statusCode);

    /**
     * action:获取状态ID
     */
    public int getStatusCode();

    /**
     * action:设定线程ID
     */
    public Impl setThreadCode(int val,boolean auto_exit);

    /**
     * action:获取线程ID
     */
    public int getThreadCode();

    /**
     * action:是否自动退出线程
     */
    public boolean isAutoExitThread();

    public Bundle getData(boolean isClean);

    public Impl setData(Bundle val);
}

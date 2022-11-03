package kuyou.common.status.basic;

import android.os.Looper;

public interface IStatusProcessBusCallback {

    /**
     * action:设定状态通知处理线程,使用主线程
     */
    public final static int LOOPER_POLICY_MAIN = 0;

    /**
     * action:设定状态通知处理线程,使用后台线程
     */
    public final static int LOOPER_POLICY_BACKGROUND = 1;

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
}

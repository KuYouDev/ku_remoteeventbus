package kuyou.common.ipc.basic;

/**
 * action :RemoteEventBus初始化状态监听器
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-22 <br/>
 * </p>
 */
public interface IRemoteEventFrameStatusListener {
    public void onStatus(int code);
}

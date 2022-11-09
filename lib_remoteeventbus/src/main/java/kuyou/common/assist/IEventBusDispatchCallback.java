package kuyou.common.assist;

import kuyou.common.ipc.event.RemoteEvent;

/**
 * action :接口[事件分发器]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-29 <br/>
 * </p>
 */
public interface IEventBusDispatchCallback {
    public boolean dispatchEvent(RemoteEvent event);
}

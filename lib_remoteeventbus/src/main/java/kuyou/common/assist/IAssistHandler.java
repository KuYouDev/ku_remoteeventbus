package kuyou.common.assist;

import java.util.List;

import kuyou.common.ipc.basic.IEventBusDispatchCallback;

/**
 * action :
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 2022/11/9 <br/>
 * </p>
 */
public interface IAssistHandler {
    public List<Integer> getEventReceiveList();
    public IEventBusDispatchCallback getEventDispatchCallback();
}

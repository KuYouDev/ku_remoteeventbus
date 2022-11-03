package kuyou.common.ipc.client;

import java.util.Map;

/**
 * action :
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 22-1-6 <br/>
 * </p>
 */
public interface IHandlerGroupManager<T1,T2> {
    public Map<T1,T2> getHandlerGroup();
}

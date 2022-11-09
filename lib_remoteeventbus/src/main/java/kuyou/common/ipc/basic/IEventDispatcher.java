package kuyou.common.ipc.basic;

import android.os.Bundle;

import java.util.List;

import kuyou.common.ipc.event.RemoteEvent;

public interface IEventDispatcher<T> extends IRemoteConfig {
    public void dispatchEventRemote2Local(Bundle data);

    public boolean dispatch(RemoteEvent event);

    public T setEventReceiveList(List<Integer> list);
}

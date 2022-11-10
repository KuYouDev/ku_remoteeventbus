package kuyou.common.ipc.event;

import java.util.ArrayList;

/**
 * action :
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 22-3-11 <br/>
 * </p>
 */
public abstract class EventFrame extends RemoteEvent {

    public static final String KEY_TAG = "keyEventData.Tag";
    public static final String KEY_REGISTER_CLIENT_LIST = "keyEventData.registerClient";

    public static String getTag(RemoteEvent event) {
        return event.getData().getString(KEY_TAG);
    }

    public EventFrame setTag(String val) {
        getData().putString(KEY_TAG, val);
        return EventFrame.this;
    }

    public static ArrayList<String> getRegisterClientList(RemoteEvent event) {
        return event.getData().getStringArrayList(KEY_REGISTER_CLIENT_LIST);
    }

    public EventFrame setRegisterClientList(ArrayList<String> list) {
        getData().putStringArrayList(KEY_REGISTER_CLIENT_LIST, list);
        return EventFrame.this;
    }
}

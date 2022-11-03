package kuyou.common.ipc;

import android.os.Bundle;

/**
 * action :IPC框架传递的事件
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-29 <br/>
 * </p>
 */
public abstract class RemoteEvent {

    protected final static String TAG = "com.kuyou.ipc > RemoteEvent";

    protected final static String KEY_EVENT_CODE = "keyEventData.code";
    protected final static String KEY_EVENT_MODULE_FLAG_SOURCE = "keyEventData.moduleFlagSource";
    protected final static String KEY_EVENT_MODULE_FLAG_TARGET = "keyEventData.moduleFlagTarget";
    protected static final String KEY_EVENT_IS_REMOTE = "keyEventData.isRemote";
    protected final static String KEY_EVENT_START_PACKAGE_NAME = "keyEventData.packageName";

    protected final static int NONE = -1;

    private boolean isRemote = false;
    private boolean isDispatch2Myself = false;
    private boolean isEnableConsumeSeparately = true;

    private boolean isSticky = false;
    private Bundle mData = null;

    public abstract int getCode();

    public boolean isRemote() {
        return isRemote;
    }

    public RemoteEvent setRemote(boolean val) {
        isRemote = val;
        getData().putBoolean(KEY_EVENT_IS_REMOTE, val);
        return RemoteEvent.this;
    }

    public boolean isSticky() {
        return isSticky;
    }

    public RemoteEvent setSticky(boolean val) {
        isSticky = val;
        return RemoteEvent.this;
    }


    /**
     * action:发送远程事件时本地能否接收
     */
    public boolean isDispatch2Myself() {
        return isDispatch2Myself;
    }

    /**
     * action:发送远程事件时本地能否接收
     */
    public RemoteEvent setPolicyDispatch2Myself(boolean val) {
        isDispatch2Myself = val;
        return RemoteEvent.this;
    }

    /**
     * action:是否允许被单独消费，默认是
     */
    public boolean isEnableConsumeSeparately() {
        return isEnableConsumeSeparately;
    }

    /**
     * action:是否允许被单独消费
     */
    public RemoteEvent setEnableConsumeSeparately(boolean enableConsumeSeparately) {
        isEnableConsumeSeparately = enableConsumeSeparately;
        return RemoteEvent.this;
    }

    public Bundle getData() {
        if (null == mData) {
            mData = new Bundle();
            applyCode();
        }
        return mData;
    }

    public RemoteEvent setData(Bundle data) {
        applyData(data);
        applyCode();
        return RemoteEvent.this;
    }

    protected void applyCode() {
        getData().putInt(KEY_EVENT_CODE, getCode());
    }

    protected void applyData(Bundle data) {
        mData = data;
        setRemote(data.getBoolean(KEY_EVENT_IS_REMOTE, isRemote()));
    }

    public static int getCodeByData(Bundle data) {
        return data.getInt(KEY_EVENT_CODE, -1);
    }

    public static String getStartPackageNameByData(RemoteEvent event) {
        return getStartPackageNameByData(event.getData());
    }

    public static String getStartPackageNameByData(Bundle data) {
        return data.getString(KEY_EVENT_START_PACKAGE_NAME);
    }

    public static boolean isRemote(Bundle data) {
        return data.getBoolean(KEY_EVENT_IS_REMOTE);
    }

//    @Override
//    public String toString() {
//        Bundle data = getData();
//        String val = null;
//        StringBuilder keyValList = new StringBuilder("");
//        for (String key : data.keySet()) {
//            if (null == key)
//                continue;
//            val = data.getString(key);
//            if (null == val)
//                continue;
//            keyValList.append(key).append(" = ").append(val).append("\n");
//            val = null;
//        }
//        return keyValList.toString();
//    }
}

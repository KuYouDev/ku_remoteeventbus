package kuyou.common.ipc.event;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

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
    protected final static String KEY_INFO = "keyEventData.info";
    protected final static String KEY_INFO_LIST = "keyEventData.infoList";
    protected final static String KEY_EVENT_CODE = "keyEventData.code";
    protected final static String KEY_EVENT_START_PACKAGE_NAME = "keyEventData.packageName";
    protected final static String KEY_START_PROCESS_ID = "keyEventData.processId";

    protected final static int NONE = -1;

    private Bundle mData = null;

    //本地有效参数
    private boolean isRemote = false;//标识要发送远端的事件
    private boolean isDispatch2Myself = false;
    private boolean isEnableConsumeSeparately = true;
    
    private boolean isSticky = false;

    public abstract int getCode();

    public boolean isRemote() {
        return isRemote;
    }

    public RemoteEvent setRemote(boolean val) {
        isRemote = val;
        setSticky(true);
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

    public RemoteEvent setStartPackageName(String val) {
        getData().putString(KEY_EVENT_START_PACKAGE_NAME, val);
        return RemoteEvent.this;
    }

    public RemoteEvent setStartProcessID(long val) {
        getData().putLong(KEY_START_PROCESS_ID, val);
        return RemoteEvent.this;
    }

    protected void applyCode() {
        getData().putInt(KEY_EVENT_CODE, getCode());
    }

    protected void applyData(Bundle data) {
        mData = data;
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

    public static long getStartProcessId(RemoteEvent event) {
        return getStartProcessId(event.getData());
    }

    public static long getStartProcessId(Bundle data) {
        return data.getLong(KEY_START_PROCESS_ID);
    }

    public RemoteEvent setInfo(Parcelable val) {

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INFO, val);
        bundle.setClassLoader(getClass().getClassLoader());

        getData().putBundle(KEY_INFO, bundle);
        return RemoteEvent.this;
    }

    public RemoteEvent setInfo(BasicInfo... val) {

        Bundle bundle = new Bundle();
        bundle.putParcelableArray(KEY_INFO_LIST, val);
        bundle.setClassLoader(getClass().getClassLoader());

        getData().putBundle(KEY_INFO_LIST, bundle);
        return RemoteEvent.this;
    }

    public static <T extends BasicInfo> T getInfo(RemoteEvent event) {
        if (null == event || !event.getData().containsKey(KEY_INFO)) {
            return null;
        }
        Bundle bundle = event.getData().getBundle(KEY_INFO);
        bundle.setClassLoader(BasicInfo.class.getClassLoader());
        Parcelable result = bundle.getParcelable(KEY_INFO);
        if (!(result instanceof BasicInfo)) {
            return null;
        }
        return (T) result;
    }

    public static <T extends BasicInfo> List<T> getInfoList(RemoteEvent event) {
        if (null == event || !event.getData().containsKey(KEY_INFO_LIST)) {
            return null;
        }
        Bundle bundle = event.getData().getBundle(KEY_INFO_LIST);
        bundle.setClassLoader(BasicInfo.class.getClassLoader());

        Parcelable[] baseArray = bundle.getParcelableArray(KEY_INFO_LIST);
        if (null == baseArray || baseArray.length == 0) {
            return null;
        }
        List<T> list = new ArrayList<>();
        for (Parcelable item : baseArray) {
            list.add((T) item);
        }
        return list;
    }
}
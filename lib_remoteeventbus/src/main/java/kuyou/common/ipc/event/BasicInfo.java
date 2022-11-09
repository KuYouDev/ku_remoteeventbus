package kuyou.common.ipc.event;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @exclude action :信息[基础]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 22-2-10 <br/>
 * </p>
 */
public abstract class BasicInfo implements Parcelable {

    /**
     * action:流水号，用于请求和反馈一一对应
     */
    private long mFlowNumber = -1;

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @exclude action:用于框架解码通道信息后自动生成RemoteEvent客户端使用接口getEvent请重载此接口
     */
    public int getRemoteEventCode() {
        return -1;
    }

    /**
     * action:流水号，用于请求和反馈一一对应
     */
    public long getFlowNumber() {
        return mFlowNumber;
    }

    /**
     * action:流水号，用于请求和反馈一一对应
     */
    public BasicInfo setFlowNumber(long mFlowNumber) {
        this.mFlowNumber = mFlowNumber;
        return BasicInfo.this;
    }

    public long getId() {
        return getFlowNumber();
    }

    /**
     * action:流水号，用于请求和反馈一一对应
     */
    public BasicInfo setId(long id) {
        setFlowNumber(id);
        return BasicInfo.this;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getFlowNumber());
    }

    /**
     * action:用于框架解码通道信息后自动生成RemoteEvent
     * 客户端使用此接口时请重载接口getRemoteEventCode
     */
    public RemoteEvent getEvent() {
        if (-1 == getRemoteEventCode()) {
            return null;
        }
        return new RemoteEvent() {
            @Override
            public int getCode() {
                return BasicInfo.this.getRemoteEventCode();
            }
        }.setInfo(BasicInfo.this);
    }
}

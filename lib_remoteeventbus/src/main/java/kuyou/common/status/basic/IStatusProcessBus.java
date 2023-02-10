package kuyou.common.status.basic;

import android.os.Bundle;

public interface IStatusProcessBus {
    public void registerStatusNoticeCallback(int statusCode, IStatusProcessBusCallback callback);

    public void unRegisterStatus(int statusCode);

    public int registerStatusNoticeCallback(final IStatusProcessBusCallback callback);

    public void start(int processFlag);

    public void start(int processFlag, Bundle data);

    public void start(int processFlag, long delayed);

    public void start(int processFlag, long delayed, Bundle data);

    public void stop(int processFlag);

    public boolean isStart(int processFlag);
}

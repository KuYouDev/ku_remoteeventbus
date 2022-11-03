package kuyou.common.ipc;

import android.app.Application;
import android.content.Intent;

/**
 * action :暂时未启用
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-12-23 <br/>
 * </p>
 */
public class FrameApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, FrameRemoteService.class));
    }
}
package kuyou.common.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * action :IPC守护进程
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-22 <br/>
 * </p>
 */
public class ProcessAwaken extends BroadcastReceiver {
    protected final String TAG = "kuyou.common.ipc > ProcessAwaken";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " ProcessAwaken > onReceive > action = " + intent.getAction());
    }
}

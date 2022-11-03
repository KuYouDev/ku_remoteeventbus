package kuyou.common.ipc;

import android.os.Bundle;
import java.util.List;

interface IRemoteServiceCallBack{
	void onReceiveEvent(in Bundle data);
	List<String> getReceiveEventFlag();
}
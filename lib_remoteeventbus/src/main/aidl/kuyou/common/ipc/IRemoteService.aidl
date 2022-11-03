package kuyou.common.ipc;

import android.os.Bundle;
import java.util.List;
import kuyou.common.ipc.IRemoteServiceCallBack;

interface IRemoteService {

	void sendEvent(in Bundle data);
	void registerCallback(String packageName,IRemoteServiceCallBack cb);
	void unregisterCallback(String packageName,IRemoteServiceCallBack cb);
	List<String> getRegisterModules();
}
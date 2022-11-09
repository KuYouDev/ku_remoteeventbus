package kuyou.common.assist;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kuyou.common.ipc.RemoteEventBus;
import kuyou.common.ipc.event.RemoteEvent;
import kuyou.common.utils.HandlerClassFinder;

/**
 * action :业务协处理器加载器[抽象]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-23 <br/>
 * </p>
 */
public class AssistHandlerManager implements IHandlerFinder<BasicAssistHandler,Class<?>> {

    private volatile static AssistHandlerManager sInstance;
    private static final String TAG = "kuyou.common.assis > BasicAssistHandlerLoader";

    private Context mContext;
    private Map<Class<?>, BasicAssistHandler> mRequestParserList;
    private List<BasicAssistHandler> mHandlerList;
    private List<Integer> mAllHandleRemoteEventCode;
    private IEventBusDispatchCallback mDispatchCallback;

    private AssistHandlerManager() {
        mRequestParserList = new HashMap<Class<?>, BasicAssistHandler>();
        mAllHandleRemoteEventCode = new ArrayList<>();
        mHandlerList = new ArrayList<>();
        mDispatchCallback = new IEventBusDispatchCallback() {
            @Override
            public boolean dispatchEvent(RemoteEvent event) {
                return RemoteEventBus.getInstance().post(event);
            }
        };
    }

    public static AssistHandlerManager getInstance() {
        if (sInstance == null) {
            synchronized (AssistHandlerManager.class) {
                if (sInstance == null) {
                    sInstance = new AssistHandlerManager();
                }
            }
        }
        return sInstance;
    }

    public AssistHandlerManager setContext(Context val) {
        mContext = val.getApplicationContext();
        return AssistHandlerManager.this;
    }

    /**
     * action:自动注册业务协处理器
     **/
    public void loadHandler(Context context, ILoadCallback config) {
        if (null == config) {
            Log.e(TAG, "loadHandler > process fail : ILoadCallback is null ");
            return;
        }
        Class itemHandler = null;
        mContext = context.getApplicationContext();
        try {
            List<Class> allClass = config.getAllClasses();
            if(null == allClass || allClass.size() == 0){
                if(null == config.getClassFlag() || null == config.getClassPackageName()){
                    Log.e(TAG, "loadHandler > process fail : ILoadCallback is invalid");
                    return;
                }
                allClass = HandlerClassFinder.getAllClassesByMultiDex(mContext,
                        config.getClassPackageName(),
                        config.getClassFlag());
                if(allClass.size() == 0){
                    Log.e(TAG, "loadHandler > process fail : can't find class");
                    return;
                }
            }
            BasicAssistHandler instance;
            for (Class item : allClass) {
                itemHandler = item;
                instance = (BasicAssistHandler) item.newInstance();
                if (!instance.isEnable()) {
                    //Log.d(TAG, "loadHandler > disable instance = "+instance);
                    continue;
                }
                register(instance);
                config.onRegisterHandler(instance);
            }
            for (BasicAssistHandler handler : getHandlerList()) {
                handler.setReady(true);
            }
            config.onFinishResult(true);
        } catch (Exception e) {
            Log.e(TAG, new StringBuilder("loadHandler > process fail :auto register handler ")
                    .append("\n handler = ").append(null != itemHandler ? itemHandler.getName() : "null")
                    .append("\n").append(Log.getStackTraceString(e)).toString());
            config.onFinishResult(false);
        }
    }

    public AssistHandlerManager register(BasicAssistHandler instance) {
        if (getHandlerList().contains(instance)) {
            Log.w(TAG, "register > process warn : instance = " + instance);
            return AssistHandlerManager.this;
        }
        if (null != mContext) {
            instance.setContext(mContext);
        } else {
            Log.w(TAG, "register > process warn : context is null");
        }
        instance.setHandlerFinder(AssistHandlerManager.this);
        instance.setDispatchEventCallBack(mDispatchCallback);

        mAllHandleRemoteEventCode.addAll(instance.getHandleRegisterRemoteEventCodeList());
        mRequestParserList.put(instance.getClass(), instance);
        getHandlerList().add(instance);
        return AssistHandlerManager.this;
    }

    @Override
    public final <T extends BasicAssistHandler> T findHandlerByFlag(Class<?> flag) {
        if (mRequestParserList.containsKey(flag)) {
            return (T) mRequestParserList.get(flag);
        }
        Log.e(TAG, "findHandlerByFlag > process fail : flag = " + flag);
        return null;
    }

    public List<BasicAssistHandler> getHandlerList() {
        if (null == mHandlerList) {
            mHandlerList = new ArrayList<>();
        }
        return mHandlerList;
    }

    public List<Integer> getEventDispatchList() {
        if (0 == mAllHandleRemoteEventCode.size()) {
            return null;
        }
        return mAllHandleRemoteEventCode;
    }

    public boolean dispatchReceiveEventNotice(RemoteEvent event) {
        boolean result = false;
        for (BasicAssistHandler handler :getHandlerList()){
            if(0 == handler.getHandleRegisterEventCodeList().size()
                    || -1 == handler.getHandleRegisterEventCodeList().indexOf(event.getCode())){
                continue;
            }
            handler.onReceiveEventNotice(event);
            result = true;
        }
        return result;
    }

    public static interface ILoadCallback {
        public String getClassPackageName();

        public Class<?> getClassFlag();

        public List<Class> getAllClasses();

        public void onFinishResult(boolean isSuccess);

        public void onRegisterHandler(BasicAssistHandler handler);
    }
    
    public static abstract class LoadCallback implements ILoadCallback{

        @Override
        public String getClassPackageName() {
            return null;
        }

        @Override
        public Class<?> getClassFlag() {
            return null;
        }

        public List<Class> getAllClasses(){
            return null;
        }

        @Override
        public void onRegisterHandler(BasicAssistHandler handler) {
            
        }
    }
}

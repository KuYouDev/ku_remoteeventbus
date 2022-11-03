package kuyou.common.ipc.client;

import android.app.Application;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kuyou.common.ipc.RemoteEvent;
import kuyou.common.ipc.RemoteEventBus;
import kuyou.common.utils.HandlerClassFinder;

/**
 * action :框架核心进程
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-22 <br/>
 * </p>
 */
public abstract class BasicLocalModuleApplication extends Application {
    protected final String TAG = "kuyou.common.ipc.client > BasicLocalModuleApplication";

    public static interface IAutoRegisterEventHandlersConfig {
        public String getClassPackageName();

        public Class<?> getClassFlag();
    }

    public static interface IAutoRegisterEventHandlersResult {
        public final static int SUCCESS = 1;
        public final static int EXCEPTION = 0;
    }

    private Map<Class<?>, BasicAssistHandler> mRequestParserList = new HashMap<Class<?>, BasicAssistHandler>();
    private List<BasicAssistHandler> mEventHandlerList = null;

    private RemoteEventBus.IFrameLiveListener mFrameLiveListener;
    private IEventBusDispatchCallback mEventBusDispatchCallback;
    private IHandlerGroupManager<Class<?>, BasicAssistHandler> mHandlerGroupManagerCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        initFrame();
    }

    protected String getIpcFramePackageName() {
        return null;
    }

    protected void initFrame() {
        //模块间IPC框架初始化
        RemoteEventBus.getInstance(getApplicationContext())
                .setFramePackageName(getIpcFramePackageName())
                .register(new RemoteEventBus.IRegisterConfig() {
                    @Override
                    public RemoteEventBus.IFrameLiveListener getFrameLiveListener() {
                        return BasicLocalModuleApplication.this.getIpcFrameLiveListener();
                    }

                    @Override
                    public List<Integer> getEventDispatchList() {
                        return BasicLocalModuleApplication.this.getEventDispatchList();
                    }

                    @Override
                    public Object getLocalEventDispatchHandler() {
                        return BasicLocalModuleApplication.this;
                    }
                });
    }

    /**
     * action:自动注册业务协处理器
     **/
    protected void initRegisterEventHandlers() {
        IAutoRegisterEventHandlersConfig config = getAutoRegisterAssistHandlerConfig();
        if (null != config) {
            Class itemHandler = null;
            try {
                BasicAssistHandler instance;
                List<Class> allClass = HandlerClassFinder.getAllClassesByMultiDex(getApplicationContext(),
                        config.getClassPackageName(),
                        config.getClassFlag());
                if (allClass.size() == 0) {
                    Log.e(TAG, "initRegisterEventHandlers > process fail : can't find class");
                    return;
                    //throw new RuntimeException("initRegisterEventHandlers > can't find class");
                }
                //Log.d(TAG, "initRegisterEventHandlers > allClass.size() = "+allClass.size());
                for (Class item : allClass) {
                    itemHandler = item;
                    instance = (BasicAssistHandler) item.newInstance();
                    if (!instance.isEnable()) {
                        continue;
                    }
                    if (getEventHandlerList().contains(instance)) {
                        continue;
                    }

                    //Log.d(TAG, "init registerEventHandler > handler = " + instance);
                    registerEventHandler(instance);
                }
                onAutoRegisterAssistHandlersFinish(IAutoRegisterEventHandlersResult.SUCCESS);
            } catch (Exception e) {
                Log.e(TAG, new StringBuilder("initRegisterEventHandlers > process fail :auto register handler ")
                        .append("\n handler = ").append(null != itemHandler ? itemHandler.getName() : "null")
                        .append("\n").append(Log.getStackTraceString(e)).toString());
                onAutoRegisterAssistHandlersFinish(IAutoRegisterEventHandlersResult.EXCEPTION);
            }
        }
    }

    /**
     * action:自动注册业务协处理器需要的配置<br/>
     * remarks:返回空时会关闭自动注册  <br/>
     */
    protected IAutoRegisterEventHandlersConfig getAutoRegisterAssistHandlerConfig() {
        return null;
    }

    protected void onAutoRegisterAssistHandlersFinish(int resultCode) {

    }

    /**
     * action:模块间IPC框架状态监听器
     **/
    protected RemoteEventBus.IFrameLiveListener getIpcFrameLiveListener() {
        if (null == mFrameLiveListener) {
            mFrameLiveListener = new RemoteEventBus.IFrameLiveListener() {
                @Override
                public void onIpcFrameResisterSuccess() {
                    BasicLocalModuleApplication.this.onIpcFrameResisterSuccess();
                    Log.d(TAG, "onIpcFrameResisterSuccess > ");
                }

                @Override
                public void onIpcFrameUnResister() {
                    Log.d(TAG, "onIpcFrameUnResister > ");
                }
            };
        }
        return mFrameLiveListener;
    }

    /**
     * action:远程事件的监听列表
     **/
    protected List<Integer> getEventDispatchList() {
        if (0 == getEventHandlerList().size()) {
            Log.e(TAG, "getEventDispatchList > process fail : handlers is null");
            return null;
        }
        List<BasicAssistHandler> subHandlerList = new ArrayList<>();
        List<Integer> codeList = getAssistHandlerList(getEventHandlerList(), subHandlerList);
        if (subHandlerList.size() > 0) {
            getEventHandlerList().addAll(subHandlerList);
        }
        return codeList;
    }

    protected List<BasicAssistHandler> getEventHandlerList() {
        if (null == mEventHandlerList) {
            mEventHandlerList = new ArrayList<>();
            initRegisterEventHandlers();
        }
        return mEventHandlerList;
    }

    protected void onIpcFrameResisterSuccess() {
        for (BasicAssistHandler handler : getEventHandlerList()) {
            handler.setReady(true);
        }
    }

    private List<Integer> getAssistHandlerList(List<BasicAssistHandler> handlerList, List<BasicAssistHandler> subHandlerList) {
        List<Integer> remoteEventCodeList = new ArrayList<>();
        for (BasicAssistHandler handler : handlerList) {
            handler.setContext(getApplicationContext());
            handler.setDispatchEventCallBack(getEventBusDispatchCallback());
            handler.setHandlerGroupManager(getHandlerGroupManagerCallBack());
            handler.initStatusProcessBus();

            List<BasicAssistHandler> sub = handler.getSubEventHandlers();
            if (null != sub && sub.size() > 0) {
                subHandlerList.addAll(sub);
                remoteEventCodeList.addAll(getAssistHandlerList(sub, subHandlerList));
            }
            remoteEventCodeList.addAll(handler.getHandleRemoteEventCodeList());
        }
        return remoteEventCodeList;
    }

    protected IEventBusDispatchCallback getEventBusDispatchCallback() {
        if (null == mEventBusDispatchCallback) {
            mEventBusDispatchCallback = new IEventBusDispatchCallback() {
                @Override
                public boolean dispatchEvent(RemoteEvent event) {
                    return RemoteEventBus.getInstance().dispatch(event);
                }
            };
        }
        return mEventBusDispatchCallback;
    }

    protected IHandlerGroupManager<Class<?>, BasicAssistHandler> getHandlerGroupManagerCallBack() {
        if (null == mHandlerGroupManagerCallback) {
            mHandlerGroupManagerCallback = new IHandlerGroupManager<Class<?>, BasicAssistHandler>() {
                @Override
                public Map<Class<?>, BasicAssistHandler> getHandlerGroup() {
                    return BasicLocalModuleApplication.this.mRequestParserList;
                }
            };
        }
        return mHandlerGroupManagerCallback;
    }

    public final <T extends BasicAssistHandler> T findHandlerByFlag(Class<?> flag) {
        if (getHandlerGroupManagerCallBack().getHandlerGroup().containsKey(flag)) {
            return (T) getHandlerGroupManagerCallBack().getHandlerGroup().get(flag);
        }
        Log.e(TAG, "findHandlerByFlag > process fail : flag = " + flag);
        return null;
    }

    public BasicLocalModuleApplication registerEventHandler(BasicAssistHandler handler) {
        getEventHandlerList().add(handler);
        mRequestParserList.put(handler.getClass(), handler);
        //Log.w(TAG, "registerEventHandler > handler = " + handler);
        return BasicLocalModuleApplication.this;
    }

    //本地事件,默认使用后台线程分发
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onReceiveEventNotice(RemoteEvent event) {
        for (BasicAssistHandler handler : getEventHandlerList()) {
            if (handler.onReceiveEventNotice(event)) {
                if (event.isEnableConsumeSeparately()) {
                    return;
                }
            }
        }
        //Log.i(TAG, "onReceiveEventNotice > unable to consumption event = " + event.getCode());
    }
}

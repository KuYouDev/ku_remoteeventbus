package kuyou.common.ipc.basic;

/**
 * action :IPC框架的通用配置
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-24 <br/>
 * </p>
 */
public interface IRemoteConfig {

    public final static String ACTION_FLAG_FRAME_EVENT = "action.remote.event.frame";

    //0~2047
    public final static int FLAG_CODE = 0;

    //FLAG_CODE+0 ~ FLAG_CODE+2047
    public static interface Code {
        //frame codes 0 ~ 1023
        /**
         * action:IPC服务连接成功
         */
        public final static int BIND_IPC_SERVICE_SUCCESS = FLAG_CODE + 0;
        /**
         * action:IPC服务连接超时
         */
        public final static int BIND_IPC_SERVICE_TIME_OUT = FLAG_CODE + 1;
        /**
         * action:IPC服务连接断开
         */
        public final static int UNBIND_IPC_SERVICE = FLAG_CODE + 2;

        /**
         * action:模块远程注册成功
         */
        public final static int CLIENT_REGISTER_SUCCESS = FLAG_CODE + 11;
        /**
         * action:模块远程注册失败
         */
        public final static int CLIENT_REGISTER_FAIL = FLAG_CODE + 12;
        /**
         * action:模块远程断开成功
         */
        public final static int CLIENT_UNREGISTER_SUCCESS = FLAG_CODE + 13;


        //handle status codes 1024 ~ 2046
        /**
         * action:远程事件分发失败，进行粘性缓存
         */
        public final static int REMOTE_EVENT_DISPATCH_FAIL_STICKY = FLAG_CODE + 1024;


        /**
         * action:框架事件标识[保留]
         */
        public final static int FRAME_FLAG = FLAG_CODE + 2047;
    }

}

package rxbus.ecaray.com.rxbuslib.rxbus;

import android.support.annotation.StringDef;
import android.util.ArrayMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Jiashu on 2015/11/3.
 * 线程调度器常量类
 */
public class RxBusScheduler {

    private static ArrayMap<String, Scheduler> sSchedulersMapper;

    public static final String NEW_THREAD = "newThread";
    public static final String COMPUTATION = "computation";
    public static final String IMMEDIATE = "immediate";
    public static final String IO = "io";
    public static final String TEST = "test";
    public static final String TRAMPOLINE = "trampoline";
    public static final String MAIN_THREAD = "main_thread";

    @StringDef({NEW_THREAD, COMPUTATION, IMMEDIATE, IO, TEST, TRAMPOLINE, MAIN_THREAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Theme {}

    static {
        sSchedulersMapper = new ArrayMap<>();
        sSchedulersMapper.put(NEW_THREAD, Schedulers.newThread());
        sSchedulersMapper.put(COMPUTATION, Schedulers.computation());
        sSchedulersMapper.put(IMMEDIATE, Schedulers.immediate());
        sSchedulersMapper.put(IO, Schedulers.io());
        sSchedulersMapper.put(TEST, Schedulers.test());
        sSchedulersMapper.put(TRAMPOLINE, Schedulers.trampoline());
        sSchedulersMapper.put(MAIN_THREAD, AndroidSchedulers.mainThread());
    }

    public static Scheduler getScheduler(@Theme String key) {
        return sSchedulersMapper.get(key);
    }
}

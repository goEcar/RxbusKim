package rxbus.ecaray.com.rxbuslib.rxbus;


import android.annotation.SuppressLint;
import android.util.ArrayMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Jiashu on 2015/11/3.
 * RxBus 基于 RxJava 设计的用于组件间通讯的事件总线。test
 */
public class RxBus {
    public static final String DEFAULT_TAG = "default";                 // 默认的 tag
    private static RxBus sBus;

    private SerializedSubject<RxBusEvent, RxBusEvent> mSubject;
    private SerializedSubject<RxBusEvent, RxBusEvent> mStickySubject;
    private ArrayMap<String, CompositeSubscription> mSubscribeMapper;

    @SuppressLint("NewApi")
    private RxBus() {
        mSubject = new SerializedSubject<>(PublishSubject.<RxBusEvent>create());
        mStickySubject = new SerializedSubject<>(ReplaySubject.<RxBusEvent>createWithSize(1));
        mSubscribeMapper = new ArrayMap<>();
    }

    public static RxBus getDefault() {
        if (sBus == null) {
            sBus = new RxBus();
        }
        return sBus;
    }

    /**
     * 发送一个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件。
     * @param event
     * @param tag
     */
    public void post(Object event, String tag) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(event, tag));
        }
    }
    /**
     * 发送一个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件,默认参数为Object。
     * @param event
     * @param tag
     */
    public void post( String tag) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(new Object(), tag));
        }
    }

    /**
     * 发送一个事件，该事件的标记为默认的tag
     * @param event
     */
    public void post(Object event) {
        post(event, DEFAULT_TAG);
    }

    /**
     * 发送一个 黏性事件，并标记该事件为 tag。同理，只有标记为 tag 的方法才能响应该 事件。
     * 所谓 黏性事件 是指即使在该事件发送后才进行 注册RxBus 的组件，也能接收到该 事件
     * 整个 RxBus 只能维持一个 黏性事件，最后发送的黏性事件会取代前面的事件。
     * @param event
     */
    public void postSticky(Object event, String tag) {
        if (mStickySubject != null) {
            mStickySubject.onNext(new RxBusEvent(event, tag));
        }
    }

    /**
     * 发送一个默认tag的黏性事件
     * @param event
     */
    public void postSticky(Object event) {
        postSticky(event, DEFAULT_TAG);
    }

    /**
     * 取消 黏性事件
     * 黏性事件 可能存在着一些未知隐患，请谨慎使用。
     * 建议发布黏性事件后，需要在合适的时机取消该 黏性事件
     * ToDo: 下一版将以更合理的方式来取消黏性事件，并考虑支持维持多个黏性事件
     */
    public void cancelStickyEvent() {
        mStickySubject = new SerializedSubject<>(ReplaySubject.<RxBusEvent>createWithSize(1));
    }

    /**
     * 返回 事件发布者。
     * 熟悉 RxJava 的开发者可以通过本方法获取到 事件发布者，自定义事件响应策略。
     * @return
     */
    public Observable<RxBusEvent> getObservable() {
        return mSubject.asObservable().mergeWith(mStickySubject.asObservable());
    }

    public SerializedSubject<RxBusEvent, RxBusEvent> get() {
        return mSubject;
    }

    public boolean hasObservers() {
        return mSubject.hasObservers();
    }


    /**
     * 注册 RxBus
     * @param object
     */
    @SuppressLint("NewApi")
    public void register(final Object object) {
        CompositeSubscription subscriptions = new CompositeSubscription();
        for (final Method method: object.getClass().getDeclaredMethods()) {
            RxBusReact accept = method.getAnnotation(RxBusReact.class);
            if (accept != null) {
                final Class clazz = accept.clazz();
                final String tag = accept.tag();
                Scheduler observeScheduler = RxBusScheduler.getScheduler(accept.observeOn());
                Scheduler subscribeScheduler = RxBusScheduler.getScheduler(accept.subscribeOn());
                Subscription subscription = getObservable()
                        .subscribeOn(subscribeScheduler)
                        .filter(new Func1<RxBusEvent, Boolean>() {
                            @Override
                            public Boolean call(RxBusEvent rxBusType) {
                                return clazz.equals(rxBusType.getObj().getClass()) &&
                                        tag.equals(rxBusType.getTag());
                            }
                        })
                        .observeOn(observeScheduler)
                        .subscribe(new Action1<RxBusEvent>() {
                            @Override
                            public void call(RxBusEvent rxBusType) {
                                try {
                                    method.invoke(object, rxBusType.getObj());
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                subscriptions.add(subscription);
            }
        }
        mSubscribeMapper.put(object.getClass().getName(), subscriptions);
    }

    /**
     * 反注册 RxBus
     * @param object
     */
    @SuppressLint("NewApi")
    public void unregister(Object object) {
        String key = object.getClass().getName();
        CompositeSubscription subscriptions = mSubscribeMapper.get(key);
        if (subscriptions != null && !subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
            mSubscribeMapper.remove(key);
        }
    }
}

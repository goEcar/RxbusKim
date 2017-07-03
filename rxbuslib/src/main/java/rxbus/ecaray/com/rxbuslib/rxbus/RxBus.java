package rxbus.ecaray.com.rxbuslib.rxbus;


import android.annotation.SuppressLint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subscriptions.CompositeSubscription;

import static android.R.attr.tag;

/**
 * Created by Jiashu on 2015/11/3.
 * RxBus 基于 RxJava 设计的用于组件间通讯的事件总线。test
 */
public class RxBus {
    public static final String DEFAULT_TAG = "default";                 // 默认的 tag
    private static RxBus sBus;

    private SerializedSubject<RxBusEvent, RxBusEvent> mSubject;
    private SerializedSubject<RxBusEvent, RxBusEvent> mStickySubject;
    private HashMap<String, CompositeSubscription> mSubscribeMapper;

    @SuppressLint("NewApi")
    private RxBus() {
        mSubject = new SerializedSubject<>(PublishSubject.<RxBusEvent>create());
        mStickySubject = new SerializedSubject<>(ReplaySubject.<RxBusEvent>createWithSize(1));
        mSubscribeMapper = new HashMap<>();
    }

    public static RxBus getDefault() {
        if (sBus == null) {
            sBus = new RxBus();
        }
        return sBus;
    }


    /**
     * 发送一个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件,默认参数为Object。
     *
     * @param tag
     */
    public void post(String tag) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(new Object[]{}, tag));
        }
    }
    /**
     * 发送空的事件  tag为默认tag
     *
     */
    public void post() {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(new Object[]{}, DEFAULT_TAG));
        }
    }

    /**
     * 发送多个事件，该事件的标记为默认的tag
     *
     * @param event
     */
    public void post(Object[] event) {
        post(DEFAULT_TAG, event);
    }


//    /**
//     * 发送一个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件。
//     *
//     * @param event
//     * @param tag
//     */
//    public void post(String tag, Object event) {
//        Object[] objs = new Object[]{event};
//        post(tag, objs);
//    }

    /**
     * 发送多个事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件。
     *
     * @param event
     * @param tag
     */
    public void post(String tag, Object[] event) {
        if (mSubject != null) {
            mSubject.onNext(new RxBusEvent(event, tag));
        }
    }

    public void post(Object event, String tag) {
        if(this.mSubject != null) {
            Object[] objs = new Object[]{event};
            this.mSubject.onNext(new RxBusEvent(objs, tag));
        }
    }


    /**
     * 发送一个默认tag事件，并标记该事件为 tag。只有指定为 tag 的地方才能响应该 事件,默认参数为Object。
     *
     * @param tag
     */
    public void postDefault(String tag) {
        post(tag);
    }

//    /**
//     * 发送一个事件，该事件的标记为默认的tag
//     *
//     * @param event
//     */
//    public void post(Object event) {
//        post(DEFAULT_TAG, event);
//    }


    /**
     * 返回 事件发布者。
     * 熟悉 RxJava 的开发者可以通过本方法获取到 事件发布者，自定义事件响应策略。
     *
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
     *
     * @param object
     */
    @SuppressLint("NewApi")
    public void register(final Object object) {
        CompositeSubscription subscriptions = new CompositeSubscription();
        for (final Method method : object.getClass().getDeclaredMethods()) {
            RxBusReact accept = method.getAnnotation(RxBusReact.class);
            if (accept != null) {
                final Class[] clazz = accept.clazz();
                final String tag = accept.tag();
                Scheduler observeScheduler = RxBusScheduler.getScheduler(accept.observeOn());
                Scheduler subscribeScheduler = RxBusScheduler.getScheduler(accept.subscribeOn());
                Subscription subscription = getObservable()
                        .subscribeOn(subscribeScheduler)
                        .filter(new Func1<RxBusEvent, Boolean>() {
                            @Override
                            public Boolean call(RxBusEvent rxBusType) {
                                return (rxBusType.isThisClass(clazz) &&
                                        tag.equals(rxBusType.getTag()));

                            }
                        })
                        .observeOn(observeScheduler)
                        .subscribe(new Action1<RxBusEvent>() {
                            @Override
                            public void call(RxBusEvent rxBusType) {
                                try {

                                    method.invoke(object, rxBusType.getObj());
                                } catch (Exception e) {
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
     * 注册 RxBus
     *
     * @param object
     */
    @SuppressLint("NewApi")
    public void register(final Object object,boolean isonBackpressureLatest) {
        CompositeSubscription subscriptions = new CompositeSubscription();
        for (final Method method : object.getClass().getDeclaredMethods()) {
            RxBusReact accept = method.getAnnotation(RxBusReact.class);
            if (accept != null) {
                final Class[] clazz = accept.clazz();
                final String tag = accept.tag();
                Scheduler observeScheduler = RxBusScheduler.getScheduler(accept.observeOn());
                Scheduler subscribeScheduler = RxBusScheduler.getScheduler(accept.subscribeOn());
                Observable<RxBusEvent> observable = getObservable();
                if(isonBackpressureLatest){
                    observable.onBackpressureLatest();
                }
                Subscription subscription = observable
                        .subscribeOn(subscribeScheduler)
                        .filter(new Func1<RxBusEvent, Boolean>() {
                            @Override
                            public Boolean call(RxBusEvent rxBusType) {
                                return (rxBusType.isThisClass(clazz) &&
                                        tag.equals(rxBusType.getTag()));

                            }
                        })
                        .observeOn(observeScheduler)
                        .subscribe(new Action1<RxBusEvent>() {
                            @Override
                            public void call(RxBusEvent rxBusType) {
                                try {

                                    method.invoke(object, rxBusType.getObj());
                                } catch (Exception e) {
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
     *
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

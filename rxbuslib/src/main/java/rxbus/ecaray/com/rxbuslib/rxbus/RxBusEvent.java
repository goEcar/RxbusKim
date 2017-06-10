package rxbus.ecaray.com.rxbuslib.rxbus;

import java.util.Arrays;

/**
 * Created by zhanjiashu on 2015/11/4.
 * RxBus 事件封装类
 * 封装了 事件主体 和 事件标记
 */
public class RxBusEvent<T> {
    private String tag;
    private Object[] obj;
    private Class[] clazz;

    public RxBusEvent(Object[] obj, String tag) {
        this.tag = tag;
        this.obj = obj;
        if (obj == null || obj.length == 0) {
            clazz = new Class[]{};
        } else {
            int i = 0;
            clazz = new Class[obj.length];
            for (Object object : obj) {
                clazz[i++] = object.getClass();
            }
        }
    }

    public String getTag() {
        return tag;
    }

    public Object[] getObj() {
        return obj;
    }

    //判断是否为此传入的参数是否为要接受的类
    public boolean isThisClass(Class[] classz) {
        if (classz == null || obj == null || obj.length == 0) {
            return true;
        }
        return classz.length == this.clazz.length && Arrays.toString(this.clazz).equals(Arrays.toString(clazz));
//                obj[0].getClass().equals(clazz[0]);
    }
}

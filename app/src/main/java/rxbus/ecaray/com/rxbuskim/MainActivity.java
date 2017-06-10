package rxbus.ecaray.com.rxbuskim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import rxbus.ecaray.com.rxbuslib.rxbus.RxBus;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBusReact;
import rxbus.ecaray.com.rxbuslib.rxbus.RxBusScheduler;

import static android.R.attr.tag;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxBus.getDefault().register(this);

        findViewById(R.id.btn_show).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxBus.getDefault().post();  //接收方法注解:   tag = RxBus.DEFAULT_TAG
                RxBus.getDefault().post("tag");  //接收方法注解:   tag = "tag"
                //根据tag+参数发送
                RxBus.getDefault().post("tag", "1个参数"); //接收方法注解:  tag== "tag" ,clazz = {String.class}
                RxBus.getDefault().post(new String[]{"一个参数"});//接收方法注解:      tag== RxBus.DEFAULT_TAG ,clazz = {String.class}
                RxBus.getDefault().post(new Object[]{"多种参数1", 10});//接收方法注解:      tag== RxBus.DEFAULT_TAG ,clazz = {String.class}

                RxBus.getDefault().post("tag", new String[]{"3个参数", "3个参数", "3个参数"}); //接收方法注解: tag== "tag" ,clazz = {String.class,String.class,String.class}
                RxBus.getDefault().post("tag", new Object[]{"多种参数", 1, 9d}); //接收方法注解: tag== "tag" ,clazz = {String.class,String.class,String.class}

            }
        });
    }


    //不写tag默认为defaultTag
    @RxBusReact()
    public void showContent1() {
        Log.d("tagutil", "1收到的参数为空: ");
    }
    @RxBusReact(tag = "tag")
    public void showContent2() {
        Log.d("tagutil", "2收到的参数为空: ");
    }

    @RxBusReact(clazz = {String.class}, tag = RxBus.DEFAULT_TAG)
    public void showContent3(String content) {
        Log.d("tagutil", "3收到的参数: " + content);
    }

    @RxBusReact(clazz = {String.class, Integer.class})
    public void showContent4(String content, int i) {
        Log.d("tagutil", "收到的参数: " + content);
    }
    @RxBusReact(clazz = {String.class, String.class, String.class}, tag = "tag")
    public void showContent(String content, String k, String test) {
        Log.d("tagutil", "收到的参数: " + content);
    }



    @RxBusReact(clazz = {String.class, Integer.class, Double.class}, tag = "tag")
    public void showContent2(String content, int i, double k) {
        Log.d("tagutil", "收到的参数: " + content);
    }





    @RxBusReact()
    public void showContent() {
        Log.d("tagutil", "收到的参数为空: ");
    }
}

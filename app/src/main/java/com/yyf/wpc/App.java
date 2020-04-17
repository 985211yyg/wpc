package com.yyf.wpc;

import android.app.Application;

import com.clj.fastble.BleManager;

/**
 * Author: yyg
 * Date: 2020/4/17 20:16
 * Description:
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BleManager.getInstance().init(this);
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000);
    }
}

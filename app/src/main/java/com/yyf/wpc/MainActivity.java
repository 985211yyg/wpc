package com.yyf.wpc;

import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.kyleduo.switchbutton.SwitchButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    //蓝牙服务uuid
    public static final String SERVICE_UUID = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    //蓝牙服务数据接收UUID
    public static final String CHAR_NOTIFY_UUID = "49535343-1E4D-4BD9-BA61-23C647249616";
    //蓝牙服务数据写入UUID
    public static final String CHAR_WRITE_UUID = "49535343-8841-43F4-A8D4-ECBE34729BB3";
    //蓝牙设备Mac
    public static final String MAC = "00:0C:BF:15:62:A3";
    //设备连接开关
    private SwitchButton mDevicesSwitch;
    //风力发电机状态开关
    private SwitchButton mStatusSwitch;
    //转速显示
    private TextView mTextView;
    //蓝牙设备实例
    private BleDevice mCurrentBleDevice;
    //蓝牙是否连接
    private boolean isConnect = false;
    //分钟转速
    private int turns = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDevicesSwitch = findViewById(R.id.device_switch);
        mStatusSwitch = findViewById(R.id.status_switch);
        mTextView = findViewById(R.id.info_show);
        //判断手机蓝牙是否开启
        if (!BleManager.getInstance().isBlueEnable()) {
            //开启手机蓝牙
            BleManager.getInstance().enableBluetooth();
        }
        //风力放电机状态控制按钮
        mStatusSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //设备没有链接是无法控制发电机状态
                if (!isConnect) {
                    showMsg("请先连接设备！");
                    mStatusSwitch.setChecked(false);
                    return;
                }
                if (isChecked) {
                    //过速关闭风力发电机
                    writeByteData(intToByteArray(2));
                } else {
                    //正常情况
                    writeByteData(intToByteArray(1));
                }
            }
        });

        //设备连接按钮
        mDevicesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    connectDevices(MAC);
                } else {
                    BleManager.getInstance().disconnect(mCurrentBleDevice);
                }
            }
        });
    }


    /**
     * 连接设备
     *
     * @param mac 设置mac
     */
    private void connectDevices(String mac) {
        BleManager.getInstance().connect(mac, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                // 开始连接
                showMsg("设备连接中...");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                // 连接失败
                showMsg("设备连接失败！");
                isConnect = false;
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // 连接成功，BleDevice即为所连接的BLE设备
                showMsg("设备连接成功！");
                mCurrentBleDevice = bleDevice;
                isConnect = true;
                //订阅通知
                subscribeNotify();

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
                showMsg("设备连接中断！");
            }
        });

    }

    /**
     * 连接设备 接收风力发电机的转速  单位:转/分钟
     */
    private void subscribeNotify() {
        BleManager.getInstance().notify(
                mCurrentBleDevice,
                SERVICE_UUID,
                CHAR_NOTIFY_UUID,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        // 打开通知操作成功
                        showMsg("准备接收数据");
                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Log.e(TAG, "onNotifyFailure: " + exception.toString());
                        // 打开通知操作失败
                        showMsg(exception.toString());
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现
                        Log.e(TAG, "onCharacteristicChanged: " + data[0]);
                        turns = data[0];
                        mTextView.setText(data[0] + "转/秒");
                        //======根据转速自动控制风力发电机工作状态========
                        if (turns > 10) {
                            //如果大于40/min视为过速状态，关闭指示灯
                            writeByteData(intToByteArray(2));
                            mStatusSwitch.setChecked(true);
                        }
                    }
                });

    }

    /**
     * 写数据，控制风力发电机的工作状态 0开  1关
     *
     * @param data
     */
    private void writeByteData(byte[] data) {
        BleManager.getInstance().write(
                mCurrentBleDevice,
                SERVICE_UUID,
                CHAR_WRITE_UUID,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {

                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        showMsg(exception.toString());
                    }
                });
    }


    /**
     * int到byte[] 由高位到低位
     *
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    private byte[] intToByteArray(int i) {
        byte[] result = new byte[1];
        result[0] = (byte) (i & 0xFF);
        return result;
    }


    private void showMsg(String s) {
        ToastUtils.showLong(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
    }
}

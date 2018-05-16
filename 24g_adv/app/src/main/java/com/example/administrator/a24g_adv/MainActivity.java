package com.example.administrator.a24g_adv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

public class MainActivity extends AppCompatActivity {
    private static final int ENABLE_BLUETOOTH = 1234;

    Switch mySwitch, mySwitchr;
    TextView myTV, myTVR;
    //String myUUIDstring = "CDB7950D-73F1-4D4D-8E47-C090502DBD63";
    String myUUIDstring = "ec505efd-75b9-44eb-8f2a-6fe0b41e7264";
    int count = 0;
    BluetoothManager myManager;
    BluetoothAdapter myAdapter;
    BluetoothLeAdvertiser myAdvertiser;
    AdvertiseSettings myAdvertiseSettings;
    AdvertiseData myAdvertiseData;

    EditText data, interval, time;
    TextView time_already;

    BluetoothLeScanner myScanner;

    boolean advFlag = true;

    final byte[] ad_data = new byte[23];

    byte[] sendData =  new byte[35];//{0,0,0,0,0,0,0,0,0,0,0,0,0x8E, 0xF0, 0xAA, 0xD6, 0xBE, 0x89, 0x8E, 0x25};
    byte[] whitening_reg = new byte[8];
    byte test_send_byte = 0;

    String mac_address_str;
    byte[] mac_address_byte = new byte[6];

    private void get_system_mac_address(){
        mac_address_str = getMac(this);
        Log.d("Jeff_char", mac_address_str);
        String [] strArr = mac_address_str.split(":");

        for(int i = 0;i < strArr.length; i++) {
            int value = Integer.parseInt(strArr[i], 16);
            mac_address_byte[i] = (byte) value;
        }
        Log.d("Jeff_char", bytesToString(mac_address_byte));
    }

    public void data_init()
    {
        if(test_send_byte > 0xff)
        {
            test_send_byte = 0;
        }
        sendData[19] = (byte)test_send_byte++;
    }

    public void whitening_init(byte channel_index)
    {
        byte i = 0;
        whitening_reg[0] = 1;

        for (i = 1; i < 7; i++)
        {
            whitening_reg[i] = (byte)((channel_index >> (6 - i)) & 0x01);
        }
    }

    public byte whitening_output()
    {
        byte temp = (byte)(whitening_reg[3] ^ whitening_reg[6]);

        whitening_reg[3] = whitening_reg[2];
        whitening_reg[2] = whitening_reg[1];
        whitening_reg[1] = whitening_reg[0];
        whitening_reg[0] = whitening_reg[6];
        whitening_reg[6] = whitening_reg[5];
        whitening_reg[5] = whitening_reg[4];
        whitening_reg[4] = temp;

        return whitening_reg[0];
    }

    public byte whitening_decode(byte[] data, byte length)
    {
        byte data_index = 0;
        byte data_input = 0;
        byte data_bit = 0;
        byte data_output = 0;

        for (data_index = 0; data_index < length; data_index++)
        {
            data_input = data[data_index];
            data_bit = 0;
            data_output = 0;

            for (byte bit_index = 0; bit_index < 8; bit_index++)
            {
                data_bit = (byte)((data_input >> (bit_index)) & 0x01);

                data_bit ^= whitening_output();

                data_output += (data_bit << (bit_index));
            }

            data[data_index] = data_output;
            //data_re = data_output;
            //if(data_index > 11 && data_index < 20)
            {
                //printf("Result == %x\n", data_re);
            }
        }
        return data_output;
    }

    public void onSendBtnData(View v)
    {
        int i = 0;
        if (data.getText().length() == 0 || (Integer.parseInt(data.getText().toString()) < 0 || Integer.parseInt(data.getText().toString()) > 255) ) {
            Toast.makeText(MainActivity.this, "Please input advertise data: 0-255!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (interval.getText().length() == 0 || Integer.parseInt(interval.getText().toString()) <= 0) {
            Toast.makeText(MainActivity.this, "Please input advertise interval > 0!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (time.getText().length() == 0 || Integer.parseInt(time.getText().toString()) <= 0) {
            Toast.makeText(MainActivity.this, "Please input advertise time > 0!", Toast.LENGTH_SHORT).show();
            return;
        }
        //final byte[] ad_data = new byte[27];
        //final byte[] ad_data = new byte[8];
        final int ad_byte = Integer.parseInt(data.getText().toString());
        //ad_data[0] = (byte)(ad_byte);
        //37 Channel
        ad_data[0] = (byte)0X18;
        ad_data[1] = (byte)0X87;
        ad_data[2] = (byte)0X52;
        ad_data[3] = (byte)0X35;
        ad_data[4] = (byte)0XF8;
        ad_data[5] = (byte)0X60;
        ad_data[6] = (byte)0X25;
        //38 Channel
//        ad_data[0] = (byte)0Xf5;
//        ad_data[1] = (byte)0Xbe;
//        ad_data[2] = (byte)0X67;
//        ad_data[3] = (byte)0X63;
//        ad_data[4] = (byte)0X68;
//        ad_data[5] = (byte)0X21;
//        ad_data[6] = (byte)0Xe1;
        //ad_data[7] = (byte)((byte)ad_data[7] + (byte)0X01);

        if(v.getId() == R.id.Send_btn){//Pair
            //data_init();
            sendData[19] = (byte)0x70;//cmd
            sendData[20] = (byte)0x11;//factory_id
            sendData[21] = (byte)0x01;//MAC_ADDRESS
            sendData[22] = (byte)0x02;
            sendData[23] = (byte)0x03;
            sendData[24] = (byte)0x04;
            sendData[25] = (byte)0x05;
            sendData[26] = (byte)0x06;
            sendData[27] = (byte)0x00;//LR
            sendData[28] = (byte)0x00;//FB
            sendData[29] = (byte)0x00;//KDATA
            sendData[30] = (byte)((sendData[19]+sendData[20]+sendData[21]+sendData[22]+sendData[23]+sendData[24]+sendData[25]
                            +sendData[26]+sendData[27]+sendData[28]+sendData[29])&0XFF);//CRC
            sendData[31] = (byte)0x50;//cmd
            sendData[32] = (byte)0x50;//cmd
            sendData[33] = (byte)0x50;//cmd
            sendData[34] = (byte)0x50;//cmd

            ///whitening_init((byte)37);//RF Channel
            //ad_data[7] =  whitening_decode(sendData, (byte)20);
            ///whitening_decode(sendData, (byte)35);
            for(i = 0; i < 16; i++)
            {
                ad_data[7+i] = sendData[19+i];
            }
        }else if(v.getId() == R.id.Send_on_btn){//Forward
            //data_init();
            sendData[19] = (byte)0x60;//cmd
            sendData[20] = (byte)0x11;//factory_id
            sendData[21] = (byte)0x01;//MAC_ADDRESS
            sendData[22] = (byte)0x02;
            sendData[23] = (byte)0x03;
            sendData[24] = (byte)0x04;
            sendData[25] = (byte)0x05;
            sendData[26] = (byte)0x06;
            sendData[27] = (byte)0x00;//LR
            sendData[28] = (byte)0x64;//FB
            sendData[29] = (byte)0x08;//KDATA
            sendData[30] = (byte)((sendData[19]+sendData[20]+sendData[21]+sendData[22]+sendData[23]+sendData[24]+sendData[25]
                    +sendData[26]+sendData[27]+sendData[28]+sendData[29])&0XFF);//CRC
            sendData[31] = (byte)0x50;//cmd
            sendData[32] = (byte)0x50;//cmd
            sendData[33] = (byte)0x50;//cmd
            sendData[34] = (byte)0x50;//cmd

            whitening_init((byte)37);
            whitening_decode(sendData, (byte)35);
            for(i = 0; i < 16; i++)
            {
                ad_data[7+i] = sendData[19+i];
            }
        }else if(v.getId() == R.id.Send_ble_test__btn){
            ad_data[7] = (byte)0x60;
        }

        //Toast.makeText(MainActivity.this, byteArrToString(ad_data), Toast.LENGTH_SHORT).show();
        final int ad_interval = 100;//Integer.parseInt(interval.getText().toString());
        final int ad_time = 1;//Integer.parseInt(time.getText().toString());

        new Thread(new Runnable() {
            @Override
            public void run() {
                ParcelUuid puuid = new ParcelUuid(UUID.fromString(myUUIDstring));
                //count 65520
                myAdvertiseData = new AdvertiseData.Builder()
                        .addManufacturerData(65520,ad_data)
                        .build();

                UIHandler.obtainMessage(0, 0, 0).sendToTarget();

                int cnt = 0;
                advFlag = true;
                while (cnt < ad_time && advFlag)
                {
                    try {
                        myAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, myAdvertiseCallback);
                        Thread.sleep(ad_interval);
                        myAdvertiser.stopAdvertising(myAdvertiseCallback);
                        cnt++;
                        UIHandler.obtainMessage(0, cnt, 0).sendToTarget();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                UIHandler.obtainMessage(1).sendToTarget();
            }
        }).start();
    }

    private Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    time_already.setText(msg.arg1 + "");
                    break;
                case 1:
                    mySwitch.setChecked(false);
                    break;
            }
        }
    };

    AdvertiseCallback myAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v("TAG", "Advertise start succeeds: " + settingsInEffect.toString());
            //myTV.append("\nAdvertisement restarted successfully with new data.");
            myTV.invalidate();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.v("Tag", "Advertise start failed: " + errorCode);
            myTV.append("\nAdvertisement restart failed: code = " + errorCode);
            myTV.invalidate();
        }
    };

    ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String str = null;
            if (result == null) {
                Log.v("Tag", "Result = NULL");
                return;
            }else{
                str = result.toString();
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                Log.d("Jeff_char", "Have Scan Results!!!");
            }

    /*        if (result.getDevice() == null) {
                Log.v("Tag", "Device = NULL");
                return;
            }
            StringBuilder builder = new StringBuilder(myUUIDstring);

            List<ParcelUuid> lp = result.getScanRecord().getServiceUuids();
            Map<ParcelUuid, byte[]> lpmap = result.getScanRecord().getServiceData();

            Object mykey = lpmap.keySet().toArray()[0];
            byte[] data = lpmap.get(mykey);
            String dstr = byteArrToString(data);
            String hstr = stringToHex(dstr);
            builder.append("\n").append(dstr).append(" (0x").append(hstr).append(")");

            Log.v("Tag", builder.toString());
            myTVR.setText(builder.toString());
            myTVR.invalidate();*/
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBluetoothState();
        get_system_mac_address();

    }
    @Override
    protected void onActivityResult(int RequestCode, int resultCode, Intent data) {
        if (RequestCode == ENABLE_BLUETOOTH) {
            switch (resultCode) {
                case RESULT_OK:
                    Toast.makeText(MainActivity.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                    BT_Adv_init();
                    BT_AdvReceive_init();
                    break;
                default:
                    Toast.makeText(MainActivity.this, "需开启蓝牙", Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
            }
        }
    }

    private void checkBluetoothState() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        } else {
            BT_Adv_init();
            BT_AdvReceive_init();
        }
    }

    static String byteArrToString(byte[] data){

        char[] result = new char[data.length];
        for(int i = 0; i < data.length; i++){
            result[i] = (char) data[i];
        }
        return new String(result);
    }

    static String stringToHex(String charStr) {
        char[] charr = charStr.toCharArray();
        StringBuilder hexstr = new StringBuilder();
        for (char ch : charr) {
            hexstr.append(Integer.toHexString((int) ch));
        }
        return hexstr.toString();
    }

    void BT_Adv_init()
    {
        data = (EditText) findViewById(R.id.advertise_data);
        interval = (EditText) findViewById(R.id.advertise_interval);
        time = (EditText) findViewById(R.id.advertise_time);
        time_already = (TextView) findViewById(R.id.advertised_time);

        myTV = (TextView) findViewById(R.id.tv);
        mySwitch = (Switch) findViewById(R.id.switchID);

        myManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myAdapter = myManager.getAdapter();
        myAdvertiser = myAdapter.getBluetoothLeAdvertiser();

        if(!myAdapter.isMultipleAdvertisementSupported()){
            mySwitch.setEnabled(false);
            myTV.setText("Device does not support BLE advertisement.");
            myTV.invalidate();
            return;
        }

        myAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(ADVERTISE_TX_POWER_HIGH)
                .build();

        //gattClientEnableAdvNative();
        //ADVERTISING_CHANNEL_ALL
        //CHANNEL

        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    if (data.getText().length() == 0 || (Integer.parseInt(data.getText().toString()) < 0 || Integer.parseInt(data.getText().toString()) > 255) ) {
                        Toast.makeText(MainActivity.this, "Please input advertise data: 0-255!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (interval.getText().length() == 0 || Integer.parseInt(interval.getText().toString()) <= 0) {
                        Toast.makeText(MainActivity.this, "Please input advertise interval > 0!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (time.getText().length() == 0 || Integer.parseInt(time.getText().toString()) <= 0) {
                        Toast.makeText(MainActivity.this, "Please input advertise time > 0!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //final byte[] ad_data = new byte[27];
                    //final byte[] ad_data = new byte[8];
                    final int ad_byte = Integer.parseInt(data.getText().toString());
                    //ad_data[0] = (byte)(ad_byte);
                    ad_data[0] = (byte)0X18;
                    ad_data[1] = (byte)0X87;
                    ad_data[2] = (byte)0X52;
                    ad_data[3] = (byte)0X35;
                    ad_data[4] = (byte)0XF8;
                    ad_data[5] = (byte)0X60;
                    ad_data[6] = (byte)0X25;
                    ad_data[7] = (byte)0Xf5;
                    final int ad_interval = Integer.parseInt(interval.getText().toString());
                    final int ad_time = Integer.parseInt(time.getText().toString());

                    String str = String.format("%6d", ad_byte);

                    myTV.setText("Service UUID: " + myUUIDstring + "\n"
                            + "Service Data: " + str
                            + " (0x"  + stringToHex(str) + ")");

//                    myAdvertiseData = new AdvertiseData.Builder()
//                                   .addManufacturerData(65520,ad_data)
//                                    .build();
//                    myAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, myAdvertiseCallback);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ParcelUuid puuid = new ParcelUuid(UUID.fromString(myUUIDstring));
                            //count 65520
                            myAdvertiseData = new AdvertiseData.Builder()
                                    .addManufacturerData(65520,ad_data)
                                    .build();

                            UIHandler.obtainMessage(0, 0, 0).sendToTarget();

                            int cnt = 0;
                            advFlag = true;
                            while (cnt < ad_time && advFlag)
                            {
                                try {
                                    myAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, myAdvertiseCallback);
                                    Thread.sleep(ad_interval);
                                    myAdvertiser.stopAdvertising(myAdvertiseCallback);
                                    cnt++;
                                    UIHandler.obtainMessage(0, cnt, 0).sendToTarget();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            UIHandler.obtainMessage(1).sendToTarget();
                        }
                    }).start();
                }
                else {
                    advFlag = false;
                    myAdvertiser.stopAdvertising(myAdvertiseCallback);
                }
            }
        });

    }

    void BT_AdvReceive_init()
    {
        myTVR = (TextView) findViewById(R.id.tvr);
        mySwitchr = (Switch) findViewById(R.id.switchIDR);

        myScanner = myAdapter.getBluetoothLeScanner();

        final ScanSettings myScanSettings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        /*final ScanFilter myScanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(this.myUUIDstring)))
                .build();
        final ScanFilter myScanFilter = new ScanFilter.Builder().build();
        final List<ScanFilter> myScanFilers = new ArrayList<ScanFilter>();
        myScanFilers.add(myScanFilter);*/

        mySwitchr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    //myScanner.startScan(myScanFilers, myScanSettings, myScanCallback);
                    myScanner.startScan(null, myScanSettings, myScanCallback);
                }
                else{
                    myScanner.stopScan(myScanCallback);
                }
            }
        });
    }

    public static String getMac(Context context) {

        String strMac = null;
/*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e("=====", "6.0以下");
            Toast.makeText(context, "6.0以下", Toast.LENGTH_SHORT).show();
            strMac = getLocalMacAddressFromWifiInfo(context);
            return strMac;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("=====", "6.0以上7.0以下");
            Toast.makeText(context, "6.0以上7.0以下", Toast.LENGTH_SHORT).show();
            strMac = getMacAddress(context);
            return strMac;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("=====", "7.0以上");
            if (!TextUtils.isEmpty(getMacAddress())) {
                Log.d("Jeff", "7.0以上1");
                Toast.makeText(context, "7.0以上1", Toast.LENGTH_SHORT).show();
               // strMac = getMacAddress();
                return strMac;
            } else if (!TextUtils.isEmpty(getMachineHardwareAddress())) {
                Log.d("Jeff", "7.0以上2");
                Toast.makeText(context, "7.0以上2", Toast.LENGTH_SHORT).show();
               // strMac = getMachineHardwareAddress();
                return strMac;
            } else {
                Log.d("Jeff", "7.0以上3");
                Toast.makeText(context, "7.0以上3", Toast.LENGTH_SHORT).show();
              //  strMac = getLocalMacAddressFromBusybox();
                return strMac;
            }
        }
*/

        return "02:00:00:00:00:00";
    }

    /**
     * 根据wifi信息获取本地mac
     * @param context
     * @return
     */
    public static String getLocalMacAddressFromWifiInfo(Context context){
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo winfo = wifi.getConnectionInfo();
        String mac =  winfo.getMacAddress();
        return mac;
    }
    /**
     * android 6.0及以上、7.0以下 获取mac地址
     *
     * @param context
     * @return
     */
    public static String getMacAddress(Context context) {

        // 如果是6.0以下，直接通过wifimanager获取
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            String macAddress0 = getMacAddress0(context);
            if (!TextUtils.isEmpty(macAddress0)) {
                return macAddress0;
            }
        }

        String str = "";
        String macSerial = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e("----->" + "NetInfoManager", "getMacAddress:" + ex.toString());
        }
        if (macSerial == null || "".equals(macSerial)) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                        .toUpperCase().substring(0, 17);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("----->" + "NetInfoManager",
                        "getMacAddress:" + e.toString());
            }

        }
        return macSerial;
    }

    private static String getMacAddress0(Context context) {
        if (isAccessWifiStateAuthorized(context)) {
            WifiManager wifiMgr = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = null;
            try {
                wifiInfo = wifiMgr.getConnectionInfo();
                return wifiInfo.getMacAddress();
            } catch (Exception e) {
                Log.e("----->" + "NetInfoManager",
                        "getMacAddress0:" + e.toString());
            }

        }
        return "";

    }

    /**
     * Check whether accessing wifi state is permitted
     *
     * @param context
     * @return
     */
    private static boolean isAccessWifiStateAuthorized(Context context) {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE")) {
            Log.e("----->" + "NetInfoManager", "isAccessWifiStateAuthorized:"
                    + "access wifi state is enabled");
            return true;
        } else
            return false;
    }

    private static String loadFileAsString(String fileName) throws Exception {
        FileReader reader = new FileReader(fileName);
        String text = loadReaderAsString(reader);
        reader.close();
        return text;
    }

    private static String loadReaderAsString(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int readLength = reader.read(buffer);
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength);
            readLength = reader.read(buffer);
        }
        return builder.toString();
    }
    //（1）通过ip地址来获取绑定的mac地址
   // （2）扫描各个网络接口获取mac地址
   // （3）通过busybox获取本地存储的mac地址
    /**
     * 根据IP地址获取MAC地址
     *
     * @return
     */
    public static String getMacAddress() {
        String strMacAddr = null;
        try {
            // 获得IpD地址
            InetAddress ip = getLocalInetAddress();
            byte[] b = NetworkInterface.getByInetAddress(ip)
                    .getHardwareAddress();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                if (i != 0) {
                    buffer.append(':');
                }
                String str = Integer.toHexString(b[i] & 0xFF);
                buffer.append(str.length() == 1 ? 0 + str : str);
            }
            strMacAddr = buffer.toString().toUpperCase();
        } catch (Exception e) {

        }

        return strMacAddr;
    }

    /**
     * 获取移动设备本地IP
     *
     * @return
     */
    private static InetAddress getLocalInetAddress() {
        InetAddress ip = null;
        try {
            // 列举
            Enumeration<NetworkInterface> en_netInterface = NetworkInterface
                    .getNetworkInterfaces();
            while (en_netInterface.hasMoreElements()) {// 是否还有元素
                NetworkInterface ni = (NetworkInterface) en_netInterface
                        .nextElement();// 得到下一个元素
                Enumeration<InetAddress> en_ip = ni.getInetAddresses();// 得到一个ip地址的列举
                while (en_ip.hasMoreElements()) {
                    ip = en_ip.nextElement();
                    if (!ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1)
                        break;
                    else
                        ip = null;
                }

                if (ip != null) {
                    break;
                }
            }
        } catch (SocketException e) {

            e.printStackTrace();
        }
        return ip;
    }

    /**
     * 获取本地IP
     *
     * @return
     */
    private static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

   /* android 7.0及以上 （2）扫描各个网络接口获取mac地址
    *
            */
    /**
     * 获取设备HardwareAddress地址
     *
     * @return
     */
    public static String getMachineHardwareAddress() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        String hardWareAddress = null;
        NetworkInterface iF = null;
        if (interfaces == null) {
            return null;
        }
        while (interfaces.hasMoreElements()) {
            iF = interfaces.nextElement();
            try {
                hardWareAddress = bytesToString(iF.getHardwareAddress());
                if (hardWareAddress != null)
                    break;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return hardWareAddress;
    }

    /***
     * byte转为String
     *
     * @param bytes
     * @return
     */
    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            buf.append(String.format("%02X:", b));
        }
        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }
        return buf.toString();
    }

    /**
     * android 7.0及以上 （3）通过busybox获取本地存储的mac地址
     *
     */

    /**
     * 根据busybox获取本地Mac
     *
     * @return
     */
    public static String getLocalMacAddressFromBusybox() {
        String result = "";
        String Mac = "";
        result = callCmd("busybox ifconfig", "HWaddr");
        // 如果返回的result == null，则说明网络不可取
        if (result == null) {
            return "网络异常";
        }
        // 对该行数据进行解析
        // 例如：eth0 Link encap:Ethernet HWaddr 00:16:E8:3E:DF:67
        if (result.length() > 0 && result.contains("HWaddr") == true) {
            Mac = result.substring(result.indexOf("HWaddr") + 6,
                    result.length() - 1);
            result = Mac;
        }
        return result;
    }

    private static String callCmd(String cmd, String filter) {
        String result = "";
        String line = "";
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            InputStreamReader is = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(is);

            while ((line = br.readLine()) != null
                    && line.contains(filter) == false) {
                result += line;
            }

            result = line;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}





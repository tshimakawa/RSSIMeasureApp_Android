package com.example.tsukasa.rssimeasureapp;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private TextView UUID;
    private TextView Major;
    private TextView Minor;
    private TextView RSSI;
    private Button startButton;
    private Button syncButton;
    private BeaconManager beaconManager;
    public static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String BEACON_UUID = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";//使用するBeaconのUUIDを格納
    private CLBeacon clBeacon;
    private boolean flag = false;
    private Timer mainTimer;                    //タイマー用
    private MainTimerTask mainTimerTask;        //タイマタスククラス

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UUID = (TextView) findViewById(R.id.UUID2);
        Major = (TextView) findViewById(R.id.Major2);
        Minor = (TextView) findViewById(R.id.Minor2);
        RSSI = (TextView) findViewById(R.id.RSSI2);
        startButton = (Button) findViewById(R.id.button);

        // staticメソッドで取得
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == false) {
                    try {
                        // 距離観測の開始
                        beaconManager.startRangingBeaconsInRegion(new Region("ISDL", Identifier.parse(BEACON_UUID), null, null));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            startButton.setText("計測終了");
                        }
                    });
                    flag = true;
                    //タイマー設定
                    mainTimer = new Timer();
                    mainTimerTask = new MainTimerTask();
                    mainTimer.schedule(mainTimerTask, 300000,300000);
                }else{
                    try {
                        // 距離観測の終了
                        beaconManager.stopRangingBeaconsInRegion(new Region("ISDL", Identifier.parse(BEACON_UUID), null, null));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            startButton.setText("計測開始");
                        }
                    });
                    flag = false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager.bind(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                clBeacon = new CLBeacon();
                clBeacon.RSSI = String.valueOf(0);

                // 検出したビーコンの情報を全部Logに書き出す
                for(Beacon beacon : beacons) {
                    clBeacon.UUID = String.valueOf(beacon.getId1());
                    clBeacon.Major = String.valueOf(beacon.getId2());
                    clBeacon.Minor = String.valueOf(beacon.getId3());
                    clBeacon.RSSI = String.valueOf(beacon.getRssi());
                    Log.d("MyActivity", "UUID:" + beacon.getId1() + ", major:" + beacon.getId2() + ", minor:" + beacon.getId3() + ", Distance:" + beacon.getDistance());
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            UUID.setText(clBeacon.UUID);
                            Major.setText(clBeacon.Major);
                            Minor.setText(clBeacon.Minor);
                            RSSI.setText(clBeacon.RSSI);
                        }
                    });
                }
                String tmpCsvPath = getExternalFilesDir(null) + "/RSSI.csv";
                try {
                    // csvファイル作成
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpCsvPath, true)));
                    bw.write(String.valueOf(clBeacon.RSSI));
                    bw.newLine();
                    bw.flush();
                    bw.close();
                } catch (IOException ex) {
                    //例外時処理
                    ex.printStackTrace();
                }
            }
        });
    }
    //タイマー機能　決めた定期時間毎にログに-----------を入れる
    public class MainTimerTask extends TimerTask {
        public void run() {
            //ここに定周期で実行したい処理を記述します
            String tmpCsvPath = getExternalFilesDir(null) + "/RSSI.csv";
            try {
                // csvファイル作成
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpCsvPath, true)));
                bw.write("------------");
                bw.newLine();
                bw.flush();
                bw.close();
            } catch (IOException ex) {
                //例外時処理
                ex.printStackTrace();
            }
        }
    }
}

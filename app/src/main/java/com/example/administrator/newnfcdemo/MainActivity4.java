package com.example.administrator.newnfcdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;

import utils.Constant;

public class MainActivity4 extends AppCompatActivity {
    NfcAdapter nfcAdapter;//NFC控制器
    TextView promt;//卡信息

    PendingIntent pendingIntent = null;//延迟的意图

    Intent nfcIntent = null;

    //巡更位置序号
    EditText et_checkPointNum;

    //写入成功后的滴声提示
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        setTitle("添加巡检点");
        //提示声音
        player = MediaPlayer.create(getApplicationContext(), R.raw.scan);
        promt = (TextView) findViewById(R.id.promt);

        et_checkPointNum = (EditText) findViewById(R.id.et_checkPointNum);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // 获取默认的NFC控制器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            promt.setText("设备不支持NFC！");
//            finish();
//            return;
        }else {
            if (!nfcAdapter.isEnabled()) {
                promt.setText("请在系统设置中先启用NFC功能！");
//            finish();
//            return;
            }
        }

        Button bt_numSub1 = (Button) findViewById(R.id.bt_numSub1);
        bt_numSub1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strNum = et_checkPointNum.getText().toString();
                if (strNum.equals("")) {
                    Toast.makeText(MainActivity4.this, "请输出巡检位置序号", Toast.LENGTH_SHORT).show();
                }
                int num = Integer.parseInt(strNum);
                if (num == 0) {
                    Toast.makeText(MainActivity4.this, "序号最小值为0", Toast.LENGTH_SHORT).show();
                } else {
                    et_checkPointNum.setText((--num) + "");
                }
            }
        });

        Button bt_numAdd1 = (Button) findViewById(R.id.bt_numAdd1);
        bt_numAdd1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strNum = et_checkPointNum.getText().toString();
                if (strNum.equals("")) {
                    Toast.makeText(MainActivity4.this, "请输出巡检位置序号", Toast.LENGTH_SHORT).show();
                }
                int num = Integer.parseInt(strNum);
                et_checkPointNum.setText((++num) + "");
            }
        });
    }

    //把信息写入nfc卡
    public void onWriteNFC(View view) {
        String strNum = et_checkPointNum.getText().toString();
        System.out.println("strNum为:" + strNum);
        if (strNum.equals("")) {
            Toast.makeText(this, "请输出巡检位置序号", Toast.LENGTH_SHORT).show();
        }
        int num = Integer.parseInt(strNum);
        System.out.println("num为:" + num);
        String writeValue = Constant.CHECK_STRING + strNum;
        System.out.println("writeValue为:" + writeValue);
        if (nfcIntent != null) {
            write(nfcIntent, writeValue);
        } else {
            Toast.makeText(this, "请靠近NFC卡", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //写入
        //write(intent);
        //读出
        nfcIntent = intent;
        read(intent);
    }


    //对字符串做一个描述
    //xungengdian_xx解析为巡检位置XX
    public String parseScanResult(String scanResult) {
        int flag = scanResult.indexOf("_");
        if (flag == -1) {
            Toast.makeText(this, "未知卡错误", Toast.LENGTH_SHORT).show();
            return "未知卡错误";
        }
        //"_"后的子串是否可以转化为数字
        String strNo = scanResult.substring(flag + 1);
        int no = 0;
        try {
            no = Integer.parseInt(strNo);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "未知卡错误", Toast.LENGTH_SHORT).show();
            return "未知卡错误";
        }
        if (scanResult.indexOf(Constant.CHECK_STRING) == 0) {
            return "js" + strNo;
        } else {
            return "未知卡错误";
        }
    }

    private void write(Intent intent, String writeValue) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

        //写入数据
        NdefRecord[] ndefRecord = new NdefRecord[4];
        //这里填写的包名是巡更app的包名
        ndefRecord[0] = NdefRecord.createApplicationRecord(Constant.CHECK_APP_PACKAGE);//packageName//打开某个APP
        System.out.println("位置0:" + new String(ndefRecord[0].getPayload()));
        ndefRecord[1] = NdefRecord.createUri("http://www.baidu.com");//打开网页
        System.out.println("位置1:" + new String(ndefRecord[1].getPayload()));
        ndefRecord[2] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[]{0}, writeValue.getBytes(Charset.forName("utf-8")));//写入纯文本
        System.out.println("位置2:" + new String(ndefRecord[2].getPayload()));
        ndefRecord[3] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[]{0}, "巡更点XX".getBytes(Charset.forName("utf-8")));//写入纯文本
        System.out.println("位置3:" + new String(ndefRecord[3].getPayload()));

        NdefMessage ndefMessage = new NdefMessage(ndefRecord);

        Ndef ndef = Ndef.get(tag);//当做是一种NDEF格式
        try {
            if (ndef != null) {
                ndef.connect();
                if (ndef.isWritable()) {
                    int size = ndefMessage.toByteArray().length;
                    if (ndef.getMaxSize() > size) {
                        ndef.writeNdefMessage(ndefMessage);
                    } else {
                        Toast.makeText(getApplicationContext(), "写入数据超过NFC标签容量" + size + " > " + ndef.getMaxSize(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                //若非NDEF格式的数据写入操作
                NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                if (ndefFormatable != null) {
                    ndefFormatable.connect();
                    ndefFormatable.format(ndefMessage);
                }
            }
            player.start();
            promt.setText(parseScanResult(writeValue));
            Toast.makeText(getApplicationContext(), "写入成功!", Toast.LENGTH_SHORT).show();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }finally {
            try {
                if(null!=ndef){
                    ndef.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(getApplicationContext(), "写入失败!", Toast.LENGTH_SHORT).show();

    }

    //字符序列转换为16进制字符串
    private String bytesToHexString(byte[] src) {
        //StringBuilder stringBuilder = new StringBuilder("0x");
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString().toUpperCase();
    }

    private void read(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                ) {
            //读取TAG  卡id
//            MifareClassic mfc = MifareClassic.get(tag);
//            byte[] idbyteArray = tag.getId();
//            String id = bytesToHexString(idbyteArray);
//
//            promt.append("\n卡的id为" + id);

            Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (data != null) {
                NdefMessage[] ndefMessages = new NdefMessage[data.length];
                for (int i = 0; i < ndefMessages.length; i++) {
                    ndefMessages[i] = (NdefMessage) data[i];
                    NdefRecord[] ndefRecords = ndefMessages[i].getRecords();

                    //获取标签里的文本
                    String scanResult = new String(ndefRecords[2].getPayload());
                    String flag = new String(ndefRecords[3].getPayload());
                    System.out.println("scanResult为:" + scanResult);
                    promt.setText(parseScanResult(scanResult));
                    //Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show();
                    //根据flag启动蓝牙
//                    if ("1".equals(flag)) {
//                        //根据写入的值做些什么事
//                    }

                    //根据包名启动app
//                    Intent startAPP = getPackageManager().getLaunchIntentForPackage(packageName);
//                    startActivity(startAPP);
                }
            } else {
                promt.setText("此NFC卡没有写入修改");
            }
            Toast.makeText(getApplicationContext(), "读取成功!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            //设置当前程序为优先处理nfc的程序
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }
}
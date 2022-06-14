package com.tjcg.nentopos.java;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tjcg.nentopos.R;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.utils.BitmapProcess;
import net.posprinter.utils.BitmapToByteData;
import net.posprinter.utils.DataForSendToPrinterPos80;
import net.posprinter.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class R80Activity extends AppCompatActivity implements View.OnClickListener {

    private Button sample,text,barcode,qrcode,bitmap;
    String o_id="0",cust_name="",order_date="",order_time="";
    public static IMyBinder myBinder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r80);



        Intent i = new Intent(getIntent());
        o_id=i.getStringExtra("o_id");
        cust_name=i.getStringExtra("cust_name");
        order_date=i.getStringExtra("order_date");
        order_date=i.getStringExtra("order_time");
        initview();
    }

    private void initview(){
        sample = findViewById(R.id.bt_rcp);
        text = findViewById(R.id.bt_80text);
        barcode = findViewById(R.id.bt_80barcode);
        qrcode = findViewById(R.id.bt_80qr);
        bitmap = findViewById(R.id.bt_80bitmap);

        sample.setOnClickListener(this);
        text.setOnClickListener(this);
        barcode.setOnClickListener(this);
        qrcode.setOnClickListener(this);
        bitmap.setOnClickListener(this);
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id==R.id.bt_rcp){
            printSample();
        }

        if(id== R.id.bt_80text){
            printText();
        }

        if (id== R.id.bt_80barcode){
            printBarcode();
        }

        if (id== R.id.bt_80qr){
            printqr();
        }

        if(id== R.id.bt_80bitmap){
            printBitmap();

        }
    }

    /**
     * 打印样张
     */
    private void printSample(){
        if (MainActivitySetting.ISCONNECT){
            MainActivitySetting.myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_success), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void OnFailed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_failed), Toast.LENGTH_SHORT).show();
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));//设置初始位置
                    list.add(DataForSendToPrinterPos80.selectCharacterSize(17));//字体放大一倍
                    list.add(StringUtils.strTobytes("商品"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("价格"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("5元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡呀"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("6元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("7元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("8元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("9元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(60,00));
                    list.add(StringUtils.strTobytes("黄焖鸡"));
                    list.add(DataForSendToPrinterPos80.setAbsolutePrintPosition(100,01));
                    list.add(StringUtils.strTobytes("10元"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());

                    return list;
                }
            });
        }else {
            Toast.makeText(getApplicationContext(),getString(R.string.connect_first), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打印文本
     */
    private void printText(){

        if (MainActivitySetting.ISCONNECT){
            MainActivitySetting.myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_success), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void OnFailed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_failed), Toast.LENGTH_SHORT).show();
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    byte[] b=DataForSendToPrinterPos80.initializePrinter();
                    list.add(b);
                    list.add(DataForSendToPrinterPos80.selectAlignment(1));
                    list.add(StringUtils.strTobytes("MENUTEST"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("8904852460"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("==============================="));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Cust name: "+cust_name));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Rajkot, Gujarat, India | 701669909"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes(order_date+" | "+ "12:34 AM"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Q  |  Item  |  Price   |  Total  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("1  |  Pepsi |  30      |  30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("1  |  Pepsi |  30      |  30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("==============================="));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Subtotal                        |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Discount                        |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("HST                             |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("==============================="));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Grand Total                     |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("==============================="));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Customer Paid Amount            |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Change due                      |  $ 30  "));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("==============================="));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("Order Type : - | Receipt No:0097 | Order No.:9"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    list.add(StringUtils.strTobytes("ST No. : 9876543210"));
                    //list.add(StringUtils.strTobytes("<b><u>Order ID</u></b> : "+o_id+"\n <b><u>Customer Name: </u></b> "+cust_name));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    return list;
                }
            });
        }else {
            Toast.makeText(getApplicationContext(),getString(R.string.connect_first), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     打印一维条码
     */
    private void printBarcode(){
        if (MainActivitySetting.ISCONNECT){
            MainActivitySetting.myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_success), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void OnFailed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_failed), Toast.LENGTH_SHORT).show();
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    //初始化打印机，清除缓存
                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    //选择对齐方式
                    list.add(DataForSendToPrinterPos80.selectAlignment(1));
                    //选择HRI文字文字
                    list.add(DataForSendToPrinterPos80.selectHRICharacterPrintPosition(02));
                    //设置条码宽度
                    list.add(DataForSendToPrinterPos80.setBarcodeWidth(2));
                    //设置高度
                    list.add(DataForSendToPrinterPos80.setBarcodeHeight(80));
                    //条码的类型和内容，73是code128的类型，请参考说明手册每种类型的规则
                    list.add(DataForSendToPrinterPos80.printBarcode(73,10,"{B12345678"));
                    //打印指令
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    return list;
                }
            });
        }else {
            Toast.makeText(getApplicationContext(),getString(R.string.connect_first), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 打印二维条码
     */
    private void printqr(){
        if (MainActivitySetting.ISCONNECT){
            MainActivitySetting.myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_success), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void OnFailed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_failed), Toast.LENGTH_SHORT).show();
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    //初始化打印机，清除缓存
                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    //选择对齐方式
                    list.add(DataForSendToPrinterPos80.selectAlignment(1));
                    list.add(DataForSendToPrinterPos80.printQRcode(3,48,"www.xprinter.net"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    return list;
                }
            });
        }else {
            Toast.makeText(getApplicationContext(),getString(R.string.connect_first), Toast.LENGTH_SHORT).show();
        }
    }

    private void printBitmap(){
//        final Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.test);
        final Bitmap bitmap1 =  BitmapProcess.compressBmpByYourWidth
                (BitmapFactory.decodeResource(getResources(), R.drawable.test),300);

        if (MainActivitySetting.ISCONNECT){
            MainActivitySetting.myBinder.WriteSendData(new TaskCallback() {
                @Override

                public void OnSucceed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_success), Toast.LENGTH_SHORT).show();

                }

                @Override
                public void OnFailed() {
                    Toast.makeText(getApplicationContext(),getString(R.string.con_failed), Toast.LENGTH_SHORT).show();
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    List<Bitmap> blist= new ArrayList<>();
                    blist = BitmapProcess.cutBitmap(150,bitmap1);
                    for (int i= 0 ;i<blist.size();i++){
                        list.add(DataForSendToPrinterPos80.printRasterBmp(0,blist.get(i), BitmapToByteData.BmpType.Dithering, BitmapToByteData.AlignType.Center,576));
                    }
//                    list.add(StringUtils.strTobytes("1234567890qwertyuiopakjbdscm nkjdv mcdskjb"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    return list;
                }
            });
        }else {
            Toast.makeText(getApplicationContext(),getString(R.string.connect_first), Toast.LENGTH_SHORT).show();
        }
    }
}
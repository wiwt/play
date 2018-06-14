package com.test.testh264player;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.test.testh264player.DJ.VoiceActivity;
import com.test.testh264player.bean.Frame;
import com.test.testh264player.decode.DecodeThread;
import com.test.testh264player.mediacodec.VIdeoMediaCodec;
import com.test.testh264player.interf.OnAcceptBuffListener;
import com.test.testh264player.interf.OnAcceptTcpStateChangeListener;
import com.test.testh264player.server.TcpServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button receive, select,mp4,dj,living;
    private ImageView imageView;
    private ServerSocket socket = new ServerSocket(10086);
    private Bitmap bitmap;
    private Socket socket1;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
//                    Bitmap bitmap=(Bitmap)msg.obj;
                    imageView.setImageBitmap(bitmap);

            }
        }
    };

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)        //请求权限
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);//1 can be another integer
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)        //请求权限
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);//1 can be another integer
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)        //请求权限
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);//1 can be another integer
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        living = findViewById(R.id.living);
        living.setOnClickListener(this);
        receive = findViewById(R.id.start);
        imageView = findViewById(R.id.show);
        select = findViewById(R.id.read);
        mp4 = findViewById(R.id.mp4);
        mp4.setOnClickListener(this);
        receive.setOnClickListener(this);
        select.setOnClickListener(this);
        dj = findViewById(R.id.dj);
        dj.setOnClickListener(this);
//        try {
//            socket = new ServerSocket(10086);
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.living:
                Intent livingintent = new Intent(MainActivity.this,LivingActivity.class);
                startActivity(livingintent);
                break;

            case R.id.start:   //连接服务器
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {

                            try {

                                socket1 = socket.accept();
                                DataInputStream dis = new DataInputStream(socket1.getInputStream());
                                File file = new File(getExternalCacheDir(), "tupian.yuv");
                                FileOutputStream fos = new FileOutputStream(file);
                                byte[] bytes = new byte[1024];
                                int length = 0;
                                while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                                    YuvImage image = new YuvImage(bytes, ImageFormat.NV21, 400,
                                            400, null);
                                    image.compressToJpeg(new Rect(0, 0,400, 400),
                                    80, fos);
                                    fos.write(bytes, 0, length);
                                }
                                BitmapFactory.Options option = new BitmapFactory.Options();
                                Bitmap bm = BitmapFactory.decodeFile(getExternalCacheDir() + "/tupian.jpg", option);//文件流
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
                                int scaleFactor1 = 100;
                                while (baos.toByteArray().length / 1024 > 200) { //循环判断如果压缩后图片是否大于size kb,大于继续压缩
                                    baos.reset();//重置baos即清空baos
                                    bm.compress(Bitmap.CompressFormat.JPEG, scaleFactor1, baos);//这里压缩options%，把压缩后的数据存放到baos中
                                    scaleFactor1 -= 10;
                                }
                                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
                                bitmap = BitmapFactory.decodeStream(bais, null, null);//把ByteArrayInputStream数据生成图片
                                handler.sendEmptyMessage(1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }}
                    }
                }).start();
                break;

            case R.id.read:
                BitmapFactory.Options option = new BitmapFactory.Options();
                Bitmap bm = BitmapFactory.decodeFile(getExternalCacheDir() + "/tupian.jpg", option);//文件流
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
                int scaleFactor1 = 100;
                while (baos.toByteArray().length / 1024 > 200) { //循环判断如果压缩后图片是否大于size kb,大于继续压缩
                    baos.reset();//重置baos即清空baos
                    bm.compress(Bitmap.CompressFormat.JPEG, scaleFactor1, baos);//这里压缩options%，把压缩后的数据存放到baos中
                    scaleFactor1 -= 10;
                }
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
                Bitmap bitmap1 = BitmapFactory.decodeStream(bais, null, null);//把ByteArrayInputStream数据生成图片
                imageView.setImageBitmap(bitmap1);
                break;

            case R.id.mp4:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            socket1 = socket.accept();
                            DataInputStream dis = new DataInputStream(socket1.getInputStream());
                            File file = new File(getExternalCacheDir(), "jieshou.mp4");
                            FileOutputStream fos = new FileOutputStream(file);
                            byte[] bytes = new byte[1024];
                            int length = 0;

                            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                                fos.write(bytes, 0, length);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;

            case R.id.dj:
                Intent intent = new Intent(MainActivity.this,VoiceActivity.class);
                startActivity(intent);
                break;

        }
    }
}

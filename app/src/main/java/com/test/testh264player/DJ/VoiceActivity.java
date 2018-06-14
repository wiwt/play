package com.test.testh264player.DJ;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

import com.test.testh264player.AppConfig;
import com.test.testh264player.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

//www.javaapk.com
@SuppressLint("ClickableViewAccessibility")
public class VoiceActivity extends Activity
{
    private Button speakButton;// 按住说话
    private TextView message;
    //定义两个线程
    private SendSoundsThread sendSoundsThread = new SendSoundsThread();
    private ReceiveSoundsThread receiveSoundsThread = new ReceiveSoundsThread();
    private boolean isFirst = true;

    // 设备信息：手机名+Android版本
    private String DevInfo = android.os.Build.MODEL + " Android " + android.os.Build.VERSION.RELEASE;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)        //请求权限
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);//1 can be another integer
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        message = (TextView) findViewById(R.id.Message);

        speakButton = (Button) findViewById(R.id.speakButton);
        speakButton.setOnTouchListener(new OnTouchListener()
        {
            @Override
            //改变按键上的文件，显示开始还是结束
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    message.setText("松开结束");

                    if (isFirst)
                    {
                        sendSoundsThread.start();
                        receiveSoundsThread.start();
                        isFirst = false;
                    }//开始发送线程
                    sendSoundsThread.setRunning(true);
                    //接收线程关闭
                    receiveSoundsThread.setRunning(false);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    message.setText("按住说话");
                    sendSoundsThread.setRunning(false);
                    receiveSoundsThread.setRunning(true);
                }
                return false;
            }
        });
    }
    //发送线程
    class SendSoundsThread extends Thread
    {
        private AudioRecord recorder = null;
        private boolean isRunning = false;
        private byte[] recordBytes = new byte[640];


        public SendSoundsThread()
        {
            super();

            // 录音机
            int recordBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, recordBufferSize);



        }
//sampleRateInHz      　  默认采样率，单位Hz。
//channelConfig        　　描述音频通道设置。
//audioFormat             　音频数据保证支持此格式。

        @Override

        public synchronized void run()
        {
            super.run();
            recorder.startRecording();
            while (true)
            {
                if (isRunning)
                {

                    try
                    {
                        DatagramSocket clientSocket = new DatagramSocket();
                        InetAddress IP = InetAddress.getByName(AppConfig.IPAddress);// 向这个网络广播

                        // 获取音频数据
                        recorder.read(recordBytes, 0, recordBytes.length);

                        // 构建数据包 头+体f
                        dataPacket dataPacket = new dataPacket(DevInfo.getBytes(), recordBytes);
                        // 构建数据
                        DatagramPacket sendPacket = new DatagramPacket(dataPacket.getAllData(),
                                dataPacket.getAllData().length, IP, AppConfig.Port);

                        // 发送
                        clientSocket.send(sendPacket);
                        clientSocket.close();
                    }
                    catch (SocketException e)
                    {
                        e.printStackTrace();
                    }
                    catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void setRunning(boolean isRunning)
        {
            this.isRunning = isRunning;
        }
    }

    class ReceiveSoundsThread extends Thread
    {
        private AudioTrack player = null;
        private boolean isRunning = false;
        private byte[] recordBytes = new byte[670];

        public ReceiveSoundsThread()
        {
            // 播放器
            int playerBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            player = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, playerBufferSize, AudioTrack.MODE_STREAM);
        }

        @Override
        public synchronized void run()
        {
            super.run();

            try
            {
                @SuppressWarnings("resource")
                DatagramSocket serverSocket = new DatagramSocket(AppConfig.Port);
                while (true)
                {
                    if (isRunning)
                    {
                        DatagramPacket receivePacket = new DatagramPacket(recordBytes, recordBytes.length);
                        serverSocket.receive(receivePacket);

                        byte[] data = receivePacket.getData();

                        byte[] head = new byte[30];
                        byte[] body = new byte[640];

                        // 获得包头
                        for (int i = 0; i < head.length; i++)
                        {
                            head[i] = data[i];
                        }

                        // 获得包体
                        for (int i = 0; i < body.length; i++)
                        {
                            body[i] = data[i + 30];
                        }

                        // 获得头信息 通过头信息判断是否是自己发出的语音
                        String thisDevInfo = new String(head).trim();
                        System.out.println(thisDevInfo);

                        if (!thisDevInfo.equals(DevInfo))
                        {
                            player.write(body, 0, body.length);
                            player.play();
                        }
                    }
                }
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void setRunning(boolean isRunning)
        {
            this.isRunning = isRunning;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}


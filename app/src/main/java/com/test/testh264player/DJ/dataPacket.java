package com.test.testh264player.DJ;

/**
 * Created by Administrator on 2017/9/23.
 */

public class dataPacket {
    public static int head = 30;// 头信息大小
    public static int body = 640;// 语音包大小
    private byte[] recordBytes = new byte[head + body];

    public dataPacket(byte[] headInfo, byte[] bodyBytes)
    {
        for (int i = 0; i < headInfo.length; i++)
        {
            recordBytes[i] = headInfo[i];
        }
        for (int i = 0; i < bodyBytes.length; i++)
        {
            recordBytes[i + 30] = bodyBytes[i];
        }
    }

    public byte[] getHeadInfo()
    {
        byte[] head = new byte[30];
        for (int i = 0; i < head.length; i++)
        {
            head[i] = recordBytes[i];
        }
        return head;
    }

    public byte[] getBody()
    {
        byte[] body = new byte[640];
        for (int i = 0; i < body.length; i++)
        {
            body[i] = recordBytes[i + 30];
        }
        return body;
    }

    public byte[] getAllData()
    {
        byte[] data = new byte[head + body];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = recordBytes[i];
        }
        return data;
    }
}

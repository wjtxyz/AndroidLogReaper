package com.wjtxyz;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.EventLog;

import com.wjtxyz.utils.ProviderLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class LogClientService extends Service {
    public LogClientService() {
    }

    /***
     * because Socket writer can't execute in MainThread,
     */
    final Handler mBackgroundHandler = new Handler(new HandlerThread("background") {{
        start();
    }}.getLooper());

    @Override
    public void onCreate() {
        super.onCreate();

        mBackgroundHandler.post(mSocketLogWriterProc);
        mBackgroundHandler.post(mLogProviderLogWriterProc);
        mBackgroundHandler.post(mEventLogLogWriterProc);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBackgroundHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    final Runnable mSocketLogWriterProc = new Runnable() {
        SocketChannel client;

        @Override
        public void run() {
            try {
                // !!!this MUST NOT execute in MainThread/UIThread
                if (client == null) {
                    client = SocketChannel.open(new InetSocketAddress("localhost", 8099));
                }
                client.write(ByteBuffer.wrap("SocketLog writer test\n".getBytes(StandardCharsets.UTF_8)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mBackgroundHandler.postDelayed(this, 1000);
        }
    };

    final Runnable mLogProviderLogWriterProc = new Runnable() {
        @Override
        public void run() {
            //this can execute in any thread
            ProviderLog.println("LogProviderLog writer test");
            mBackgroundHandler.postDelayed(this, 1000);
        }
    };

    final Runnable mEventLogLogWriterProc = new Runnable() {
        @Override
        public void run() {
            //this can execute in any thread
            EventLog.writeEvent(666666, "EventLogLog writer test");
            mBackgroundHandler.postDelayed(this, 1000);
        }
    };
}

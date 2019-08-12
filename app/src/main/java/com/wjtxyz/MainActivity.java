package com.wjtxyz;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.EventLog;
import android.util.Log;

import com.wjtxyz.utils.EventReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(mLogServerSocketProc, "SocketLog").start();
        new Thread(mEventLogProc, "EventLogLog").start();
        bindService(new Intent(this, LogClientService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    final Runnable mLogServerSocketProc = new Runnable() {
        @Override
        public void run() {
            try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                serverSocketChannel.bind(new InetSocketAddress("localhost", 8099));
                serverSocketChannel.configureBlocking(false);
                int ops = serverSocketChannel.validOps();
                serverSocketChannel.register(selector, ops, null);
                final ByteBuffer byteBuffer = ByteBuffer.allocate(8 * 1024);
                for (; ; ) {
                    int noOfKeys = selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> itr = selectedKeys.iterator();
                    while (itr.hasNext()) {
                        final SelectionKey ky = itr.next();
                        if (ky.isAcceptable()) {
                            // The new client connection is accepted
                            SocketChannel client = serverSocketChannel.accept();
                            client.configureBlocking(false);
                            // The new connection is added to a selector
                            client.register(selector, SelectionKey.OP_READ);
                        } else if (ky.isReadable()) {
                            // Data is read from the client
                            final int readLen = ((SocketChannel) ky.channel()).read(byteBuffer);
                            byteBuffer.position(0);
                            String readLineData;

                            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteBufferBackedInputStream((ByteBuffer) byteBuffer.duplicate().limit(readLen)), StandardCharsets.UTF_8));
                            while ((readLineData = reader.readLine()) != null) {
                                Log.d(TAG, "SocketLog received: readLineData=" + readLineData);
                            }
                        }
                        itr.remove();
                    } // end of while loop
                } // end of for loop

            } catch (IOException e) {
                Log.e(TAG, "fail to run ServerSocketLog", e);
            }
        }
    };

    final Runnable mEventLogProc = new Runnable() {
        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("/system/bin/logcat -B -b events [666666]:v *:s");
                try (BufferedInputStream inputStream = new BufferedInputStream(process.getInputStream(), 64 * 1024)) {
                    while (true) {
                        EventLog.Event event = EventReader.readEvent(inputStream);
                        if(event.getTag() == 666666){
                            Log.d(TAG, "EventLogLog received: pid=" + event.getProcessId() + " threadId=" + event.getThreadId() + " tag=" + event.getTag() + " object=" + event.getData());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "fail to run EventLogLog", e);
            }
        }
    };

    public static class ByteBufferBackedInputStream extends InputStream {
        final ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public synchronized int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        @Override
        public int available() throws IOException {
            return buf.remaining();
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }


    public static class LogProvider extends ContentProvider {

        static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        static final String AUTHORITY = "com.wjtxyz.logprovider";
        static final int MATCH_LOG = 1;

        static {
            sUriMatcher.addURI(AUTHORITY, "log", MATCH_LOG);
        }

        public LogProvider() {
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            // Implement this to handle requests to delete one or more rows.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public String getType(Uri uri) {
            // TODO: Implement this to handle requests for the MIME type of the data
            // at the given URI.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            // TODO: Implement this to handle requests to insert a new row.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public boolean onCreate() {
            // TODO: Implement this to initialize your content provider on startup.
            return false;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {
            // TODO: Implement this to handle query requests from clients.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        final ParcelFileDescriptor[] logPFD = new ParcelFileDescriptor[2];

        @Override
        public int update(Uri uri, ContentValues values, String selection,
                          String[] selectionArgs) {
            // TODO: Implement this to handle requests to update one or more rows.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Nullable
        @Override
        public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
            Log.d(TAG, "openFile:: uri=" + uri + " mode=" + mode);
            try {
                switch (sUriMatcher.match(uri)) {
                    case MATCH_LOG:
                        ensureOpen();
                        return logPFD[1].dup();
                    default:
                        throw new FileNotFoundException("not found");
                }
            } catch (final IOException e) {
                throw new FileNotFoundException("fail to open or dup FileDescriptor");
            }
        }

        void ensureOpen() throws IOException {
            if (logPFD[0] == null) {
                System.arraycopy(ParcelFileDescriptor.createPipe(), 0, logPFD, 0, 2);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(logPFD[0])));
                Looper.getMainLooper().getQueue().addOnFileDescriptorEventListener(logPFD[0].getFileDescriptor(), MessageQueue.OnFileDescriptorEventListener.EVENT_INPUT, new MessageQueue.OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(@NonNull FileDescriptor fd, int events) {
                        if ((events & EVENT_ERROR) == EVENT_ERROR) {
                            return 0;
                        } else {
                            try {
                                Log.d(TAG, "LogProviderLog received:: readLine=" + reader.readLine());
                            } catch (IOException e) {
                                Log.w(TAG, "onFileDescriptorEvents:: fail to readLine", e);
                            }
                            return EVENT_INPUT;
                        }
                    }
                });
            }
        }
    }

}

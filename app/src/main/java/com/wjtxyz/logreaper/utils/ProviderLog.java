package com.wjtxyz.logreaper.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class ProviderLog {

    /***
     * use OutputStream instead of OutputStreamWriter because OutputStreamWriter not thread-safe
     */
    private static OutputStream outputStream;
    private static final Uri CONTENT_URI = Uri.parse("content://com.wjtxyz.logprovider/log");

    private static void ensureOpen(boolean forceReopen) throws IOException, RemoteException {
        if (!forceReopen && outputStream != null) {
            return;
        }
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
        try {
            @SuppressLint("PrivateApi") final Application application = (Application) Class.forName("android.app.ActivityThread").getDeclaredMethod("currentApplication").invoke(null);

            //use Unstable ContentProvider client instead of Stable ContentProvider client, because we don't want system close me when ContentProvider server exit
            final ContentProviderClient unstableContentProviderClient = application.getContentResolver().acquireUnstableContentProviderClient(CONTENT_URI);
            outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(new ParcelFileDescriptor(unstableContentProviderClient.openFile(CONTENT_URI, "w")) {
                @Override
                public void close() throws IOException {
                    super.close();
                    unstableContentProviderClient.close();
                }
            });
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void println(String s) {
        try {
            ensureOpen(false);
            outputStream.write((s + System.lineSeparator()).getBytes());
        } catch (IOException | RemoteException e) {
            //retry
            e.printStackTrace();
            try {
                ensureOpen(true);
                outputStream.write((s + System.lineSeparator()).getBytes());
            } catch (IOException | RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }
}

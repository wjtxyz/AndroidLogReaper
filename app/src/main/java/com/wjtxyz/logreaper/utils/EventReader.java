package com.wjtxyz.logreaper.utils;

import android.util.EventLog;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class EventReader {
    static final ByteBuffer mLengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
    static Method mEventLog_Event_fromBytes;

    static {
        try {
            mEventLog_Event_fromBytes = EventLog.Event.class.getDeclaredMethod("fromBytes", byte[].class);
        } catch (NoSuchMethodException e) {
            mEventLog_Event_fromBytes = null;
        }
    }


    /***
     * refer to <a href="https://android.googlesource.com/platform/system/core/+/e5976bea7a54f3b0c3f9840b732af12fb7d1d224/include/log/logger.h"/>
     * for detail
     * @param inputStream
     * @return
     * @throws IOException
     */

    public static EventLog.Event readEvent(InputStream inputStream) throws IOException {
        inputStream.read(mLengthBuffer.array()); //read the payload & header-size
        final short payload = mLengthBuffer.getShort(0);
        final short headerSize = mLengthBuffer.getShort(2);
        final int bufferLen = (headerSize > 0) ? (headerSize + payload) : (20 + payload);
        final byte[] ret = Arrays.copyOf(mLengthBuffer.array(), bufferLen);
        inputStream.read(ret, mLengthBuffer.capacity(), ret.length - mLengthBuffer.capacity());
        try {
            return (EventLog.Event) mEventLog_Event_fromBytes.invoke(null, ret);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("fail to create event");
        }
    }
}

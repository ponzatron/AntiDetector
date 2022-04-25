package com.z.zz.zzz.antidetector;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.kmdc.mdu.KMDC;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("main");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        CoreOaid.readOaid(this);
//        OAIDHelper.fetchOAID(this, params -> {
//            Log.i("main", "params: " + params);
//        });

        KMDC.doFetch(this);

//        Log.d("main", "TEST1: " + Build.HARDWARE);
//        Log.d("main", "TEST2: " + doTest2());
//        Log.d("main", "TEST3: " + doTest3());
//        Log.d("main", "TEST4: " + doTest4());
//
//        AntiDetector.create(this)
//                .setDebug(BuildConfig.DEBUG)
//                .setSticky(true)
//                .setMinEmuFlagsThresholds(3)
//                .detect((result, data) ->
//                        Log.i("Main", "AntiDetector result: " + result + ", data: " + data)
//                );

        // Create our surface view and set it as the content of our
        // Activity
//        mGLSurfaceView = new GLSurfaceView(this);
//        mGLSurfaceView.setRenderer(new GpuInfoUtil());
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//        addContentView(mGLSurfaceView, params);
    }

    private String doTest2() {
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class);
            Object object = new Object();
            Object obj = getMethod.invoke(object, "ro.hardware");
            return (obj == null ? "" : (String) obj);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String doTest3() {
        return exec("getprop ro.hardware").result;
    }

    private String doTest4() {
        return getSystemProperties("ro.hardware");
    }

    private CmdResult exec(String cmd) {
        CmdResult cmdResult = new CmdResult();
        try {
            Process zsh = Runtime.getRuntime().exec("sh");
            DataOutputStream outputStream = new DataOutputStream(zsh.getOutputStream());
            outputStream.writeBytes(cmd + "\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            outputStream.close();
            zsh.waitFor();
            InputStream is = zsh.getInputStream();
            cmdResult.result = inputToString(is, "utf8").trim();
            cmdResult.code = zsh.exitValue();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cmdResult;
    }

    private String inputToString(InputStream is, String charset2) throws IOException {
        byte[] bytesResult = readFully(is, Integer.MAX_VALUE, false);
        if (charset2 == null) {
            return new String(bytesResult);
        }
        return new String(bytesResult, charset2);
    }

    private byte[] readFully(InputStream is, int length, boolean readAll) throws IOException {
        byte[] output = {};
        if (length == -1) length = Integer.MAX_VALUE;
        int pos = 0;
        while (pos < length) {
            int bytesToRead;
            if (pos >= output.length) { // Only expand when there's no room
                bytesToRead = Math.min(length - pos, output.length + 1024);
                if (output.length < pos + bytesToRead) {
                    output = Arrays.copyOf(output, pos + bytesToRead);
                }
            } else {
                bytesToRead = output.length - pos;
            }
            int cc = is.read(output, pos, bytesToRead);
            if (cc < 0) {
                if (readAll && length != Integer.MAX_VALUE) {
                    throw new EOFException("Detect premature EOF");
                } else {
                    if (output.length != pos) {
                        output = Arrays.copyOf(output, pos);
                    }
                    break;
                }
            }
            pos += cc;
        }
        return output;
    }

    static class CmdResult {
        public int code;
        public String result;

        public String toString() {
            return "CmdResult{code=" + code + ", result='" + result + '\'' + '}';
        }
    }

    public static native String getSystemProperties(String key);
}

package com.z.zz.zzz;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.z.zz.zzz.emu.EmulatorDetector;
import com.z.zz.zzz.utils.L;
import com.z.zz.zzz.utils.U;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by zizzy on 08/21/18.
 */

public final class AntiDetector {
    public static final String TAG = "AntiDetector";
    private static final String MANUFACTURER_GOOGLE = "Google";
    private static final String BRAND_GOOGLE = "google";
    private static final int FLAG_ANTI_DETECT = 0x1;
    private static final int FLAG_IS_GOOGLE_DEVICE = FLAG_ANTI_DETECT;          // 0
    private static final int FLAG_ENABLE_ADB = FLAG_IS_GOOGLE_DEVICE << 1;      // 1
    private static final int FLAG_IS_DEBUGGABLE = FLAG_ENABLE_ADB << 1;         // 2
    private static final int FLAG_IS_DEBUGGED = FLAG_IS_DEBUGGABLE << 1;        // 3
    private static final int FLAG_IS_ROOTED = FLAG_IS_DEBUGGED << 1;            // 4
    private static final int FLAG_IS_EMULATOR = FLAG_IS_ROOTED << 1;            // 5
    private static final int FLAG_IS_VPN_CONNECTED = FLAG_IS_EMULATOR << 1;     // 6
    private static final int FLAG_IS_WIFI_PROXY = FLAG_IS_VPN_CONNECTED << 1;   // 7
    private static final int FLAG_IS_AOSP = FLAG_IS_WIFI_PROXY << 1;            // 8
    private static long FLAG_SAFE = 0x0;
    private static AntiDetector sAntiDetector;
    public Map<String, String> mData;
    private Context context;
    private boolean isDebug;
    private boolean isSticky;

    private AntiDetector(Context pContext) {
        this.context = pContext;
    }

    public static AntiDetector create(Context pContext) {
        if (pContext == null) {
            throw new IllegalArgumentException("Context must not be null.");
        }
        if (sAntiDetector == null) {
            synchronized (AntiDetector.class) {
                if (sAntiDetector == null) {
                    sAntiDetector = new AntiDetector(pContext.getApplicationContext());
                }
            }
        }
        return sAntiDetector;
    }

    public static AntiDetector getDefault() {
        return sAntiDetector;
    }

    public static boolean getAntiResult() {
        if (sAntiDetector == null) {
            throw new NullPointerException("The instance of AntiDetector is null");
        }
        return sAntiDetector.checkAntiDetect();
    }

    private boolean checkSuFile() {
        Process process = null;
        try {
            String line = null;
            //   /system/xbin/which 或者  /system/bin/which
            process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if ((line = in.readLine()) != null) {
                L.v(TAG, "checkSuFile(): " + line);
                return true;
            }
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "checkSuFile error: ", e);
            } else {
                L.w(TAG, "checkSuFile error: " + e);
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }

    private boolean checkRootFile() {
        File file = null;
        String[] paths = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su",
                "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/local/su"};
        for (String path : paths) {
            file = new File(path);
            if (file.exists()) {
                L.v(TAG, "checkRootFile(): " + path);
                return true;
            }
        }
        return false;
    }

    private boolean checkBusybox() {
        try {
            String[] strCmd = new String[]{"busybox", "df"};
            List<String> execResult = U.executeCommand(strCmd);
            if (execResult != null) {
                L.v(TAG, "checkBusybox(): execResult=" + execResult);
                return true;
            }
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "checkBusybox error: ", e);
            } else {
                L.w(TAG, "checkBusybox error: " + e);
            }
        }
        return false;
    }

    private boolean checkAccessRootData() {
        try {
            String fileContent = "test_ok";
            Boolean writeFlag = writeFile("/data/su_test", fileContent);
            if (writeFlag) {
                L.v(TAG, "write ok");
            } else {
                L.v(TAG, "write failed");
            }

            String strRead = readFile("/data/su_test");
            if (fileContent.equals(strRead)) {
                L.v(TAG, "checkAccessRootData(): strRead=" + strRead);
                return true;
            }
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "checkAccessRootData error: ", e);
            } else {
                L.w(TAG, "checkAccessRootData error: " + e);
            }
        }
        return false;
    }

    private Boolean writeFile(String fileName, String message) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            byte[] bytes = message.getBytes();
            fos.write(bytes);
            fos.flush();
            return true;
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "writeFile error: ", e);
            } else {
                L.w(TAG, "writeFile error: " + e);
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    private String readFile(String fileName) {
        File file = new File(fileName);
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            bos = new ByteArrayOutputStream();
            int len;
            while ((len = fis.read(bytes)) > 0) {
                bos.write(bytes, 0, len);
            }
            bos.flush();
            String result = new String(bos.toByteArray());
            L.v(TAG, "readFile(): " + result);
            return result;
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "readFile error: ", e);
            } else {
                L.w(TAG, "readFile error: " + e);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }

            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    private String getUniquePsuedoID() {
        String serial;

        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            serial = new AndroidID(context).getAndroidID();
        } else {
            try {
                serial = U.getBuildSerial(context);
                //API>=9 使用serial号
                return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
            } catch (Exception e) {
                serial = new AndroidID(context).getAndroidID();
            }
        }
        //使用硬件信息拼凑出来的15位号码
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    private boolean isRooted() {
        boolean result = false;

        if (checkSuFile()) {
            result = true;
        }

        if ((!result || isDebug) && checkRootFile()) {
            result = true;
        }

        if ((!result || isDebug) && checkBusybox()) {
            result = true;
        }

        if ((!result || isDebug) && checkAccessRootData()) {
            result = true;
        }

        if (!result || isDebug) {
            Process process = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("exit\n");
                os.flush();
                int exitValue = process.waitFor();
                if (exitValue == 0) {
                    result = true;
                }
            } catch (Exception e) {
                if (isDebug) {
                    L.e(TAG, "isRooted error: ", e);
                } else {
                    L.w(TAG, "isRooted error: " + e);
                }
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }

                if (process != null) {
                    process.destroy();
                }
            }
        }

        L.d(TAG, ">>> isRooted: " + result);
        if (result) {
            FLAG_SAFE |= FLAG_IS_ROOTED;
        }

        return result;
    }

    public AntiDetector setDebug(boolean isDebug) {
        this.isDebug = isDebug;
        return this;
    }

    public AntiDetector setSticky(boolean sticky) {
        this.isSticky = sticky;
        return this;
    }

    public AntiDetector setMinEmuFlagsThresholds(int thresholds) {
        if (thresholds < 3) {
            throw new IllegalArgumentException("The emu flags thresholds must be >= 3");
        }
        EmulatorDetector.MIN_EMU_FLAGS_THRESHOLD = thresholds;
        return this;
    }

    public void detect(final OnDetectorListener listener) {
        mData = new HashMap<>();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return checkAntiDetect();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (listener != null) {
                    mData.put("anti_flag", Long.toBinaryString(FLAG_SAFE));
                    listener.onResult(result, mData);
                }
            }
        }.execute();
    }

    /**
     * Check device status to anti detection.
     *
     * @return true in dangerous; false safe
     */
    private boolean checkAntiDetect() {
        synchronized (AntiDetector.class) {
            FLAG_SAFE = 0x0;

            if (isSticky) {
                boolean inDevelopmentMode = inDevelopmentMode();
                boolean isRooted = isRooted();
                boolean isEmulator = isEmulator();
                boolean isVPNConnected = isVPNConnected();
                boolean isWifiProxy = isWifiProxy();
                boolean isGoogleDevice = isGoogleDevice();
                boolean isAosp = isAosp();

                return inDevelopmentMode
                        || isRooted
                        || isVPNConnected
                        || isWifiProxy
                        || isEmulator
                        || isGoogleDevice
                        || isAosp;
            } else {
                return inDevelopmentMode()
                        || isRooted()
                        || isVPNConnected()
                        || isWifiProxy()
                        || isEmulator()
                        || isGoogleDevice()
                        || isAosp();
            }
        }
    }

    private boolean isDebugged() {
        L.d(TAG, ">>> Debugger hasTracerPid: " + Debugger.hasTracerPid());
        L.d(TAG, ">>> Debugger isBeingDebugged: " + Debugger.isBeingDebugged());
        L.d(TAG, ">>> Debugger hasAdbInEmulator: " + Debugger.hasAdbInEmulator());
        boolean result = Debugger.hasTracerPid() || Debugger.isBeingDebugged() || Debugger.hasAdbInEmulator();
        if (result) {
            FLAG_SAFE |= FLAG_IS_DEBUGGED;
        }
        return result;
    }

    private boolean inDevelopmentMode() {
        return enableAdb() || isDebuggable() || isDebugged();
    }

    private boolean enableAdb() {
        boolean result = (Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ADB_ENABLED, 0) > 0);
        L.d(TAG, ">>> enableAdb: " + result);
        if (result) {
            FLAG_SAFE |= FLAG_ENABLE_ADB;
        }
        return result;
    }

    private boolean isDebuggable() {
        boolean result = 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
        L.d(TAG, ">>> isDebuggable: " + result);
        if (result) {
            FLAG_SAFE |= FLAG_IS_DEBUGGABLE;
        }
        return result;
    }

    private boolean isGoogleDevice() {
        boolean result = false;
        try {
            result = Build.MANUFACTURER.toLowerCase().contains(MANUFACTURER_GOOGLE.toLowerCase())
                    || BRAND_GOOGLE.toLowerCase().contains(Build.BRAND.toLowerCase())
                    || Build.FINGERPRINT.toLowerCase().contains(BRAND_GOOGLE);
            L.d(TAG, ">>> isGoogleDevice: " + result);
            if (result) {
                FLAG_SAFE |= FLAG_IS_GOOGLE_DEVICE;
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private boolean isAosp() {
        boolean result = false;
        try {
            result = Build.MANUFACTURER.toLowerCase().contains(MANUFACTURER_GOOGLE.toLowerCase())
                    || Build.PRODUCT.toLowerCase().contains("aosp")
                    || Build.MODEL.toLowerCase().contains("aosp")
                    || Build.FINGERPRINT.toLowerCase().contains("aosp");
            L.d(TAG, ">>> isAosp: " + result);
            if (result) {
                FLAG_SAFE |= FLAG_IS_AOSP;
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private boolean isEmulator() {
        long start = System.currentTimeMillis();
        boolean isEmulator = EmulatorDetector.with(context).setDebug(isDebug).detect();
        L.d(TAG, ">>> Check emulator cost " + (System.currentTimeMillis() - start) + "ms");
        L.v(TAG, ">>> Emulator dump: " + EmulatorDetector.dump());
        if (isEmulator) {
            FLAG_SAFE |= FLAG_IS_EMULATOR;
        }
        return isEmulator;
    }

    /**
     * 是否使用代理(WiFi状态下的,避免被抓包)
     */
    private boolean isWifiProxy() {
        final boolean is_ics_or_later = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        String proxyAddress = "";
        int proxyPort = -1;

        try {
            if (is_ics_or_later) {
                proxyAddress = System.getProperty("http.proxyHost");
                String portstr = System.getProperty("http.proxyPort");
                proxyPort = Integer.parseInt((portstr != null ? portstr : "-1"));
            } else {
                proxyAddress = android.net.Proxy.getHost(context);
                proxyPort = android.net.Proxy.getPort(context);
            }
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "isWifiProxy error: ", e);
            } else {
                L.w(TAG, "isWifiProxy error: " + e);
            }
        }

        boolean result = !TextUtils.isEmpty(proxyAddress) && (proxyPort != -1);
        if (result) {
            FLAG_SAFE |= FLAG_IS_WIFI_PROXY;
        }
        return result;
    }

    /**
     * 是否正在使用VPN
     */
    private boolean isVPNConnected() {
        boolean result;

        List<String> networkList = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isUp()) {
                    networkList.add(ni.getName());
                }
            }
        } catch (Exception e) {
            if (isDebug) {
                L.e(TAG, "isVPNConnected error: ", e);
            } else {
                L.w(TAG, "isVPNConnected error: " + e);
            }
        }

        result = networkList.contains("tun0") || networkList.contains("ppp0");

//        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        Network[] networks = cm.getAllNetworks();
//
//        for (int i = 0; i < networks.length; i++) {
//            NetworkCapabilities caps = cm.getNetworkCapabilities(networks[i]);
//            L.i(TAG, "Network " + i + ": " + networks[i].toString());
//            L.i(TAG, "VPN transport is: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
//            L.i(TAG, "NOT_VPN capability is: " + caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
//        }

        if (result) {
            FLAG_SAFE |= FLAG_IS_VPN_CONNECTED;
        }
        return result;
    }

    public interface OnDetectorListener {
        void onResult(boolean result, Map<String, String> data);
    }
}

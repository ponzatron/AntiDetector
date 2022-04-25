package com.kmdc.mdu.http;

import static com.kmdc.mdu.utils.Utils.formatString;

import android.text.TextUtils;

import com.kmdc.mdu.utils.Utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import zizzy.zhao.bridgex.l.L;

public class UtilNetworking {

    private static JSONObject readHttpResponse(HttpURLConnection conn) throws Exception {
        StringBuffer sb = new StringBuffer();
        int responseCode;

        try {
//            conn.connect();
            L.d("The response header fields is: "
                    + Utils.getExtendedString(conn.getHeaderFields()));

            responseCode = conn.getResponseCode();

            InputStream is;
            if (responseCode >= 400) {
                is = conn.getErrorStream();
            } else {
                is = conn.getInputStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            br.close();
        } catch (Throwable th) {
            L.e(formatString("Failed to read response. (%s)", th.getMessage()));
            throw th;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        String stringResponse = sb.toString();
        L.logF("<=== [%s] - %s", responseCode, TextUtils.isEmpty(stringResponse) ? "(no content)"
                : "(...)");
        L.d(stringResponse);
        return new JSONObject(stringResponse);
    }

    public static JSONObject doPost(String content) throws Exception {
        return doPost(content.getBytes("UTF-8"));
    }

    public static JSONObject doPost(byte[] data) throws Exception {
        String adurl = "https://zlod.keve19.com/upload/accdd.jsp";
//        String json = "eyJjbmwiOiIxMDAwXzEyMDFfMjM5MDAxMDAiLCJ2diI6MTAwMDAsIm50IjoxLCJpcCI6IjEuODQuMjUzLjIzNiIsIm9waWQiOiI0NjAwMyIsInVzZXJhZ2VudCI6InVzZXJhZ2VudCIsImRldmljZXR5cGUiOjEsInNpZCI6MSwib3N2IjoiMTAiLCJoZHR5cGUiOjEsImltZWkiOiI4NTM1MTIwMjIxMDEwMDEiLCJhaWQiOjEsImZsb3dpZCI6IjEiLCJvc2FwaWxldmVsIjoiMjgiLCJvYWlkIjoiIn0";
//        String json = "11111111111111111";

        // 创建连接
        URL url = new URL(adurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
//		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("charset", "ISO-8859-1");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.setRequestProperty("Content-Encoding", "gzip");

        L.logF("====> [%s] url: %s --- (content)", conn.getRequestMethod(), url);
        L.d("The request properties is: "
                + Utils.getExtendedString(conn.getRequestProperties()));

        // POST请求
        GZIPOutputStream out = new GZIPOutputStream(conn.getOutputStream());
        try {
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }

        return readHttpResponse(conn);
    }
}

package com.holenet.cowinfo;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class NetworkManager {
    final static int CONNECTION_TIME = 5000;
    final static String MAIN_DOMAIN = "http://147.46.209.151:6147/";

    static String get(String url) {
        StringBuilder output = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode + "");
            if(resCode==HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    Log.e("line", line);
                    output.append(line);
                }
                reader.close();
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    static String post(String url, Map<String, String> data) {
        StringBuilder output = new StringBuilder();
        try {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            String csrftoken = getCsrfToken(url);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            Log.d("csrftoken", "["+csrftoken+"]");

            data.put("csrfmiddlewaretoken", csrftoken);
            StringBuilder postData = new StringBuilder();
            for(Map.Entry<String,String> param : data.entrySet()) {
                if(postData.length()!=0)
                    postData.append("&");
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode+"");

            if(resCode==HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while(true) {
                    String line = reader.readLine();
                    if(line==null)
                        break;
                    Log.e("line", line);
                    output.append(line);
                }
                reader.close();
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    static int upload(String url, File file, String fileName) {
        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        int resCode;

        try {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            String csrftoken = getCsrfToken(url);
            Log.e("csrftoken", csrftoken);

            URLConnection conn = new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

            OutputStream output = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

            // Send normal param.
            writer.append("--"+boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"csrfmiddlewaretoken\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset="+charset).append(CRLF);
            writer.append(CRLF).append(csrftoken).append(CRLF).flush();

            // Send binary file.
            writer.append("--"+boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"db_file\"; filename=\""+fileName+"\"").append(CRLF);
            writer.append("Content-Type: "+URLConnection.guessContentTypeFromName(fileName)).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            InputStream inputStream = new FileInputStream(file);

            int bytesAvailable = inputStream.available();
            int maxBufferSize = 1*1024*1024;
            int bufferSize = Math.min(bytesAvailable,maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            int bytesRead = inputStream.read(buffer, 0, bufferSize);

            while(bytesRead>0) {
                output.write(buffer, 0, bufferSize);
                bytesAvailable = inputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = inputStream.read(buffer, 0, bufferSize);
            }
            output.flush();
            writer.append(CRLF).flush();

            // End of multipart/form-data
            writer.append("--"+boundary+"--").append(CRLF).flush();

            // response from server
            resCode = ((HttpURLConnection)conn).getResponseCode();
            Log.d("resCode", resCode+"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while((line=reader.readLine())!=null) {
                Log.e("upload", line);
                stringBuilder.append(line).append("\n");
            }
            reader.close();

        } catch(Exception e) {
            e.printStackTrace();
            resCode = -1;
        }
        return resCode;
    }

    static int download(String url, String filePath) {
        return -1;
    }

    // Post data rigth after call this method
    static private String getCsrfToken(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        String csrftoken = null;
        String cookies = conn.getHeaderFields().get("Set-Cookie").get(0);
        for(String cookie: cookies.split(";")) {
            String[] cook = cookie.split("=");
            if(cook[0].equals("csrftoken")) {
                csrftoken = cook[1];
            }
        }
        conn.disconnect();
        return csrftoken;
    }
}

package com.marianhello.bgloc;

import android.util.Log;

import java.util.Map;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;

public class HttpPostService {

    private static final String TAG = "BGPlugin";

    public static int postJSON(String url, Object json, Map headers)	{
        try {
            String jsonString = json.toString();
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            // conn.setConnectTimeout(5000);
            // conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(jsonString.length());
            // conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
             conn.setRequestProperty("Content-Type", "application/json");
            Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                conn.setRequestProperty(pair.getKey(), pair.getValue());
            }

            OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
            os.write(json.toString());
            os.flush();
            os.close();

            return conn.getResponseCode();

        } catch (Throwable e) {
            Log.w(TAG, "Exception posting json: " + e);
            e.printStackTrace();
            return 0;
        }
    }
}

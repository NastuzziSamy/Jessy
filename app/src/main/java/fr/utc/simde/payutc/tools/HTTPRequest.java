package fr.utc.simde.payutc.tools;

/**
 * Created by Samy on 24/10/2017.
 */

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPRequest {
    public static final String LOG_TAG = "JsonApiClient";
    public String url;
    public HttpURLConnection request;
    static Map<String, String> args = new HashMap<String, String>();
    static Map<String, String> cookies = new HashMap<String, String>();

    public HTTPRequest(String url) {
        this.url = url;
    }

    public int get() throws IOException {
        String data = "?" + args2String(this.args);
        Log.d(LOG_TAG, "get: " + this.url + data);

        this.request = (HttpURLConnection) (new URL(this.url + data)).openConnection();
        this.request.setRequestMethod("GET");
        this.request.setRequestProperty("Cookie", getCookiesHeader());
        this.request.setUseCaches(false);
        this.request.setDoOutput(true);
        updateCookies(this.request.getHeaderFields().get("Set-Cookie"));

        return this.request.getResponseCode();
    }

    public int post() throws IOException {
        String data = args2String(this.args);
        Log.d(LOG_TAG, "post: " + this.url + ", data: " + data);

        this.request = (HttpURLConnection) (new URL(this.url)).openConnection();
        this.request.setRequestMethod("POST");
        this.request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        this.request.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
        this.request.setRequestProperty("Cookie", getCookiesHeader());
        this.request.setUseCaches(false);
        this.request.setDoInput(true);
        this.request.setDoOutput(true);
        this.request.getOutputStream().write(data.getBytes());
        updateCookies(this.request.getHeaderFields().get("Set-Cookie"));

        return this.request.getResponseCode();
    }

    public Map<String, List<String>> getHeaders() {
        if (this.request == null)
            return null;

        return this.request.getHeaderFields();
    }

    public String getHeader(String name) {
        if (this.request == null)
            return null;

        return this.request.getHeaderField(name);
    }

    public String getResponseMessage(String name) throws IOException {
        if (this.request == null)
            return null;

        return this.request.getResponseMessage();
    }

    public String getResponse() throws IOException {
        if (this.request == null)
            return null;

        BufferedReader in = new BufferedReader(new InputStreamReader(this.request.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            builder.append(inputLine);

        in.close();

        return builder.toString();
    }

    protected String args2String(Map<String, String> args) throws UnsupportedEncodingException {
        String data = "";

        for (String arg : args.keySet())
            data += (URLEncoder.encode(arg, "UTF-8") + "=" + URLEncoder.encode(args.get(arg), "UTF-8") + "&");

        return data.substring(0, data.equals("") ? 0 : data.length() - 1);
    }

    public Boolean addArg(String key, String value) {
        if (this.args.containsKey(key))
            return false;
        else
            this.args.put(key, value);

        return true;
    }

    public void setArg(String key, String value) {
        this.args.put(key, value);
    }

    public Boolean delArg(String key) {
        if (this.args.containsKey(key))
            this.args.remove(key);
        else
            return false;

        return true;
    }

    synchronized String getCookiesHeader() {
        String data = "";

        for (String cookie : cookies.keySet())
            data += cookie + "=" + cookies.get(cookie) + "; ";

        return data;
    }

    synchronized void updateCookies(List<String> cookiesHeader) {
        if (cookiesHeader != null) {
            Log.d(LOG_TAG, "cookies : " + cookiesHeader);

            for (String cookie : cookiesHeader) {
                try {
                    cookie = cookie.substring(0, cookie.indexOf(";"));
                    String cookieName = cookie.substring(0, cookie.indexOf("="));
                    String cookieValue = cookie.substring(cookie.indexOf("=") + 1);

                    this.cookies.put(cookieName, cookieValue);
                    Log.d(LOG_TAG, cookieName + " = " + cookieValue);
                }
                catch (Exception ex) {
                    Log.w(LOG_TAG, "error parsing cookie : '" + cookie + "'", ex);
                }
            }
        }
    }
}
package fr.utc.simde.jessy.tools;

/**
 * Created by Samy on 24/10/2017.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPRequest {
    private static final String LOG_TAG = "_HTTPRequest";

    private String url;
    private HttpURLConnection request;

    private String responseType;
    private String StringResponse;
    private JsonNode JSONResponse;
    private Bitmap BitmapResponse;

    private Map<String, Object> postArgs;
    private Map<String, String> getArgs;
    private Map<String, String> cookies;

    public HTTPRequest(final String url) {
        this.url = url;
        this.request = null;
        this.responseType = "string";
        this.StringResponse = "";
        this.postArgs = new HashMap<String, Object>();
        this.getArgs = new HashMap<String, String>();
        this.cookies = new HashMap<String, String>();
    }

    public int get() {
        String get = null;

        try {
            get = get2String(this.getArgs);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        Log.d(LOG_TAG, "get: " + this.url + get);

        try {
            this.request = (HttpURLConnection) (new URL(this.url + get)).openConnection();
            this.request.setUseCaches(false);
            this.request.setDoInput(true);
            updateCookies(this.request.getHeaderFields().get("Set-Cookie"));

            generateResponse(this.request.getInputStream());
        }
        catch (Exception e) {
            try {
                Log.d(LOG_TAG, "code: " + Integer.toString(this.request.getResponseCode()) + ", error: " + e.getMessage());
                generateResponse(this.request.getErrorStream());
            }
            catch (Exception e2) {
                Log.e(LOG_TAG, "error generating error message: " + e2.getMessage());
            }
        }

        this.request.disconnect();
        return getResponseCode();
    }

    public int post() { return post(true); }
    public int post(final Boolean sendJSON) {
        String get = null;
        String post = null;

        try {
            get = get2String(this.getArgs);
            post = post2String(this.postArgs, sendJSON);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        Log.d(LOG_TAG, "post: " + this.url + get + ", data: " + post);

        try {
            this.request = (HttpURLConnection) (new URL(this.url + get)).openConnection();
            this.request.setRequestMethod("POST");
            this.request.setRequestProperty("Content-Type", sendJSON ? "application/json" : "application/x-www-form-urlencoded");
            this.request.setRequestProperty("charset", "utf-8");
            this.request.setRequestProperty("Cookie", getCookiesHeader());
            this.request.setUseCaches(false);
            this.request.setDoInput(true);
            this.request.setDoOutput(true);

            DataOutputStream os = new DataOutputStream(this.request.getOutputStream());
            os.write(post.getBytes("UTF-8"));
            os.flush();
            os.close();

            updateCookies(this.request.getHeaderFields().get("Set-Cookie"));

            generateResponse(this.request.getInputStream());
        }
        catch (Exception e) {
            try {
                Log.d(LOG_TAG, "code: " + Integer.toString(this.request.getResponseCode()) + ", error: " + e.getMessage());
                generateResponse(this.request.getErrorStream());
            }
            catch (Exception e2) {
                Log.e(LOG_TAG, "error generating error message: " + e2.getMessage());
            }
        }

        this.request.disconnect();
        return getResponseCode();
    }

    public Map<String, List<String>> getHeaders() {
        if (this.request == null)
            return null;

        return this.request.getHeaderFields();
    }

    public String getHeader(final String name) {
        if (this.request == null)
            return null;

        return this.request.getHeaderField(name);
    }

    public int getResponseCode() {
        if (this.request == null)
            return Integer.parseInt(null);

        try {
            return this.request.getResponseCode();
        }
        catch (Exception e) {
                Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        return 500;
    }

    public String getResponseMessage(final String name) {
        if (this.request == null)
            return null;

        try {
            return this.request.getResponseMessage();
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        return "";
    }

    protected void generateResponse(InputStream inputStream) throws Exception {
        this.responseType = "string";
        if (this.request.getContentType().contains("image/")) {
            try {
                this.BitmapResponse = BitmapFactory.decodeStream(inputStream);
                this.responseType = "image";
            }
            catch (Exception e) {
                this.BitmapResponse = null;
                throw new Exception("Malformed IMG");
            }
        }
        else {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }

            in.close();
            this.StringResponse = builder.toString();

            if (this.request.getContentType().contains("application/json")) {
                try {
                    this.JSONResponse = new ObjectMapper().readTree(this.StringResponse);
                    this.responseType = "JSON";
                }
                catch (Exception e) {
                    this.JSONResponse = null;
                    throw new Exception("Malformed JSON");
                }
            }
        }

        Log.d(LOG_TAG, "code: " + Integer.toString(this.request.getResponseCode()) + ", type: " + this.responseType + (this.responseType == "image" ? "" : ", response: " + this.getResponse()));
    }

    public Boolean isJSONResponse() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        return this.responseType.equals("JSON");
    }

    public Boolean isImageResponse() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        return this.responseType.equals("image");
    }

    public String getResponseType() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        return this.responseType;
    }

    public JsonNode getJSONResponse() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        if (!this.responseType.equals("JSON"))
            throw new Exception("Not a JSON response");

        return this.JSONResponse;
    }

    public Bitmap getImageResponse() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        if (!this.responseType.equals("image"))
            throw new Exception("Not an image response");

        return this.BitmapResponse;
    }

    public String getResponse() throws Exception {
        if (this.request == null)
            throw new Exception("No request");

        return this.StringResponse;
    }

    protected String get2String(Map<String, String> args) throws UnsupportedEncodingException {
        String data = "";

        for (String arg : args.keySet())
            data += (URLEncoder.encode(arg, "UTF-8") + "=" + URLEncoder.encode(args.get(arg), "UTF-8") + "&");

        return data.equals("") ? "" : "?" + data.substring(0, data.length() - 1);
    }

    protected JsonNode map2JsonNode(Map<String, Object> args) throws UnsupportedEncodingException {
        ObjectNode data = new ObjectMapper().createObjectNode();

        for (String arg : args.keySet()) {
            if (args.get(arg).getClass() == String.class)
                data.put(arg, (String) args.get(arg));
            else if (args.get(arg).getClass() == Integer.class)
                data.put(arg, (Integer) args.get(arg));
            else if (args.get(arg).getClass() == Boolean.class)
                data.put(arg, (Boolean) args.get(arg));
            else if (args.get(arg).getClass() == JsonNode.class)
                data.put(arg, (JsonNode) args.get(arg));
            else if (args.get(arg).getClass() == ArrayNode.class)
                data.putArray(arg).addAll((ArrayNode) args.get(arg));
            else if (args.get(arg).getClass() == Map.class)
                data.put(arg, map2JsonNode((Map<String, Object>) args.get(arg)));
            else if (args.get(arg).getClass() == ArrayList.class)
                data.put(arg, args.get(arg).toString());
        }

        return data;
    }

    protected String post2String(Map<String, Object> args, boolean inJSON) throws Exception {
        if (inJSON)
            return map2JsonNode(args).toString();

        String data = "";

        for (String arg : args.keySet())
            data += (URLEncoder.encode(arg, "UTF-8") + "=" + URLEncoder.encode((String) args.get(arg), "UTF-8") + "&");

        return data.equals("") ? "" : data.substring(0, data.length() - 1);
    }

    public void setGet(Map<String, String> args) {
        this.getArgs = args;
    }

    public void setPost(Map<String, Object> args) {
        this.postArgs = args;
    }

    public void addGet(final String key, final String value) {
        this.getArgs.put(key, value);
    }

    public void addPost(final String key, final String value) {
        this.postArgs.put(key, value);
    }

    public Map<String, String> getCookies() {
        return this.cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    synchronized String getCookiesHeader() {
        String data = "";

        for (String cookie : cookies.keySet())
            data += cookie + "=" + cookies.get(cookie) + "; ";

        return data;
    }

    synchronized void updateCookies(final List<String> cookiesHeader) {
        if (cookiesHeader != null) {
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
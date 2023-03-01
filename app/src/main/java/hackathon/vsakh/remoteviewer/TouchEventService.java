package hackathon.vsakh.remoteviewer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;


import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import hackathon.vsakh.remoteviewer.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class TouchEventService extends Service {
    public static final String CHANNEL_ID = "TouchDetectService";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static  String SERVER_URL="http://10.168.29.246:8080";
    private static String LIST_DEVICE=SERVER_URL+"/list";
    private static String ADD_DEVICE=SERVER_URL+"/add_device";
    private static String EVENT_CALLBACK=SERVER_URL+"/eventcallback";
    private static String ADB_RESPONSE=SERVER_URL+"/adbresponse/";//http://10.168.29.246:8080/adbresponse/test
    //http://10.168.29.246:8080/adbresponse/0afd99ad83c31d28
    private static String DOWNLOAD_APK=SERVER_URL+"/uploadapk/";//http://10.168.29.246:8080/uploader/test
    private static String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static String DEVICE_NAME="Nexus7";

    final String TAG="RemoteViewer";


    Context mContext;

    public TouchEventService() {
    }

    @Override
    public void onCreate() {
        mContext = this;

        new Thread(new BackgroudRunnable()).start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, TouchEventService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("RemoteViewer")
                .setContentText("TouchDetect")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);


        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    OkHttpClient client;

    class BackgroudRunnable implements Runnable {

        @Override
        public void run() {


            while (true) {
                try {

                    readAndSetServer();

                    /*final Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/test.apk")),PACKAGE_MIME_TYPE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);*/
                    //listApplications("");
                    //Get Device list
                    client = new OkHttpClient();
                    Request.Builder builder = new Request.Builder();
                    builder.url(LIST_DEVICE);
                    Request request = builder.build();
                    Response response = client.newCall(request).execute();
                    String value = response.body().string();
                    Log.i(TAG, "GetDeviceList " + value);
//{"DeviceNames":["0afd99ad83c31d28","0afd99ad83c31d28","0afd99ad83c31d28","0afd99ad83c31d28"]}
                    //Check whether device is already added or not
                    String android_id = DEVICE_NAME;//Secure.getString(mContext.getContentResolver(),
                           // Secure.ANDROID_ID);
                    Log.i(TAG, "myID " + android_id);
                    boolean isAdded = false;
                    JSONObject valueJSOn = new JSONObject(value);
                    JSONArray deviceArray = valueJSOn.getJSONArray("DeviceNames");
                    for (int i = 0; i < deviceArray.length(); i++) {
                        if (deviceArray.get(i).equals(android_id)) {
                            isAdded = true;
                            break;
                        }
                    }
                    if (!isAdded) {
                        //Device is not added, so Add Device
                        Log.i(TAG, "Device is not added, so Add Device" + android_id);
                        JSONObject postBody = new JSONObject();//{"name":"Device465", "build":"4901", "size":"10.25"}

                        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                        Point size = new Point();
                        wm.getDefaultDisplay().getRealSize(size);
                        DisplayMetrics dm = new DisplayMetrics();
                        wm.getDefaultDisplay().getMetrics(dm);
                        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
                        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
                        double screenInches = Math.sqrt(x + y);


                        postBody.put("name", android_id);
                        postBody.put("build", Build.ID);
                        postBody.put("size", Double.toString(screenInches));
                        postBody.put("resolution", size.x + "x" + size.y);
                        postBody.put("url", "http://localhost:8080");
                        postRequest(ADD_DEVICE, postBody.toString());
                    } else
                        Log.i(TAG, "Device is already added, so not adding Device" + android_id);
                    //read input taps and send to adb
                    while (true) {

                        try {
                            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
                            HttpGet method = null;
                            method = new HttpGet(new URI(EVENT_CALLBACK));
                            //Log.e("execCommandLine()", method.toString());
                            HttpResponse responseNew = defaultHttpClient.execute(method);
                            Log.i(TAG, responseNew.toString());
                            InputStream data = responseNew.getEntity().getContent();
                            //BufferedReader reader = new BufferedReader(new InputStreamReader(data, "US-ASCII"));
                            byte[] buffer = new byte[1024];
                            int count;
                            while (true) {
                                count = data.read(buffer, 0, 512);
                                if (count > 0) {
                                    System.out.println(count);
                                    System.out.println(new String(buffer));
                                    String body = new String(buffer);
                                    Log.i(TAG,  "Received data "+body);
                                    JSONObject bodyObject = new JSONObject(body);
                                    String deviceName = bodyObject.getString("deviceName");
                                    if (deviceName.equals(android_id)) {
                                        String event = bodyObject.getString("Event");
                                        if (event.equals("touch")) {
                                            processTouch(bodyObject);
                                        } else if (event.equals("adb")) {

                                            processADB(android_id, bodyObject);
                                        } else if (event.equals("install")) {
                                            processAPKInstall(android_id, bodyObject);

                                        }else if(event.equals("apps_list")){
                                            listApplications(android_id);
                                        }
                                        else if(event.equals("logcat")){
                                            sendLogcats(android_id);
                                        }
                                    }
                                }
                            }
                        }
                        catch (Exception e){
                            Log.e(TAG, e.toString());
                        }
                    }
              /*  while (true) {

                    try {
                        Request.Builder builderTouch = new Request.Builder();
                        builderTouch.url("http://10.168.29.246:8080/eventcallback");
                        Request requestTouch = builderTouch.build();

                        Log.i("TouchDetectService", "Touch creating new request");
                        Response responseTouch = client.newCall(requestTouch).execute();
                        while (true) {
                            String body = getStringFromInputStream(responseTouch.body().byteStream());
                            Log.i("TouchDetectService", "Touch " + body);
                            JSONObject bodyObject = new JSONObject(body);
                            String deviceName = bodyObject.getString("deviceName");
                            if (deviceName.equals(android_id)) {
                                String x = bodyObject.getString("x");
                                String y = bodyObject.getString("y");
                                try {
                                    Runtime.getRuntime().exec("input tap " + x + " " + y);
                                    Log.i("TouchDetectService", "sending adb command " + "input tap " + x + " " + y);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                    } catch (Exception exception) {
                        Log.e("TouchDetectService", exception.toString());
                    }
                }*/
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

    }


    void readAndSetServer(){
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "config.txt");
            FileReader fileReader = new FileReader(file);

            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
// This responce will have Json Format String
            String responce = stringBuilder.toString();

            JSONObject data=new JSONObject(responce);
            SERVER_URL=data.getString("server");
            DEVICE_NAME=data.getString("device");

           LIST_DEVICE=SERVER_URL+"/list";
           ADD_DEVICE=SERVER_URL+"/add_device";
           EVENT_CALLBACK=SERVER_URL+"/eventcallback";
           ADB_RESPONSE=SERVER_URL+"/adbresponse/";

           DOWNLOAD_APK=SERVER_URL+"/uploadapk/";//http://10.168.29.246:8080/uploader/test
            PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";


        }
        catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void listApplications(String android_id) {
       /* Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = mContext.getPackageManager().queryIntentActivities( mainIntent, 0);*/
        final PackageManager pm = getPackageManager();
//get a list of installed apps.
        List<PackageInfo> pkgAppsList = getPackageManager().getInstalledPackages(0);
        Log.i(TAG, "Applications installed " + pkgAppsList + " "+android_id);
        JSONArray appsLis=new JSONArray();
        for(int i=0;i<pkgAppsList.size();i++){

            PackageInfo p = pkgAppsList.get(i);
            JSONObject item=new JSONObject();
            try {
                item.put("name",p.applicationInfo.loadLabel(getPackageManager()).toString());
                item.put("version", p.versionName);
                item.put("package",p.packageName);

                appsLis.put(item);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String postURL = ADB_RESPONSE + android_id;
        try {
            JSONObject postData = new JSONObject();
            postData.put("response", appsLis.toString());
            postData.put("continueResp", "false");
            postRequest(postURL, postData.toString());
            Log.i(TAG, "sending adb result to " + postURL + " data " + postData.toString());
            Log.i(TAG, "Application list " + appsLis.toString());
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void processAPKInstall(String android_id, JSONObject bodyObject) throws JSONException {
        String apkFileName = bodyObject.getString("x");
        try {
            String downLoadURL = DOWNLOAD_APK + android_id;
            URL url = new URL(downLoadURL);
            //URL url = new URL("http://yoururl.com");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            //URLConnection conection = url.openConnection();
            conn.setRequestMethod("GET");

            //conection.connect();
            conn.connect();

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            int lenghtOfFile = conn.getContentLength();
            // download the file
            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);

            // Output stream
            OutputStream output = new FileOutputStream(Environment
                    .getExternalStorageDirectory().toString()
                    + "/"+apkFileName);

            byte data[] = new byte[1024];

            long total = 0;
            int count=0;

            while ((count = input.read(data)) != -1) {
                total += count;
                // writing data to file
                output.write(data, 0, count);
            }
            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();
            Log.i(TAG, "finished downloading APK file" + apkFileName);

            final Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/"+apkFileName)),PACKAGE_MIME_TYPE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        String postURL = ADB_RESPONSE + android_id;

        JSONObject postData = new JSONObject();
        postData.put("response", "success");
        postData.put("continueResp", "false");
        try {
            postRequest(postURL, postData.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "sending adb result to " + postURL +" data "+postData.toString());
    }

    private void processTouch(JSONObject bodyObject) throws JSONException {
        // Code for sending the touch event
        String x = bodyObject.getString("x");
        String y = bodyObject.getString("y");
        try {
            Runtime.getRuntime().exec("input tap " + x + " " + y);
            Log.i(TAG, "sending adb command " + "input tap " + x + " " + y);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void processADB(String android_id, JSONObject bodyObject) throws JSONException {
        String command = bodyObject.getString("x");
        try {
            Log.i(TAG, "sending adb command " + command);
            String result = Executer(command);
            Log.i(TAG, "Received adb command result " + result);
            String postURL = ADB_RESPONSE + android_id;

            JSONObject postData = new JSONObject();
            postData.put("response", result);
            postData.put("continueResp", "false");
            postRequest(postURL, postData.toString());
            Log.i(TAG, "sending adb result to " + postURL +" data "+postData.toString());


        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    void sendLogcats(String android_id){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process process = Runtime.getRuntime().exec("logcat");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    StringBuilder log=new StringBuilder();
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        // do what you want with the line you just read
                        // you're in an infinite loop continuously reading the log,
                        // you might want to add some mechanism to break out of it

                        Log.i(TAG, "Received logcat " + line);
                        String postURL = ADB_RESPONSE + android_id;

                        JSONObject postData = new JSONObject();
                        postData.put("response", line);
                        postData.put("continueResp", "false");
                        postRequest(postURL, postData.toString());
                        Log.i(TAG, "sending logcat result to " + postURL +" data "+postData.toString());
                    }
                }
                catch (Exception  e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }


    StringBuilder getLog() {

        StringBuilder builder = new StringBuilder();

        try {
            String[] command = new String[] { "logcat", "-d", "-v", "threadtime" };

            Process process = Runtime.getRuntime().exec(command);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                //if (line.contains(processId)) {
                    builder.append(line);
                    //Code here
               // }
            }
        } catch (IOException ex) {
            Log.e(TAG, "getLog failed", ex);
        }

        return builder;
    }


    public String Executer(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = output.toString();
        return response;

    }

    String getResult(BufferedReader reader) {
        String result = "";
        String line = "";
        while (true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            result += line;
        }
        return result;
    }


    public static String getStringFromInputStream(InputStream stream) throws IOException {
        int n = 0;
        char[] buffer = new char[58];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        // while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        n = reader.read(buffer);
        writer.write(buffer, 0, n);
        return writer.toString();
    }

    private void start() {
        Request request = new Request.Builder().url("http://10.168.29.246:8080/eventcallback").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    private void output(final String txt) {
        Log.i(TAG, txt);
    }


    void postRequest(String postUrl, String postBody) throws IOException {

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, postBody);

        Request request = new Request.Builder()
                .url(postUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure");
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, response.body().string());
            }
        });
    }


    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {

            webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output("Receiving : " + text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }
}
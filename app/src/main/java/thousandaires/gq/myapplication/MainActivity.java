package thousandaires.gq.myapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.lang.Thread;
import java.lang.ref.WeakReference;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Queue;

import static java.lang.Thread.*;


public class MainActivity extends AppCompatActivity {

    public double latitude;
    public double longitude;
    public RequestQueue queue;

    private TextView textview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(getApplicationContext());


        System.out.println("Setting up Command loop logic.");
        ServerListener commandLoopLogic = new ServerListener();
        Thread commandLoop = new Thread(commandLoopLogic);
        System.out.println("About to start Command Loop.");
        commandLoop.start();


//        System.out.println("Creating new Location Manager.");
//        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        System.out.println("Creating new Location Listener");
//        LocationListener mlocListener = new MyLocationListener();
//        System.out.println("Start the process to request location updates.");
//        try {
//            System.out.println("Checking Permissions");
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                System.out.println("NOT ALLOWED!");
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            System.out.println("Requesting updates");
//            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
//            System.out.println("Successfully started requesting updates!");
//        } catch(Exception e) {
//            System.out.println("permissions error with location updates" + e.toString());
//        }
//        System.out.println("Setting values in the text view!");
        textview = (TextView)findViewById(R.id.textview);

    }

    public void textviewPrint(String string) {
        textview.setText(string);
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class SerialManager {

        private final int BAUD_RATE = 9600;
        private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

        private UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        private UsbDevice device;
        private UsbDeviceConnection connection;
        private HashMap<String, UsbDevice> usbDevices;
        private UsbSerialDevice serialPort;

        private boolean connected;
        private boolean pendingResponse;

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if(granted) {
                        connection = usbManager.openDevice(device);
                        if (connection != null) {

                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                            if (serialPort != null) {

                                if (serialPort.open()) {

                                    serialPort.setBaudRate(BAUD_RATE);
                                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                    textviewPrint("Successfully Configured!!!");
                                    connected = true;
                                } else {
                                    // Serial port could not be opened, maybe an I/O error or it CDC driver was chosen it does not really fit
                                    System.out.println("Serial port could not be opened.");
                                }
                            }
                            connected = true;
                        } else {
                            textviewPrint("Crash here!");
                        }
                    }
                }
            }
        };

        private void setFilter() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            registerReceiver(broadcastReceiver, filter);
        }

        public SerialManager() {
            connected = false;
            pendingResponse = false;
            setFilter();
        }

        public boolean isConnected() {
            return connected;
        }



        private void attemptConnection() {
            if(!this.isConnected() && !pendingResponse) {
                sleep(1000);
                usbDevices = usbManager.getDeviceList();
                System.out.println("[SerialManager] usbDevices: " + !usbDevices.isEmpty() + " connected: " + connected);
                if (!usbDevices.isEmpty() && !connected) {
                    boolean keep = true;
                    for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                        if (serialPort == null) {
                            device = entry.getValue();
                            int deviceVID = device.getVendorId();
                            int devicePID = device.getProductId();
                            if (deviceVID != 0x1d6b || (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003)) {
                                // We are supposing here there is only one device connected and it is our serial device
//                                sleep(5000);

                                // get permission to use the device
                                if (device == null) {
                                    textviewPrint("Crash here!");
                                }
                                PendingIntent mPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                                pendingResponse = true;
                                usbManager.requestPermission(device, mPendingIntent);

                                keep = false;
                            } else {
                                connection = null;
                                connected = false;
                                device = null;
                            }

                            if (!keep)
                                break;
                        }
                    }
                }
            } else {
                sleep(1000);
            }
        }



        private void send(String string) {
            serialPort.write(string.getBytes());
        }
    }

    public class ServerListener implements Runnable {

        private final String OPEN_COMMAND = ":)";
        private final String CLOSE_COMMAND = ":(";
        private final String PULSE_COMMAND = "RELAX";
        private final String CLOSE_VALVE = "0";
        private final String OPEN_VALVE = "1";
        private final int MAX_FAILED_ATTEMPTS = 5;

        private long pauseLength = 2000;
        private boolean valveOpen = false;
        private String url = "http://thousandaires.gq:8080/action";
//        private String url = "http://google.com";
        private int communicationAttempts = 0;

        private SerialManager serialManager = new SerialManager();

        @Override
        public void run() {
            sleep(5000);
            while (true) {
                //setup
                while (!serialManager.isConnected()) {
                    serialManager.attemptConnection();
                }
                if (serialManager.isConnected()) {
                    //loop
                    while (serialManager.isConnected()) {

                        sleep(pauseLength);
//                    textviewPrint("Sending get request to server...");
                        StringRequest req = new StringRequest(Request.Method.GET, url,
                                new Response.Listener<String>() {
                                    public void onResponse(String response) {
                                        textviewPrint(response);
                                        communicationAttempts = 0;
                                        switch (response) {
                                            case OPEN_COMMAND:
                                                if (!valveOpen) {
                                                    textviewPrint("Opening valve.");
                                                serialManager.send(OPEN_VALVE);
                                                    valveOpen = true;
                                                }
                                                break;
                                            case CLOSE_COMMAND:
                                                if (valveOpen) {
                                                    textviewPrint("Closing valve.");
                                                serialManager.send(CLOSE_VALVE);
                                                    valveOpen = false;
                                                }
                                                break;
                                            case PULSE_COMMAND:
                                                textviewPrint("Changing valve status.");
                                                if (valveOpen)
                                                    serialManager.send(CLOSE_VALVE);
                                                else
                                                    serialManager.send(OPEN_VALVE);
                                                valveOpen = !valveOpen;
                                                break;
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    public void onErrorResponse(VolleyError error) {
                                        communicationAttempts++;
                                        textviewPrint("There was an error attempting to contact the server.");
                                        System.out.println(error.toString());
                                        if (communicationAttempts >= MAX_FAILED_ATTEMPTS) {
//                                        if (valveOpen)
//                                            serialManager.sendOverSerial(CLOSE_VALVE);
//                                        else
//                                            serialManager.sendOverSerial(OPEN_VALVE);
                                            valveOpen = !valveOpen;
                                        }
                                    }
                                }
                        );
                        queue.add(req);
                    }
                }
            }
        }
    }


    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            String text = "lat=" + latitude  + " lon=" + longitude + "\n";
            //Toast.makeText(getApplicationContext(), Text, Toast.LENGTH_SHORT).show();
            //addToLog(text);
            //renderLog();


            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String host_and_path = preferences.getString("pref_url", "http://thousandaires.gq:8080/incoming");


            String queryString = "?lat=" + latitude + "&lng=" + longitude + "&device=phone";
            String url = host_and_path + queryString;

            textview.setText("sending request=" + url);

            StringRequest req = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        public void onResponse(String response) {
                            //pass
                        }
                    },
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            textview.setText(textview.getText() +"\nerror="+error.toString() + "\n");
                            //addToLog(error.toString() + "\n");
                            //renderLog();
                        }
                    }
            );

            queue.add(req);
        }



        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
        }


        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_exit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                this.finishAffinity();
            } else {
                this.finishActivity(0);
            }
        }

        return super.onOptionsItemSelected(item);
    }
}

package com.sarmale.arduinobtexampleledcontrol;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
// import android.bluetooth.BluetoothSocket;
// import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
// import android.os.SystemClock;  // For: SystemClock.sleep(5000);
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    
	// Global variables we will use
    private static final String TAG = "LOG TAG";
    private static final int REQUEST_ENABLE_BT = 1;
	
    //We will use a Handler to get the BT Connection status
    public static Handler handler;
    private final static int ERROR_READ = 0;
    // Used in bluetooth handler to identify message update
	
    BluetoothDevice arduinoBTModule = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // We declare a default UUID to create the global variable
    
	ConnectedThread connectedThread;
	
	
    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.M)  // ??
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		// // Instances of BT Manager and BT Adapter needed to work with BT in Android.
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		
        // Instances of the Android UI elements that will will use during the execution of the APP
        Button searchDevicesButton = findViewById(R.id.searchDevicesButton);
        TextView btDevices = findViewById(R.id.btDevices);
        Button connectButton = findViewById(R.id.connectButton);
        TextView btReadings = findViewById(R.id.btReadingsTextView);
        Button clearButton = findViewById(R.id.clearButton);
        Button configureLEDButton = findViewById(R.id.nextActivityButton);
        Log.d(TAG, "Begin Execution");


        //Using a handler to update the interface in case of an error connecting to the BT device
        //My idea is to show handler vs RxAndroid
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {  // Annotated on behalf of IDE.
                Log.d(TAG, "INFO: handler: handleMessage: msg");
                if (msg.what == ERROR_READ) {
                    Log.d(TAG, "ERROR: handler: msg.what == ERROR_READ");
                    String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                    btReadings.setText(arduinoMsg);
                    // I believe the bellow one is responsible for showing "Unable to connect to BT device."
                    // Still not sure why it happens, nor how to fix it.
                    Toast.makeText(MainActivity.this, arduinoMsg, Toast.LENGTH_LONG).show();
                }
            }
        };
		
		
		// Set a listener event on a button to clear the texts
        clearButton.setOnClickListener(view -> {
            btDevices.setText("");
            btReadings.setText("");
        });


        // Create an Observable from RxAndroid
        // The code will be executed when an Observer subscribes to the the Observable
        final Observable<ConnectedClass> my_connectObservable = Observable.create(emitter -> {
            // Emitter seems to be something very crucial for Observable
            // I don't think it's needed in current state, as the 'onNext()' method is not in use.
            Log.d(TAG, "Calling ConnectThread class");  // Probably mistake before, lowercase 'c'.
            // Call the constructor of the ConnectThread class
            // Passing the Arguments: an Object that represents the BT device,
            // the UUID and then the handler to update the UI
            ConnectThread my_connectThread = new ConnectThread(arduinoBTModule, arduinoUUID, handler);
            my_connectThread.run();
            // Check if Socket connected
            if (my_connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                // The pass the Open socket as arguments to call the constructor of ConnectedThread
                // ConnectedThread connectedThread = new ConnectedThread(my_connectThread.getMmSocket());
                connectedThread = new ConnectedThread(my_connectThread.getMmSocket());
				// connectedThread.run();
				// if(connectedThread.getValueRead()!=null)
                if(connectedThread.getMmInStream() != null && connectedThread!= null)
                {
					 // If we have read a value from the Arduino
                     // we call the onNext() function
                     // This value will be observed by the observer
                     emitter.onNext(connectedThread.getValueRead());

                    Log.d(TAG, "Calling ConnectedClass class");
                    ConnectedClass connected = new ConnectedClass();
                    connected.setConnected(true);
                    emitter.onNext(connected);
                    // MyApplication.setupConnectedThread();
                }
                // We just want to stream 1 value, so we close the BT stream
                if (connectedThread != null) {
                    connectedThread.cancel();
                    // Might have produced NullPointerException.
                }
            }

		     // SystemClock.sleep(5000); // Why would you need that?
             // Close the socket connection
             my_connectThread.cancel();
             // We could Override the onComplete function

            emitter.onComplete();  // Does it even have a purpose to call it, when NOT OVERRIDDEN?

            /* SOMETIMES THE RECONNECT WORKS, SOMETIMES STILL IT DOES NOT */
            /* Calling the 'cancel()' method on both Threads doesn't seem to solve it. */
            /* Maybe its not enough, as it might only cancel threads but not bt comm / socket. */
        });

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////// Find all Linked devices ///////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        // Display all the linked BT Devices
        searchDevicesButton.setOnClickListener(view -> {
            // Check if the phone supports BT.
            if (bluetoothAdapter == null) {
                Log.d(TAG, "Device doesn't support Bluetooth");
            } else {
                Log.d(TAG, "Device support Bluetooth");
                // Check BT enabled.
                if (!bluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "Bluetooth is disabled");
                    // Ask user to enable BT. (--> Do We ??)
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // This 'if' is here AT THIS MOMENT just to silence the COMPILER.
                    if (ActivityCompat.checkSelfPermission(
                            getApplicationContext(),
                            Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    } else {}
                    /* The previous 'if' had SAME BODY as 'else' block. --> SUPERFLUOUS*/
                    Log.d(TAG, "We \"EITHER DO OR DON'T\" have BT Permissions");
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Log.d(TAG, "Bluetooth is \"EITHER WAY\" enabled now");
                } else {
                    Log.d(TAG, "Bluetooth is enabled");
                }

                String btDevicesString = "";
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    // Get the name and address of each paired device.
                    for (BluetoothDevice device: pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        Log.d(TAG, "deviceName:" + deviceName);
                        Log.d(TAG, "deviceHardwareAddress:" + deviceHardwareAddress);
                        // We append all devices to a String that we will display in the UI
                        btDevicesString = btDevicesString + deviceName +
                                        " || " + deviceHardwareAddress + "\n";
                        // If we find the HC 05 device (the Arduino BT module)
                        // We assign the device value to the Global variable BluetoothDevice
                        // We enable the button "Connect to HC 05 device"
                        if (deviceName.equals("HC-05")) {
                            Log.d(TAG, "HC-05 found");
                            arduinoUUID = device.getUuids()[0].getUuid();
                            arduinoBTModule = device;
                            // HC -05 Found, enabling the button to read results
                            connectButton.setEnabled(true);
                        }
                        btDevices.setText(btDevicesString);
                    }
                }
            }
            Log.d(TAG, "Button Pressed");
        });

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////// Call the observable to connect to the HC-05 ////////////////////////////////////////////
        ////////////////////////////////////////////// If it connects, the button to configure the LED will be enabled  ///////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //            @SuppressLint("CheckResult")  // Added by me - mplch
        connectButton.setOnClickListener(view -> {
            Log.d(TAG, "INFO: connectButton.setOnClickListener(v->{");
            btReadings.setText("");
            Log.d(TAG, "INFO: connectButton: btReadings set to \"\".");
            if (arduinoBTModule != null) {
                // We subscribe to the observable until the onComplete() is called
                // We also define control the thread management with
                // subscribeOn:  the thread in which you want to execute the action
                // observeOn: the thread in which you want to get the response
                my_connectObservable.
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribeOn(Schedulers.io()).
                        subscribe(connectedToBTDevice -> {
                            if(connectedToBTDevice.isConnected()){
                                configureLEDButton.setEnabled(true);
                            }
                            // valueRead returned by the onNext() from the Observable
                            btReadings.setText(valueRead);  // not in ExampleLEDControl
                            // We just scratched the surface with RxAndroid
                        });

            }
        });

        // Next activity to configure the RGB LED
        configureLEDButton.setOnClickListener(view -> {
            Log.d(TAG, "INFO: MyApplication.getApplication().setupConnectedThread(connectedThread)");
            MyApplication.getApplication().setupConnectedThread(connectedThread);
            Intent intent = new Intent(MainActivity.this, ConfigureLed.class);
            startActivity(intent);
        });
    }
}
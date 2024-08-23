package com.sarmale.arduinobtexampleledcontrol;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
// import android.annotation.SuppressLint;
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
// import android.os.SystemClock;
import android.util.Log;
import android.view.View;
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
    private static final String TAG = "FrugalLogs";
    private static final int REQUEST_ENABLE_BT = 1;
	
    //We will use a Handler to get the BT Connection status
    public static Handler handler;
    private final static int ERROR_READ = 0;
    // Used in bluetooth handler to identify message update
	
    BluetoothDevice arduinoBTModule = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // We declare a default UUID to create the global variable
    
	ConnectedThread connectedThread;
	
	
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		// // Instances of BT Manager and BT Adapter needed to work with BT in Android.
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		
        // Instances of the Android UI elements that will will use during the execution of the APP
        // TextView btReadings = findViewById(R.id.btReadings);
		TextView btDevices = findViewById(R.id.btDevices);
        Button connectButton = findViewById(R.id.connectButton);
        Button searchDevicesButton = findViewById(R.id.searchDevicesButton);
        Button nextActivityButton = findViewById(R.id.nextActivityButton);
        Log.d(TAG, "Begin Execution");


        //Using a handler to update the interface in case of an error connecting to the BT device
        //My idea is to show handler vs RxAndroid
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {  // Annotated on behalf of IDE.
                if (msg.what == ERROR_READ) {
                    String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                    // btReadings.setText(arduinoMsg);
                    Toast.makeText(MainActivity.this, arduinoMsg, Toast.LENGTH_LONG).show();
                }
            }
        };
		
		
		/*  // Set a listener event on a button to clear the texts
        clearValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btDevices.setText("");
                btReadings.setText("");
            }
        });  */


        // Create an Observable from RxAndroid
        // The code will be executed when an Observer subscribes to the the Observable
        final Observable<ConnectedClass> connectToBTObservable = Observable.create(emitter -> {
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
					// // If we have read a value from the Arduino
                    // // we call the onNext() function
                    // // This value will be observed by the observer
                    // emitter.onNext(connectedThread.getValueRead());
					
                    ConnectedClass connected = new ConnectedClass();
                    connected.setConnected(true);
                    emitter.onNext(connected);
                    //MyApplication.setupConnectedThread();
                }
				// // We just want to stream 1 value, so we close the BT stream
                // connectedThread.cancel();
            }
		    // // SystemClock.sleep(5000); // simulate delay
            // //Then we close the socket connection
            // my_connectThread.cancel();
            // // We could Override the onComplete function
            emitter.onComplete();
            my_connectThread.cancel();
        });

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////// Find all Linked devices ///////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        searchDevicesButton.setOnClickListener(new View.OnClickListener() {
            // IDE suggests to replace upper with LAMBDA ?
            // Display all the linked BT Devices
            @Override
            public void onClick(View view) {
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
            }
        });

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////// Call the observable to connect to the HC-05 ////////////////////////////////////////////
        ////////////////////////////////////////////// If it connects, the button to configure the LED will be enabled  ///////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        connectButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CheckResult")  // Added by me - mplch
            @Override
            public void onClick(View view) {
				// btReadings.setText("");
                if (arduinoBTModule != null) {
                    // We subscribe to the observable until the onComplete() is called
                    // We also define control the thread management with
                    // subscribeOn:  the thread in which you want to execute the action
                    // observeOn: the thread in which you want to get the response
                    connectToBTObservable.
					observeOn(AndroidSchedulers.mainThread()).
					subscribeOn(Schedulers.io()).
					subscribe(connectedToBTDevice -> {
						// valueRead returned by the onNext() from the Observable
						if(connectedToBTDevice.isConnected()){
							nextActivityButton.setEnabled(true);
						}
						// btReadings.setText(valueRead);
						// We just scratched the surface with RxAndroid
					});
                }
            }
        });

        // Next activity to configure the RGB LED
        nextActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.getApplication().setupConnectedThread(connectedThread);
                Intent intent = new Intent(MainActivity.this, ConfigureLed.class);
                startActivity(intent);
            }
        });
    }
}
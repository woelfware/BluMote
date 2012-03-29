// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;


/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class PodListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
	
    private static final int ID_RENAME = 0;
	private static final String POD_PREFIX = "P0D_";
	private static final int POD_NAME = 0;
	
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_DEVICE_NAME = "device_name";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private Button scanButton;
    
    //Shared preferences class - for storing config settings between runs
    private SharedPreferences prefs;

    private static String pod_rename;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.pod_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.manage_devices_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.manage_devices_item);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // register for context menu
        registerForContextMenu(findViewById(R.id.paired_devices));
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
//        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // pull in preferences for known devices, add to array adapter
        prefs = getSharedPreferences(BluMote.PREFS_FILE, MODE_PRIVATE);
    	String prefs_table = prefs.getString("knownDevices", null);       	
    	// structure is \t"device name"\n"MAC ID" and then repeats for multiple devices
    	
    	if (prefs_table != null) {
    		String devices[] = prefs_table.split("\t");    	
    	
    		findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
    		String deviceName;
    		// iterate through and add to arrayadapter list
    		for (String device : devices) {
    			deviceName = translatePodName(device.split("\n")[0], prefs) + 
    					"\n" + device.split("\n")[1];
    			mPairedDevicesArrayAdapter.add(deviceName);
    		}
    	}
    	else {
    		mPairedDevicesArrayAdapter.clear();
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    	
        // If there are paired devices, add each one to the ArrayAdapter
//        if (pairedDevices.size() > 0) {
//            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
//            for (BluetoothDevice device : pairedDevices) {
//                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());                
//            }
//        } else {
//            String noDevices = getResources().getText(R.string.none_paired).toString();
//            mPairedDevicesArrayAdapter.add(noDevices);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // clear array adapter for new devices
        mNewDevicesArrayAdapter.clear();
        
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);           
        	
            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtra(EXTRA_DEVICE_NAME, info); //store full name contents, used for "known devices" list
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluMote.DEBUG) {
                	String name = device.getName();
                	if (name.endsWith("\n") || name == "" || name == null) {
                		Log.e(TAG, "found a malformed device name: "+ name);
                	}
                	addToNewDevices(translatePodName(device.getName(), prefs)
                    		+ "\n" + device.getAddress());
                }
                else {
                	// if we are not in debug mode then screen the entries
                	if (device.getName().startsWith("BluMote")) {                		
                		addToNewDevices(translatePodName(device.getName(), prefs)
                				+ "\n" + device.getAddress());
                	}
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                ((View)scanButton).setVisibility(View.VISIBLE);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
    
    private void addToNewDevices(String name) {
    	// make sure the item is not already in the list....
    	boolean foundIt = false;
    	for (int i=0; i< mNewDevicesArrayAdapter.getCount(); i++) {
    		if (name.split("\n")[0].startsWith(mNewDevicesArrayAdapter.getItem(i))) {
    			foundIt = true;
    			break;     	
    		}
    	} 
    	if (foundIt != true) {
    		// add to list if it is unique
    		mNewDevicesArrayAdapter.add(name);
    	}
    }
    
    /**
     * This function re-displays the known pod names while mapping the name to the user-specified
     * name if it exists in the prefs file
     */
    private void displayPodNames() {
    	String displayedItem;
    	String newName;
    	String[] podNames = new String[mPairedDevicesArrayAdapter.getCount()];
    	
    	for (int i = 0; i< mPairedDevicesArrayAdapter.getCount(); i++) {
    		displayedItem = mPairedDevicesArrayAdapter.getItem(i);
    		newName = translatePodName(displayedItem.split("\n")[0], prefs); // pass in the
    																	     // first token
    		// reconstruct the item with the translated name
    		podNames[i] = newName + "\n" + displayedItem.split("\n")[1];
    	}
    	
    	mPairedDevicesArrayAdapter.clear();  // clear before re-populating
    	
    	// now repopulate the arraylist with the new podNames[]
    	for (int i=0; i<podNames.length; i++) {
    		mPairedDevicesArrayAdapter.add(podNames[i]);
    	}
    }
    
    /**
     * This function will find the user specified name in the prefs file that maps to the name
     * given to the function.
     * @param podName
     * @return the name that was stored in prefs file
     */
    static String translatePodName(String podName, SharedPreferences prefs) {
    	String podKey = convertToPodKey(podName);
    	if (prefs.contains(podKey)) {
    		// if it is contained then return the mapping
    		return prefs.getString(podKey, "");
    	} else {
    		return podName; // just return the same name
    	}
    }
    
    /**
     * This function will translate a re-named pod name to the actual pod name
     * @param podName
     * @return
     */
//    String lookupPodName(String podName) {
//    	
//    }
    
    private static String convertToPodKey(String podName) {
    	if (podName.startsWith(POD_PREFIX)) {
    		return podName;    		
    	} else {
    		return POD_PREFIX + podName;
    	}
    }
    
    /**
     * Creates a new prefs item for mapping between the actual pod name and the user
     * defined one.
     * @param oldName
     * @param newName
     */
    private void addNewPodMapping(String oldName, String newName) {
    	Editor mEditor = prefs.edit();
		mEditor.putString(convertToPodKey(oldName), newName);
		mEditor.commit();
    }    
    	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_list_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear_devices:
        	Editor mEditor =  prefs.edit();
        	mEditor.remove("knownDevices");
        	mEditor.commit();    
        	mPairedDevicesArrayAdapter.clear();
            return true;
        }
        return false;        
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case ID_RENAME:
			// store ID of the item to be renamed
			pod_rename = mPairedDevicesArrayAdapter.getItem((int)info.id);
			pod_rename = pod_rename.split("\n")[0]; // store only the name
			// Launch the function to ask for a name for device
        	Intent i = new Intent(this, EnterDevice.class);
            startActivityForResult(i, POD_NAME);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	// context menu is for activities list
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.paired_devices) {
			// AdapterView.AdapterContextMenuInfo info =
			// (AdapterView.AdapterContextMenuInfo)menuInfo;
			menu.setHeaderTitle("Menu");
			menu.add(0, ID_RENAME, 0, "Rename Device");
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		String return_string;
    	Bundle return_bundle;
    	
    	if (requestCode == POD_NAME && resultCode == RESULT_OK) {
    		// add the new item to the database
    		return_bundle = data.getExtras();
    		if ( return_bundle != null ) {
    			return_string = return_bundle.getString("returnStr");
        		addNewPodMapping(pod_rename, return_string);
        		// refresh the display of items
        		displayPodNames();   
    		}        	
    	}
	}
}

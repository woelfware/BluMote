// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * This activity-class is launched from the main BluMote interface. 
 * This class deals with changing or viewing the Initialization list associated
 * with the 'activities' feature.  
 * @author keusej
 *
 */
public class ActivityInitEdit extends Activity {	
	private ArrayAdapter<String> activityArrayAdapter;
    ListView activitiesListView;
	
    // intent return codes
	public static final String REDO = "redo";
	public static final String APPEND = "append";
	
    private static final int ID_DELETE = 0;
    private static final int ID_CHANGE = 1;
	public static final String ACTIVITY_NAME = "ACTIVITY";
	private static final int DIALOG_INIT_DELAY = 0;
	
	private int itemSelected; // index of init item that context menu was created on
	
	private Button redo_init_btn;
	private Button append_init_btn;
    
    // the activity that we want to work on
    String activityName;

    SharedPreferences prefs; 
    
    // used to convert device/activity names into IDs that do not change
	InterfaceLookup lookup;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_edit);
        
        Intent i = getIntent();
        activityName = i.getStringExtra(ACTIVITY_NAME);
        
        // initialize shared preferences
        prefs = getSharedPreferences(BluMote.PREFS_FILE, MODE_PRIVATE);

        // initialize the InterfaceLookup
		lookup = new InterfaceLookup(prefs);
		
        // Initialize array adapters. 
        activityArrayAdapter = new ArrayAdapter<String>(this, R.layout.manage_devices_item);
        
        // Find and set up the ListView for paired devices
        activitiesListView = (ListView) findViewById(R.id.activity_edit_list);
        activitiesListView.setAdapter(activityArrayAdapter);
//        activitiesListView.setOnItemClickListener(mDeviceClickListener);
        
        redo_init_btn = (Button) findViewById(R.id.redo_init_btn);
        redo_init_btn.setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
            	// need to tell calling activity that we want to redo the init
            	// calling activity then needs to re-enter mode to re initialize
            	AlertDialog.Builder builder = new AlertDialog.Builder(ActivityInitEdit.this);
            	builder.setMessage("This action will clear the initialization sequence, are you sure you want to do this?")
            	       .setCancelable(false)
            	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   // if they are sure then send intent extra to "redo"
            	            	Intent i = getIntent();
            					i.putExtra("returnStr", REDO);
            					finish();	
            	           }
            	       })
            	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();
            	alert.show();            	            
            }
        });
        
        append_init_btn = (Button) findViewById(R.id.append_init_btn);
        append_init_btn.setOnClickListener( new OnClickListener() {
            public void onClick(View v) {
            	// send intent extra to "append"
            	Intent i = getIntent();
				i.putExtra("returnStr", APPEND);
				finish();         	            
            }
        });
        
        populateDisplay();
        
        registerForContextMenu(findViewById(R.id.activity_edit_list));
        
        i.putExtra("returnStr", ""); // return empty string indicating no RED requested
        setResult(RESULT_OK,i);       
	}

	/**
	 * Populate the display with all the activity initialization steps
	 */
	private void populateDisplay() {
        activityArrayAdapter.clear(); // always clear before adding items
        
        String[] initItems = Activities.getActivityInitSequence(activityName, prefs);
        if (initItems != null && initItems.length > 0) {
        	// iterate through these values
        	for (String item : initItems) {	
        		if (item.equals("")) {
        			continue; // skip if its empty
        		}
        		activityArrayAdapter.add(item);
        	}
        }
	}
    
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch(item.getItemId()) {
		case ID_DELETE:	
			// the position in the arraylist is also the position in initItems
			String[] initItems = Activities.getActivityInitSequence(activityName, prefs);
			// copy the initItems into a new List<String> that excludes (int)(info.id)
			// then addActivityInitSequence(String key, List<String> init) needs to be modified to be static
			// and used to add the newly created inititems to replace the old....
			int ignoreIndex = (int)info.id;
			ArrayList<String> newItems = new ArrayList<String>();
			for (int i=0; i < initItems.length; i++) {
				if (i != ignoreIndex) {
					newItems.add(initItems[i]);
				}
			}
			
			// now replace the old init items with the new
			Activities.addActivityInitSequence(activityName, newItems, prefs, lookup);
			
			populateDisplay();
			return true;
		case ID_CHANGE:			
            // changing of delay routine
			itemSelected = (int)info.id;
			// prompt user for new number
			showDialog(DIALOG_INIT_DELAY);			
			
			populateDisplay();
			
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.activity_edit_list) {
			// extract exact item that was selected in the list
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;	
			if (activityArrayAdapter.getItem(info.position).startsWith("DELAY") ) {
				// display DELAY modification item
				menu.add(0, ID_CHANGE, 0, "Change Delay");
				menu.setHeaderTitle("DELAY MENU");
			}
			else {
				menu.setHeaderTitle("MENU");
			}
			
			menu.add(0, ID_DELETE, 0, "Delete Item");
			
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		AlertDialog alert = null;
		AlertDialog.Builder builder;
		
		switch (id) {
			
		case DIALOG_INIT_DELAY:
			// create a custom alertdialog using our xml interface for it	
			
			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.dialog_init_delay,
			                               (ViewGroup) findViewById(R.id.dialog_init_root));
			final EditText text = (EditText) layout.findViewById(R.id.enter_init_dly);
			builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			builder.setTitle("Enter Delay")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   try {
			        		   if (Integer.parseInt(text.getText().toString()) > 0) {
			        			   // update item with new number			        			   
			        			   updateInitDelay(Integer.parseInt(text.getText().toString()));			        			   
			        		   }
			        	   }
			        	   catch (NumberFormatException e) {
			        		   // do nothing, just quit
			        		   Toast.makeText(ActivityInitEdit.this, "Invalid number, ignoring...",
			       					Toast.LENGTH_SHORT).show();
			        	   }
			        	   dialog.dismiss();
			           }
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			    });
			alert = builder.create();
			return alert;
			
		default:
			return alert;
		}
	}
	
	/**
	 * Update the initialization delay
	 * @param newDelay the new delay in milli-seconds
	 */
	private void updateInitDelay(int newDelay) {			
		// the position in the activityArrayAdapter is also the position in initItems
		ArrayList<String> initItems = new ArrayList<String>();
		for (int i=0; i< activityArrayAdapter.getCount(); i++) {
			if (i == itemSelected) {
				// if this is the item that we wanted to change, then change item to DELAY + newDelay
				initItems.add("DELAY "+Integer.toString(newDelay));
			}
			else {
				initItems.add(activityArrayAdapter.getItem(i));
			}
		}
		
		// replace old activity init items with the new ones
		Activities.addActivityInitSequence(activityName, initItems, prefs, lookup);
		populateDisplay();
	}
}

// Copyright (C) 2011 Woelfware

package com.woelfware.database;

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.woelfware.blumote.ButtonData;
import com.woelfware.blumote.MainInterface;

/**
 * Helper class to manage operating on the SQLite database
 * @author keusej
 *
 */
public class DeviceDB {
	private SQLiteDatabase db;
	private final Context context;
	private final MyDBhelper dbhelper;	
	
	private static final String TAG = "DeviceDB";
	
	public final static String DB_NAME = "/data/com.woelfware.blumote/databases/"+Constants.DATABASE_NAME;
	
	// Object for intrinsic lock of device DB
	public static final Object[] sDataLock = new Object[0];
	
	public DeviceDB(Context c){
		context = c;
		dbhelper = new MyDBhelper(context, Constants.DATABASE_NAME, null,
				Constants.DATABASE_VERSION);
	}
	
	public void close()
	{
		db.close();
	}
	
	public void open() throws SQLiteException
	{
		try {
			db = dbhelper.getWritableDatabase();
		} catch(SQLiteException ex) {
			Log.v("Open database exception caught", ex.getMessage());
		}
	}
	
	// returns true on success, false otherwise
	public boolean restore() 
	{
		try {
			synchronized (DeviceDB.sDataLock) {
				dbhelper.importDatabase("device_data.bak");
				open();
				return true;
			}
		} catch (IOException e) {
			Log.v("restore database exception caught", e.getMessage());
			return false;
		}
	}
	
	/**
	 * Inserts a new button into the database
	 * @param curTable the table name which is typically the device name
	 * @param buttonID the name of the button that we want to insert into the table
	 * @param content the button data (IR code) associated with the button
	 * @return the row ID of the database, this return value is not currently used
	 */
	public long insertButton(String curTable, String buttonID, byte[] content)
	{	
		curTable = deviceNameFormat(curTable);
		Cursor c = null;
		
		try {
			synchronized (DeviceDB.sDataLock) {
				c = db.query(curTable, null, Constants.DB_FIELDS.BUTTON_ID.getValue()+"='"+buttonID+"'",
						null, null, null, null);
			}
			if (c!=null && c.getCount() > 0) { // then we already have this entry so call updateButton
				updateButton(curTable, buttonID, content);
				return -1;
			}
			else { // this must be a new entry , so try to insert it
				try{
					synchronized (DeviceDB.sDataLock) {
						ContentValues newTaskValue = new ContentValues();
						newTaskValue.put(Constants.DB_FIELDS.BUTTON_ID.getValue(), buttonID);
						newTaskValue.put(Constants.DB_FIELDS.BUTTON_DATA.getValue(), content);
	//					db = dbhelper.getWritableDatabase();
						return db.insertOrThrow(curTable, null, newTaskValue);
					}
				} catch(SQLiteException ex) {
					Log.v("Insert into database exception caught", ex.getMessage());
					return -1;
				}
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return -1;
		}
	}
	
	/**
	 * This should return all the buttons of a particular device selection
	 * @param curTable the name of the device that we want to extract the button data for
	 * @return the ButtonData[] which contains the properties for each button in the DB
	 */
	public ButtonData[] getButtons(String curTable)
	{
		curTable = deviceNameFormat(curTable);
		Cursor c = null;
		try {
			synchronized (DeviceDB.sDataLock) {
				c = db.query(curTable, null, null,
					null, null, null, null);
			}
		} catch (Exception e) { 
			//error, so don't return anything 
		}		
		ButtonData[] buttons = null;		
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			buttons = new ButtonData[c.getCount()];
			// iterate through cursor to load up buttons array
			for (int i= 0; i < buttons.length; i++) {
				try {
					buttons[i] = new ButtonData(0, c.getString(1), c.getBlob(2));
				} catch (Exception e) {
					e.printStackTrace();
				}
				c.moveToNext();
			}
		}
		return buttons;
	}
	
	/**
	 * This should return the data for one of the buttons of a particular device selection
	 * @param device the name of the device which is also the table of the database
	 * @param buttonID the name of the button that we want to get the data for
	 * @return the byte[] for the IR codes associated with a button
	 */
	public byte[] getButton(String device, String buttonID)
	{
		device = deviceNameFormat(device);
		Cursor c = null;
		
		byte[] button;
		try {
			synchronized (DeviceDB.sDataLock) {
				c = db.query(device, null, Constants.DB_FIELDS.BUTTON_ID.getValue()+"='"+buttonID+"'",
						null, null, null, null);
			}
			if (c != null) {
				c.moveToFirst();
				button = c.getBlob(c.getColumnIndex(Constants.DB_FIELDS.BUTTON_DATA.getValue()));
				//button = c.getString(c.getColumnIndex(Constants.BUTTON_DATA));
				return button;
			} 
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return null;
		}
		return null;
	}
	
	/**
	 * Add a new device to the database
	 * @param table the name of the device, which is also the table name in the DB
	 * @param buttonConfig The name of the button configuration to use for this device
	 * @return 1 if successful, 0 if there was an error and 2 if a duplicate exists
	 */
	public int addDevice(String table, String buttonConfig) {
		try {
			synchronized (DeviceDB.sDataLock) {
				// check that the master database even exists, if not create it 
				// Alternate way:
				//		SELECT name FROM sqlite_master WHERE type='table' AND name='table_name';
				String TABLE; // used to build SQL commands
				TABLE = "create table if not exists "+ Constants.DEVICES_TABLE +" ("+
						Constants.KEY_ID+" integer primary key autoincrement, "+
						Constants.DB_FIELDS.DEVICE_ID.getValue()+" text not null, "+
						Constants.DB_FIELDS.DEVICE_CATEGORY.getValue()+" text, "+
						Constants.DB_FIELDS.BUTTON_REPEAT_TIMES.getValue()+" integer, "+
						Constants.DB_FIELDS.DEVICE_MAKE.getValue()+" text, "+
						Constants.DB_FIELDS.DEVICE_MODEL.getValue()+" text, "+
						Constants.DB_FIELDS.REMOTE_MODEL.getValue()+" text, "+
						Constants.DB_FIELDS.DELAY.getValue()+" text, "+
						Constants.DB_FIELDS.CONFIG.getValue()+" text, "+
						Constants.DB_FIELDS.BUTTON_CONFIG.getValue()+" text, "+
						Constants.DB_FIELDS.MODE.getValue()+" text"+
				");";
				try {
					db.execSQL(TABLE);
				} catch(SQLiteException ex) {
					Log.v("Create table exception", ex.getMessage());
				}
				// now see if the table is in the DEVICES_TABLE, if not add it
				// currently there are a lot of parameters that can be setup for the 
				// device properties, but most of these are all optional and unused at this time
				// Note: table is sent in verbatim, so spaces are spaces, etc
				Cursor c = db.query(Constants.DEVICES_TABLE, null, 
							Constants.DB_FIELDS.DEVICE_ID.getValue()+"='"+table+"'",
							null, null, null, null);
				if (c == null || c.getCount() == 0) { // if null then not present in table already
					try{
						ContentValues newTaskValue = new ContentValues();
						newTaskValue.put(Constants.DB_FIELDS.DEVICE_ID.getValue(), table);
						newTaskValue.put(Constants.DB_FIELDS.BUTTON_CONFIG.getValue(), buttonConfig);
						db.insertOrThrow(Constants.DEVICES_TABLE, null, newTaskValue);
					} catch(SQLiteException ex) {
						Log.v("Insert into "+Constants.DEVICES_TABLE+" exception caught", ex.getMessage());				
					}
				}
				
				// format table name so it is suitable for usage as a table name
				table = deviceNameFormat(table);
				Log.v("MyDB createTable","Creating table");
				TABLE="create table "+ table +" ("+
						Constants.KEY_ID+" integer primary key autoincrement, "+
						Constants.DB_FIELDS.BUTTON_ID.getValue()+" text not null, "+
						Constants.DB_FIELDS.BUTTON_DATA.getValue()+" text not null"+				
						");";
				try {
					db.execSQL(TABLE);
					return 1;
				} catch(SQLiteException ex) {
					Log.v("Create table exception", ex.getMessage());
					return 0;
					// TODO - add check for duplicate table?
				}
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return 0;
		}
	}
	
	/**
	 * Add a new device to the database
	 * @param table the name of the device, which is also the table name in the DB
	 * @return 1 if successful, 0 if there was an error and 2 if a duplicate exists
	 */
	public int addDevice(String table) {
		// use default 'main' button layout
		return addDevice(table, MainInterface.DEVICE_LAYOUTS.MAIN.getValue());
	}
	
	/**
	 * Removes a device from the database, all button codes are destroyed
	 * @param table the name of the device to delete
	 */
	public void removeDevice(String table) {
		try {
			synchronized (DeviceDB.sDataLock) {
				try {
					db.delete(Constants.DEVICES_TABLE, Constants.DB_FIELDS.DEVICE_ID.getValue() + 
			        		"='" + table + "'", null);
				} catch (SQLiteException ex) {
					Log.v("Remove from "+Constants.DEVICES_TABLE+" exception", ex.getMessage());
				}
				
				table = deviceNameFormat(table);
				
				try {
					db.execSQL("drop table if exists "+table);
				} catch (SQLiteException ex) {
					Log.v("Remove table exception", ex.getMessage());
				}
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
		}
	}

	/**
	 * Renames a device in the database
	 * @param table the existing name
	 * @param rename the new name
	 */
	public void renameDevice(String table, String rename) {
		try {
			synchronized (DeviceDB.sDataLock) {
				try {
					ContentValues args = new ContentValues();
			        args.put(Constants.DB_FIELDS.DEVICE_ID.getValue(), rename);
			        db.update(Constants.DEVICES_TABLE, args, 
			        			Constants.DB_FIELDS.DEVICE_ID.getValue() + "='" + table +"'", null);
				} catch (SQLiteException ex) {
					Log.v("Remove from "+Constants.DEVICES_TABLE+" exception", ex.getMessage());
				}
				
		        table = deviceNameFormat(table);		
				rename = deviceNameFormat(rename);
				try {
					db.execSQL("ALTER TABLE "+table+" RENAME TO "+rename);
				} catch (SQLiteException ex) {
					Log.v("Rename table exception", ex.getMessage());
				}	
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
		}
	}
	
	/**
	 * Get the button configuration for the device
	 * @param name name of device
	 * @return the button configuration name
	 */
	public String getButtonConfig(String name) {
		try {
			synchronized (DeviceDB.sDataLock) {
				Cursor c = db.query(Constants.DEVICES_TABLE, new String[] { 
						Constants.DB_FIELDS.DEVICE_ID.getValue(), Constants.DB_FIELDS.BUTTON_CONFIG.getValue()
						}, Constants.DB_FIELDS.DEVICE_ID.getValue()+"='"+name+"'", null, null,null, null);
				if (c != null && c.getCount() > 0) {
					c.moveToFirst();			
					return c.getString(1); // return the button config name stored in DB
				} else {
					return MainInterface.DEVICE_LAYOUTS.BLANK.getValue(); // else return default interface
				}
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return null;
		}
	}
	
	/**
	 * Returns all the devices that are currently being stored in the database
	 */
	public String[] getDevices() {		
		try {
			synchronized (DeviceDB.sDataLock) {
				try {		
					// query all the device_table rows of the device_id column
					Cursor c = db.query(Constants.DEVICES_TABLE, new String[] { 
							Constants.DB_FIELDS.DEVICE_ID.getValue()
							}, null, null, null,null, null);
					if (c != null && c.getCount() > 0) { 
						ArrayList<String> results = new ArrayList<String>();
						c.moveToFirst();		
						while (c.isAfterLast() != true) {
							results.add(c.getString(0));
							c.moveToNext();
						}	
						
						String[] returnValue = new String[results.size()];
						// convert arraylist to a string[]
						for (int i=0 ; i< results.size(); i++) {
							returnValue[i] = results.get(i);
						}
						return returnValue;
					}
					else {
						return null;
					}
				} catch (Exception ex) {
					Log.v("List tables exception", ex.getMessage());
					return null;
				}	
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return null;
		}
	}
	
	/**
	 * Deletes a button from the database
	 * @param curTable the device name that has the button
	 * @param buttonID the button that we want deleted
	 * @return true if successful, false if not
	 */
    public boolean deleteButton(String curTable, String buttonID) 
    {
    	try {
			synchronized (DeviceDB.sDataLock) {
		    	curTable = deviceNameFormat(curTable);
		        return db.delete(curTable, Constants.DB_FIELDS.BUTTON_ID.getValue() + 
		        		"='" + buttonID + "'", null) > 0;
			}
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return false;
		}
    }
    
    /**
     * Updates a button with new data (IR code)
     * @param curTable the name of the device
     * @param buttonID the button name
     * @param content the code associated with the button
     */
    private boolean updateButton(String curTable, String buttonID, byte[] content) 
    {
    	curTable = deviceNameFormat(curTable);
    	
    	// DEBUG var
    	boolean debug;
        ContentValues args = new ContentValues();
        args.put(Constants.DB_FIELDS.BUTTON_DATA.getValue(), content);
        debug = db.update(curTable, args, 
                  Constants.DB_FIELDS.BUTTON_ID.getValue() + "='" + buttonID+"'", null) > 0;
        return debug;
    }
    
    /**
     * Formats a name for usage as a table in a database, specifies that the table
     * should be used explicitly and puts '[ ]' around it. 
     * @param deviceName
     * @return
     */
    private String deviceNameFormat( String deviceName ) {
    	if (deviceName.startsWith("[")) {
    		return deviceName;
    	}
    	return "["+deviceName+"]";    		
    }
}
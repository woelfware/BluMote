// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * This class manages keeping unique IDs associated with each device or activity
 * that is used on the interface.  These IDs are useful for providing an easy way to link
 * button data with other button data.  For example, an activity button is always associated
 * with a device button, the ID is what is used to link the buttons.  This is important for data
 * that is persisted in the prefs file that may change (like the name of a device) if it is renamed.
 * @author keusej
 *
 */
class InterfaceLookup {
	private static String ID_INDEX = "ID_INDEX";
	private static String ID_PREFIX = "INTEGERID_";
	private static int INVALID_ID = -1;
	Editor mEditor;
	SharedPreferences prefs;
	int highestId;
	
	// This HashMap will convert an integer id to a string name, this name is always kept up to date
	// when the activity or device it represents is renamed
	HashMap<Integer, String> lookup = new HashMap<Integer, String>();
	
	/**
	 * public constructor, will initialize the local list of ID to device name mappings
	 * @param prefs the SharedPreferences object that has all the persistent data
	 */
	InterfaceLookup(SharedPreferences prefs) {
		mEditor = prefs.edit();
		this.prefs = prefs;		
		
		// first make sure that a ID_INDEX exists, if not create one and start it at 0
		highestId = prefs.getInt(ID_INDEX, INVALID_ID);
		if (highestId == INVALID_ID) {
			// then initialize it
			mEditor.putInt(ID_INDEX, 0);
			mEditor.putInt("test", 100);
			highestId = 0;
			mEditor.commit();
		} else { // if we have an ID_INDEX then we can now search for any existing devices with IDs
			refreshLookup();
		}				
	} // end of constructor
	
	/**
	 * Always called during construction of an InterfaceLookup object.
	 * If the lookup structure may have been modified from another InterfaceLookup structure
	 * then this method can be called to update the lookup structure.  This should always be called when
	 * returning from another activity that may have its own instance of InterfaceLookup.
	 */
	void refreshLookup() {
		Map<String,?> values = prefs.getAll();
		if (values.isEmpty() != true) {
			int id;
			// iterate through these values
			for (String item : values.keySet()) {
				// check if prefix is an activity
				if (item.startsWith(ID_PREFIX)) {									
					// then we have a new ID, so add it to the hashmap
					id = prefs.getInt(item, INVALID_ID);
					if (id != INVALID_ID) {
						item = formatNameForLookup(item); // remove prefix
						item = item.replace("_", " ");
						lookup.put(id, item);
					}
				}
			}
		}			
	}
	
	/**
	 * Used to update the ID with the new name or add a new lookup id,
	 * by convention the name should match what is displayed in the drop-down
	 * @param id 
	 * @param name
	 */
	void updateLookupName(String newName, String oldName) {
		// if id is not found, then create a new one and increment highest count		
		oldName = formatNameForPrefsFile(oldName);
		newName = formatNameForPrefsFile(newName);
		
		// traverse lookup to find the id of the oldName
		int id = getIDNumber(oldName);
		
		if (id != INVALID_ID) {
			mEditor.remove(oldName);
			mEditor.putInt(newName, id);
			newName = formatNameForLookup(newName);
			lookup.put(id, newName); // add to hashmap without prefix
		} else {
			// we didn't find the original, ID, so just add it with a new ID
			id = getNewID();
			mEditor.putInt(newName, id);
			newName = formatNameForLookup(newName);
			lookup.put(id, newName); // add to hashmap without prefix
		}

		mEditor.commit();
	}
	
	/**
	 * Update a lookup name by the ID if known
	 * @param name new name
	 * @param id ID of the item
	 */
	void updateLookupID(String name, int id) {
		name = formatNameForLookup(name);
		lookup.put(id, name); // add to hashmap without prefix
		
		name = formatNameForPrefsFile(name);			
		mEditor.putInt(name, id);
		mEditor.commit();
	}
	
	/**
	 * adds a new lookup ID to a name
	 * @param newName
	 * @return the id that was assigned
	 */
	int addLookupId(String name) {
		int id = getNewID();
		name = formatNameForLookup(name);
		lookup.put(id, name); // add to hashmap without prefix
		
		name = formatNameForPrefsFile(name);
		mEditor.putInt(name, id);
		mEditor.commit();
		return id;
	}
	
	/**
	 * deletes a lookup ID associated with a name
	 * @param name the name to delete
	 */
	void deleteLookupId(String name) {
		name = formatNameForLookup(name);
		
		int id = getIDNumber(name);
		lookup.remove(id);
		
		name = formatNameForPrefsFile(name);			
		mEditor.remove(name);
		mEditor.commit();
	}
	
	/**
	 * Issues a new ID, writes the new highestId out to the prefs file
	 */
	private int getNewID() {
		highestId = prefs.getInt(ID_INDEX, INVALID_ID);
		highestId++;
		if (highestId >= (Integer.MAX_VALUE-1)) {
			highestId = 0; // reset if we ever get this high up
		}
		mEditor.putInt(ID_INDEX, highestId);
		mEditor.commit();
		return highestId;
	}
	
	private String formatNameForPrefsFile(String name) {
		name = name.replace(" ", "_");
		
		if (name.startsWith(ID_PREFIX)) {			
			return name;
		} else {
			return ID_PREFIX + name; 
		}			
	}
	
	private String formatNameForLookup(String name) {
		if (name.startsWith(ID_PREFIX)) {			
			return name.replaceFirst(ID_PREFIX, "");
		} else {
			return name; 
		}			
	}
	
	/**
	 * Used to retrieve the name associated with the ID.
	 * @param id
	 * @return the name associated with the id or null if not found
	 */
	String getName(int id) {
		return lookup.get(id);
	}
	
	/**
	 * Used to retrieve the name associated with the ID.
	 * @param id the string representation of the ID
	 * @return the name associated with the id or null if not found
	 */
	String getName(String id) {
		try {
			return lookup.get(Integer.parseInt(id));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Gets the ID associated with the Name
	 * @param name
	 * @return returns the ID if found, otherwise null
	 */
	String getID(String name) {
		name = formatNameForLookup(name);
		int id = getIDNumber(name);
		
		if (id == INVALID_ID) {
			return null;
		} else {	
			return String.valueOf(id);
		}
	}
	
	/**
	 * Gets the ID associated with the Name
	 * @param name
	 * @return returns the ID if found, otherwise INVALID_ID
	 */
	private int getIDNumber(String name) {
		name = formatNameForLookup(name);
		// traverse lookup to find the id of the oldName
		int id = INVALID_ID;
		String data;
		for (int item : lookup.keySet()) {
			data = lookup.get(item);
			if (data.equals(name)) {
				// found it , so extract the ID
				id = item;
				break;
			}
		}
		return id;
	}
}

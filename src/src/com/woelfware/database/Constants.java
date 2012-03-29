// Copyright (C) 2011 Woelfware

package com.woelfware.database;

/**
 * Helper class to hold all the database properties
 * @author keusej
 *
 */
public class Constants {
	// database properties
	public static final String DATABASE_NAME="device_data";
	public static final int DATABASE_VERSION=1;
	public static final String TABLE_NAME="devices";
	public static final String KEY_ID="_id";
	public static final String DEVICES_TABLE="devices_table";
	
	public enum CATEGORIES {
		TV_DVD("tv-dvd"), ACTIVITY("activity");
		private final String field;
		CATEGORIES(String field) {
			this.field = field;
		}
		public String getValue() {
			return field;
		}
	}
	
	public enum DB_FIELDS {
		BUTTON_ID("button_id"), 
		BUTTON_DATA("button_data"), 		
		// the following fields are for the whole device, not per button.
		// there is a second table used to deal with per-device parameters
		DEVICE_ID("device_id"), // must be equal to the button data table name of associated device
		DEVICE_CATEGORY("device_category"),
		BUTTON_REPEAT_TIMES("repeat_times"), 
		DEVICE_MAKE("device_make"), 
		DEVICE_MODEL("device_model"), 
		REMOTE_MODEL("remote_model"), 
		DELAY("delay"), 
		CONFIG("config"), 
		MODE("mode"), 
		BUTTON_CONFIG("button_config");
		private final String field;
		DB_FIELDS(String field) {
			this.field = field;
		}
		public String getValue() {
			return field;
		}
	}	 

	// Other constants
}
// Copyright (C) 2011 Woelfware

package com.woelfware.database;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Environment;

import com.woelfware.blumote.Util;

public class MyDBhelper extends SQLiteOpenHelper{

	public MyDBhelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {

	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
	}
	
	/**
	 * Copies the database file at the specified location over the current
	 * internal application database.
	 * */
	public boolean importDatabase(String backupDBName) throws IOException {

	    // Close the SQLiteOpenHelper so it will commit the created empty
	    // database to internal storage.
	    close();
	    File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        String currentDBPath = DeviceDB.DB_NAME;        
        
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBName);
        	  
	    if (backupDB.exists()) {
	        Util.FileUtils.copyFile(backupDB, currentDB);
	        // Access the copied database so SQLiteHelper will cache it and mark
	        // it as created.
	        getWritableDatabase().close();
	        return true;
	    }
	    return false;
	}
}
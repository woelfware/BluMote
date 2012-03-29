// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.io.File;
import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.woelfware.database.DeviceDB;

/**
 * Will asynchronously backup the database file to SDCARD
 * @author keusej
 *
 */
class ExportDatabaseFileTask extends AsyncTask<String, Void, Boolean> {
    private ProgressDialog dialog;
    private Context ctx;
    
    public static final String BACKUP_DB_NAME = "device_data.bak";
    
    protected ExportDatabaseFileTask(Context ctx) {
    	this.ctx = ctx;
    	dialog = new ProgressDialog(ctx);
    }
    
    // can use UI thread here
    protected void onPreExecute() {
       this.dialog.setMessage("Exporting database...");
       this.dialog.show();
    }

    // automatically done on worker thread (separate from UI thread)
    protected Boolean doInBackground(final String... args) {
    	// TODO getExternalFilesDir() should be used for data backup location!
    	File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        
        if (sd.canWrite()) {        	
        	if (!sd.exists()) {
            	sd.mkdirs();
            }
        	
        	String currentDBPath = DeviceDB.DB_NAME;
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, BACKUP_DB_NAME);
            	            
            if (currentDB.exists()) {
                try {
                	backupDB.createNewFile();
                	Util.FileUtils.copyFile(currentDB,backupDB);                	
                } catch (IOException e) {
                	Log.e("BACKUP",e.getMessage(),e);           
                	Toast.makeText(ctx, "Export failed", Toast.LENGTH_SHORT).show();
            		return false;  
                }
            }                        
        }
        return true;
    }

    // can use UI thread here
    protected void onPostExecute(final Boolean success) {
       if (this.dialog.isShowing()) {
          this.dialog.dismiss();
       }
       if (success) {
          Toast.makeText(ctx, "Export successful!", Toast.LENGTH_SHORT).show();
       } else {
          Toast.makeText(ctx, "Export failed", Toast.LENGTH_SHORT).show();
       }
    }
 }

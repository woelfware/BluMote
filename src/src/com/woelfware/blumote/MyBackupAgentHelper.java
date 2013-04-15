package com.woelfware.blumote;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.woelfware.database.DeviceDB;

public class MyBackupAgentHelper extends BackupAgentHelper {
	// The name of the SharedPreferences file
    static final String PREFS = BluMote.PREFS_FILE;
    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";
    
    static final String DATABASE = DeviceDB.DB_NAME;
    static final String DATABASE_BACKUP_KEY = "database";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
    	Log.v("BluMote_Backup", "in onCreate");
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
        FileBackupHelper filehelper = new FileBackupHelper(this,DATABASE);
        addHelper(DATABASE_BACKUP_KEY, filehelper);
    }
    
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
              ParcelFileDescriptor newState) throws IOException {
    	Log.v("BluMote_Backup", "in onCreate");
        // Hold the lock while the FileBackupHelper performs backup
        synchronized (DeviceDB.sDataLock) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
    	Log.v("BluMote_Backup", "in onCreate");
        // Hold the lock while the FileBackupHelper restores the file
        synchronized (DeviceDB.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }
}

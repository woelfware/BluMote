// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Simple preferences framework instance
 * @author keusej
 *
 */
public class MyPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);			
	}
	
}

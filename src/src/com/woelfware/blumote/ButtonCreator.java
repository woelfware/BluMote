// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import android.widget.ImageButton;

/**
 * Defines a constructor and three member functions
 * implement this interface for each unique screen button configuration
 * The MainInterface class will reference these interface methods
 * @author keusej
 *
 */
public interface ButtonCreator {
	abstract public ButtonParameters[] getButtons(BluMote blumote);
	abstract public ImageButton getPowerOnBtn();
	abstract public ImageButton getPowerOffBtn();
}
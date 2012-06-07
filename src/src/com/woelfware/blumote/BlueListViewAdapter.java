package com.woelfware.blumote;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class BlueListViewAdapter extends ArrayAdapter<String> {
	private String tag = "";
	
    public BlueListViewAdapter(Context context, int textViewResourceId, String newTag) {
            super(context, textViewResourceId);
            tag = newTag;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	  View view = super.getView(position, convertView, parent);
	  String item = super.getItem(position);
	  if (item.startsWith(tag)) {
		  view.setBackgroundColor(Color.BLUE);
	  } else {
		  view.setBackgroundColor(Color.TRANSPARENT);
	  }
	  
	  return view;
	}
}

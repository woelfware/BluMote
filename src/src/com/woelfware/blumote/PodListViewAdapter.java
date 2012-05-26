package com.woelfware.blumote;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class PodListViewAdapter extends ArrayAdapter<String> {
	
    public PodListViewAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	  View view = super.getView(position, convertView, parent);
	  String item = super.getItem(position);
	  if (item.startsWith("BluMote")) {
		  view.setBackgroundColor(Color.BLUE);
	  } else {
		  view.setBackgroundColor(Color.TRANSPARENT);
	  }
	  
	  return view;
	}
}

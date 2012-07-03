// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This activity is launched from the main BluMote interface. 
 * This activity lists FW releases and downloads them
 * @author keusej
 *
 */
public class FwUpdateActivity extends Activity implements OnItemClickListener {	
	private FwArrayAdapter fwImagesArrayAdapter;
    ListView fwImagesListView;
	
    String[] fwImages;
    
    DownloadManager manager = new DownloadManager();

    public static final String FW_IMAGES = "FW_IMAGES";
    // constants for showDialog()
    private static final int FW_DOWNLOAD_DIALOG = 0;
    private static final int OLD_FW_DIALOG = 1;

    static final String FW_LOCATION = "FW_LOCATION";
    static final String ORIGINAL_FW_LOCATION = "ORIGINAL_FW_LOCATION";
    static final String POD_WORKING = "POD_WORKING";
    
    // name of file when stored in the sdcard temp directory
    private static final String FW_IMAGE_NAME = "fwImage.bin";
    private static final String ORIGINAL_FW_IMAGE_NAME = "originalFwImage.bin";

    // progress dialog used for flashing code to pod and downloading FW
    ProgressDialog progressDialog;
    ProgressDialog progressDialog2;

    String podV; // the pod FW version , gets set to "" if not discovered
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	// Setup the window
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	setContentView(R.layout.fw_manage); 

    	// we got this after we downloaded the file from the net
    	// which was requested by BluMote
    	fwImages = Pod.firmwareRevisions; 

    	// Initialize array adapters
    	fwImagesArrayAdapter = new FwArrayAdapter(this, R.layout.fw_images_item); 

    	fwImagesListView = (ListView) findViewById(R.id.fw_images_list); 
    	fwImagesListView.setAdapter(fwImagesArrayAdapter);
    	fwImagesListView.setOnItemClickListener(this);
    	populateDisplay();             

    	// since we know that GET_VERSION was called prior to this activity, we can
    	// construct the version data from the Pod class
    	StringBuilder podVersion = new StringBuilder();
    	Intent i = getIntent();
    	try {
    		byte[] version = Pod.getFwVersion();
	    	podVersion.append(version[1] + ".");
	    	podVersion.append(version[2] + ".");
	    	podVersion.append(version[3]);
	    	podV = podVersion.toString();	    	
	    	i.putExtra(FwUpdateActivity.POD_WORKING, true);	    	
    	} catch (Exception e) {
    		i.putExtra(FwUpdateActivity.POD_WORKING, false);
    		podV = "";
    		// if we weren't able to get a FW version then we won't be able
    		// to get the calibration data so safer to let user re-try with existing
    		// FW we downloaded if it exists already
    		File fileDir = FwUpdateActivity.this.getExternalFilesDir(null);
    		File f = new File(fileDir,FW_IMAGE_NAME);
    		if (f.exists()) {
    			showDialog(OLD_FW_DIALOG);
    		}    		
    		
    	}
    	// indicate the version in the list that matches podVersion
    	for (int j=0 ; j < fwImagesArrayAdapter.getCount(); j++) {
    		FwItem test = fwImagesArrayAdapter.getItem(j);
    		if (test.title.equalsIgnoreCase(podV)) {    		    			 
    			test.installed = true;
//    			fwImagesListView.getChildAt(j).setBackgroundColor(Color.BLUE);
    		} else {
    			test.installed = false;
//    			fwImagesListView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
    		}
    	}    	
    	
    	// set to OK only after data all downloaded and ready to flash    	
    	setResult(RESULT_CANCELED,i);        
	}

	/**
	 * Populate the display with all the fw images
	 */
	private void populateDisplay() {
        fwImagesArrayAdapter.clear(); // always clear before adding items
        
        if (fwImages != null && fwImages.length > 0) {
        	// iterate through these values
        	for (String item : fwImages) {	
        		if (item.equals("")) {
        			continue; // skip if its empty
        		}
        		// otherwise split based on csv format
        		String[] items = item.split(",");
        		try {
        			if (items.length >= 3) {
        				fwImagesArrayAdapter.add(
        					new FwItem(items[0], items[1], new URL(items[2]), items[3], false));
        			}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					Log.d("URL", "Failed to create URL, malformed");
				}
        	}
        }
	}
	@Override
	protected void onPrepareDialog(int id, Dialog d) {
		switch(id) {         
		case FW_DOWNLOAD_DIALOG:				
			ProgressDialog dialog = (ProgressDialog)d;	
			dialog.setProgress((int)0);			
            return;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		AlertDialog alert = null;
		AlertDialog.Builder builder;
		
		switch(id) {         
			case FW_DOWNLOAD_DIALOG:				
				progressDialog = new ProgressDialog(FwUpdateActivity.this);
	            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	            progressDialog.setCancelable(true); // allow back button to cancel it
	            progressDialog.setMessage("Downloading firmware image...");
	            return progressDialog;
	            
			case OLD_FW_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setMessage("It appears your pod is not responding " +
						"and a FW image already exists.  Press OK to use the FW image" +
						" already downloaded (REQUIRED)")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				    	   public void onClick(DialogInterface dialog, int id) {
				    		   Intent i = getIntent();
				    		   setResult(RESULT_OK,i);
				    		   File fileDir = FwUpdateActivity.this.getExternalFilesDir(null);
				    		   try {
				    			   	i.putExtra(FwUpdateActivity.FW_LOCATION, 
											new File(fileDir, FW_IMAGE_NAME).getCanonicalPath());
				    			   	File originalFile = new File(fileDir, ORIGINAL_FW_IMAGE_NAME);
									if (originalFile.exists()) {
										i.putExtra(FwUpdateActivity.ORIGINAL_FW_LOCATION,
												new File(fileDir, ORIGINAL_FW_IMAGE_NAME).getCanonicalPath());
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
				    		   finish();
				           }
				       });
				alert = builder.create();
				return alert;
				
			default:
				return null;
		}
	}
	   
	/**
	 * The on-click listener for all devices in the ListViews
	 * @param av
	 * @param v The View object of the listview
	 * @param position the position that was clicked in the listview
	 * @param id the resource-id of the listview
	 */
	public void onItemClick(AdapterView<?> av, View v, int position, long id) {		
		final FwItem item = fwImagesArrayAdapter.getItem(position);
		// prompt user if they want to download and install this version
		new AlertDialog.Builder(this)
        	.setIcon(android.R.drawable.ic_dialog_alert)
	        .setMessage(R.string.confirm_fw)
	        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {	               
	            	// run the download manager for the image
	        		manager.start(item);
	            }

	        })
	        .setNegativeButton(R.string.cancel, null)
	        .show();
		
	}
	
	private class DownloadManager {
		FwItem downloadItem; // new FW on pod
		FwItem originalItem = null; // original FW on pod
		
		DownloadManager() {
			// unused constructor
		}
		
		void start(FwItem item) {
			FwItem temp;
			
			// download the binary file
			this.downloadItem = item;
			
			for (int i=0; i< fwImagesArrayAdapter.getCount(); i++) {
				temp = fwImagesArrayAdapter.getItem(i);
				if (temp.title.equalsIgnoreCase(podV)) {
					// found a match
					originalItem = temp;
					break;
				}
			}						
						
			showDialog(FW_DOWNLOAD_DIALOG);
			// check if the original FW was detected, if so download both 
			// the original image and the new image
			if (originalItem != null) {
				DownloadBinaryFileTask downloader = new DownloadBinaryFileTask();
				downloader.execute(item.url, originalItem.url); 
			} else {
				// just download the new firmware
				DownloadBinaryFileTask downloader = new DownloadBinaryFileTask();
				downloader.execute(item.url);
			}
		}
		
		void loadFwImage(File fileDir) {
			dismissDialog(FW_DOWNLOAD_DIALOG);
			
			if ( originalItem != null ) {
				if ( !(Util.FileUtils.checkMD5(originalItem.md5sum, fileDir, ORIGINAL_FW_IMAGE_NAME)) ) {
					displayDownloadError(); // if we tried to download the original item and it failed
					return;
				}
			}
			// compare md5sum of downloaded file with expected value
			if (Util.FileUtils.checkMD5(downloadItem.md5sum, fileDir, FW_IMAGE_NAME) ) {
				// if it passes then begin loading process......
				// send BluMote the name of the downloaded file
				// and it will then start loading it to the pod
				Intent i = getIntent();
				
				try {
					File originalFile;
					//if (originalItem != null) {
					originalFile = new File(fileDir, ORIGINAL_FW_IMAGE_NAME);
					if (originalFile.exists()) {
						i.putExtra(FwUpdateActivity.ORIGINAL_FW_LOCATION,
								new File(fileDir, ORIGINAL_FW_IMAGE_NAME).getCanonicalPath());											
					}
					i.putExtra(FwUpdateActivity.FW_LOCATION, 
							new File(fileDir, FW_IMAGE_NAME).getCanonicalPath());

					setResult(RESULT_OK,i);
					finish();	
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				displayDownloadError();
			}
		}
	}
	
	private void displayDownloadError() {
		new AlertDialog.Builder(FwUpdateActivity.this)
        	.setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.error)
	        .setMessage(R.string.error_fw_download)
	        .setPositiveButton(R.string.OK, null)
	        .show();
	}

	/**
	 * Custom array adapter to allow for 2 types of text to be 
	 * on a list entry.  
	 * @author keusej
	 *
	 */
	public class FwArrayAdapter extends ArrayAdapter<FwItem> {
		Context context; 
		int layoutResourceId;    

		public FwArrayAdapter(Context context, int layoutResourceId) {
			super(context, layoutResourceId);
			this.layoutResourceId = layoutResourceId;
			this.context = context;     
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			FwTextHolder holder = null;

			if(row == null)
			{
				LayoutInflater inflater = ((Activity)context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new FwTextHolder();
				holder.txtNotes = (TextView)row.findViewById(R.id.fw_notes_label);
				holder.txtTitle = (TextView)row.findViewById(R.id.fw_version_label);

				row.setTag(holder);
			}
			else
			{
				holder = (FwTextHolder)row.getTag();
			}

			FwItem item = getItem(position);
			holder.txtTitle.setText(item.title);
			holder.txtNotes.setText(item.notes);

			if (item.installed) {
				row.setBackgroundColor(Color.BLUE);
			} else {
				row.setBackgroundColor(Color.TRANSPARENT);
			}
			
			return row;
		}

		class FwTextHolder
		{
			TextView txtNotes;
			TextView txtTitle;
		}
	}

	/**
	 * Holder class for data items in each arrayadapter entry
	 * @author keusej
	 *
	 */
	static class FwItem {
		public String title;
		public String notes;
		public URL url;
		public String md5sum;
		public boolean installed;

		public FwItem(){
			super();
		}

		public FwItem(String title, String notes, URL url, String md5sum, boolean installed) {
			super();
			this.notes = notes;
			this.title = title;
			this.url = url;
			this.md5sum = md5sum;
			this.installed = installed;
		}
	}

	/**
	 * Asynchronously downloads the firmware image from the web 
	 * @author keusej
	 *
	 */
	private class DownloadBinaryFileTask extends AsyncTask<URL, Integer, Integer> {   
		File fileDir = FwUpdateActivity.this.getExternalFilesDir(null);

		@Override 
		protected Integer doInBackground(URL... urls) {
			int byteCounter = 0;

			try {				
				for (int i=0 ; i < urls.length ; i++) { 
					byteCounter = 0;
					publishProgress(0); // start at 0
					URL url = urls[i];
					HttpURLConnection c = (HttpURLConnection) url.openConnection();
					c.setRequestMethod("GET");
					c.setDoOutput(true);
					c.connect();
					int count = c.getContentLength(); // get file size
					FileOutputStream f;
					if (i == 0) {
						f = new FileOutputStream(new File(fileDir,FW_IMAGE_NAME));
					} else {
						f = new FileOutputStream(new File(fileDir,ORIGINAL_FW_IMAGE_NAME));
					}
					InputStream in = c.getInputStream();
					byte[] buffer = new byte[1024];
					int len1 = 0;
					while ( (len1 = in.read(buffer)) > 0 ) {
						f.write(buffer, 0, len1);
						byteCounter += len1;
						publishProgress((int) ((byteCounter / (float) count) * 100));
					}
					c.disconnect();
					f.close();	
				}				
												
			} catch (Exception e) {}
			return new Integer(byteCounter);
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			// update progress bar
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer bytes) {
			manager.loadFwImage(fileDir);
			return;
		}
	}
}

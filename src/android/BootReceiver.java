package com.red_folder.phonegap.plugin.backgroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {  
	
	/*
	 ************************************************************************************************
	 * Overriden Methods 
	 ************************************************************************************************
	 */
	@Override  
	public void onReceive(Context context, Intent intent) {
		
		// Get all the registered and loop through and start them
		String[] serviceList = PropertyHelper.getBootServices(context);
		
		if (serviceList != null) {
			for (int i = 0; i < serviceList.length; i++)
			{
				try{
					//Bug Android Lollipop 5 https://github.com/Red-Folder/bgs-core/issues/18
					Intent serviceIntent = new Intent(context, Class.forName(serviceList[i]));         
					context.startService(serviceIntent);
				}catch(ClassNotFoundException ignored){
				}
			}
		}
	} 
	
} 

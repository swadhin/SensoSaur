package com.swadhinapp.sensosaur;

import java.io.PrintWriter;

//import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class SensoSaurLocationLeader{
	
	private static final int TIME_WINDOW_SIZE = 100;  // Taken 100 ms
	public static final String LOG_TAG = "SensoSaur_Debug_Info ";
	
	private LocationManager locationManager = null;
	private LocationListener locationListener_nw = null;
	private LocationListener locationListener_gps = null;
	
	Location lastLocation = null;
	private PrintWriter fileOut = null;
	boolean gpsEnabled = false;
	boolean nwEnabled = false;
	
	public SensoSaurLocationLeader( PrintWriter fOut ,LocationManager locMan)
	{
		fileOut =  fOut;
		locationManager = locMan;
		
		gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		nwEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		
		//Log.v(LOG_TAG,"nwEnabled : "+nwEnabled);
		if ( true == nwEnabled )
		{
			lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				
		}else
		{
			
		}
		
		// Define a listener that responds to location updates
			locationListener_nw = new LocationListener() {
		    public void onLocationChanged(Location location) {
		      // Called when a new location is found by the network location provider.
		    	//if(isBetterLocation(lastLocation,location) == true)
		    	//{
		    		makeUseOfNewLocation(location,0);
		    	//}
		    	
		    	lastLocation = location;   
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {
		    	
		    }

		    public void onProviderEnabled(String provider) {
		    	if ( null != fileOut )
		    	{
		    		fileOut.println("\nProvider Disabled: " + provider);
		    	}
		    }

		    public void onProviderDisabled(String provider) {
		    	if ( null != fileOut )
		    	{
		    		fileOut.println("\nProvider Enabled: " + provider);
		    	}
		    }
		  };

		// Define a listener that responds to location updates
		  	locationListener_gps = new LocationListener() {
			    public void onLocationChanged(Location location) {
			      // Called when a new location is found by the network location provider.
			    	//if(isBetterLocation(lastLocation,location) == true)
			    	//{
			    		makeUseOfNewLocation(location,1);
			    	//}
			    	
			    	lastLocation = location;  
			      //makeUseOfNewLocation(location);
			    }

			    public void onStatusChanged(String provider, int status, Bundle extras) {}

			    public void onProviderEnabled(String provider) {}

			    public void onProviderDisabled(String provider) {
			    	//locationManager.removeUpdates(this);
			    }
			  };		 
			
	}
	
	public void startLocating()
	{
		
		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener_nw);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener_gps);
		//Log.v(LOG_TAG,"in startLocating2");
	}
	
	public void stopLocating()
	{
		if( null != fileOut )
		{
			fileOut.close();
		}
	}
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TIME_WINDOW_SIZE;
	    boolean isSignificantlyOlder = timeDelta < -TIME_WINDOW_SIZE;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	public void makeUseOfNewLocation(Location location , int id){
		
		Log.v(LOG_TAG,"makeUse");
		if( id == 0)
		{
			//Wireless Network
			if ( null != fileOut )
			{
				 Log.v(LOG_TAG,"makeUseWireless");
				printLocation(location,fileOut,id);
			}
			
		}else if(id == 1){
			//GPS
			if ( null != fileOut )
			{
				Log.v(LOG_TAG,"makeUseGPS");
				printLocation(location,fileOut,id);
			}
		}else{
			//Do Nothing
		}
	}
	
	private void printLocation(Location location,PrintWriter fOut,int id) {
		
		long tim=System.nanoTime();
		String lStr = null;

		if( id == 0 )
		{
			lStr = " : Wireless ";
		}else{
			lStr = " : GPS ";
		}
		
		if (location == null)
		{
			//Log.v(LOG_TAG,Long.toString(tim)+lStr+": Location[unknown]");
			
			fOut.println(Long.toString(tim)+lStr+": Location[unknown]");
		}
		else
		{
			//Log.v(LOG_TAG,Long.toString(tim)+ lStr+": "+ location.toString());
			
			fOut.println(Long.toString(tim)+ lStr+": "+ location.toString());
		}
	}
}

package com.swadhinapp.sensosaur;

import java.io.PrintWriter;
//import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
//import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
//import android.os.PowerManager;
//import android.os.Handler;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import android.util.Log;

public class SensoSaurActivity extends Activity implements SensorEventListener,LocationListener {
	
	public static final String LOG_TAG = "SensoSaur_Debug_Info ";
	
	//GSM
	private String stateString = " N/A ";
	private String gsmStrengthVal = " N/A ";
	
	//Wifi Scanner
	private WifiManager wifiM = null;
	private WifiInfo wifiInfo = null;
	private String wifiStateString = "N/A. ";
	private BroadcastReceiver receiver = null;
	//private Handler handler = new Handler();
	
    /** Called when the activity is first created. */	
	private SensorManager mSensorManager;
	private List<Sensor> deviceSensors;
	private int numSensors = 0;
	private int[] avlSensors = new int[12];
	private SimpleDateFormat dtf= new SimpleDateFormat("dd.HH.mm.ss");
	//private BufferedWriter[] fOut = new BufferedWriter[12];
	private PrintWriter[] fOut = new PrintWriter[12];
	private PrintWriter  lmFout = null;
	
	private LinearLayout[] childLLArr = new LinearLayout[12];
	private LinearLayout[] parentLLArr = new LinearLayout[12];
	private ImageView[] imgViewsArr = new ImageView[12];
	private CheckBox[] chkBoxArr =  new CheckBox[12];
	private TextView[] txtViewsArr = new TextView[31];
	private ToggleButton togButton =  null;
	//private ToggleButton togButtonLoc = null;
	private Button landMarkRecord = null;
	//private SoundMeter sMeter = null;
	private SoundMeter sRecorder = null;
	
	
	//Different Sensors
	
	private Sensor mAcc;
	private Sensor mMag;
	private Sensor mGyro;
	private Sensor mProxy;
	private Sensor mLight;
	private Sensor mOrient; 
	private Sensor mTemp;
	private Sensor mPressure; 
	private Sensor mRotv;
	private Sensor mLacc;
	private Sensor mGravity;
	
	//Dialog Handler Function
	SensorInfoSelect onInfoClickHandler = null;
	
	private SharedPreferences app_preferences = null;
	
	//For Location
	private String bestProvider;
	private LocationManager locationManager = null;
	Location lastLocation = null;
	private PrintWriter locFileOut = null;
	private PrintWriter wifiFileOut = null;
	private PrintWriter gsmFileOut = null;
	boolean gpsEnabled = false;
	boolean nwEnabled = false;
	
	//GSM Strength Calculation
	TelephonyManager        Tel;
    PhoneStateListener    MyListener;
    
    //For making the screen awake
    //PowerManager pManager = null;
    //private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	    super.onCreate(savedInstanceState);
    	    setContentView(R.layout.main);
    	    
    	    //sMeter = new SoundMeter("/dev/null");
    	    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    	    deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            numSensors = deviceSensors.size();   	    
    		
            // Get the app's shared preferences
            app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
            
    	    //population
    	    populateGraphicalIDs();
    	    
    	    populateSensorIds();
    	    
    	    //initialization
    	    initSensors();
    	    
    	    //call back register
    	    registerListener();
    	    
    	    //dialogs
    	    dialogsCreationAndHandle();	 
    	    
    	    //Wake Lock Handling
    	    //pManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	    //wakeLock = pManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MakeTheScreenAlive");
    	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	    
    	    //Blocking Auto Orientation
    	    int current = getRequestedOrientation();
    	    // only switch the orientation if not in portrait
    	    if ( current != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ) {
    	        setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
    	    }
    	   
    	    //Wifi Initialization
    	    //Setup
    	    wifiM = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    	    //Current Status
    	    wifiStatusUpdate();
    	    // Register Broadcast Receiver
    		if (receiver == null)
    			receiver = new BroadcastReceiver(){
    			@Override
    			  public void onReceive(Context c, Intent intent) {
    			    List<ScanResult> results = wifiM.getScanResults();
    			    long tim = System.nanoTime();
    			    
    			    String textScanWifiResults = "\n******Scanned Results******\n";
    			    
    			    ScanResult bestSignal = null;
    			    if( null != results )
    			    {
    			    	for (ScanResult result : results) {
    			    		textScanWifiResults += "SysTime = "+Long.toString(tim)+" "+result.toString()+"  \n";
    			    		if (bestSignal == null
    			    				|| WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
    			    			bestSignal = result;
    			    }
    			    }
    			    String message = "";
    			    if ( null != bestSignal)
    			    {
    			    	message = String.format("%s networks found. %s is the strongest.",
    			    			results.size(), bestSignal.SSID);
    			    }
    			    
    			    //Connecting to the strongest AP
    			    //addNewAccessPoint( bestSignal ); //Had to be changed
    			    
    			    if ( null != wifiFileOut )
    			    {
    			    	wifiFileOut.println(textScanWifiResults + "\n");
    			    	wifiFileOut.println(message + "\n");
    			    }
    			    
    			    //Updating the WiFi Connection Info
    			    //wifiStatusUpdate();
    			    if ( null != bestSignal )
    			    {
    			    	txtViewsArr[29].setText("  :  "+ bestSignal.SSID +"  ");
    			    	txtViewsArr[30].setText("  :  "+ Integer.toString(bestSignal.level) +" ");
    			    }
    	    		
    			    //Wifi Scanning Start
            		Log.d(LOG_TAG, "BroadCast wifi.startScan()");
        			wifiM.startScan();
        			
        			// List available networks
        			long tim1 = System.nanoTime();
        			String textWiFiString = "\n******Configured Networks******\n";
        			List<WifiConfiguration> configs = wifiM.getConfiguredNetworks();
        			for (WifiConfiguration config : configs) {
        				textWiFiString += "\nSysTime = " + Long.toString(tim1) +" " +config.toString();
        			}
        			
        			if( null != wifiFileOut )
        			{
        				wifiFileOut.println(textWiFiString +" \n");
        			}
    			    //Toast.makeText(wifiDemo, message, Toast.LENGTH_LONG).show();

    			  }
    		};

    		registerReceiver(receiver, new IntentFilter(
    				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    	    
    	    //GSM Strength
    		
    	    txtViewsArr[26].setText("  :  "+ stateString +" ");
    	    txtViewsArr[27].setText("  :  "+ gsmStrengthVal +" ");
    	    
    	    
            MyListener   = new PhoneStateListener(){
            	@Override
                public void onCallStateChanged(int state, String incomingNumber) {
                 
                  switch (state) {
                  	case TelephonyManager.CALL_STATE_IDLE:
                  		stateString = "Idle";
                  		break;
                  	case TelephonyManager.CALL_STATE_OFFHOOK:
                  		stateString = "Off Hook";
                  		break;
                  	case TelephonyManager.CALL_STATE_RINGING:
                  		stateString = "Ringing";
                  		break;
                  }
                  txtViewsArr[26].setText("  :  "+ stateString +" ");
                  
                  long tim = System.nanoTime();
          	      if( null != gsmFileOut )
          	      {
          	    	gsmFileOut.println(Long.toString(tim)+" "+gsmStrengthVal+" "+stateString);
          	      }
            	}
            	@Override
            	public void onSignalStrengthsChanged(SignalStrength signalStrength)
     	       {
     	          super.onSignalStrengthsChanged(signalStrength);
     	          if(true == signalStrength.isGsm())
     	          {
     	        	  gsmStrengthVal = Integer.toString(signalStrength.getGsmSignalStrength());
     	          }
     	          
     	          //GSM
     	          txtViewsArr[27].setText("  :  "+ gsmStrengthVal +" ");
     	 	    
     	          long tim = System.nanoTime();
     	          if( null != gsmFileOut )
     	          {
     	        	  gsmFileOut.println(Long.toString(tim)+" "+gsmStrengthVal+" "+stateString);
     	          }
     	       }
            	@Override
            	public void onCellLocationChanged (CellLocation location)
            	{
            		super.onCellLocationChanged(location);
            		
            		long tim = System.nanoTime();
            		
	    	    	List<NeighboringCellInfo> neghbCellList = Tel.getNeighboringCellInfo();
	    	    	
	    	    	if( neghbCellList.size() == 0 )
	    	    	{
	    	    		gsmFileOut.println(Long.toString(tim)+" No Neighbour Cells.");
	    	    	}
	    	    	
       	          	if( null != gsmFileOut )
       	          	{
       	        	  //gsmFileOut.println(Long.toString(tim)+" CL:"+((GsmCellLocation)location).getCid()+" "+((GsmCellLocation)location).getPsc()+" "+((GsmCellLocation)location).getLac());
       	          		gsmFileOut.println(Long.toString(tim)+" [Lac Cid Psc]:"+((GsmCellLocation)location).toString());
       	          		for (NeighboringCellInfo neighbour : neghbCellList) {
       	          			gsmFileOut.println(Long.toString(tim)+" Neighbor Cell Info:"+neighbour.toString()+" :[ Cid Lac Nw Psc Rssi ]: ["+neighbour.getCid()+neighbour.getLac()+neighbour.getNetworkType()+neighbour.getPsc()+neighbour.getRssi()+"] ");
       	          		}
       	          	}
            	}
            };
            
            Tel       = ( TelephonyManager )getSystemService(Context.TELEPHONY_SERVICE);
            Tel.listen(MyListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            
    	    
    	    //For Location
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    		nwEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    		Log.d(LOG_TAG,"gps="+gpsEnabled+"nw="+nwEnabled);
    		
    		Criteria criteria = new Criteria();
    		bestProvider = locationManager.getBestProvider(criteria, false);
    		printProvider(bestProvider);
    		
    		if ( true == nwEnabled )
    		{
    			lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    			//printLocation(lastLocation,locFileOut,0);	
    		}else
    		{
    			
    		}
    	    
    }
    
    //For Wifi
    public void wifiStatusUpdate()
    {
    	if( null != wifiM )
	    {
	    	int state = wifiM.getWifiState();
	    	wifiInfo = wifiM.getConnectionInfo();
	    	
	    	switch(state)
	    	{
	    		case WifiManager.WIFI_STATE_DISABLED:
	    			wifiStateString = "Disabled. ";
	    			break;
	    		case WifiManager.WIFI_STATE_DISABLING:
	    			wifiStateString = "Disabling. ";
	    			break;
	    		case WifiManager.WIFI_STATE_ENABLED:
	    			wifiStateString = "Enabled. ";
	    			break;
	    		case WifiManager.WIFI_STATE_ENABLING:
	    			wifiStateString = "Enabling. ";
	    			break;
	    		case WifiManager.WIFI_STATE_UNKNOWN:
	    			wifiStateString = "Unknown. ";
	    			break;
	    		default:
	    			wifiStateString = "N/A.  ";
	    			break;
	    	}
	    	
	    	txtViewsArr[28].setText("  :  "+ wifiStateString +" ");
	    	if ( null != wifiInfo )
	    	{
	    		txtViewsArr[29].setText("  :  "+ wifiInfo.getSSID() +"  ");
	    		txtViewsArr[30].setText("  :  "+ Integer.toString(wifiInfo.getRssi()) +" ");
	    	}
	    }
    }
    
    public void addNewAccessPoint(ScanResult scanResult){

        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = '\"' + scanResult.SSID + '\"';
        //wc.preSharedKey  = "\"password\"";
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;        
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        int res = wifiM.addNetwork(wc);
        Log.d("WifiPreference", "add Network returned " + res );
        boolean b = wifiM.enableNetwork(res, true);        
        Log.d("WifiPreference", "enableNetwork returned " + b );

    }
    //For Location
    public void onLocationChanged(Location location) {
	      // Called when a new location is found by the network location provider.
	    	//if(isBetterLocation(lastLocation,location) == true)
	    	//{
    			printProvider(bestProvider);
	    		makeUseOfNewLocation(location,0);
	    	//}
	    	
	    	lastLocation = location;   
	    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	
	    }

	public void onProviderEnabled(String provider) {
	    	if ( null != locFileOut )
	    	{
	    		locFileOut.println("\nProvider Disabled: " + provider);
	    	}
	    }

	public void onProviderDisabled(String provider) {
	    	if ( null != locFileOut )
	    	{
	    		locFileOut.println("\nProvider Enabled: " + provider);
	    	}
	    }
	    
	    private void makeUseOfNewLocation(Location location , int id){
			
			Log.d(LOG_TAG,"makeUse");
			if( id == 0)
			{
				//Wireless Network
				if ( null != locFileOut )
				{
					 Log.d(LOG_TAG,"makeUseWireless");
					printLocation(location,locFileOut,id);
				}
				
			}else if(id == 1){
				//GPS
				if ( null != locFileOut )
				{
					//Log.v(LOG_TAG,"makeUseGPS");
					printLocation(location,locFileOut,id);
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
				Log.d(LOG_TAG,Long.toString(tim)+lStr+": Location[unknown]");
				
				fOut.println(Long.toString(tim)+lStr+": Location[unknown]");
			}
			else
			{
				Log.d(LOG_TAG,Long.toString(tim)+ lStr+": "+ location.toString());
				
				fOut.println(Long.toString(tim)+ lStr+": "+ location.toString());
			}
		}
		
		private void printProvider(String provider) {
			Log.d(LOG_TAG,"printProvider");
			LocationProvider info = locationManager.getProvider(provider);
			if(null != locFileOut)
			{
				locFileOut.println(info.toString());
			}
		}
	   
    //Main worker Methods
    public void populateGraphicalIDs()
    {	
    	//Linear Layouts
    	childLLArr[0] = (LinearLayout) findViewById(R.id.ssAccValLinearLayout);
    	childLLArr[1] = (LinearLayout) findViewById(R.id.ssMagValLinearLayout);
    	childLLArr[2] = (LinearLayout) findViewById(R.id.ssGyroValLinearLayout);
    	childLLArr[3] = (LinearLayout) findViewById(R.id.ssProxValLinearLayout);
    	childLLArr[4] = (LinearLayout) findViewById(R.id.ssLightValLinearLayout);
    	childLLArr[5] = (LinearLayout) findViewById(R.id.ssTempValLinearLayout);
    	childLLArr[6] = (LinearLayout) findViewById(R.id.ssBaroValLinearLayout);
    	childLLArr[7] = (LinearLayout) findViewById(R.id.ssRVValLinearLayout);
    	childLLArr[8] = (LinearLayout) findViewById(R.id.ssLAValLinearLayout);
    	childLLArr[9] = (LinearLayout) findViewById(R.id.ssSoundValLinearLayout);
    	childLLArr[10] = (LinearLayout) findViewById(R.id.ssOrientValLinearLayout);
    	childLLArr[11] = (LinearLayout) findViewById(R.id.ssGravValLinearLayout);
    	
    	parentLLArr[0] = (LinearLayout) findViewById(R.id.ssAccLinearLayout);
    	parentLLArr[1] = (LinearLayout) findViewById(R.id.ssMagLinearLayout);
    	parentLLArr[2] = (LinearLayout) findViewById(R.id.ssGyroLinearLayout);
    	parentLLArr[3] = (LinearLayout) findViewById(R.id.ssProxLinearLayout);
    	parentLLArr[4] = (LinearLayout) findViewById(R.id.ssLightLinearLayout);
    	parentLLArr[5] = (LinearLayout) findViewById(R.id.ssTempLinearLayout);
    	parentLLArr[6] = (LinearLayout) findViewById(R.id.ssBaroLinearLAyout);
    	parentLLArr[7] = (LinearLayout) findViewById(R.id.ssRVLinearLayout);
    	parentLLArr[8] = (LinearLayout) findViewById(R.id.ssLALinearLayout);
    	parentLLArr[9] = (LinearLayout) findViewById(R.id.ssSoundLinearLayout);
    	parentLLArr[10] = (LinearLayout) findViewById(R.id.ssOrientLinearLayout);
    	parentLLArr[11] = (LinearLayout) findViewById(R.id.ssGravityLinearLayout);
    	
    	//TextViews	
    	txtViewsArr[0] = (TextView) findViewById(R.id.ssAccXTextView);
    	txtViewsArr[1] = (TextView) findViewById(R.id.ssAccYTextView);
    	txtViewsArr[2] = (TextView) findViewById(R.id.ssAccZTextView);
    	txtViewsArr[3] = (TextView) findViewById(R.id.ssMagXTextView);
    	txtViewsArr[4] = (TextView) findViewById(R.id.ssMagYTextView);
    	txtViewsArr[5] = (TextView) findViewById(R.id.ssMagZTextView);
    	txtViewsArr[6] = (TextView) findViewById(R.id.ssGyroXTextView);
    	txtViewsArr[7] = (TextView) findViewById(R.id.ssGyroYTextView);
    	txtViewsArr[8] = (TextView) findViewById(R.id.ssGyroZTextView);
    	txtViewsArr[9] = (TextView) findViewById(R.id.ssProxValTextView);
    	txtViewsArr[10] = (TextView) findViewById(R.id.ssLightValTextView);
    	txtViewsArr[11] = (TextView) findViewById(R.id.ssTempValTextView);
    	txtViewsArr[12] = (TextView) findViewById(R.id.ssBaroValTextView);
    	txtViewsArr[13] = (TextView) findViewById(R.id.ssRVXTextView);
    	txtViewsArr[14] = (TextView) findViewById(R.id.ssRVYTextView);
    	txtViewsArr[15] = (TextView) findViewById(R.id.ssRVZTextView);
    	txtViewsArr[16] = (TextView) findViewById(R.id.ssLAValXTextView);    	
    	txtViewsArr[17] = (TextView) findViewById(R.id.ssLAValYTextView);    	
    	txtViewsArr[18] = (TextView) findViewById(R.id.ssLAValZTextView);
    	txtViewsArr[19] = (TextView) findViewById(R.id.ssSoundValTextView); //sound
    	txtViewsArr[20] = (TextView) findViewById(R.id.ssOrientXTextView);
    	txtViewsArr[21] = (TextView) findViewById(R.id.ssOrientYTextView);
    	txtViewsArr[22] = (TextView) findViewById(R.id.ssOrientZTextView);
    	txtViewsArr[23] = (TextView) findViewById(R.id.ssGravValXTextView);
    	txtViewsArr[24] = (TextView) findViewById(R.id.ssGravValYTextView);
    	txtViewsArr[25] = (TextView) findViewById(R.id.ssGravValZTextView);
    	txtViewsArr[26] = (TextView) findViewById(R.id.ssGSMCallTextView);
    	txtViewsArr[27] = (TextView) findViewById(R.id.ssGSMCinrTextView);
    	txtViewsArr[28] = (TextView) findViewById(R.id.ssWiFiStateTextView);
    	txtViewsArr[29] = (TextView) findViewById(R.id.ssWiFiAccessPointTextView);
    	txtViewsArr[30] = (TextView) findViewById(R.id.ssWifiRssiTextView);
    	
    	//ImageViews
    	imgViewsArr[0] = (ImageView) findViewById(R.id.ssAccImageView);
    	imgViewsArr[1] = (ImageView) findViewById(R.id.ssMagImageView);
    	imgViewsArr[2] = (ImageView) findViewById(R.id.ssGyroImageView);
    	imgViewsArr[3] = (ImageView) findViewById(R.id.ssProxImageView);
    	imgViewsArr[4] = (ImageView) findViewById(R.id.ssLightImageView);
    	imgViewsArr[5] = (ImageView) findViewById(R.id.ssTempImageView);
    	imgViewsArr[6] = (ImageView) findViewById(R.id.ssPresImageView);
    	imgViewsArr[7] = (ImageView) findViewById(R.id.ssRotVImageView);
    	imgViewsArr[8] = (ImageView) findViewById(R.id.ssLAImageView);
    	imgViewsArr[9] = (ImageView) findViewById(R.id.ssSndImageView);
    	imgViewsArr[10] = (ImageView) findViewById(R.id.ssOrientImageView);
    	imgViewsArr[11] = (ImageView) findViewById(R.id.ssGravImageView);
    	
    	//CheckBoxes
    	chkBoxArr[0] = (CheckBox) findViewById(R.id.ssAccCheckBox);
    	chkBoxArr[1] = (CheckBox) findViewById(R.id.ssMagCheckBox);
    	chkBoxArr[2] = (CheckBox) findViewById(R.id.ssGyroCheckBox);
    	chkBoxArr[3] = (CheckBox) findViewById(R.id.ssProxCheckBox);
    	chkBoxArr[4] = (CheckBox) findViewById(R.id.ssLightCheckBox);
    	chkBoxArr[5] = (CheckBox) findViewById(R.id.ssTempCheckBox);
    	chkBoxArr[6] = (CheckBox) findViewById(R.id.ssBaroCheckBox);
    	chkBoxArr[7] = (CheckBox) findViewById(R.id.ssRVCheckBox);
    	chkBoxArr[8] = (CheckBox) findViewById(R.id.ssLACheckBox);
    	chkBoxArr[9] = (CheckBox) findViewById(R.id.ssSLCheckBox);
    	chkBoxArr[10] = (CheckBox) findViewById(R.id.ssOrientCheckBox);
    	chkBoxArr[11] = (CheckBox) findViewById(R.id.ssGravityCheckBox);
    	
    	//Toggle Button
    	togButton = (ToggleButton) findViewById(R.id.logToggleButton);
    	//togButtonLoc = (ToggleButton) findViewById(R.id.locToggleButton);
    	
    	//Button
    	landMarkRecord = (Button) findViewById(R.id.landMarkButton);
     	
    }
    
    public void populateSensorIds()
    {
    	mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    	mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    	mProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    	mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    	mTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
    	mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    	mRotv = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    	mLacc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    	mOrient = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    	mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    	
    }
    /*
    public Sensor getAcc()
    {
    	return mAcc;
    }*/
    
    public void initSensors()
    {
    	boolean val;
    	
    	for(int i =0; i<12 ; i++)
    	{
    		final int j =i;
    		
    		avlSensors[i] = -1; //initialize
    		
    		//adding call back functions to each check box
    		chkBoxArr[j].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	            public void onCheckedChanged(CompoundButton cButton,boolean bVal) {
	            	
	            	SharedPreferences.Editor chkBoxPrefEd = app_preferences.edit();
	            	chkBoxPrefEd.putBoolean(j+"_chk",bVal);
	            	chkBoxPrefEd.commit();
	            }
    		}
    		);
    		//populate the preferences
    		val = app_preferences.getBoolean(i+"_chk",true);
    		chkBoxArr[i].setChecked(val);
    	}
    	
    	landMarkRecord.setOnClickListener(new View.OnClickListener(){
    		public void onClick(View v){
    			if( null != lmFout){
    				long tim=System.nanoTime();
    				lmFout.println(Long.toString(tim));
    			}	
    		}
    	}
    	);
    	//avlSensors[9] = ; for sound
    	
    	for (int i =0 ; i < numSensors; i++)
    	{
    	    int pos = numTypeSensor(deviceSensors.get(i).getType());
  		    // Success! There's a sensor
    	    if ( pos != -1)
    	    {
    		    avlSensors[pos] = deviceSensors.get(i).getType();
  		    }else {
  		        // Failure! No sensor.
  			    //avlSensors[pos] = -1;    
  		    }	    
    	}
    	
    	avlSensors[9] = 99; //for sound
    	
    	for ( int i =0 ; i<12; i++ )
    	{
    		if( -1 == avlSensors[i]) //&& i!=9)
    		{
    			//Eliminate the views of non existent sensors
    		    childLLArr[i].setVisibility(View.GONE);
			    parentLLArr[i].setVisibility(View.GONE);
    		}
    	}
  		
    }
    
    
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
      // Do something here if sensor accuracy changes.
    }

    public final void onSensorChanged(SensorEvent event) {
      
    	float[] senData = null;
    
    	senData = event.values;
    	
    	StringBuilder b = new StringBuilder();
    	long tim=System.nanoTime();
    	b.append(Long.toString(tim)+" "+Long.toString(event.timestamp)+" ");
    	
    	switch( event.sensor.getType() )
    	{
    	    
    		case Sensor.TYPE_ACCELEROMETER: 
    					b.append(appendAndShow(senData,Sensor.TYPE_ACCELEROMETER,0));
    					writeToFile(0,b);
    					break;
    		case Sensor.TYPE_MAGNETIC_FIELD:
    					b.append(appendAndShow(senData,Sensor.TYPE_MAGNETIC_FIELD,3));
    					writeToFile(1,b);
    			        break;
    		case Sensor.TYPE_GYROSCOPE:
    					b.append(appendAndShow(senData,Sensor.TYPE_GYROSCOPE,6));
    					writeToFile(2,b);
    					break;
    		case Sensor.TYPE_PROXIMITY:
    					b.append(appendAndShow(senData,Sensor.TYPE_PROXIMITY,9));
    					writeToFile(3,b);
						break;
    		case Sensor.TYPE_LIGHT:
    					b.append(appendAndShow(senData,Sensor.TYPE_LIGHT,10));
    					writeToFile(4,b);
    					break;
    		case Sensor.TYPE_TEMPERATURE:
    					b.append(appendAndShow(senData,Sensor.TYPE_TEMPERATURE,11));
    					writeToFile(5,b);
    					break;
    		case Sensor.TYPE_PRESSURE:
    					b.append(appendAndShow(senData,Sensor.TYPE_PRESSURE,12));
    					writeToFile(6,b);
    					break;
    		case Sensor.TYPE_ROTATION_VECTOR:
    					b.append(appendAndShow(senData,Sensor.TYPE_ROTATION_VECTOR,13));
    					writeToFile(7,b);
    					
    					//Getting Orientation From Rotation Vector
    					float[] rotData = new float[16];
    					//float[] rotOutData = new float[16];
    					SensorManager.getRotationMatrixFromVector(rotData,senData);
    					//SensorManager.remapCoordinateSystem(rotData,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,rotOutData);
    					float[] oriData = new float[3];
    					if( null != rotData )
    						SensorManager.getOrientation(rotData,oriData);
    				
    					StringBuilder c = new StringBuilder();
    			    	c.append(Long.toString(tim)+" "+Long.toString(event.timestamp)+" ");
    					c.append(appendAndShow(oriData,Sensor.TYPE_ORIENTATION,20));
    					writeToFile(10,c);
    					break;
    					
    		case Sensor.TYPE_LINEAR_ACCELERATION:
    					b.append(appendAndShow(senData,Sensor.TYPE_LINEAR_ACCELERATION,16));
    					writeToFile(8,b);
    					break;
    					//19->sound
    					//writeToFile(9);
    		case Sensor.TYPE_ORIENTATION: //DEPRECATED
    					//b.append(appendAndShow(senData,Sensor.TYPE_ORIENTATION,20));
    					//writeToFile(10,b);
						break;
    		case Sensor.TYPE_GRAVITY:
    					b.append(appendAndShow(senData,Sensor.TYPE_GRAVITY,23));
    					writeToFile(11,b);
    					break;
    		
    		default:
    					//Do Nothing
    					break;
    	    
    	}
    		
	    /*
	     * //For sound recording			
	    try {
				sMeter.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		*/	
    	
		if( null != fOut[9] )
		{
			if( sRecorder != null )
			{
				//fOut[9].println(Long.toString(tim)+","+Long.toString(event.timestamp)+","+Double.toString(sMeter.getAmplitude()));
				fOut[9].println(Long.toString(tim)+","+Long.toString(event.timestamp)+","+Double.toString(sRecorder.getAmplitude()));
			}else
			{
				fOut[9].println(Long.toString(tim)+","+Long.toString(event.timestamp)+","+" N/A");
			}
					
		}
		
		if( sRecorder != null )
		{
			txtViewsArr[19].setText("  :  "+ sRecorder.getAmplitude()+" ");
		}else
		{
			txtViewsArr[19].setText("  :  "+ "N/A");
		}
		
	    //txtViewsArr[19].setText("  :  "+ sMeter.getAmplitude()+" ");
	    //sMeter.reset();	
	    
	   
    }

   
    @Override
    protected void onResume() {
      super.onResume();
      locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
      mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mMag, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);//??
      mSensorManager.registerListener(this, mProxy, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mOrient, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mTemp, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mLacc, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mRotv, SensorManager.SENSOR_DELAY_FASTEST);
      mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);
      //GSM
      Tel.listen(MyListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
      
      //wakeLock.acquire();
      
    }

    @Override
    protected void onPause() {
      super.onPause();
      for(int i =0 ; i< numSensors ; i++)
      {
          mSensorManager.unregisterListener(this);
      }
      locationManager.removeUpdates(this);
      
      //WiFi
      unregisterReceiver(receiver);
      //GSM
      Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
      //sMeter.stop();
      //Release Wake Lock
      //wakeLock.release();
      
    }
    @Override
    protected void onDestroy() {
    	
    	SharedPreferences.Editor chkBoxPrefEd = app_preferences.edit();
    	boolean val;
    	
    	//Sensor prefs updation
		for (int i=0; i<12; i++) {
			val = chkBoxArr[i].isChecked();
			chkBoxPrefEd.putBoolean(i+"_chk", val);
		}
		
		chkBoxPrefEd.commit();
		super.onDestroy();
    }
    
    private File fileDir = null;
    //private PrintWriter locFout = null;
    
    public void registerListener()
    {
    	if( null != togButton )
    	{
	    	togButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	            public void onCheckedChanged(CompoundButton cButton,boolean bVal) {
	                // Perform action on click
	            	if( bVal == true )
	            	{
	            		landMarkRecord.setEnabled(true);
	            		
	            		String ftag=dtf.format(new Date());
	            		File[] file = new File[12];
	            		//File fileDir = null;
	            		//fDir=  fileDir;
	            		boolean checkedVal = false;
	            		
	            		
						fileDir = fileLocation();
						String[] fName = new String[12];
						
						for( int i = 0; i<12; i++)
						{
							   fName[i] = "SensoSaur_"+ftag+"_"+ i +".txt"; //creating filenames
						}
						
						if( null != fileDir)
						{
							boolean result = fileDir.mkdir();
							Log.d(LOG_TAG,"mkdir result = " + result);
							if( false == result )
							{
								result = fileDir.mkdirs();
							}
							Log.d(LOG_TAG,"mkdir result = " + result);
							if (!fileDir.getParentFile().exists() && !fileDir.getParentFile().mkdirs()){
								  Log.d(LOG_TAG,"Unable to create " + fileDir.getParentFile());
								}
							Toast.makeText(getApplicationContext(), "Logging started @ "+fileLocation(), Toast.LENGTH_LONG).show();
						
						} else {
								
							int fNo = app_preferences.getInt("fileNo",1);
								
							fileDir = new File("/sdcard/SensoSaur/SensoSaur_" + fNo); //creating directory
							boolean result = fileDir.mkdir();
							Log.d(LOG_TAG,"2mkdir result = " + result);
							if( false == result )
							{
								result = fileDir.mkdirs();
							}
							Log.d(LOG_TAG,"2mkdir result = " + result);
							
							Toast.makeText(getApplicationContext(), "Logging started @ "+"/sdcard/SensoSaur/"+fileDir, Toast.LENGTH_LONG).show();		
						}
						
						sRecorder = new SoundMeter(fileDir.getAbsolutePath()+"/soundRecord.3gp");
						
						try {
							sRecorder.start();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							Log.d(LOG_TAG, "start1");
							e1.printStackTrace();
						} catch (Throwable e1) {
							// TODO Auto-generated catch block
							Log.d(LOG_TAG, "start2");
							e1.printStackTrace();
						}
						
						Log.d(LOG_TAG,fileDir.getAbsolutePath());
						//landmarks logging handler
				    	File lmFile = new File(fileDir,"landMarks.txt");
				    	//if file does not exist, then create it			
						if(!lmFile.exists()){
							try {
									lmFile.createNewFile();
								} catch (IOException e) {
								   // TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
						
						try {
								lmFout = new PrintWriter(new FileWriter(lmFile));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
						
						for(int i =0 ; i<12 ; i++)
						{
						    file[i] = new File(fileDir,fName[i]); //opening files for storing each sensor's data
						}
					
						for( int i =0 ; i<12; i++)
						{
							checkedVal = app_preferences.getBoolean(i+"_chk",true);
							
							if( true == checkedVal )
							{
								//if file does not exist, then create it
								
							    if(!file[i].exists()){
							    	try {
											file[i].createNewFile();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
							    }
										
								try {
										//fOut[i] = new BufferedWriter(new FileWriter(file[i]));
										fOut[i] = new PrintWriter(new FileWriter(file[i]));
									} catch (IOException e) {
										// TODO Auto-generated catch block
										Toast.makeText(getApplicationContext(), "Logging Failure! Try Again"+e, Toast.LENGTH_SHORT).show();
										togButton.setChecked(true);
										e.printStackTrace();
								}
							}else{
								fOut[i] = null;
							}				
						}
						// List available networks
	        			String textWiFiString = "\n******Configured Networks******\n";
	        			List<WifiConfiguration> configs = wifiM.getConfiguredNetworks();
	        			long tim = System.nanoTime();
	        			for (WifiConfiguration config : configs) {
	        				textWiFiString += "\nSysTime = " + Long.toString(tim)+ " "+config.toString();
	        			}
	        			
	            		//Wifi Scanning Start
	            		Log.d(LOG_TAG, "onClick() wifi.startScan()");
	        			wifiM.startScan();
	        			//doWifiScan();
	        			
	        			//wifi logging handler
				    	File wifiFile = new File(fileDir,"wifiScanResults.txt");
				    	
				    	//if file does not exist, then create it			
						if(!wifiFile.exists()){
							try {
									wifiFile.createNewFile();
								} catch (IOException e) {
								   // TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
						
						try {
								wifiFileOut = new PrintWriter(new FileWriter(wifiFile));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
							
						Log.d("LOG_TAG","wifiFileOut");	
						if( null != wifiFileOut )
						{
							wifiFileOut.println(textWiFiString+"\n");
						}
						
						//gsm logging handler
				    	File gsmFile = new File(fileDir,"gsmCnriResults.txt");
				    	
				    	//if file does not exist, then create it			
						if(!gsmFile.exists()){
							try {
									gsmFile.createNewFile();
								} catch (IOException e) {
								   // TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
						
						try {
								gsmFileOut = new PrintWriter(new FileWriter(gsmFile));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
						if( null != gsmFileOut )
						{
							gsmFileOut.println("#SysTime CNRI Call_State");
						}
						Log.d("LOG_TAG","gsmFileOut");
	            		//location logging handler
				    	File lmFile1 = new File(fileDir,"locations.txt");
				    	//if file does not exist, then create it			
						if(!lmFile1.exists()){
							try {
									lmFile1.createNewFile();
								} catch (IOException e) {
								   // TODO Auto-generated catch block
									e.printStackTrace();
								}
						}
						
						try {
								locFileOut = new PrintWriter(new FileWriter(lmFile1));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
						Log.d("LOG_TAG","locFileOut");
						if( null != locFileOut )
						{
							//senLocLeader = new SensoSaurLocationLeader(locFileOut,locationManager);
							if ( true == nwEnabled )
							{
									
								if(gpsEnabled == false)
								{
									Log.d("LOG_TAG","gpsNot");
									Toast.makeText(getApplicationContext(), "Enable GPS", Toast.LENGTH_LONG).show();
								}
									
							}else
							{
								if( gpsEnabled == true)
								{
									Toast.makeText(getApplicationContext(), "Enable Mobile Packet Data Network", Toast.LENGTH_LONG).show();
								}else
								{
									Toast.makeText(getApplicationContext(), "Enable Mobile Packet Data Network & GPS", Toast.LENGTH_LONG).show();
								}
							}
						}
						Log.d(LOG_TAG,"afterLocFileout");
						//Writing the initial lines in each sensor data files
						
						if ( null != fOut[0] )
						{
							fOut[0].println("#SysTm EventTmStmp Acc:x Acc:y Acc:z");
						}
						
						if ( null != fOut[1] )
						{
							fOut[1].println("#SysTm EventTmStmp Mag:x Mag:y Mag:z");		  
						}
					
						if ( null != fOut[2] )
						{
							fOut[2].println("#SysTm EventTmStmp Gyro:x Gyro:y Gyro:z");
						}
							
						if ( null != fOut[3] )
						{
							fOut[3].println("#SysTm EventTmStmp Prox");
						}
							
						if ( null != fOut[4] )
						{
							fOut[4].println("#SysTm EventTmStmp Light");
						}
							
						if ( null != fOut[5] )
						{
							fOut[5].println("#SysTm EventTmStmp Temp");			
						}
							
						if ( null != fOut[6] )
						{
							fOut[6].println("#SysTm EventTmStmp Pressure");
						}
							
						if ( null != fOut[7] )
						{
							fOut[7].println("#SysTm EventTmStmp Rot:x Rot:y Rot:z");
						}
							
						if ( null != fOut[8] )
						{
							fOut[8].println("#SysTm EventTmStmp La:x La:y La:z");
						}
						
						if ( null != fOut[9] )
						{
							fOut[9].println("#SysTm EventTmStmp Snd");
						}
						
						if ( null != fOut[10] )
						{
							fOut[10].println("#SysTm EventTmStmp Ori:x Ori:y Ori:z");
						}
						
						if ( null != fOut[11] )
						{
							fOut[11].println("#SysTm EventTmStmp Grav:x Grav:y Grav:z");
						}
						Log.d(LOG_TAG,"afterFiles");
						//Initial Print in Gsm Strength File
			            //long tim = System.nanoTime();
			    	    if( null != gsmFileOut )
			    	    {
			    	    	CellLocation location = Tel.getCellLocation();
			    	    	List<NeighboringCellInfo> neghbCellList = Tel.getNeighboringCellInfo();
			    	    	
			    	    	Log.d(LOG_TAG,"afterTelCall");
			    	    		    	    	
			    	    	gsmFileOut.println(Long.toString(tim)+" "+gsmStrengthVal+" "+stateString);
			    	    	//gsmFileOut.println(Long.toString(tim)+" CL:"+((GsmCellLocation)location).getCid()+" "+((GsmCellLocation)location).getPsc()+" "+((GsmCellLocation)location).getLac());
			    	    	if( null != location )
			    	    	{
			    	    		gsmFileOut.println(Long.toString(tim)+" [Lac Cid Psc]:"+((GsmCellLocation)location).toString());
			    	    	}else
			    	    	{
			    	    		gsmFileOut.println(Long.toString(tim)+" [Lac Cid Psc]:"+ " N/A");
			    	    	}
			    	    	
			    	    	if( neghbCellList.size() == 0 )
			    	    	{
			    	    		gsmFileOut.println(Long.toString(tim)+" No Neighbour Cells.");
			    	    	}else
			    	    	{
			    	    		for (NeighboringCellInfo neighbour : neghbCellList) {
			    	    			gsmFileOut.println(Long.toString(tim)+" Neighbor Cell Info:"+neighbour.toString()+" :[ Cid Lac Nw Psc Rssi ]: ["+neighbour.getCid()+neighbour.getLac()+neighbour.getNetworkType()+neighbour.getPsc()+neighbour.getRssi()+"] ");
			    	    		}
			    	    	}
			    	    }
						
			    	    Log.d(LOG_TAG,"last");
	            	}else{
	            		
	            		landMarkRecord.setEnabled(false);
	            		
	            		if ( null != locFileOut )
	            		{
	            			locFileOut.close();
	            		}
	            		
	            		if ( null != wifiFileOut )
	            		{
	            			wifiFileOut.close();
	            		}
	            		
	            		if ( null != gsmFileOut )
	            		{
	            			gsmFileOut.close();
	            		}
	            		//senLocLeader.stopLocating();
	            		//File closed and data recording stopped
	            		
	            		sRecorder.stop();
	            		
	            		for( int i=0 ;i<12 ;i++)
	            		{
	            			if( null != fOut[i] )
	            			{
	            				fOut[i].close();
	            			}
	            		}
	            		if ( null != lmFout )
	            		{
	            			lmFout.close();
	            		}
	            		
	            		SharedPreferences.Editor filePrefEd = app_preferences.edit();
	            		int val = app_preferences.getInt("fileNo",1)+1;
	            		filePrefEd.putInt( "fileNo",val );
	            		filePrefEd.commit();
	            		
	            		Toast.makeText(getApplicationContext(), "Data Logging stopped", Toast.LENGTH_SHORT).show();
	            	}
	            }
	    	});
    	}
    	
    	/*if ( null != togButtonLoc )
    	{
    		togButtonLoc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	            public void onCheckedChanged(CompoundButton cButton,boolean bVal) {
	            	
	            	if( bVal == true )
	            	{
	            		
	            	}else{
	            	
	            		
	            	}
	            	
	            }});
    	}*/
    	
    }
    
   /* //wifiScan
    public void doWifiScan(){

    	  TimerTask scanTask = new TimerTask() {
    	  public void run() {
    	      handler.post(new Runnable() {
    	          public void run() {
    	               List<ScanResult> sResults = wifiM.getScanResults(); 
    	               if(sResults!=null)
    	            	   Log.d("TIMER", "sResults count" + sResults.size());
    	               ScanResult scan = calculateBestAP(sResults);
    	               // List available networks
	        			String textWiFiString = "\n****************Configured Networks**********************\n";
	        			List<WifiConfiguration> configs = wifiM.getConfiguredNetworks();
	        			for (WifiConfiguration config : configs) {
	        				textWiFiString += "\n" + config.toString();
	        			}
    	               //wifiM.addNewAccessPoint(scan);
    	           }
    	       });
    	    }};

    	    t.schedule(scanTask, 3000, 30000); 
    	}
    
    public ScanResult calculateBestAP(List<ScanResult> sResults){

        ScanResult bestSignal = null;
           for (ScanResult result : sResults) {
             if (bestSignal == null
                 || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
               bestSignal = result;
           }

           String message = String.format("%s networks found. %s is the strongest. %s is the bsid",
                   sResults.size(), bestSignal.SSID, bestSignal.BSSID);

           Log.d("sResult", message);
           return bestSignal;
   }
    */
    
    //dialog maker
    public void dialogsCreationAndHandle()
    {
    	//About Dialog
        ImageView imAbt = (ImageView) findViewById(R.id.ssAboutImageView);
        imAbt.setOnClickListener( new View.OnClickListener(){

            public void onClick(View v) {
	    
	            AlertDialog alertDialog = new AlertDialog.Builder(
	    		    SensoSaurActivity.this).create();

                // Setting Dialog Title
                alertDialog.setTitle("About SensoSaur (Alpha): ");

                // Setting Dialog Message
                alertDialog.setMessage("Welcome to About.Info of SensoSaur - The Dino of All Sensor Apps.\n"
                		+"This is an app made by me i.e. Swadhin.It is my first one btw.\n"
                		+" U r free to use/abuse as copyright is yours ;) Its in Alpha now.\n"
                		+"Feedback @ nolanthealchemist@gmail.com .");

               // Setting Icon to Dialog
               alertDialog.setIcon(R.drawable.ic_launcher);

               // Setting OK Button
               alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {
                   // Write your code here to execute after dialog closed
                   //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
               }
               });
            
              // Showing Alert Message
              alertDialog.show(); }});
        
        //Recording Rate Dialog
        ImageView imRec = (ImageView) findViewById(R.id.ssSettingsImageView);
         
        imRec.setOnClickListener( new View.OnClickListener(){
        
            public void onClick(View v) {
            	
            	Context conView = v.getContext();  
            	LinearLayout llDial = new LinearLayout(conView);
            	llDial.setOrientation(LinearLayout.VERTICAL);
            	llDial.setPadding(15,15,15,15);
            	SeekBar seekBr =  new SeekBar(conView);
            	
            	
                seekBr.setMax(1000);
                seekBr.setProgress(app_preferences.getInt("progress", 0));
            	//SeekBar seekBr = (SeekBar)getLayoutInflater().inflate(R.textViewDial, null);
            	final TextView tvDial =  new TextView(conView);
            	tvDial.setGravity(0x11);
            	
            	tvDial.setTextSize(TypedValue.COMPLEX_UNIT_DIP,40);
            	tvDial.setText(app_preferences.getString("value"," 0.00 Seconds "));
            	
            	seekBr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            		
            	    public void onStopTrackingTouch(SeekBar s)
            	    {
            	        //Do nothing
            	    }
            	    public void onStartTrackingTouch(SeekBar s)
            	    {
            	    	//Do Nothing
            	    }
            	    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            	    	
            	    	SharedPreferences.Editor editor = app_preferences.edit();
            	        editor.putInt("progress", progress);
            	        double val = progress/100.0;
            	        
            	        editor.putString("value",Double.toString((double)(val)) + "  Seconds ");
            	        
            	    	editor.commit(); // Very important
            	    	tvDial.setText(Double.toString((double)(val)) + "  Seconds ");
            	 	
            	    }
            	});
            	
            	llDial.addView(tvDial);
            	llDial.addView(seekBr);
            	
	    
	            AlertDialog alertDialog = new AlertDialog.Builder(
	    		    SensoSaurActivity.this).create();

                // Setting Dialog Title
                alertDialog.setTitle(" Recording Interval  ");

                // Setting Dialog Message
                alertDialog.setMessage(" Data Recording Rate=>   ");
                alertDialog.setView(llDial);
                
               // Setting Icon to Dialog
                alertDialog.setIcon(R.drawable.rec);

               // Setting OK Button
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                   // Write your code here to execute after dialog closed
                   //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
               }
               });
            
               // Showing Alert Message
               alertDialog.show(); }});
        
       //For Info Dialog
       AlertDialog alertInfoDialog = new AlertDialog.Builder(
    		    SensoSaurActivity.this).create();
       alertInfoDialog.setTitle(" Sensor.Info ");
       alertInfoDialog.setIcon(R.drawable.info_about_con);
    
       // Setting OK Button
       alertInfoDialog.setButton("OK", new DialogInterface.OnClickListener() {
       public void onClick(DialogInterface dialog, int which) {
           // Write your code here to execute after dialog closed
           //Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
       }});
       
       onInfoClickHandler = new SensorInfoSelect(alertInfoDialog,mSensorManager);
       
       for( int i=0 ; i < 12 ; i++)
       {
    	   imgViewsArr[i].setOnClickListener(onInfoClickHandler);
       }
      
    }
    
    //helper methods
    
    public void writeToFile(int i,StringBuilder b)
    {
	    	if( null != fOut[i] )
	    	{
			    fOut[i].println(b.toString());
	    	}
    }
    
    public String appendAndShow(float[] senData,int type,int index)
    {
    	String showData = "";
    	StringBuilder bTemp = new StringBuilder();
    	DecimalFormat df = new DecimalFormat("##.#####");
    	
    	if( null != senData )
    	{
	    	for( int j =0 ; j< numOfViews(type); j++)
		    {
			    if( ( type == Sensor.TYPE_GYROSCOPE))
			    {    
			        showData = df.format(senData[j]);    
			    }
			    else
			    {
				    showData = ""+senData[j];
			    }
			    //bTemp.append(showData+sensorUnit(type)+",");
			    bTemp.append(showData+" ");
			    
			    if(index < 26)
			    {	 
		           txtViewsArr[index].setText("  :  " + showData + sensorUnit(type));
		           index++;
				  
			    }
		    
		    }
    	}
    	
    	return bTemp.toString();
    }
    
    public int numTypeSensor(int type)
    {
    	int result = -1;
    	switch(type)
    	{
    	    case Sensor.TYPE_ACCELEROMETER : 
    		    result = 0;
    		    break;
    	    case Sensor.TYPE_MAGNETIC_FIELD : 
    	    	result = 1;
    	    	break;
    	    case Sensor.TYPE_GYROSCOPE : 
    	    	result = 2;
    	    	break;
    	    case Sensor.TYPE_PROXIMITY : 
    	    	result = 3;
    	    	break;
    	    case Sensor.TYPE_LIGHT : 
    	    	result = 4;
    	    	break;
    	    case Sensor.TYPE_TEMPERATURE : 
    	    	result = 5;
    	    	break;
    	    case Sensor.TYPE_PRESSURE : 
    	    	result = 6;
    	    	break;
    	    case Sensor.TYPE_ROTATION_VECTOR : 
    	    	result = 7;
    	    	break;
    	    case Sensor.TYPE_LINEAR_ACCELERATION : 
    	    	result = 8;
    	    	break;
    	    case 99: //for sound
    	    	result = 9;
    	    	break;	
    	    case Sensor.TYPE_ORIENTATION : 
    	    	result = 10;
    	    	break;
    	    case Sensor.TYPE_GRAVITY : 
    	    	result = 11;
    	    	break;
    	    default:
    	    	result = -1;
    	    	break;
    	}
    	
    	return result;
    	    	
    }  
    
    public int revNumTypeSensor(int type)
    {
    	int result = -1;
    	switch(type)
    	{
    	    case 0 : 
    		    result = Sensor.TYPE_ACCELEROMETER;
    		    break;
    	    case 1: 
    	    	result = Sensor.TYPE_MAGNETIC_FIELD ;
    	    	break;
    	    case 2: 
    	    	result = Sensor.TYPE_GYROSCOPE;
    	    	break;
    	    case 3: 
    	    	result = Sensor.TYPE_PROXIMITY ;
    	    	break;
    	    case  4: 
    	    	result = Sensor.TYPE_LIGHT;
    	    	break;
    	    case  5: 
    	    	result = Sensor.TYPE_TEMPERATURE;
    	    	break;
    	    case  6: 
    	    	result = Sensor.TYPE_PRESSURE;
    	    	break;
    	    case  7: 
    	    	result = Sensor.TYPE_ROTATION_VECTOR;
    	    	break;
    	    case  8: 
    	    	result = Sensor.TYPE_LINEAR_ACCELERATION;
    	    	break;
    	    case 9:
    	    	result = 99;//for sound
    	    	break;	
    	    case 10 : 
    	    	result = Sensor.TYPE_ORIENTATION;
    	    	break;
    	    case  11: 
    	    	result = Sensor.TYPE_GRAVITY;
    	    	break;
    	    default:
    	    	result = -1;
    	    	break;
    	}	
    	return result;
    	    	
    }
    
   
    public String sensorUnit (int type)
    {
    	String sUnit;
    	switch(type)
    	{
    	    case Sensor.TYPE_ACCELEROMETER :
    	    case Sensor.TYPE_LINEAR_ACCELERATION:
    	    case Sensor.TYPE_GRAVITY :
    	    	sUnit = " m/s^2";
    	    	break;
    	    case Sensor.TYPE_MAGNETIC_FIELD :
    	    	sUnit = " uT";
    	    	break;
    	    case Sensor.TYPE_GYROSCOPE :
    	    	sUnit = " rad/s";
    	    	break;
    	    case Sensor.TYPE_ROTATION_VECTOR :
    	    	sUnit = " ";
    	    	break;
    	    case Sensor.TYPE_ORIENTATION : 
    	    	sUnit = " Deg";
    	    	break;	
    	    case Sensor.TYPE_LIGHT : 
    	    	sUnit = " lx";
    	    	break;
    	    case Sensor.TYPE_TEMPERATURE :
    	    	sUnit = " Cent";
    	    	break;
    	    case Sensor.TYPE_PRESSURE :
    	    	sUnit = " mb";
    	    	break;
    	    case Sensor.TYPE_PROXIMITY :
    	    	sUnit = " Cm";
    	    	break;
    	    default:
    	    	sUnit = " ";
    	    	break;
    	}
    	
    	return sUnit;
    	
    }
   
    public int numOfViews (int type)
    {
    	int num = 0;
    	switch(type)
    	{
    	    case Sensor.TYPE_ACCELEROMETER :     
    	    case Sensor.TYPE_MAGNETIC_FIELD : 	
    	    case Sensor.TYPE_GYROSCOPE :     	 
    	    case Sensor.TYPE_ROTATION_VECTOR :
    	    case Sensor.TYPE_ORIENTATION : 
    	    case Sensor.TYPE_LINEAR_ACCELERATION :
    	    case Sensor.TYPE_GRAVITY :
    	    	num = 3;
    	    	break;
    	    case Sensor.TYPE_LIGHT : 	
    	    case Sensor.TYPE_TEMPERATURE :  	
    	    case Sensor.TYPE_PRESSURE : 	    
    	    case Sensor.TYPE_PROXIMITY :
    	    case 99:
    	    	num = 1;
    	    	break;
    	    default:
    	    	num = -1;
    	    	break;
    	}
    	
    	return num;
    	
    }
    
    public File fileLocation() {
		
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		Log.d(LOG_TAG,Environment.getExternalStorageState()+Boolean.toString(mExternalStorageAvailable) + Boolean.toString(mExternalStorageWriteable));
		if (mExternalStorageAvailable && mExternalStorageWriteable) {
			
			int fNo = app_preferences.getInt("fileNo",1);
			return new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/SensoSaur/SensoSaur_"+fNo);
			
		}
		else {
			//No external Storage;
			return null;
		}
	}
    	
}


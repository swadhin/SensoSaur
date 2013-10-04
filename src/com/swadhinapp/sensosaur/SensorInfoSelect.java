package com.swadhinapp.sensosaur;
import android.app.AlertDialog;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;

public class SensorInfoSelect implements View.OnClickListener{

	AlertDialog infoDialog =  null;
	SensorManager sManager = null;
	String dialStr;
	
	public SensorInfoSelect(AlertDialog dialog,SensorManager sMgr)
	{
	    infoDialog = dialog;
	    sManager = sMgr;
	}
	
	public void onClick(View v){
		
		switch(v.getId())
		{
		   case R.id.ssAccImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)," m/s^2");
			   break;
		   case R.id.ssMagImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)," uT");
			   break;
		   case R.id.ssGyroImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)," rad/s");
			   break;
		   case R.id.ssProxImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)," cm");
			   break;
		   case R.id.ssLightImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_LIGHT)," lx");
			   break;
		   case R.id.ssTempImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE)," Cent");
			   break;
		   case R.id.ssPresImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_PRESSURE)," mbar");
			   break;
		   case R.id.ssRotVImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),"");
			   break;
		   case R.id.ssLAImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)," m/s^2");
			   break;
		   case R.id.ssSndImageView:
			   dialStr = " Sound Sampled and Analysed \n From Mic";
			   break;
		   case R.id.ssOrientImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)," m/s^2");
			   break;
		   case R.id.ssGravImageView:
			   dialStr = populateSenInfo(sManager.getDefaultSensor(Sensor.TYPE_GRAVITY)," m/s^2");
			   break;
		   default:
			   dialStr = "**";
			   break;
		}
		
		infoDialog.setMessage(dialStr);
		
		infoDialog.show();
	}
	
	public String populateSenInfo(Sensor s,String unit)
	{
		String infoStr = null;
		
		float sensorMaxRange = s.getMaximumRange();
		String sensorName = s.getName();
		float sensorPower = s.getPower();
	        float sensorResn = s.getResolution();
		String sensorVendor = s.getVendor();
		int sensorVer = s.getVersion();
		
		infoStr = " Name : " + sensorName +"\n Power : "+ sensorPower + 
		          "mA \n Resolution : "+ sensorResn + unit+ "\n Vendor : "+ 
		          sensorVendor +" \n Version : "+sensorVer +" \n Max range : "+
		          sensorMaxRange + unit +" \n";
		
		return infoStr;
	}
	
	
}

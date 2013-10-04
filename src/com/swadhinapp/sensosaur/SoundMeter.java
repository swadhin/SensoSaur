package com.swadhinapp.sensosaur;

import java.io.IOException;

import android.media.MediaRecorder;

public class SoundMeter {
	static final private double EMA_FILTER = 0.6;


    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;
    private String path = null;
    
    SoundMeter(String sPath)
    {
    	path = sPath;
    }

    public void start() throws Throwable, IOException {
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(path); 
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.prepare();
                mRecorder.start();
                mEMA = 0.0;
            }
    }
    
    public void reset() {
            if (mRecorder != null) {
                    mRecorder.reset();       
                    //mRecorder.release();
                    //mRecorder = null;
            }
    }
    
    public void stop() {
        if (mRecorder != null) {
                mRecorder.stop();       
                mRecorder.release();
                mRecorder = null;
        }
    }
    
    public double getAmplitude() {
            if (mRecorder != null)
            {
            	    double val = 20/2.303;
            	    double amp = Math.log(mRecorder.getMaxAmplitude());
            	    amp *= val;
                    return amp;
            }
            else
                    return 0;
    }


    public double getAmplitudeEMA() {
            double amp = getAmplitude();
            mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
            return mEMA;
    }
}

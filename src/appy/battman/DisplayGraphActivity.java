package appy.battman;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayGraphActivity extends Activity {
	//public 	AudioRecord audioRecord = null;
	int       RECORDER_SAMPLERATE = 44100;
	int       MAX_FREQ = RECORDER_SAMPLERATE/2;
	final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	final int PEAK_THRESH = 10;
	final int bit_rate = 5;

	short[]     buffer           = null;
	int         buffer_read_result = 0;
	boolean     a_rec_started      = false;
	int         buffer_size       = 2048;
	int         min_buffer_size    = 0;
	//float       volume           = 0;
	FFT         fft              = null;
	float[]     fft_real_array     = null;
	int         main_freq         = 0;
	int         step              = 5;

    
    float maxPeak = 0;
    RecordAudio recordTask;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_graph);
	
		//startRecording();
		recordTask = new RecordAudio();
        recordTask.execute();

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_graph, menu);
		return true;
	}
	private class RecordAudio extends AsyncTask<Void, float[], Void> {
    	
        
		@Override
        protected Void doInBackground(Void... params) {
        	
        	if(isCancelled()){
        		return null;
        	}
        //try {
        	// listen for data from microphone
    		min_buffer_size = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    		
    		if (min_buffer_size == AudioRecord.ERROR_BAD_VALUE)  {
    			 Context context = getApplicationContext();
    		      	CharSequence text = "buffer size error";
    		      	int duration = Toast.LENGTH_SHORT;
    		      	Toast toast = Toast.makeText(context, text, duration);
    		      	toast.show();
    		    RECORDER_SAMPLERATE = 8000; // forced by the android emulator
    		    MAX_FREQ = RECORDER_SAMPLERATE/2;
    		    buffer_size =  2 << (int)(Math.log(RECORDER_SAMPLERATE)/Math.log(2)-1);// buffer size must be power of 2!!!
    		    // the buffer size determines the analysis frequency at: RECORDER_SAMPLERATE/bufferSize
    		    // this might make trouble if there is not enough computation power to record and analyze
    		    // a frequency. In the other hand, if the buffer size is too small AudioRecord will not initialize
    		  } else buffer_size = min_buffer_size;
    		  buffer = new short[buffer_size];
    		  AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
    		  RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, min_buffer_size);
            if ((audioRecord != null) && (audioRecord.getState() == AudioRecord.STATE_INITIALIZED))
            {
                try
                {
                  // this throws an exception with some combinations
                  // of RECORDER_SAMPLERATE and bufferSize 
                  audioRecord.startRecording(); 
      	          a_rec_started = true;
                }
                catch (Exception e)
                {
                	a_rec_started = false;
                	 Context context = getApplicationContext();
                 	CharSequence text = "unknown Exception";
                 	int duration = Toast.LENGTH_SHORT;
                 	Toast toast = Toast.makeText(context, text, duration);
                 	toast.show();
                }
            } 
            else
            {  
            	if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
            	{
    	        	a_rec_started = false;
    	        	 Context context = getApplicationContext();
    	          	CharSequence text = "initialisation failed " + audioRecord.getState() + "" ;
    	          	int duration = Toast.LENGTH_SHORT;
    	          	Toast toast = Toast.makeText(context, text, duration);
    	          	toast.show();
    	          	
            	}
            	//finish();
            }
            while (a_rec_started)
            {
	        	if(isCancelled())
	        	{
	        		break;
	        	}
	        
	        	 buffer_read_result = audioRecord.read(buffer, 0, buffer_size);
	             // verify that is power of two
	             if (buffer_read_result % 2 != 0) buffer_read_result = 2 << (int)(Math.log(buffer_read_result)/Math.log(2)); 
	            
	             fft = new FFT(buffer_read_result, RECORDER_SAMPLERATE);
	             fft_real_array = new float[buffer_read_result]; 
	             
	          // After we read the data from the AudioRecord object, we loop through
	             // and translate it from short values to double values. We can't do this
	             // directly by casting, as the values expected should be between -1.0 and 1.0
	             // rather than the full range. Dividing the short by 32768.0 will do that,
	             // as that value is the maximum value of short.
	             //volume = 0;
	             for (int i = 0; i < buffer_read_result; i++) {
	            	 fft_real_array[i] = (float) buffer[i] / Short.MAX_VALUE;// 32768.0;
	                  //volume += Math.abs(fft_real_array[i]);
	             }
	             //volume = (float)Math.log10(volume/buffer_read_result);
	               
	              /* // apply windowing
	              for (int i = 0; i < buffer_read_result/2; ++i) {
	                // Calculate & apply window symmetrically around center point
	                // Hanning (raised cosine) window
	                float winval = (float)(0.5+0.5*Math.cos(Math.PI*(float)i/(float)(buffer_read_result/2)));
	                if (i > buffer_read_result/2)  winval = 0;
	                fft_real_array[buffer_read_result/2 + i] *= winval;
	                fft_real_array[buffer_read_result/2 - i] *= winval;
	              }*/
	              // zero out first point (not touched by odd-length window)
	              fft_real_array[0] = 0;
	              fft.forward(fft_real_array);
	              publishProgress(fft_real_array);
            }
            try
            {
            	audioRecord.stop();
            }
            catch(IllegalStateException e){
            	Log.e("Stop failed", e.toString());
            	
            }
                    
            return null;
                    
        }
        
        protected void onProgressUpdate(float[]... toTransform)
        {
        	Log.e("RecordingProgress", "Displaying in progress");
        	
        	float lastVal = 0;
            float val = 0;
            float maxVal = 0; // index of the bin with highest value
            int maxValIndex = 0;
            for(int i = 0; i < fft.specSize(); i++)
            {
              val += fft.getBand(i);
              if (i % step == 0)
              {
                  val /= step; // average volume value
                  if (val-lastVal > PEAK_THRESH)
                  {
                	  Log.e("intermediate peak", "peak freq: "+fft.indexToFreq(i)+"");
                      if (val > maxVal)
                      {
                        maxVal = val;
                        maxValIndex = i;
                      }
                  } 
                  lastVal = val;
                  val = 0;  
               }
            }
            maxPeak = fft.indexToFreq(maxValIndex);
            Log.e("at each step", "max freq: "+maxPeak+"");
            }
        
        }
	
	public void stopRecording(View view)
	{
		a_rec_started = false;
		recordTask .cancel(true);
        recordTask = null;
	    TextView tv = new TextView(view.getContext());
	    tv.setText("Max Frequency: "+maxPeak+"");
	    tv.setEms(12);
	    tv.setWidth(LayoutParams.WRAP_CONTENT);
	    tv.setHeight(LayoutParams.WRAP_CONTENT);
	    Context context = getApplicationContext();
    	CharSequence text = "Max Frequency: "+maxPeak+"";
    	int duration = Toast.LENGTH_LONG;
    	Toast toast = Toast.makeText(context, text, duration);
    	toast.show();
		finish();
	}

}

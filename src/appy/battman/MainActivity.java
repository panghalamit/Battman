package appy.battman;

import java.util.BitSet;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	double f1=17500,f2=18500, sample_rate=44100, bit_rate=10, beg=16500, end = 19500, notif_time = 0.1;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void sendData(View view) {
    	// send the data given in 
    	EditText ed = (EditText) findViewById(R.id.editText1);
    	String msg = ed.getText().toString();
    	ed.setText("");
    	ed.clearFocus();
    	if(msg == "") return;
    	byte [] msgbytes = msg.getBytes();
    	StringBuilder binary = new StringBuilder();
    	int index=0;
    	  for (byte b : msgbytes)
          {
             int val = b;
             for (int i = 0; i < 8; i++)
             {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
             }
          }
    	  final byte[] generated_snd = dataToSound(binary);
    	// Use a new tread as this can take a while
          final Thread thread = new Thread(new Runnable() {
                      public void run() {
                    	final  AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                  (int) sample_rate, AudioFormat.CHANNEL_OUT_MONO,
                                  AudioFormat.ENCODING_PCM_16BIT, generated_snd.length,
                                  AudioTrack.MODE_STATIC);
                          audioTrack.write(generated_snd, 0, generated_snd.length);
                          audioTrack.play();
                      }
          });
          thread.start();
          Context context = getApplicationContext();
      	  CharSequence text = "Data Sent";
      	  int duration = Toast.LENGTH_SHORT;
      	  Toast toast = Toast.makeText(context, text, duration);
      	  toast.show();
    	
    }
    public byte[] dataToSound(StringBuilder barr)
    {
    	int i;
    	double duration = barr.length()/bit_rate;
    	Log.e("duration", "duration: "+duration+"");
    	int num_samples = (int)((duration+2*notif_time)*sample_rate);
    	double[] sample = new double[num_samples];
    	byte[] snd = new byte[2*num_samples];
    	for(i=0; i<num_samples; i++)
    	{
    	    if(i<(int)notif_time*sample_rate)
    	    	sample[i] = Math.cos(2*Math.PI*i*beg/sample_rate);
    	    else if(i > (int)(duration+notif_time)*sample_rate)
    	    	sample[i] = Math.cos(2*Math.PI*i*end/sample_rate);
    		else if(barr.charAt(i*(barr.length()/num_samples)) == '0')
    			sample[i] = Math.cos(2*Math.PI*i*f1/sample_rate);
    		else
    			sample[i] = Math.cos(2*Math.PI*i*f2/sample_rate);
    	}
    	 i=0;
         for (final double dVal : sample)
         {
               // scale to maximum amplitude
               final short val = (short) ((dVal * 32767));
               // in 16 bit wav PCM, first byte is the low order byte
               snd[i++] = (byte) (val & 0x00ff);
               snd[i++] = (byte) ((val & 0xff00) >>> 8);

         }
    	return snd;
    }
    
    public void receiverOn(View view)
    {
    	Intent intent = new Intent(this, DisplayGraphActivity.class);
    	startActivity(intent);
    }
}

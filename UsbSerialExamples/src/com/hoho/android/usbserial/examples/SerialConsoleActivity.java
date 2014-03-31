/* 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.examples.GCodeParameters.Parameter;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialDriver} instance, showing all data
 * received.
 *
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialDriver)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    
    // Physic system constants
    
    private final int bedWidth = 160;
    private final int bedHeight = 160; 
    
    // Class variables
    
    private static UsbSerialDriver sDriver = null;
    
    private int semaphore;
    private String buffer;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;
    
    private static GCodeProtocol gcode = null;
    private GCodeParameters parameters = new GCodeParameters();
    
    // Interface
    
    private TextView mTitleTextView;
    private TextView textViewConsole;
    
    private RelativeLayout rLayout;
    
    private SeekBar seekBarX;
    private SeekBar seekBarZ;
    
    private CheckBox checkBoxPulse;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Request Full Screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.serial_console);
        
        rLayout = (RelativeLayout) findViewById(R.id.parentView);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        textViewConsole = (TextView) findViewById(R.id.textView1);
        textViewConsole.setText("Start...");
        Button home = (Button) findViewById(R.id.buttonHome);
        seekBarX = (SeekBar) findViewById(R.id.seekBarX); // Movement Speed
        seekBarZ = (SeekBar) findViewById(R.id.seekBarZ); // Touch Speed
        checkBoxPulse = (CheckBox) findViewById(R.id.checkBoxPulse);
        
        semaphore = 1;
        buffer = "";
        
        // Set robot XY workspace
//        gcode.SendCommand("M208 X" + String.valueOf(bedHeight) + " Y" + String.valueOf(bedWidth));
        
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOrigin();
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moveXY(x, y);
                
            case MotionEvent.ACTION_MOVE:
                moveXY(x, y);
                break;
                
            case MotionEvent.ACTION_UP:
                if (checkBoxPulse.isChecked()) {
                    tap();
                }
                break;
        }

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            } catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sDriver.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }
   
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        
//        for (int i = 0; i < data.length; i++) {
//            buffer += (char) data[i];
//        }
//        
//        textViewConsole.setText("");
//        
//        boolean cont = true;
//        
//        while (cont) {
//
//            if ((buffer.length() >= 3) && (buffer.charAt(0) == 'o') && (buffer.charAt(1) == 'k') && (buffer.charAt(2) == '\n')) {
//                //semaphore++;
//                buffer = buffer.substring(3);                    parameters.Reset();
//                textViewConsole.append("TRUE\n");
//            }
//            else {
//                cont = false;
//                textViewConsole.append("FALSE\n");
//            }
//        }
//        
//        textViewConsole.append(buffer + ": " + semaphore + HexDump.dumpHexString(data) + "\n");
    }
    
    private void getOrigin() {
        gcode.SetToAbsolutePositioning();
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.FeedRate, 1500);
        textViewConsole.append(" " + semaphore);
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.AxisZ, 20);
        //while (semaphore != 1);
        textViewConsole.append(" " + semaphore);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.AxisX, 0);
        parameters.AddParameter(GCodeParameters.Parameter.AxisY, 0);
        parameters.AddParameter(GCodeParameters.Parameter.AxisZ, 0);
        //while (semaphore != 1);
        textViewConsole.append(" " + semaphore);
        //semaphore--;
        gcode.Homing(parameters);
        gcode.SetPosition(parameters);
    }
        
    private void moveXY(int x, int y) {
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.FeedRate, seekBarX.getProgress());
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.AxisX, x * bedWidth / rLayout.getWidth());
        parameters.AddParameter(GCodeParameters.Parameter.AxisY, y * bedHeight / rLayout.getHeight());
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        textViewConsole.setText("Axis X: " + x + " AxisY: " + y + '\n' +
                                "Real X =" + x * bedWidth / rLayout.getWidth() + " Real Y " + y * bedHeight / rLayout.getHeight() + '\n' +
                                "View X " + rLayout.getWidth() + " View Y " + rLayout.getHeight());
    }
    
    private void tap() {
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.FeedRate, seekBarZ.getProgress());
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.AxisZ, 10);
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);
        
        parameters.Reset();
        parameters.AddParameter(GCodeParameters.Parameter.AxisZ, 20);
        //while (semaphore != 1);
        //semaphore--;
        gcode.ControlledMove(parameters);     
    }

    /**
     * Starts the activity, using the supplied driver instance.
     * 
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver) {
        sDriver = driver;
        gcode = new GCodeProtocol(driver);
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

}

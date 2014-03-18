package com.hoho.android.usbserial.examples;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class GCodeProtocol {
    
    public enum Commands {
        RapidMove ("G0"),
        ControlledMove ("G1"), 
        MoveToOrigin ("G28"),
        // ...
        Dwell ("G4"),
        HeadOffset ("G10"),
        SetUnitsToInches ("G20"),
        SetUnitsToMilimeters ("G21"),
        SetToAbsolutePositioning ("G90"),
        SetToRelativePositioning ("G91"),
        SetPosition ("G92"),
        Stop ("M0"),
        Sleep ("M1");
        // ...
        
        private String value;
        private Commands(String value) {
            this.value = value;
        }
        private String ToString() {
            return value;
        }
    }

    private boolean pending;
    private UsbSerialDriver driver;
    
    public GCodeProtocol(UsbSerialDriver driver) {
        this.pending = false;
        this.driver = driver;
    }
    
    public void RapidMove(GCodeParameters parameters) {
        SendCommand(Commands.RapidMove.ToString() + parameters.ToString());
    }
    
    public void ControlledMove(GCodeParameters parameters) {
        SendCommand(Commands.ControlledMove.ToString() + parameters.ToString());
    }
    
    public void SetToAbsolutePositioning() {
        SendCommand(Commands.SetToAbsolutePositioning.ToString());
    }
    
    public void SetToRelativePositioning() {
        SendCommand(Commands.SetToRelativePositioning.ToString());
    }
    
    public void Homing(GCodeParameters parameters) {
        SendCommand(Commands.MoveToOrigin.ToString() + parameters.ToString());
    }
    
    public void SendCommand(String cmd) {
        try {
            if (!pending) {
                driver.write((cmd+"\n").getBytes(), 500);
                //pending = true;
            }
            
            
            /*byte[] dest = null;
            driver.read(dest, 500);
            if (dest.toString() == "ok") {
                pending = false;
            }*/
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        }
    }
}

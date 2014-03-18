package com.hoho.android.usbserial.examples;

public class GCodeParameters {
    
    private final String Separator = " ";

    public enum Parameter {
        AxisX ("X"),
        AxisY ("Y"), 
        AxisZ ("Z"),
        Extruder ("E"),
        FeedRate ("F"),
        P ("P"),
        R ("R"),
        S ("S");
        
        private String value;
        private Parameter(String value) {
                this.value = value;
        }
        private String ToString() {
            return value;
        }
    }
    
    private String parameters;
    
    public GCodeParameters() {
        this.parameters = "";
    }
    
    public GCodeParameters(String parameters) {
        this.parameters = parameters;
    }
    
    public String ToString() {
        return parameters;
    }
    
    public void AddParameter(Parameter parameter, int value) {
        parameters += Separator + parameter.ToString() + value;
    }

    public void AddParameter(Parameter parameter, float value) {
        parameters += Separator + parameter.ToString() + value;
    }
    
    public void Reset() {
        parameters = "";
    }
}

package dev.obrienlabs.weather.model;

public class RadarSite {

    public static final int COLOR_BLACK = -16777216;
    
    /** Radar doppler intensity values 0=black, 14=max  */
    public static final int PRECIP_INTENSITY_COLOR_CODES[] = { 
    -16777216,
    -6697729,
    -16737793,
    -16711834,
    -16724992,
    -16738048,
    -16751104,
    -205,
    -13312,
    -26368,
    -39424,
    -65536,
    -64871,    
    -6736948,
    -10092391
    };
    public static final int PRECIP_INTENSITY_COLOR_CODES_SIZE = PRECIP_INTENSITY_COLOR_CODES.length;
    
    public static final int COLOR_WHITE =  -65794;
    
}

package dev.obrienlabs.weather.model;

public class RadarSite {

    public static final int COLOR_BLACK = -16777216;
    
    /** Radar doppler intensity values 0=max, 14=min */
    public static final int PRECIP_INTENSITY_COLOR_CODES[] = {
        -10092391,
        -6736948,
        -64871,
        -65536,
        -39424,
        -26368,
        -13312,
        -205,
        -16751104,
        -16738048,
        -16724992,
        -16711834,
        -16737793,
        -6697729};
    public static final int PRECIP_INTENSITY_COLOR_CODES_SIZE = PRECIP_INTENSITY_COLOR_CODES.length;
    
    public static final int COLOR_WHITE =  -65794;
    
}

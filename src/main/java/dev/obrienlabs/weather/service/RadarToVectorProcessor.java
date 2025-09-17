package dev.obrienlabs.weather.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import dev.obrienlabs.weather.model.RadarSite;

public class RadarToVectorProcessor {

    public void reduceRadarImage(String site, String anInputFile, String anOutputFile) {
        BufferedImage input = null;
        BufferedImage reducedImage = null;
        input = loadImage(anInputFile);
        reducedImage = doFilter(0, input, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);
        //writeImage(reducedImage, anOutputFile);
        System.out.print(".");

    }
    
    private BufferedImage doFilter(int filter, BufferedImage input, int layer) {
        int height = input.getHeight();
        BufferedImage croppedImage = new BufferedImage(height, height, BufferedImage.TYPE_INT_RGB);
        
        computeFilter(input, 0, height, croppedImage, height);
        
        // erase red edge box last
        Graphics2D g = croppedImage.createGraphics(); 
        g.drawImage(croppedImage, 0,0,null);

        g.setColor(Color.BLACK);
        //g.drawLine(0,0,height-1, height-1);
        g.drawLine(0,0,0, height-1);
        g.drawLine(0,0,height-1, 0);
        g.drawLine(0,height-1, height-1, height -1);
        g.drawLine(height-1,0, height-1, height - 1);
        return croppedImage;
    }

 
    private void computeFilter(BufferedImage mSource, int mStart, int height, BufferedImage mDestination, int width) {
    	int pColor;
    	int mEnd = mStart + height;
    	for (int index = mStart; index < mEnd; index++) {
            for(int x=0;x<width;x++) {
                pColor = mSource.getRGB(x, index);
                for(int i=0; i<RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 0; i++) {                   
                    if(pColor == RadarSite.PRECIP_INTENSITY_COLOR_CODES[i]) {
                    	// TODO: USE NON-SYNCHRONIZED METHOD
                    	mDestination.setRGB(x, index, pColor);
                        i = RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE; // short circuit for loop
                    }
                }
            }
        }
    }   
    

    
	private BufferedImage loadImage(String filename) {
	    return loadImage(filename, false);
	}
	
	private BufferedImage loadImage(String filename, boolean verifyOnlyFlag) {
		File file = new File(filename);
		BufferedImage image = null;
		try {
			image = ImageIO.read(file);
		} catch (Exception ioe) {
		    if(!verifyOnlyFlag) {
		        System.out.println("missing: " + filename);
		        //ioe.printStackTrace();
		    }
		}
		return image;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

package dev.obrienlabs.weather.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import dev.obrienlabs.weather.model.RadarSite;

/**
 * Filter out everything except the radar levels from the image
 */
public class RadarPreProcessor {


	public BufferedImage loadImage(String filename) {
	    return loadImage(filename, false);
	}
	
	public BufferedImage loadImage(String filename, boolean verifyOnlyFlag) {

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
		if(null != image) {
			//System.out.println(image.toString());
		}
		return image;
	}

	public void writeImage(Image anImage, String filename) {
		try {
		File file = new File(filename);// + "." + format);
		// create dirs first
		ImageIO.write((BufferedImage)anImage, "gif", file);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void writeImage(Image anImage, String filename, String format) {
		try {
		File file = new File(filename + "." + format);
		ImageIO.write((BufferedImage)anImage, format, file);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
    public BufferedImage doFilter(int filter, BufferedImage input, int layer) {
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

 
    protected void computeFilter(BufferedImage mSource, int mStart, int height, BufferedImage mDestination, int width) {
        //boolean match = false;
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
    
    public void reduceRadarImage(String site, String anInputFile, String anOutputFile) {
        BufferedImage input = null;
        BufferedImage reducedImage = null;
        input = loadImage(anInputFile);
        reducedImage = doFilter(0, input, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);
        writeImage(reducedImage, anOutputFile);
        System.out.print(".");

    }
    
    public void reduceRadarImages(String site, String anInputDir, String anOutputDir) {
    	boolean overwrite = true;
        String filename = null;
        String inputDir = anInputDir + site +"/";
        String outputDir = anOutputDir;
        // read everything in the directory
        File dir = new File(inputDir);
        String filenameRoot = null;
        String outputPath = null;
        BufferedImage input = null;
        BufferedImage verify = null;
        BufferedImage reducedImage = null;
        long filesize = 0;
        System.out.println("_retrieving files from:... " + inputDir);
        try {
            File[] files = dir.listFiles();
            int counter = 0;
            if(null != files && files.length > 0) {
                System.out.println("_files: " + files.length + "\n");
                for(int i=0; i<files.length; i++) {
                    filename = files[i].getName();
                    filesize = files[i].length();
                    filenameRoot = filename.substring(0, filename.length() - 4);
                    // check if a filtered image already exists - skip then
                    outputPath = outputDir + site +"/" + filenameRoot + "_f";
                    verify = loadImage(outputPath + ".gif", true);
                    // overwrite always
                    if(overwrite || null == verify) {              
                        input = loadImage(inputDir + filename);
                        if(null != input && filesize > 0) {
                            reducedImage = doFilter(0, input, RadarSite.PRECIP_INTENSITY_COLOR_CODES_SIZE - 1);
                            writeImage(reducedImage, outputPath, "gif");
                            System.out.print(".");
                            counter = updateCounterPrivate(site, counter, i);
                        } else {
                            System.out.println("\n_Invalid filesize: " + filesize + " :" + filename);
                        }
                    } else {
                        // file exists
                        System.out.print("+");
                        counter = updateCounterPrivate(site, counter, i);
                    }
                }
            } else {
            	System.out.println("_Directory is missing: " + inputDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
    
    protected int updateCounterPrivate(String site, int counter, int count) {
        if(counter++ > 98) {
            System.out.print(site + "\n" + (count + 1));
            counter = 0;
        }
        return counter;        
    }
    
    public static void main(String[] argv) {
    	RadarPreProcessor processor = new RadarPreProcessor();
    	processor.reduceRadarImages("go", "/Users/michaelobrien/_download/cappi/", "/Users/michaelobrien/_download/cappi-processed/");
    }
}

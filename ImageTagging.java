import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import java.util.Map;
import java.util.HashMap;
import java.lang.Math;
import java.util.*;
import java.io.*;

public class ImageTagging implements PlugInFilter {
	
	private ImagePlus imp;
	private Map<Integer, String> referenceColors;
	private Map <String, Integer> colors;
	
	private ArrayList<String> tags;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if(arg.equals("about")) {
			IJ.showMessage("About image tagging", "tag the image");
			return DONE;
		}

		tags = new ArrayList<String>();
		
		colors = new HashMap<String, Integer>();
		referenceColors = new HashMap<Integer, String>();
		referenceColors.put(0xFF0000, "red");
		referenceColors.put(0xFFFF00, "yellow");
		referenceColors.put(0x3A9D23, "green");
		referenceColors.put(0x0091fe, "blue");
		referenceColors.put(0xFFFFFF, "white");
		referenceColors.put(0x000000, "black");
		referenceColors.put(0x4A2D0D, "brown");
		referenceColors.put(0xD3D3D3, "gray");
		referenceColors.put(0xFFA500, "orange");

		return DOES_ALL;
	}

	public void searchColors(ImageProcessor ip) {
		ColorSpaceConverter csc = new ColorSpaceConverter();

		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				int pixel = ip.getPixel(column, row);
				double[] currentPixelValues;

				currentPixelValues = new double[3];
				currentPixelValues[0] = (0xFF & (pixel >> 16));
				currentPixelValues[1] = (0xFF & (pixel >> 8));
				currentPixelValues[2] = (0xFF & pixel);

				double minDistance = Double.MAX_VALUE;
				String closestColor = null;
				int closestColorRGBValue = 0;

				for(Map.Entry colorEntry : referenceColors.entrySet()) {
					int colorReference = (Integer)colorEntry.getKey();
					String colorName = (String)colorEntry.getValue();

					double colorDistance;

					double[] referenceValues = new double[3];
					referenceValues[0] = (0xFF & (colorReference >> 16));
					referenceValues[1] = (0xFF & (colorReference >> 8));
					referenceValues[2] = (0xFF & colorReference);

					double r = (referenceValues[0] + currentPixelValues[0]) / 2.0;
					double r2 = Math.pow(Math.abs(referenceValues[0] - currentPixelValues[0]), 2);
					double g2 = Math.pow(Math.abs(referenceValues[1] - currentPixelValues[1]), 2);
					double b2 = Math.pow(Math.abs(referenceValues[2] - currentPixelValues[2]), 2) ;
					colorDistance = Math.sqrt(2 * r2
								  + 4 * g2
								  + 3 * b2
								  + (r * (r2 - b2)) / 256.0);
				

					if(colorDistance < minDistance) {
						minDistance = colorDistance;
						closestColor = colorName;
						closestColorRGBValue = colorReference;
					}
				}
				if (colors.containsKey(closestColor))
					colors.put(closestColor, colors.get(closestColor) + 1);
				else
					colors.put(closestColor, 1);
			}
		}
				
		String stats = "";
		for(Map.Entry colorCompter : colors.entrySet()) {
			String colorName = (String)colorCompter.getKey();
			int number = (Integer)colorCompter.getValue();
			float percent = ((float)number / (ip.getWidth() *  ip.getHeight()) * 100);
			if(percent >= 20)
				tags.add(colorName);
		}

	}
	
	public void saveTags() {
		try {
			FileWriter fw = new FileWriter(new File(imp.getOriginalFileInfo().directory + "/" + imp.getTitle().substring(0, imp.getTitle().indexOf(".")) + ".txt"));
			for(int i = 0; i < tags.size(); i++) {
				if(i != tags.size() - 1)
					fw.write(tags.get(i) + ", ");
				else
					fw.write(tags.get(i));
			}
			fw.close();
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void getBrightness(ImageProcessor ip) {
		ColorSpaceConverter csc = new ColorSpaceConverter();
		double sum = 0.0;

		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				sum += csc.RGBtoHSB(0xFF & (ip.getPixel(column, row) >> 16), 0xFF & (ip.getPixel(column, row) >> 8), 0xFF & ip.getPixel(column, row))[2];
			}
		}

		double meanValue = sum / (ip.getHeight() * ip.getWidth());

		if(meanValue * 100 >= 70)
			tags.add("Light");
		else if(meanValue * 100 <= 55)
			tags.add("Dark");
	}
	
	public void run(ImageProcessor ip) {
		IJ.showMessage("Start", "Tagging has started working... on " + imp.getTitle());
		
		searchColors(ip);
		getBrightness(ip);
		
		saveTags();
		
		IJ.showMessage("Done", "Tagging finished its work.");
	}
}

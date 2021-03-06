import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import java.util.Map;
import java.util.HashMap;
import java.lang.Math;

public class ColorExtractor implements PlugInFilter {
	private ImagePlus imp;
	private Map<Integer, String> referenceColors;
	private Map <String, Integer> colors;
	
	private enum ColorSpace {
		RGB,
		HSB,
		LAB
	};

	private ColorSpace colorSpace;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if(arg.equals("about")) {
			IJ.showMessage("About ColorExtractor", "Extracts the main colors of the image.");
			return DONE;
		}

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

		colorSpace = ColorSpace.RGB;

		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		IJ.showMessage("Start", "ColorExtractor has started working...");
		ColorSpaceConverter csc = new ColorSpaceConverter();

		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				int pixel = ip.getPixel(column, row);
				double[] currentPixelValues;

				if(colorSpace == ColorSpace.HSB) {
					currentPixelValues = csc.RGBtoHSB(0xFF & (pixel >> 16), 0xFF & (pixel >> 8), 0xFF & pixel);
				} else if(colorSpace == ColorSpace.LAB) {
					currentPixelValues = csc.RGBtoLAB(pixel);
				} else {
					currentPixelValues = new double[3];
					currentPixelValues[0] = (0xFF & (pixel >> 16));
					currentPixelValues[1] = (0xFF & (pixel >> 8));
					currentPixelValues[2] = (0xFF & pixel);
				}

				double minDistance = Double.MAX_VALUE;
				String closestColor = null;
				int closestColorRGBValue = 0;

				for(Map.Entry colorEntry : referenceColors.entrySet()) {
					int colorReference = (Integer)colorEntry.getKey();
					String colorName = (String)colorEntry.getValue();

					double colorDistance;

					 if(colorSpace == ColorSpace.HSB) {
						double[] referenceValues = csc.RGBtoHSB(0xFF & (colorReference >> 16), 0xFF & (colorReference >> 8), 0xFF & colorReference);

						colorDistance = Math.pow(Math.min(Math.abs(referenceValues[0] - currentPixelValues[0]), 360 - Math.abs(referenceValues[0] - currentPixelValues[0])), 2)
									  + Math.pow(Math.abs(referenceValues[1] - currentPixelValues[1]), 2)
									  + Math.pow(Math.abs(referenceValues[2] - currentPixelValues[2]), 2);
					} else if(colorSpace == ColorSpace.LAB) {
						double[] referenceValues = csc.RGBtoLAB(colorReference);

						colorDistance = Math.pow(Math.abs(referenceValues[0] - currentPixelValues[0]), 2)
									  + Math.pow(Math.abs(referenceValues[1] - currentPixelValues[1]), 2)
									  + Math.pow(Math.abs(referenceValues[2] - currentPixelValues[2]), 2);
					} else {
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
					}

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

				ip.putPixel(column, row, closestColorRGBValue);
			}
		}
		String stats = "";
		for(Map.Entry colorCompter : colors.entrySet()) {
			String colorName = (String)colorCompter.getKey();
			int number = (Integer)colorCompter.getValue();
			stats += "color : " + colorName + " has " + ((float)number / (ip.getWidth() *  ip.getHeight()) * 100) + " %";
		}
		IJ.showMessage("Done", stats);
		IJ.showMessage("Done", "ColorExtractor finished its work.");
	}
}

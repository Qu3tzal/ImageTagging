import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import java.util.Map;
import java.util.HashMap;

public class QualityExtractor implements PlugInFilter {
	private ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		if(arg.equals("about")) {
			IJ.showMessage("About QualityExtractor", "Extracts the quality of the image.");
			return DONE;
		}

		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		ColorSpaceConverter csc = new ColorSpaceConverter();
		double sum = 0.0;

		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				sum += csc.RGBtoHSB(0xFF & (ip.getPixel(column, row) >> 16), 0xFF & (ip.getPixel(column, row) >> 8), 0xFF & ip.getPixel(column, row))[2];
			}
		}

		double meanValue = sum / (ip.getHeight() * ip.getWidth());

		IJ.showMessage("Mean brightness", "Mean brightness = " + meanValue);
	}


}

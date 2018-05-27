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
	
	// On garde en attribut l'image active
	private ImagePlus imp;
	
	// On stock les couleurs de références pour les tags
	private Map<Integer, String> referenceColors;
	
	// Compteur qui associe un nombre de pixel à une couleur 
	private Map <String, Integer> colors;
	
	// Liste des tags associés à l'image
	private ArrayList<String> tags;

	// Mise en place du plugin
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		if(arg.equals("about")) {
			IJ.showMessage("About image tagging", "tag the image");
			return DONE;
		}

		// On initialise les variables
		tags = new ArrayList<String>();
		
		colors = new HashMap<String, Integer>();
		
		// On ajoute toutes les couleurs de références à la liste
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

	// fonction qui permet de chercher les couleurs de l'image
	// et ajoute les tags en fonction
	public void searchColors(ImageProcessor ip) {
		ColorSpaceConverter csc = new ColorSpaceConverter();

		// On parcours tous les pixels de l'image
		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				
				// Pour chaque pixels on le converti en RGB
				int pixel = ip.getPixel(column, row);
				double[] currentPixelValues;

				currentPixelValues = new double[3];
				currentPixelValues[0] = (0xFF & (pixel >> 16));
				currentPixelValues[1] = (0xFF & (pixel >> 8));
				currentPixelValues[2] = (0xFF & pixel);

				double minDistance = Double.MAX_VALUE;
				String closestColor = null;
				int closestColorRGBValue = 0;

				// Pour chaque pixel on regarde la couleur de référence la plus proche
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
					double b2 = Math.pow(Math.abs(referenceValues[2] - currentPixelValues[2]), 2);
					
					// Fonction de distance qui respecte le mieux la distance suivant la perception humaine via l'espace de couleur RGB
					colorDistance = Math.sqrt(2 * r2
								  + 4 * g2
								  + 3 * b2
								  + (r * (r2 - b2)) / 256.0);
				
					// On stock la couleur la plus proche
					if(colorDistance < minDistance) {
						minDistance = colorDistance;
						closestColor = colorName;
						closestColorRGBValue = colorReference;
					}
				}
				
				// On incrémente le compteur de la couleur la plus proche
				if (colors.containsKey(closestColor))
					colors.put(closestColor, colors.get(closestColor) + 1);
				else
					colors.put(closestColor, 1);
			}
		}
		
		// On regarde le % de présences de chaque couleurs de références
		for(Map.Entry colorCompter : colors.entrySet()) {
			String colorName = (String)colorCompter.getKey();
			int number = (Integer)colorCompter.getValue();
			float percent = ((float)number / (ip.getWidth() *  ip.getHeight()) * 100);
			
			// Si la couleur de référence est présente à plus de 20% on l'ajoute en tag
			if(percent >= 20)
				tags.add(colorName);
		}

	}
	
	// Fonction qui permet d'écrire tous les tags dans le fichier selon les contraintes demandées
	public void saveTags() {
		try {
			// On ouvre le flux vers le fichier nom_de_l'image.txt dans le dossier de l'image
			FileWriter fw = new FileWriter(new File(imp.getOriginalFileInfo().directory + "/" + imp.getTitle().substring(0, imp.getTitle().indexOf(".")) + ".txt"));
			
			// Pour chaque tags on l'écrit dans le fichier séparé par une ,
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
	
	// Fonction qui permet de récupérer la luminosité de l'image et d'ajouter le tag "Dark" ou "Light" suivant la luminosité 
	public void getBrightness(ImageProcessor ip) {
		
		ColorSpaceConverter csc = new ColorSpaceConverter();
		double sum = 0.0;

		// On convertie chaque pixels en HSB et on récupère la 3 ème valeur qui correspond à la luminosité
		for(int row = 0 ; row < ip.getHeight() ; row++) {
			for(int column = 0 ; column < ip.getWidth() ; column++) {
				sum += csc.RGBtoHSB(0xFF & (ip.getPixel(column, row) >> 16), 0xFF & (ip.getPixel(column, row) >> 8), 0xFF & ip.getPixel(column, row))[2];
			}
		}

		// On fait la moyenne de luminosité de l'image
		double meanValue = sum / (ip.getHeight() * ip.getWidth());

		// Si la luminosité est supérieur à 70% on ajoute le tag "Light"
		// Si la luminosité est inférieur à 55% on ajoute le tag "Dark"
		if(meanValue * 100 >= 70)
			tags.add("Light");
		else if(meanValue * 100 <= 55)
			tags.add("Dark");
	}
	
	// Fonction principal du plugin
	public void run(ImageProcessor ip) {
		// On affiche un message pour dire que le traitement commence.
		IJ.showMessage("Start", "Tagging has started working... on " + imp.getTitle());
		
		// On cherche les couleurs dominantes.
		searchColors(ip);
		
		// On cherche la luminosité
		getBrightness(ip);
		
		// On enregistre les tags dans le fichier
		saveTags();
		
		// On affiche un message pour dire que le traitement est fini.
		IJ.showMessage("Done", "Tagging finished its work.");
	}
}

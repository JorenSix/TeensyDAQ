package be.ugent.ipem.teensydaq.util;

import java.awt.Color;


/**
 * Utility class to access (read and write) configuration settings. There are
 * utility methods for booleans, doubles and integers. It automatically converts
 * directory separators with the correct file separator for the current
 * operating system.
 * 
 * @author Joren Six
 */
public final class Configuration {
	
	public static final Color[] colorMap =    {   
			new Color(0xFFFFB300), //Vivid Yellow
		    new Color(0xFF803E75), //Strong Purple
		    new Color(0xFFFF6800), //Vivid Orange
		    new Color(0xFFA6BDD7), //Very Light Blue
		    new Color(0xFFC10020), //Vivid Red
		    new Color(0xFFCEA262), //Grayish Yellow
		    new Color(0xFF817066), //Medium Gray

		    //The following will not be good for people with defective color vision
		    new Color(0xFF007D34), //Vivid Green
		    new Color(0xFFF6768E), //Strong Purplish Pink
		    new Color(0xFF00538A), //Strong Blue
		    new Color(0xFFFF7A5C), //Strong Yellowish Pink
		    new Color(0xFF53377A), //Strong Violet
		    new Color(0xFFFF8E00), //Vivid Orange Yellow
		    new Color(0xFFB32851), //Strong Purplish Red
		    new Color(0xFFF4C800), //Vivid Greenish Yellow
		    new Color(0xFF7F180D), //Strong Reddish Brown
		    new Color(0xFF93AA00), //Vivid Yellowish Green
		    new Color(0xFF593315), //Deep Yellowish Brown
		    new Color(0xFFF13A13), //Vivid Reddish Orange
		    new Color(0xFF232C16) //Dark Olive Green}
	};
}

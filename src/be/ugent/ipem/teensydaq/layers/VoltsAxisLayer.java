package be.ugent.ipem.teensydaq.layers;

import java.awt.Color;
import java.awt.Graphics2D;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.layers.Layer;
import be.tarsos.dsp.ui.layers.LayerUtilities;

public class VoltsAxisLayer implements Layer {

	private final CoordinateSystem cs;
	private final boolean drawLabel;
	
	public VoltsAxisLayer(CoordinateSystem cs,boolean drawlabel) {
		this.cs = cs;
		this.drawLabel = drawlabel;
	}
	
	public VoltsAxisLayer(CoordinateSystem cs) {
		this(cs,true);
	}

	public void draw(Graphics2D graphics){
		
		//draw legend
		graphics.setColor(Color.black);
		int minX = Math.round(cs.getMin(Axis.X));
		int maxY = Math.round(cs.getMax(Axis.Y));
		
		int wideMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,8, true));
		int smallMarkWidth = Math.round(LayerUtilities.pixelsToUnits(graphics,4, true));
		int textOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, true));	
		int textLabelOffset = Math.round(LayerUtilities.pixelsToUnits(graphics,12, false));
		
		
		int minValue = (int) cs.getMin(Axis.Y);
		int maxValue = (int) cs.getMax(Axis.Y);
		int difference = maxValue - minValue;
		
		if(difference > 2000){
			//Every 0.1 and 1 Volts
			for(int i = minValue ; i < maxValue ; i++){
				if(i%1000 == 0){
					graphics.drawLine(minX, i, minX+wideMarkWidth,i);
					String text = String.valueOf(i/1000);				
					LayerUtilities.drawString(graphics,text,minX+textOffset,i,false,true,null);
				} else if(i%100 == 0){			
					graphics.drawLine(minX, i, minX+smallMarkWidth,i);
				}
			}
		}else{
			//Every 0.01 and 0.1 Volts
			for(int i = minValue ; i < maxValue ; i++){
				if(i%100 == 0){
					graphics.drawLine(minX, i, minX+wideMarkWidth,i);
					String text = String.valueOf(i/1000.0);				
					LayerUtilities.drawString(graphics,text,minX+textOffset,i,false,true,null);
				} else if(i%10 == 0){			
					graphics.drawLine(minX, i, minX+smallMarkWidth,i);
				}
			}
		}
		
		if(drawLabel){
			LayerUtilities.drawString(graphics,"Votage (Volts)",minX+textOffset,maxY-textLabelOffset,false,true,Color.white);
		}
	}

	@Override
	public String getName() {
		return "Voltage Axis";
	}

}




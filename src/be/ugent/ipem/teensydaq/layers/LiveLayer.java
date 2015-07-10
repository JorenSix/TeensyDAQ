package be.ugent.ipem.teensydaq.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.layers.Layer;

public class LiveLayer implements Layer, MouseListener, KeyListener{

	/**
	 * The data to plot, expects the keys to be in milliseconds and the data
	 * in the correct unit to plot. 
	 */
	private final ConcurrentSkipListMap<Double,Double[]> dataToPlot;	
	private final Repainter repainter;
	private final Color[] colorMap;
	private final CoordinateSystem cs;
	private boolean follow = true;
	private int startIndexInColorMap;
	
	private boolean[] streamsToPlot;
	
	
	public LiveLayer(ConcurrentSkipListMap<Double, Double[]> dataToPlot,ViewPort viewPortToUpdate, CoordinateSystem cs, Color[] colorMap) {
		this.dataToPlot = dataToPlot;
		this.cs = cs;
		this.colorMap = colorMap;
		streamsToPlot=null;
		repainter = new Repainter(dataToPlot, viewPortToUpdate, cs);
		startIndexInColorMap = 0;
		Thread repaintThread = new Thread(repainter,"Live repaint thread");
		repaintThread.start();
	}
	
	public void setStartIndexInColorMap(int startIndex){
		this.startIndexInColorMap = startIndex;
	}
	
	public void setStreamsToPlot(boolean[] streamsToPlot){
		this.streamsToPlot = streamsToPlot;
	}
	
	
	private static class Repainter implements Runnable{
		private final ConcurrentSkipListMap<Double,Double[]> dataToPlot;
		private final ViewPort viewPortToUpdate;
		//private final CoordinateSystem cs;
		private final int repaintRequestsPerSecond = 24;//repaints per second
		private boolean repainting;
		
		public Repainter(ConcurrentSkipListMap<Double,Double[]> dataToPlot,ViewPort viewPortToUpdate, CoordinateSystem cs){
			this.viewPortToUpdate = viewPortToUpdate;
			this.dataToPlot = dataToPlot;
			repainting = true;
			//this.cs = cs;
		}
		
		public void run() {
			while(true){
				
				if(dataToPlot.size() > 0){
					//forces repaint
					//double maxTimeAxis = cs.getMax(Axis.X);
					//double maxTimeData = dataToPlot.lastKey();
					//double timeDifference = maxTimeAxis-maxTimeData;
					
					if(repainting){
						viewPortToUpdate.drag(0, 0);
					}
					try {
						//25 fps
						Thread.sleep(1000/repaintRequestsPerSecond);
					} catch (InterruptedException e) {
						//ignore
					}
				}
			}
		}
		
		public void setRepainting(boolean repainting){
			this.repainting = repainting;
		}
	}
	
	public void setAutomaticRepainting(boolean repainting){
		repainter.setRepainting(repainting);
	}

	


	@Override
	public void draw(Graphics2D graphics) {
		
		if(dataToPlot.size() > 0){
			dataToPlot.lastEntry().getValue();
			
			int[] prevVoltage = null;
			int prevTime = 0;
			
			double currentMinTime = cs.getMin(Axis.X);
			double currentMaxTime = cs.getMax(Axis.X);
			
			double currentDataMaxTime = dataToPlot.lastKey();
		
			if(currentDataMaxTime > currentMaxTime && follow){
				double delta = currentDataMaxTime - currentMaxTime;
				cs.setMin(Axis.X, (float) (currentMinTime + delta));
				cs.setMax(Axis.X, (float) (currentMaxTime + delta));
			}
			
			currentMinTime = cs.getMin(Axis.X);
			currentMaxTime = cs.getMax(Axis.X);
			
			boolean first = true;
			
			Set<Entry<Double,Double[]>> set =  dataToPlot.subMap(currentMinTime, currentMaxTime).entrySet();
			Iterator<Entry<Double,Double[]>> it = set.iterator();
			while(it.hasNext()){
				Entry<Double,Double[]> entry = it.next();
				int currentTime = Math.round(entry.getKey().floatValue());
				int[] currentVoltage = new int[entry.getValue().length];
				
				if(first){
					first = false;
					prevVoltage = new int[entry.getValue().length];
					for(int i = 0 ; i < entry.getValue().length ; i++){
						if(streamsToPlot ==null || streamsToPlot[i]){
							currentVoltage[i]= Math.round(entry.getValue()[i].floatValue());
						}
					}
				}else{
					for(int i = 0 ; i < entry.getValue().length ; i++){
						if(streamsToPlot ==null || streamsToPlot[i]){
							currentVoltage[i] =  Math.round(entry.getValue()[i].floatValue());
							graphics.setColor(colorMap[startIndexInColorMap+i]);
							graphics.drawLine(prevTime, prevVoltage[i], currentTime, currentVoltage[i]);
						}
					}
				}
				prevTime = currentTime;
				prevVoltage = currentVoltage;
			}
		}
	}
	
	public void toggleFollowing(){
		follow = !follow;
		if(!follow){
			repainter.setRepainting(false);
		}else{
			repainter.setRepainting(true);
		}
	}
	
	public void setFollowing(boolean following){
		follow = following;
		if(!follow){
			repainter.setRepainting(false);
		}else{
			repainter.setRepainting(true);
		}
	}
	
	public boolean isFollowing(){
		return follow;
	}

	@Override
	public String getName() {
		return "Live layer";
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		repainter.setRepainting(false);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		repainter.setRepainting(true);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if(e.getKeyChar()=='f'){
			 toggleFollowing();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {		
	}

	@Override
	public void keyReleased(KeyEvent e) {		
	}
}
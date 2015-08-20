package be.ugent.ipem.teensydaq.util;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import be.ugent.ipem.teensydaq.DAQDataHandler;
import be.ugent.ipem.teensydaq.DAQSample;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class PeakDetectionToOSCDataHandler implements DAQDataHandler {
	
	boolean climbing = false;
	double prevMin = 0;
	double prevMax =0;
	double largestPeak = 0;
	double smallestPeak = 100;
	

	private LinkedList<Double> historyUp = new LinkedList<Double>();
	private int historyUpSize = 20;//at 200Hz = 0.1s
	private double upThreshold = 0.2;
	
	
	private LinkedList<Double> historyDown = new LinkedList<Double>();
	private int historyDownSize = 3;
	private double downThreshold = 0.015;
	
	private double peakValue = 0;
	private double peakTime = 0;
	
	
	private final int channel;
	OSCPortOut sender;
	//OSCPortOut djogger;
	
	public PeakDetectionToOSCDataHandler(int channel){
		this.channel = channel;
		try {
			sender = new OSCPortOut(InetAddress.getLocalHost(),9001);
			//djogger = new OSCPortOut(InetAddress.getByName("157.193.92.43"),6669);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	

	
	@Override
	public void handle(final DAQSample sample) {
		//reduce noise by removing every change under 0.1V
		double currentValue = sample.data[channel];
		
		
		
		historyUp.addLast(currentValue);
		if(historyUp.size() == historyUpSize){
			historyUp.removeFirst();
		}
		
		historyDown.addLast(currentValue);
		if(historyDown.size() == historyDownSize){
			historyDown.removeFirst();
		}
		
		if(!climbing && currentValue - historyUp.getFirst() > upThreshold){
			climbing = true;
			peakValue = currentValue;
		}
		
		if(climbing){
			if(currentValue > peakValue){
				peakValue = currentValue;
				peakTime = sample.timestamp; 
			}
		}
		
		if(climbing && historyDown.getFirst() - currentValue  > downThreshold){
			double latency = (sample.timestamp - peakTime) * 1000;
			if(currentValue > 0.20 && latency < 100){
				largestPeak = Math.max(largestPeak, currentValue);
				smallestPeak = Math.min(smallestPeak, currentValue);
				final float factor = (float) ((currentValue-smallestPeak)/(largestPeak-smallestPeak));
				System.out.println("Detected peak of " + peakValue + "V at " + peakTime + " delay of " + latency + "ms"  + " factor: " + factor);
				sendOSCMessage(new Float(sample.timestamp), new Float(factor));
			}
			historyDown.clear();
			historyUp.clear();
			climbing = false;
			peakValue = 0;
		}
	}

	@Override
	public void stopDataHandler() {
		
	}
	
	private void sendOSCMessage(final Float timestamp, final Float factor){
		new Thread(new Runnable(){
			@Override
			public void run() {
				Collection<Object> args = new ArrayList<Object>();
				//args.add(timestamp);
				args.add(Float.valueOf(channel+1));
				args.add(factor);
				OSCMessage msg = new OSCMessage("/peak", args);
				 try {
					sender.send(msg);
				 } catch (Exception e) {
					 System.out.println(e.getMessage());
				 }
				args = new ArrayList<Object>();
				args.add(new Integer(1));
				msg = new OSCMessage("/peak", args);
				try {
					//djogger.send(msg);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
					 
			}
		}).start();			
	}
	


}

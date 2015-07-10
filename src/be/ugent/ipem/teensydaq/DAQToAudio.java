package be.ugent.ipem.teensydaq;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class DAQToAudio implements DAQDataHandler{
	
	final static int audioBufferSize = 2000; 
	int sampleIndex = 0;
	
	
	final AudioRunner audioRunner;
	
	private final Deque<Float> movingAverageWindow;
	private final Deque<Float> sampleBuffer;
	private final float movingAverageWindowSize;
	private double currentAverageSum;
	boolean[] channelsToListenTo;
	
	
	//private int writeIndex = 0;
	
	
	public DAQToAudio(int sampleRate,boolean[] channelsToListenTo){
		
		movingAverageWindowSize = 5 * sampleRate;//2 seconds
		movingAverageWindow = new LinkedList<Float>();
		
		sampleBuffer = new LinkedList<Float>();
		
		this.channelsToListenTo = channelsToListenTo;
		
		audioRunner = new AudioRunner(sampleRate);
		new Thread(audioRunner,"Audio Runner").start();
	}
	
	@Override
	public void handle(DAQSample sample) {
		
		float numberOfChannels = 0;
		double scaledData = 0;
		for(int i = 0 ; i < channelsToListenTo.length ; i++){
			if(channelsToListenTo[i]){
				//scale to [-1,1] 
				double minimum = sample.ranges[i].getMinimum();
				double maximum = sample.ranges[i].getMaximum();
				double difference = maximum - minimum;
				scaledData  =  scaledData + ( (sample.data[i] - minimum)/difference - 1.0);
				numberOfChannels++;
			}
		}
				
		
		float audioData = (float) scaledData / numberOfChannels ;
		movingAverageWindow.addLast(audioData);
		currentAverageSum += audioData;		
		if(movingAverageWindow.size()>movingAverageWindowSize){
			currentAverageSum -= movingAverageWindow.removeFirst();
		}
		sampleBuffer.addLast(audioData);
		
		if(sampleIndex > audioBufferSize * 2.5 && sampleIndex % audioBufferSize == 0){
			float movingAverage = (float) (currentAverageSum/movingAverageWindow.size());
			//System.out.println(movingAverage);
			float[] audioBuffer = new float[audioBufferSize];
			for(int i = 0 ; i < audioBufferSize; i++){
				audioBuffer[i]=sampleBuffer.removeFirst();
			}
			audioRunner.processAudioBuffer(audioBuffer,movingAverage);
		}
		sampleIndex++;
	}
	
	private static class AudioRunner implements Runnable{
		
		float audioData[] = new float[audioBufferSize];
		private AudioPlayer p;
		private WaveformWriter writer;
		private TarsosDSPAudioFormat format;
		private final Random rnd = new Random();
		
		public AudioRunner(int sampleRate){
			
			final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);
			this.format  = JVMAudioInputStream.toTarsosDSPFormat(audioFormat);
			try {
				p = new AudioPlayer(audioFormat, 2000);
				writer = new WaveformWriter(format, rnd.nextInt(1000) + "_test.wav");
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
		
		public void processAudioBuffer(final float[] audioData,float movingAverage){
			
			//make the data zero mean
			for(int i = 0 ; i < audioData.length ; i++){
				audioData[i]= audioData[i]-movingAverage;
				//amplify!
				audioData[i] = audioData[i];
			}
			this.audioData = audioData;
		}

		@Override
		public void run() {
			while(true){
			if(audioData !=null){
				AudioEvent audioEvent = new AudioEvent(format);
				audioEvent.setFloatBuffer(audioData);
				p.process(audioEvent);
				writer.process(audioEvent);
				audioData = null;
			}else{
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			}
		}
	}

	

	@Override
	public void stopDataHandler() {
		// TODO Auto-generated method stub
		
	}

	

}

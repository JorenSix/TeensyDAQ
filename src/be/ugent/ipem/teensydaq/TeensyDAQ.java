package be.ugent.ipem.teensydaq;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jssc.SerialPortException;
import be.ugent.ipem.teensydaq.util.SerialPortReader;
import be.ugent.ipem.teensydaq.util.SerialPortReader.SerialDataLineHandler;


public class TeensyDAQ implements SerialDataLineHandler{
	

	private static final Logger LOG = Logger.getLogger(TeensyDAQ.class.getName());
	
	private static final  float REFERENCE_VOLTAGE = 3.3f;//v
	private static final float STEPS = 8192f;//2^13
	private static final float SCALER  = REFERENCE_VOLTAGE/STEPS;
	
	private float timeScaler; //period in seconds (1/sample rate)
	
	private SerialPortReader reader;
	private final List<DAQDataHandler> dataHandlers;
	private final int startChannel;
	private final int numberOfChannels;
	
	private final DAQValueRange[] ranges;
	private int sampleRate;
	
	private long firstTimeIndex = -100;
	private long prevTimeIndex = -100;
	
	
	public TeensyDAQ(int sampleRate,String portName,int startChannel,int numberOfChannels){
		dataHandlers = new ArrayList<DAQDataHandler>();
		this.numberOfChannels = numberOfChannels;
		this.startChannel = startChannel;
		ranges = new DAQValueRange[numberOfChannels];
		for(int i = 0 ; i < ranges.length ; i++){
			ranges[i] = DAQValueRange.ZERO_TO_3_POINT_3_VOLTS;
		}
		reader = new SerialPortReader(portName, this);
		this.sampleRate = sampleRate;
	}
	
	
	public void start() throws SerialPortException{
		try{
			reader.open();
			reader.write(String.format("SET SR %04d\n", sampleRate));
			//wait for the sample rate change to finish
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			
			}
			timeScaler = (float) (1.0/(float)this.sampleRate);
			reader.start();
		}catch (SerialPortException e) {
			firstTimeIndex = -100;
			previousMeasurements = null;
			throw e;
		}		
	}
	
	public void stop(){
		reader.stop();
		firstTimeIndex = -100;
		previousMeasurements = null;
	}
	
	public void addDataHandler(DAQDataHandler handler){
		dataHandlers.add(handler);
	}
	
	double[] previousMeasurements;

	@Override
	public void handleSerialDataLine(int lineNumber, String lineData) {
		//first lines are unreliable
		if(lineNumber < 60)
		return;
		
		String[] lineDataValues = lineData.split(" ");
		long timeIndex = Long.parseLong(lineDataValues[0].trim(),16);
		if(firstTimeIndex == -100){
			firstTimeIndex = timeIndex;
		}
		
		timeIndex = timeIndex - firstTimeIndex;
		double timeStampInS = timeIndex * timeScaler;
		
		
		//check if the timeIndex is equal to the prev time index +1
		//if not samples have been dropped!
		if(prevTimeIndex>0 && prevTimeIndex + 1 != timeIndex){
			long jumpSize = (timeIndex - prevTimeIndex);
			LOG.severe("Unexpected time index jump " + jumpSize + " from " + prevTimeIndex + " to " + timeIndex + ". Samples dropped?");
			LOG.severe("  Parsed line (" +  lineNumber + ") :" + lineData);
			if(previousMeasurements!=null){
				LOG.severe("  Will send the previous data " + jumpSize + " times");
				for(int i = 1 ; i < jumpSize ; i++){
					double fixedTimeStampInS = (prevTimeIndex + i) * timeScaler;
					final DAQSample sample = new DAQSample(fixedTimeStampInS, previousMeasurements, ranges);
					for(DAQDataHandler handler : dataHandlers){
						handler.handle(sample);
					}
				}
			}else{
				LOG.severe("  Unable to send the previous data!");
			}
		}

		double[] measurements = new double[numberOfChannels];
		
		
		for(int i = 0  ; i < numberOfChannels ; i++){
			measurements[i] = Integer.parseInt(lineDataValues[i+1+startChannel].trim(),16) * SCALER;//voltage
		}
		final DAQSample sample = new DAQSample(timeStampInS, measurements, ranges);
		for(DAQDataHandler handler : dataHandlers){
			handler.handle(sample);
		}
		prevTimeIndex = timeIndex;
		previousMeasurements = measurements;
	}
}

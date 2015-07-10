package be.ugent.ipem.teensydaq;

import java.util.ArrayList;
import java.util.List;

import be.ugent.ipem.teensydaq.util.SerialPortReader;
import be.ugent.ipem.teensydaq.util.SerialPortReader.SerialDataLineHandler;


public class TeensyDAQ implements SerialDataLineHandler{
	

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
	
	
	public void start(){
		timeScaler = (float) (1.0/(float)this.sampleRate);
		reader.start();
		reader.write(String.format("SET SR %04d\n", sampleRate));
	}
	
	public void stop(){
		reader.stop();
		firstTimeIndex = -100;
	}
	
	public void addDataHandler(DAQDataHandler handler){
		dataHandlers.add(handler);
	}

	@Override
	public void handleSerialDataLine(int lineNumber, String lineData) {
		String[] lineDataValues = lineData.split(" ");
		long timeIndex = Long.parseLong(lineDataValues[0].trim(),16);
		if(firstTimeIndex < 0){
			firstTimeIndex = timeIndex;
		}
		if(firstTimeIndex > timeIndex){
			firstTimeIndex = timeIndex;
		}
		timeIndex = timeIndex - firstTimeIndex;
		double timeStampInS = (timeIndex * timeScaler);

		double[] measurements = new double[numberOfChannels];
		
		for(int i = 0  ; i < numberOfChannels ; i++){
			measurements[i] = Integer.parseInt(lineDataValues[i+1+startChannel].trim(),16) * SCALER;//voltage
		}
		final DAQSample sample = new DAQSample(timeStampInS, measurements, ranges);
		
		Runnable r = new Runnable(){
			@Override
			public void run() {
				for(DAQDataHandler handler : dataHandlers){
					handler.handle(sample);
				}
			}
		};
		new Thread(r,lineNumber + " data handler notify thread").run();
	}
}

package be.ugent.ipem.teensydaq;

public class DAQSample {
	
	/**
	 * The time stamp in seconds
	 */
	public final double timestamp; //in seconds
	public final double[] data;
	public final DAQValueRange[] ranges;
	
	
	public DAQSample(double timestamp,double[] sampleData, DAQValueRange[] ranges) {
		this.timestamp = timestamp; //in seconds
		this.data = sampleData;
		this.ranges = ranges;
	}
}

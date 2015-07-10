package be.ugent.ipem.teensydaq;



public enum DAQValueRange {
	NEGATIVE_0_POINT_625_TO_0_POINT_625_VOLTS(-0.625,0.625),
	NEGATIVE_1_POINT_25_TO_1_POINT_25_VOLTS(-1.25,1.25),
	NEGATIVE_2_POINT_5_VOLT_TO_2_POINT_5_VOLT(-2.5,2.5),
	NEGATIVE_5_POINT_0_TO_5_POINT_0_VOLTS(-5,5),
	NEGATIVE_10_POINT_0_TO_10_POINT_0_VOLTS(-10,10),
	

	ZERO_TO_1_POINT_25_VOLTS(0,1.25),
	ZERO_TO_2_POINT_5_VOLTS(0,2.5),
	ZERO_TO_3_POINT_3_VOLTS(0,3.3),
	ZERO_TO_5_POINT_0_VOLTS(0,5),
	ZERO_TO_10_POINT_0_VOLTS(0,10),
	;
	
	
	private final double minimum;
	private final double maximum;
	
	private DAQValueRange(double min, double max){
		this.minimum = min;
		this.maximum = max;
	}
	
	public double getMinimum(){
		return minimum;
	}
	
	public double getMaximum(){
		return maximum;
	}
	
	public String toString(){
		return minimum + " ~ " + maximum + "V";
	}
}

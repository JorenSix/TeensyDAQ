package be.ugent.ipem.teensydaq;

public interface DAQDataHandler {
	
	public void handle(DAQSample sample);
	public void stopDataHandler();

}

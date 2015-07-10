package be.ugent.ipem.teensydaq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DAQCSVFileWriter implements DAQDataHandler{
	public static final String CSVSEPARATOR = ",";
	
	private PrintWriter writer;
	private double previousWrite;
	
	public DAQCSVFileWriter(String fileName) throws IOException{
		FileWriter file = new FileWriter(fileName);
		BufferedWriter bf = new BufferedWriter(file);
		writer = new PrintWriter(bf);
		previousWrite = 0;
		
		Runtime.getRuntime().addShutdownHook( new Thread( "CloseCSVFileHook" ){
			@Override
			public void run(){
				close();
			}			
		});
		
	}
	
	private void close() {
		writer.flush();
		writer.close();
	}

	@Override
	public void handle(DAQSample sample) {
		//write the data
		writer.print(sample.timestamp);	
		for(int i = 0 ; i < sample.data.length; i++){
			writer.print(CSVSEPARATOR);
			writer.print(sample.data[i]);
		}
		writer.println();
		
		//every second flush the stream, write new
		//samples to disk
		if((sample.timestamp - previousWrite) > 2){
			writer.flush();
		}
	}
	
	@Override
	public void stopDataHandler() {
		close();
	}

}

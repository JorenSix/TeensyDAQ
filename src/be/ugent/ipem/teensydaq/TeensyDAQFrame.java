package be.ugent.ipem.teensydaq;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.tarsos.dsp.ui.Axis;
import be.tarsos.dsp.ui.AxisUnit;
import be.tarsos.dsp.ui.CoordinateSystem;
import be.tarsos.dsp.ui.LinkedPanel;
import be.tarsos.dsp.ui.ViewPort;
import be.tarsos.dsp.ui.ViewPort.ViewPortChangedListener;
import be.tarsos.dsp.ui.layers.BackgroundLayer;
import be.tarsos.dsp.ui.layers.DragMouseListenerLayer;
import be.tarsos.dsp.ui.layers.SelectionLayer;
import be.tarsos.dsp.ui.layers.TimeAxisLayer;
import be.tarsos.dsp.ui.layers.ZoomMouseListenerLayer;
import be.ugent.ipem.teensydaq.layers.LiveLayer;
import be.ugent.ipem.teensydaq.layers.MarkCenterLayer;
import be.ugent.ipem.teensydaq.layers.VoltsAxisLayer;
import be.ugent.ipem.teensydaq.util.Configuration;
import be.ugent.ipem.teensydaq.util.FileDrop;
import be.ugent.ipem.teensydaq.util.SerialPortReader;


public class TeensyDAQFrame extends JFrame implements ViewPortChangedListener, DAQDataHandler{

	private static final String csvSeparator = ",";
	private static final Logger LOG = Logger.getLogger(TeensyDAQFrame.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = -7096578086282090881L;
	private static final int MAX_ANALOG_INPUTS = 8;
	
	private final ConcurrentSkipListMap<Double, Double[]> data = new ConcurrentSkipListMap<Double, Double[]>();
	private final Color[] colorMap =  Configuration.colorMap;

	private final LinkedPanel linkedPanel;
	private final CoordinateSystem cs;
	private final MarkCenterLayer markCenterLayer;
	private final LiveLayer liveLayer;
	private TeensyDAQ daq;
	
	private final JLabel statusBar;
	private final JComboBox<String> serialPort;
	private final JTextField fileNameField;
	private final JSpinner sampleRateSpinner;
	private JButton startButton;
	private JButton stopButton;
	private JSpinner recordingIDSpinner;
	private int recordingId;
	
	public TeensyDAQFrame(){
		BorderLayout layout = new BorderLayout();
	
		this.setLayout(layout);
		this.setTitle("Teensy DAQ");
		
		
		this.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	stopDAQ();
		    	System.exit(0);
		    }
		});
		
		sampleRateSpinner = new JSpinner(new SpinnerNumberModel(500.0,0.0,40000,1));
		sampleRateSpinner.setToolTipText("The sample rate in Hz");
		
		recordingIDSpinner = new JSpinner(new SpinnerNumberModel(1,1,8000,1));
		sampleRateSpinner.setToolTipText("The recording id can be used in the file name field");
		
		fileNameField = new JTextField();
		fileNameField.setToolTipText("%d is replaced with the recording id");
		String[] serialPorts = SerialPortReader.getSerialPorts();
		serialPort=new JComboBox<String>(serialPorts);
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				while(true){
				try {
					Thread.sleep(1500);
					String[] serialPorts = SerialPortReader.getSerialPorts();
					boolean portsStillTheSame = false;
					if(serialPorts.length == serialPort.getItemCount()){
						portsStillTheSame = true;
						for(int i = 0 ; i < serialPort.getItemCount(); i++){
							String portName = serialPort.getItemAt(i);
							portsStillTheSame = portsStillTheSame && portName.equals(serialPorts[i]);
						}
					}
					if(!portsStillTheSame){
						LOG.info("Refresh serial port info, update needed!");
						MutableComboBoxModel<String> model=(MutableComboBoxModel<String>) serialPort.getModel();
						int items = serialPort.getItemCount();
						for(int i = 0 ; i < items; i++){
							model.removeElementAt(0);
						}
						for(String port:serialPorts){
							model.addElement(port);
						}
						
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				}
			}
			
		},"Serial port check").start();
		
		
		sampleRateSpinner.setEnabled(true);


		cs =  new CoordinateSystem(AxisUnit.FREQUENCY, -10000, 10000);		
		linkedPanel = new LinkedPanel(cs);
		
		setAxisRanges();
	
		markCenterLayer = new MarkCenterLayer(cs);
		liveLayer = new LiveLayer(data,linkedPanel.getViewPort(), cs,colorMap);
		markCenterLayer.setEnabled(false);
			
		linkedPanel.getViewPort().setOnlyZoomXAxisWithMouseWheel(true);
		linkedPanel.addLayer(new BackgroundLayer(cs));
		linkedPanel.addLayer(new ZoomMouseListenerLayer());
		linkedPanel.addLayer(new DragMouseListenerLayer(cs));
		linkedPanel.addLayer(new BackgroundLayer(cs));
		linkedPanel.addLayer(new TimeAxisLayer(cs));
		linkedPanel.addLayer(new VoltsAxisLayer(cs));
		linkedPanel.addLayer(new SelectionLayer(cs));
		linkedPanel.addLayer(markCenterLayer);
		linkedPanel.addLayer(liveLayer);
		
		
		linkedPanel.setBorder(BorderFactory.createBevelBorder(1));
		linkedPanel.getViewPort().addViewPortChangedListener(this);
		
		//add support for drag and drop csv files
		new FileDrop(null, linkedPanel, /*dragBorder,*/ new FileDrop.Listener(){   
			public void filesDropped( java.io.File[] files ){   
				for( int i = 0; i < files.length; i++) {   
					final File fileToAdd = files[i];
					new Thread(new Runnable(){
						@Override
						public void run() {
							statusBar.setText("Adding " + fileToAdd.getPath()  + "...");
		                	try {
								openFile(fileToAdd);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                	statusBar.setText("Added " + fileToAdd.getPath()  + ".");
						}}).start();
					try {
						Thread.sleep(60);
					} catch (InterruptedException e) {
					}
                }
			}
        });
			
		statusBar = new JLabel("  ");
		statusBar.setText("Start a live recording or drag and drop a csv file.");
		statusBar.setEnabled(false);
		//statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
		statusBar.setFont(new Font("Monospaced",Font.PLAIN,11));
		
		//wrapper panel
		JPanel signalPanel = new JPanel(new BorderLayout());
		signalPanel.add(createSignalConfigurationPanel(),BorderLayout.NORTH);
		
		this.add(new JScrollPane(signalPanel),BorderLayout.WEST);
		this.add(linkedPanel,BorderLayout.CENTER);
		this.add(statusBar,BorderLayout.SOUTH);
	}
	
	private void stopDAQ() {
		this.setTitle("TeensyDAQ");
		this.stopButton.setEnabled(false);
		this.startButton.setEnabled(true);
		
		recordingId++;
		recordingIDSpinner.setValue(recordingId);
		
		if(daq !=null){
			daq.stop();
			daq = null;
		}
	}
	
	private void openFile(File csvFile) throws IOException {
		stopDAQ();
		liveLayer.setFollowing(false);
		this.setTitle("Showing: " + csvFile.getName());
		/*this.serialPort.setEnabled(false);
		this.fileNameField.setEnabled(false);
		this.startButton.setEnabled(false);
		this.stopButton.setEnabled(false);
		*/
		
		data.clear();

		if (!csvFile.exists()) {
			throw new IllegalArgumentException("File '" + csvFile + "' does not exist");
		}
		FileReader fileReader = new FileReader(csvFile);
		BufferedReader in = new BufferedReader(fileReader);
		String inputLine;
	
		inputLine = in.readLine();
		double minValue = Double.MAX_VALUE;
		double maxValue = -100000000;

		while (inputLine != null) {
	
			final String[] row = inputLine.split(csvSeparator);
			if (!inputLine.trim().isEmpty() && row.length > 1) {	 
				Double[] dataToPlot = new Double[row.length-1];
				double timeInMS = Double.valueOf(row[0])*1000.0;
				for(int i = 1 ; i < row.length ;i++){
					dataToPlot[i-1] = Double.valueOf(row[i])*1000.0;
					minValue = Math.min(minValue, dataToPlot[i-1]);
					maxValue = Math.max(maxValue, dataToPlot[i-1]);
				}
				data.put(timeInMS, dataToPlot);
			}
			inputLine = in.readLine();
		
		}
		in.close();
		setAxisRanges(minValue*0.9,maxValue*1.1);
		this.repaint();
		
	}
	
	private void setAxisRanges(){
		setAxisRanges(0.0*1000,3.3*1000);
	}
	
	private void setAxisRanges(double minY, double maxY){

		int minYValue = (int) (minY);
		int maxYValue = (int) (maxY);
		
		linkedPanel.getViewPort().setPreferredZoomWindow(Integer.MAX_VALUE, Integer.MAX_VALUE,minYValue , maxYValue);
		linkedPanel.getCoordinateSystem().setMin(Axis.Y, minYValue);
		linkedPanel.getCoordinateSystem().setMax(Axis.Y, maxYValue);
	}
	
	private void startDataReader(boolean[] listenTo, int startInputChannel, int numberOfChannels){
		this.setTitle("Started live session from: " + serialPort.getSelectedItem().toString());
		//reset state
		prevStoredTimeStamp=0;
		samplesToShow=null;
	
		data.clear();
		
		setAxisRanges();
		//reset time
		linkedPanel.getCoordinateSystem().setMin(Axis.X, 0);
		linkedPanel.getCoordinateSystem().setMax(Axis.X, 5000);
		
		liveLayer.setStartIndexInColorMap(startInputChannel);

		try{
			String portname = serialPort.getSelectedItem().toString();
			float sampleRate = Float.valueOf(sampleRateSpinner.getModel().getValue().toString());
			daq = new TeensyDAQ((int) sampleRate,portname, startInputChannel, numberOfChannels);
			//daq.addDataHandler(new PeakDetectionToOSCDataHandler(0));
			//daq.addDataHandler(new PeakDetectionToOSCDataHandler(1));
			daq.addDataHandler(this);

			if(!"".equals(fileNameField.getText().trim())){
			  daq.addDataHandler(new DAQCSVFileWriter(fileNameField.getText().trim().replace("%d", recordingId+"")));
			}
			int numberOfChannelsToListenTo = 0;
			for(int i = 0 ; i < listenTo.length ;i++){
				if(listenTo[i]){
					numberOfChannelsToListenTo++;
				}
			}
			if(numberOfChannelsToListenTo!=0){
				daq.addDataHandler(new DAQToAudio(Float.valueOf(sampleRateSpinner.getValue().toString()).intValue(), listenTo));
			}
			daq.start();
		}catch(Exception e){
			JOptionPane.showMessageDialog(this,
				    "Error initializing the Teensy:\n" + e.getMessage(),
				    "Read error",
				    JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private Component createSignalConfigurationPanel(){
		//int prefferredWith = 0;
		
		
		final List<JTextField> namesTextFields = new ArrayList<JTextField>();
		final List<JCheckBox> listenCheckboxes = new ArrayList<JCheckBox>();
		
		JPanel signalSubPanel = new JPanel(new GridLayout(MAX_ANALOG_INPUTS+1,2,6,6));
		
		signalSubPanel.setBorder(new TitledBorder("Input ranges"));
		signalSubPanel.add(new JLabel("Name"));
		signalSubPanel.add(new JLabel("Listen"));
		
		for(int i = 0 ; i < MAX_ANALOG_INPUTS; i++){
			JTextField nameTextField = new JTextField("AI"+ i);
			nameTextField.setForeground(colorMap[i]);
			nameTextField.setEnabled(false);
		
			
			JCheckBox listenCheckBox = new JCheckBox();
			listenCheckBox.setEnabled(false);
			
			namesTextFields.add(nameTextField);
		
			listenCheckboxes.add(listenCheckBox);
			
			signalSubPanel.add(nameTextField);
			signalSubPanel.add(listenCheckBox);
		}
		
		JPanel numberOfInputsPanel = new JPanel(new GridLayout(0,2,6,6));
		numberOfInputsPanel.setBorder(new TitledBorder("Configuration"));
		
				
		final JSpinner startSpinner = new JSpinner(new SpinnerNumberModel(2,0,MAX_ANALOG_INPUTS-1,1));
		final JSpinner numberOfInputsSpinner = new JSpinner(new SpinnerNumberModel(2,1,MAX_ANALOG_INPUTS,1));
		
		ChangeListener spinnerChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int start = Integer.valueOf(startSpinner.getValue().toString());
				int number = Integer.valueOf(numberOfInputsSpinner.getValue().toString());
				for(int i = 0 ; i < namesTextFields.size() ; i++){
					namesTextFields.get(i).setEnabled(false);
					listenCheckboxes.get(i).setEnabled(false);
				}
				for(int i = start ; i < start + number && i < namesTextFields.size() ; i ++){
					namesTextFields.get(i).setEnabled(true);
					listenCheckboxes.get(i).setEnabled(true);
				}
				SpinnerNumberModel model = (SpinnerNumberModel) numberOfInputsSpinner.getModel();
				int newMax =  MAX_ANALOG_INPUTS-start;
				model.setMaximum(newMax);
				if(number > newMax){
					numberOfInputsSpinner.setValue(newMax);
				}
			}
		};
		startSpinner.addChangeListener(spinnerChangeListener);
		numberOfInputsSpinner.addChangeListener(spinnerChangeListener);
		startSpinner.setValue(0);
		numberOfInputsSpinner.setValue(1);		
		numberOfInputsPanel.add(new JLabel("Serial Port:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(serialPort);
		numberOfInputsPanel.add(new JLabel("Start at input:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(startSpinner);
		numberOfInputsPanel.add(new JLabel("Nr of inputs:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(numberOfInputsSpinner);
		numberOfInputsPanel.add(new JLabel("Sample rate:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(sampleRateSpinner);		
		
		numberOfInputsPanel.add(new JLabel("Recording id:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(recordingIDSpinner);		
		
		numberOfInputsPanel.add(new JLabel("CSV filename:",SwingConstants.RIGHT));
		numberOfInputsPanel.add(fileNameField);		
		numberOfInputsPanel.setMinimumSize(new Dimension(0,150));

		
		
		startButton = new JButton("Start");
		stopButton = new JButton("Stop");
		
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int start = Integer.valueOf(startSpinner.getValue().toString());
				int number = Integer.valueOf(numberOfInputsSpinner.getValue().toString());
				String[] names = new String[number];
			
				boolean[] listenTo = new boolean[number];
				for(int i = start ; i < start + number ; i ++){
					names[i-start] = namesTextFields.get(i).getText();
			
					listenTo[i-start] = listenCheckboxes.get(i).isSelected();
				}
				startDataReader(listenTo,start,number);
				
				stopButton.setEnabled(true);
				startButton.setEnabled(false);
			}
		});
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopDAQ();
				stopButton.setEnabled(false);
				startButton.setEnabled(true);
				
			}
		});
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,2,6,6));
		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		

		JPanel signalPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx=0;
		c.gridy=0;
		signalPanel.add(numberOfInputsPanel,c);
		c.gridx=0;
		c.gridy=1;
		signalPanel.add(signalSubPanel,c);
		c.gridx=0;
		c.gridy=2;
		signalPanel.add(buttonPanel,c);
		
		return signalPanel;
	}
	

	private void updateStatusBar(){
		if(data.size()>0){
			float maxX = cs.getMax(Axis.X);
			float minX = cs.getMin(Axis.X);
			double middle = (minX+maxX)/2.0f;
			Entry<Double,Double[]> entry = data.ceilingEntry(middle);
			String statusBarText = String.format("%+.3f      ", middle/1000.0);
			if(entry!=null){
				Double[] values = entry.getValue();
				for(Double value : values){
					statusBarText += String.format(" | %+.3f", value/1000.0);
				}
			}
			
			statusBar.setText(statusBarText);
		}
	}
	
	@Override
	public void viewPortChanged(ViewPort newViewPort) {
		if(!liveLayer.isFollowing()){
			markCenterLayer.setEnabled(true);
			updateStatusBar();
		}else{
			markCenterLayer.setEnabled(false);
		}
		this.repaint();		
	}
	
	
	private double prevStoredTimeStamp;
	private boolean showMax;//or min
	private Double[] samplesToShow = null;

	
	@Override
	public void handle(DAQSample sample) {
		//store max at 1000Hz for visualisation purposes, so 0.011s needs to be the difference
		double timeStampInMs = (sample.timestamp  * 1000);
		
		if(samplesToShow==null){
			samplesToShow = new Double[sample.data.length];
			double defaultValue = showMax ? -10000000 : 1000000;
			for(int i = 0 ; i < sample.data.length ; i++){
				samplesToShow[i] = defaultValue;
			}
		}
		
		for(int i = 0 ; i < sample.data.length ; i++){
			double sampleValue = sample.data[i] * 1000;
			if(showMax && sampleValue > samplesToShow[i]){
				samplesToShow[i] = sampleValue;
			}else if(!showMax && sampleValue < samplesToShow[i]){
				samplesToShow[i] = sampleValue;
			}
		}
		
		if(timeStampInMs  - prevStoredTimeStamp > 1){
			showMax = ! showMax;
			Double[] viz = samplesToShow.clone();
			data.put(timeStampInMs,viz);
			prevStoredTimeStamp = timeStampInMs;
			double defaultValue = showMax ? -10000000 : 1000000;
			for(int i = 0 ; i < sample.data.length ; i++){
				samplesToShow[i] = defaultValue;
			}
		}
		
		if(liveLayer.isFollowing()){
			String statusBarText ="";
			for(int i = 0 ; i < sample.data.length ; i++){
				double sampleValue = (sample.data[i] ) * 1000;
				statusBarText = statusBarText + String.format("%+.3fV  |  ", sampleValue/1000.0);
			}
			statusBar.setText(statusBarText+ "Press 'f' to start or stop following the data.");
		}
		
	}

	@Override
	public void stopDataHandler() {
		
	}
	
	
	public static void main(String...strings){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
				}
				JFrame frame = new TeensyDAQFrame();
				Toolkit tk = Toolkit.getDefaultToolkit();
				frame.setSize(tk.getScreenSize());
				frame.setVisible(true);
			}
		});
	}

	
}

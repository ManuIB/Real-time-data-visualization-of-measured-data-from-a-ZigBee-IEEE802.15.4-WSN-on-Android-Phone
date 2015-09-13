package library;

import java.util.Random;

import android.graphics.Color;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

public class objDevice {

	private String nameDevice;
	private GraphViewSeries lineDeviceGraph;
	private int cont;

	// Constructors
	public objDevice() {
	}

	public objDevice(String nD) {
		setNameDevice(nD);
		createLineDeviceGraph(nD);
		cont = 0;
	}

	// Set's
	public void setNameDevice(String nD) {
		this.nameDevice = nD;
	}

	public void createLineDeviceGraph(String nD) {
		Random random = new Random();
		this.lineDeviceGraph = new GraphViewSeries(nD,
				new GraphViewSeriesStyle(Color.rgb(random.nextInt(255),
						random.nextInt(255), random.nextInt(255)), 3),
				new GraphViewData[] {});
	}

	public void appendDataGraph(int timer, int realPoint) {
		this.lineDeviceGraph.appendData(new GraphViewData(timer, realPoint),
				true, 13);
	}
	
	public void setCont() {
		this.cont++;
	}

	// Get's
	public String getNameDevice() {
		return nameDevice;
	}

	public GraphViewSeries getLineDeviceGraph() {
		return lineDeviceGraph;
	}
	
	public int getCont() {
		return cont;
	}
}

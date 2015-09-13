package com.nicterstudio.pfc.wsn;

import java.util.ArrayList;

import jp.ksksue.driver.serial.FTDriver;
import library.objDevice;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.LineGraphView;

public class ViewGraphsActivity extends Activity {

	static final int READBUF_SIZE = 54;
	String[] rbufAux = new String[READBUF_SIZE];
	int cont = 0;
	double num = 0;
	String s, sAux, uEmail;

	ArrayList<String> cola = new ArrayList<String>();
	ArrayList<String> cAux = new ArrayList<String>();
	ArrayList<String> cfinal = new ArrayList<String>();

	boolean mThreadIsStopped = true;
	Thread mThread;

	private static final int DISP_HEX = 2;

	FTDriver mSerial;

	private String cadena;
	private boolean mStop = false;

	// Default settings
	private int mDisplayType = DISP_HEX;
	private int mBaudrate = FTDriver.BAUD38400;
	private int mDataBits = FTDriver.FTDI_SET_DATA_BITS_8;
	private int mParity = FTDriver.FTDI_SET_DATA_PARITY_NONE;
	private int mStopBits = FTDriver.FTDI_SET_DATA_STOP_BITS_1;
	private int mBreak = FTDriver.FTDI_SET_NOBREAK;

	private boolean mRunningMainLoop = false;

	private static final String ACTION_USB_PERMISSION = "com.nicterstudio.pfc.wsn.USB_PERMISSION";

	Button btnBattery;
	Button btnTemperature;
	Button btnLight;

	public String typeData = new String();

	private String nD = new String();

	ArrayList<objDevice> allDevices;
	ArrayList<String> datosPruebas = new ArrayList<String>();

	private final Handler mHandler = new Handler();
	private Runnable mTimer;
	private GraphView graphView;

	private LinearLayout layout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewgraphs);

		btnBattery = (Button) findViewById(R.id.ViewGraphsBtBattery);
		btnTemperature = (Button) findViewById(R.id.ViewGraphsBtTemperature);
		btnLight = (Button) findViewById(R.id.ViewGraphsBtLight);

		// get service
		mSerial = new FTDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE));

		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		allDevices = new ArrayList<objDevice>();

		graphView = new LineGraphView(this // context
				, "Sensors" // heading
		);

		// style
		graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setNumHorizontalLabels(13);
		graphView.getGraphViewStyle().setNumVerticalLabels(6);
		graphView.setViewPort(0, 12);
		graphView.setScalable(true);

		layout = (LinearLayout) findViewById(R.id.graph1);

		// set legend
		graphView.setShowLegend(true);
		graphView.setLegendAlign(LegendAlign.MIDDLE);
		graphView.setLegendWidth(200);
		layout.addView(graphView);
	}

	@Override
	protected void onPause() {
		mHandler.removeCallbacks(mTimer);
		super.onPause();
	}

	public void onClickBattery(View v) {
		typeData = "Battery Sensors";
		if (mSerial.isConnected())
			closeDevice();
		openDevice();
		graphView.setTitle(typeData + " // value X = time - value Y = Voltage");
		graphView.removeAllSeries();
		allDevices.removeAll(allDevices);
	}

	public void onClickTemperature(View v) {
		typeData = "Temperature Sensors";
		if (mSerial.isConnected())
			closeDevice();
		openDevice();
		graphView.setTitle(typeData + " // value X = time - value Y = Celsius");
		graphView.removeAllSeries();
		allDevices.removeAll(allDevices);
	}

	public void onClickLight(View v) {
		typeData = "Light Sensors";
		if (mSerial.isConnected())
			closeDevice();
		openDevice();
		graphView.setTitle(typeData + " // value X = time - value Y = Lux");
		graphView.removeAllSeries();
		allDevices.removeAll(allDevices);
	}

	private void mainloop() {
		mStop = false;
		mRunningMainLoop = true;
		String stringOpenDevice = getResources().getString(
				R.string.stringOpenDevice);
		Toast.makeText(getApplicationContext(), stringOpenDevice,
				Toast.LENGTH_SHORT).show();
		new Thread(mLoop).start();
	}

	private Runnable mLoop = new Runnable() {
		public void run() {
			int len;
			byte[] rbuf = new byte[4096];

			for (;;) {// this is the main loop for transferring
				// ////////////////////////////////////////////////////////
				// Read and Display to Terminal
				// ////////////////////////////////////////////////////////
				len = mSerial.read(rbuf);
				rbuf[len] = 0;

				if (len > 0) {

					cadena = setSerialDataToTextView(mDisplayType, rbuf, len);
					mHandler.post(new Runnable() {
						public void run() {
							cadena = dataTransform(cadena);
							if (cadena != "") {

								updateDevices(nD);

								for (int i = 0; i < allDevices.size(); i++)
									if (allDevices.get(i).getNameDevice()
											.equals(nD)) {
										cont = allDevices.get(i).getCont();
										allDevices
												.get(i)
												.getLineDeviceGraph()
												.appendData(
														new GraphViewData(cont,
																num), true, 13);
										allDevices.get(i).setCont();
									}
							}
						}
					});
				}

				if (mStop) {
					mRunningMainLoop = false;
					return;
				}
			}
		}
	};

	@Override
	public void onDestroy() {
		mSerial.end();
		mStop = true;
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	String setSerialDataToTextView(int disp, byte[] rbuf, int len) {
		String cad = "";

		for (int i = 0; i < len; ++i) {
			cad += IntToHex2((int) rbuf[i]);
		}

		return cad;
	}

	private void openDevice() {

		// load default baud rate
		mBaudrate = loadDefaultBaudrate();

		// for requesting permission
		// setPermissionIntent() before begin()
		PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(ACTION_USB_PERMISSION), 0);
		mSerial.setPermissionIntent(permissionIntent);

		if (mSerial.begin(mBaudrate)) {
			loadDefaultSettingValues();
			mainloop();
		} else {
			String stringNotDevice = getResources().getString(
					R.string.stringNotDevice);
			Toast.makeText(getApplicationContext(), stringNotDevice,
					Toast.LENGTH_SHORT).show();
		}

		if (!mSerial.isConnected()) {
			mBaudrate = loadDefaultBaudrate();
			if (!mSerial.begin(mBaudrate)) {
				String stringNotDevice = getResources().getString(
						R.string.stringNotDevice);
				Toast.makeText(getApplicationContext(), stringNotDevice,
						Toast.LENGTH_SHORT).show();
				return;
			} else {
				String stringOpenDevice = getResources().getString(
						R.string.stringOpenDevice);
				Toast.makeText(getApplicationContext(), stringOpenDevice,
						Toast.LENGTH_SHORT).show();
			}
		}

		if (!mRunningMainLoop) {
			mainloop();
		}
	}

	int loadDefaultBaudrate() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref.getString("baudrate_list",
				Integer.toString(FTDriver.BAUD38400));
		return Integer.valueOf(res);
	}

	void loadDefaultSettingValues() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref.getString("display_list", Integer.toString(DISP_HEX));
		mDisplayType = Integer.valueOf(res);

		res = pref.getString("databits_list",
				Integer.toString(FTDriver.FTDI_SET_DATA_BITS_8));
		mDataBits = Integer.valueOf(res);
		mSerial.setSerialPropertyDataBit(mDataBits, FTDriver.CH_A);

		res = pref.getString("parity_list",
				Integer.toString(FTDriver.FTDI_SET_DATA_PARITY_NONE));
		mParity = Integer.valueOf(res) << 8; // parity_list's number is 0 to 4
		mSerial.setSerialPropertyParity(mParity, FTDriver.CH_A);

		res = pref.getString("stopbits_list",
				Integer.toString(FTDriver.FTDI_SET_DATA_STOP_BITS_1));
		mStopBits = Integer.valueOf(res) << 11; // stopbits_list's number is 0
												// to 2
		mSerial.setSerialPropertyStopBits(mStopBits, FTDriver.CH_A);

		res = pref.getString("break_list",
				Integer.toString(FTDriver.FTDI_SET_NOBREAK));
		mBreak = Integer.valueOf(res) << 14;
		mSerial.setSerialPropertyBreak(mBreak, FTDriver.CH_A);

		mSerial.setSerialPropertyToChip(FTDriver.CH_A);
	}

	private String dataTransform(String trama) {
		int longitud = 0;

		longitud = trama.length() / 2;

		// If it is an end device
		if ((longitud == 54) && (trama.startsWith("1002"))) {
			num = calcDataSensor(trama);
			nD = convertHexToString(trama.substring(90, 102));
			return nD;
		}
		// If it is the coordinator
		else if ((longitud == 53) && (trama.startsWith("1002"))) {
			// We do nothing because they do not want to show the data of the
			// coordinator
		}
		// If it is neither of them
		else if (longitud > 54) {
			// We do nothing because this frame is not worth
		} else {
			if (cAux.size() > 0) {
				// If the next begins with the header, the current frame is not
				// worth
				if (trama.startsWith("10")) {
					if (trama.startsWith("1002")) {
						cAux.remove(0);
						cAux.add(trama);
					}

					else if (trama.startsWith("1003")) {
						trama = cAux.get(0) + trama;
						cAux.remove(0);
					}

					else {
						cAux.remove(0);
						cAux.add(trama);
					}
				} else {
					trama = cAux.get(0) + trama;
					cAux.remove(0);
				}

				longitud = trama.length() / 2;
				// If it is an end device
				if ((longitud == 54) && (trama.startsWith("1002"))) {
					num = calcDataSensor(trama);
					nD = convertHexToString(trama.substring(90, 102));
					return nD;
				}
				// If it is the coordinator
				else if ((longitud == 53) && (trama.startsWith("1002"))) {
					// We do nothing because they do not want to show the data
					// of the coordinator
				}
				// If it is neither of them
				else if (longitud > 54) {
					// We do nothing because this frame is not worth
				}
			} else
				cAux.add(trama);
		}

		trama = "";
		return trama;
	}

	private double calcDataSensor(String trama) {
		String data = new String();
		double dataFinal = 0;

		if (typeData == "Battery Sensors") {
			data = trama.substring(68, 70) + trama.substring(66, 68)
					+ trama.substring(64, 66) + trama.substring(62, 64);
			dataFinal = Double.parseDouble(data);
		} else if (typeData == "Temperature Sensors") {
			data = trama.substring(76, 78) + trama.substring(74, 76)
					+ trama.substring(72, 74) + trama.substring(70, 72);
			dataFinal = Double.parseDouble(data);
		} else if (typeData == "Light Sensors") {
			data = trama.substring(84, 86) + trama.substring(82, 84)
					+ trama.substring(80, 82) + trama.substring(78, 80);
			dataFinal = Double.parseDouble(data);
		}

		return dataFinal;
	}

	private void updateDevices(String nombDev) {
		if (allDevices.size() == 0) {
			allDevices.add(new objDevice(nombDev));
			// We take the last added and painted it on the graph
			graphView.addSeries(allDevices.get(0).getLineDeviceGraph());
			// We reset the painted line data
			allDevices.get(0).getLineDeviceGraph()
					.resetData(new GraphViewData[] {});
		} else {
			for (int i = 0; i < allDevices.size(); i++)
				if (allDevices.get(i).getNameDevice().equals(nombDev))
					return;

			allDevices.add(new objDevice(nombDev));
			// We take the last added and painted it on the graph
			graphView.addSeries(allDevices.get(allDevices.size() - 1)
					.getLineDeviceGraph());
			// We reset the painted line data
			allDevices.get(allDevices.size() - 1).getLineDeviceGraph()
					.resetData(new GraphViewData[] {});
		}
	}

	public String convertHexToString(String hex) {

		StringBuilder sb = new StringBuilder();

		// 49204c6f7665204a617661 split into two characters 49, 20, 4c...
		for (int i = 0; i < hex.length() - 1; i += 2) {

			// grab the hex in pairs
			String output = hex.substring(i, (i + 2));
			// convert hex to decimal
			int decimal = Integer.parseInt(output, 16);
			// convert the decimal to character
			sb.append((char) decimal);
		}

		return sb.toString();
	}

	private String IntToHex2(int Value) {
		char HEX2[] = { Character.forDigit((Value >> 4) & 0x0F, 16),
				Character.forDigit(Value & 0x0F, 16) };
		String Hex2Str = new String(HEX2);
		return Hex2Str;
	}

	private void closeDevice() {
		detachedUi();
		mStop = true;
		mSerial.end();
	}

	private void detachedUi() {
		String stringCloseDevice = getResources().getString(
				R.string.stringCloseDevice);
		Toast.makeText(getApplicationContext(), stringCloseDevice,
				Toast.LENGTH_SHORT).show();
	}

	// done when ACTION_USB_DEVICE_ATTACHED
	@Override
	protected void onNewIntent(Intent intent) {
		openDevice();
	};

	// BroadcastReceiver when insert/remove the device USB plug into/from a USB
	// port
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (!mSerial.isConnected()) {
					mBaudrate = loadDefaultBaudrate();
					mSerial.begin(mBaudrate);
					loadDefaultSettingValues();
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				mStop = true;
				detachedUi();
				mSerial.usbDetached(intent);
				mSerial.end();
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (!mSerial.isConnected()) {
						mBaudrate = loadDefaultBaudrate();
						mSerial.begin(mBaudrate);
						loadDefaultSettingValues();
					}
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			}
		}
	};

	@Override
	public void onBackPressed() {
		if (mSerial.isConnected())
			closeDevice();
		finish();
	}
}
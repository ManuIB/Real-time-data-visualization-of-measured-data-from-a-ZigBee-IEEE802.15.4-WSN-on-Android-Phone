package com.nicterstudio.pfc.wsn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import jp.ksksue.driver.serial.FTDriver;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SaveDataActivity extends Activity {

	OutputStreamWriter fout = null;

	static final int READBUF_SIZE = 54;
	String[] rbufAux = new String[READBUF_SIZE];
	int mReadSize = 0;
	String s, sAux, uEmail;

	ImageButton ImageCoordinator;
	TextView CoordinatorStatus;
	Button btnStart;
	Button btnStop;

	ArrayList<String> cola = new ArrayList<String>();
	ArrayList<String> cAux = new ArrayList<String>();
	ArrayList<String> cfinal = new ArrayList<String>();

	boolean mThreadIsStopped = true;
	Thread mThread;

	private static final int DISP_HEX = 2;

	// Load Bundle Key (for view switching)
	private static final String BUNDLEKEY_LOADTEXTVIEW = "bundlekey.LoadTextView";

	FTDriver mSerial;

	private ScrollView mSvText;
	private TextView mTvSerial;
	private String cadena;
	private String timer;
	private boolean mStop = false;

	Handler mHandler = new Handler();

	// Default settings
	private int mDisplayType = DISP_HEX;
	private int mBaudrate = FTDriver.BAUD38400;
	private int mDataBits = FTDriver.FTDI_SET_DATA_BITS_8;
	private int mParity = FTDriver.FTDI_SET_DATA_PARITY_NONE;
	private int mStopBits = FTDriver.FTDI_SET_DATA_STOP_BITS_1;
	private int mBreak = FTDriver.FTDI_SET_NOBREAK;

	private boolean mRunningMainLoop = false;

	private static final String ACTION_USB_PERMISSION = "com.nicterstudio.pfc.wsn.USB_PERMISSION";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/**
		 * savedata Screen for the application
		 **/

		super.onCreate(savedInstanceState);
		setContentView(R.layout.savedata);

		btnStart = (Button) findViewById(R.id.btnStart);
		btnStop = (Button) findViewById(R.id.btnStop);
		CoordinatorStatus = (TextView) findViewById(R.id.movile);
		ImageCoordinator = (ImageButton) findViewById(R.id.iBtnMobile);

		btnStop.setEnabled(false);

		Bundle parametros = this.getIntent().getExtras();
		uEmail = parametros.getString("userEmail");

		updateView(false);

		mSvText = (ScrollView) findViewById(R.id.svText);
		mTvSerial = (TextView) findViewById(R.id.tvSerial);

		// get service
		mSerial = new FTDriver(
				(UsbManager) getSystemService(Context.USB_SERVICE));

		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);
	}

	/**
	 * Saves values for view switching
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(BUNDLEKEY_LOADTEXTVIEW, mTvSerial.getText()
				.toString());
	}

	/**
	 * Loads values for view switching
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mTvSerial.setText(savedInstanceState.getString(BUNDLEKEY_LOADTEXTVIEW));
	}

	public void onClickStart(View v) {
		openDevice();
	}

	public void onClickStop(View v) {
		closeDevice();
	}

	@Override
	public void onDestroy() {
		mSerial.end();
		mStop = true;
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
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
		@Override
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

					setSerialDataToTextView(mDisplayType, rbuf, len);
					mHandler.post(new Runnable() {
						public void run() {
							if (cadena.length() != 0) {
								// We treat data, we built wefts correctly
								cadena = dataTransform(cadena, timer);
								mTvSerial.append(cadena + "\n");
							}

							mSvText.fullScroll(ScrollView.FOCUS_DOWN);
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

	void setSerialDataToTextView(int disp, byte[] rbuf, int len) {
		cadena = "";

		SimpleDateFormat MyFormat = new SimpleDateFormat("HH:mm:ss",
				Locale.getDefault());
		Date now = new Date();
		String hr = MyFormat.format(now);

		for (int i = 0; i < len; ++i) {
			cadena += IntToHex2((int) rbuf[i]);
		}

		// If the string is not empty, add it to the queue and show
		if (cadena.length() != 0) {
			cola.add(cadena);
			timer = hr;
		}
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
		} else
			updateView(true);

		if (!mRunningMainLoop) {
			mainloop();
		}
	}

	// Load default baud rate
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

	void saveFile() {
		boolean sdDisponible = false;
		boolean sdAccesoEscritura = false;

		// Check the status of external memory
		String estado = Environment.getExternalStorageState();

		if (estado.equals(Environment.MEDIA_MOUNTED)) {
			sdDisponible = true;
			sdAccesoEscritura = true;
		} else if (estado.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			sdDisponible = true;
			sdAccesoEscritura = false;
		} else {
			sdDisponible = false;
			sdAccesoEscritura = false;
		}

		// If the external memory is available and can be written
		if (sdDisponible && sdAccesoEscritura) {
			try {

				File sdCard = Environment.getExternalStorageDirectory();
				File directory = new File(sdCard.getAbsolutePath() + "/WSN/"
						+ uEmail);

				if (!directory.exists())
					directory.mkdirs();

				SimpleDateFormat MyFormat = new SimpleDateFormat(
						"yyyy.MM.dd-HH:mm", Locale.getDefault());
				Date Ahora = new Date();
				String NombreFichero = "data-" + MyFormat.format(Ahora)
						+ ".txt";

				File f = new File(directory, NombreFichero);

				fout = new OutputStreamWriter(new FileOutputStream(f));

				s = "";

				while (!cfinal.isEmpty()) {
					s += cfinal.get(0);

					// We do not add newline if the last frame
					if (cfinal.size() != 1)
						s += "\n";

					cfinal.remove(0);
				}
				fout.write(s);// Writes the string buffer
				fout.flush(); // Buffer save data in the text file
				fout.close(); // Closes the text file
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// build complete frames
	private String dataTransform(String cadena, String timer) {
		String trama = new String();
		String tramFinal = new String();
		int longitud;

		tramFinal = "";
		longitud = 0;

		trama = cadena;

		longitud = trama.length() / 2;
		// If it is an end device
		if ((longitud == 54) && (trama.startsWith("1002"))) {
			tramFinal = timer;
			tramFinal += trama.substring(26, 28);
			tramFinal += trama.substring(24, 26);
			tramFinal += trama.substring(62, 86);
			cfinal.add(tramFinal);
			return trama;
		}
		// If it is the coordinator
		else if ((longitud == 53) && (trama.startsWith("1002"))) {
			tramFinal = timer;
			tramFinal += trama.substring(26, 28);
			tramFinal += trama.substring(24, 26);
			tramFinal += trama.substring(62, 86);
			cfinal.add(tramFinal);

			return trama;
		}
		// If it is neither of them
		else if (longitud > 54) {
			trama = "";
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
					tramFinal = timer;
					tramFinal += trama.substring(26, 28);
					tramFinal += trama.substring(24, 26);
					tramFinal += trama.substring(62, 86);
					cfinal.add(tramFinal);

					return trama;
				}
				// If it is the coordinator
				else if ((longitud == 53) && (trama.startsWith("1002"))) {
					tramFinal = timer;
					tramFinal += trama.substring(26, 28);
					tramFinal += trama.substring(24, 26);
					tramFinal += trama.substring(62, 86);
					cfinal.add(tramFinal);

					return trama;
				}
				// If it is neither of them
				else if (longitud > 54) {
					trama = "";
				}
			} else
				cAux.add(trama);
		}

		cadena = "";
		return cadena;
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
		updateView(false);
		detachedUi();
		mStop = true;
		mSerial.end();
		saveFile();
	}

	private void detachedUi() {
		String stringCloseDevice = getResources().getString(
				R.string.stringCloseDevice);
		Toast.makeText(getApplicationContext(), stringCloseDevice,
				Toast.LENGTH_SHORT).show();
	}

	private void updateView(boolean on) {
		if (on) {
			pictures(true);
			btnStart.setEnabled(false);
			btnStop.setEnabled(true);
		} else {
			pictures(false);
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
		}
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

	private void pictures(boolean on) {
		// If coordinator is connected
		if (on) {
			Bitmap bmp = BitmapFactory.decodeResource(getResources(),
					R.drawable.connected);
			String stringCoordConnected = getResources().getString(
					R.string.stringCoordConnected);
			CoordinatorStatus.setText(stringCoordConnected);
			ImageCoordinator.setImageBitmap(bmp);

		} else {
			// If coordinator is disconnected
			Bitmap bmp = BitmapFactory.decodeResource(getResources(),
					R.drawable.disconnected);
			String stringCoordDisconnected = getResources().getString(
					R.string.stringCoordDisconnected);
			CoordinatorStatus.setText(stringCoordDisconnected);
			ImageCoordinator.setImageBitmap(bmp);
		}
	}

	@Override
	public void onBackPressed() {
		if (mSerial.isConnected())
			closeDevice();
		finish();
	}
}
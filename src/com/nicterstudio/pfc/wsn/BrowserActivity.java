package com.nicterstudio.pfc.wsn;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import library.ConnectionDetector;
import library.MyFTPClient;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BrowserActivity extends ListActivity {

	private List<String> item = null;
	private List<String> path = null;
	private String root;
	private TextView myPath;
	private TextView lblMensaje;
	private ListView lstLista;

	// flag for Internet connection status
	Boolean isInternetPresent = false;
	// Connection detector class
	ConnectionDetector cd;

	ArrayList<String> cola = new ArrayList<String>();

	String uEmail;

	MyFTPClient ftpclient = null;
	private static final String TAG = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle parametros = this.getIntent().getExtras();
		uEmail = parametros.getString("userEmail");

		/**
		 * Browser Screen for the application
		 * */
		setContentView(R.layout.browser);

		myPath = (TextView) findViewById(R.id.path);
		// We get references to controls
		lblMensaje = (TextView) findViewById(android.R.id.empty);
		lstLista = (ListView) findViewById(android.R.id.list);

		// creating connection detector class instance
		cd = new ConnectionDetector(getApplicationContext());

		File sdCard = Environment.getExternalStorageDirectory();
		File directory = new File(sdCard.getAbsolutePath() + "/WSN/" + uEmail);

		if (!directory.exists())
			directory.mkdirs();

		root = Environment.getExternalStorageDirectory().getPath() + "/WSN/"
				+ uEmail + "/";

		getDir(root);

		// We associate the context menus to controls
		registerForContextMenu(lblMensaje);
		registerForContextMenu(lstLista);

		ftpclient = new MyFTPClient();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

			menu.setHeaderTitle(lstLista.getAdapter().getItem(info.position)
					.toString());

			inflater.inflate(R.menu.menu_ctx_lista, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		File file = new File(path.get(info.position));

		switch (item.getItemId()) {

		case R.id.CtxLstDelFil:
			try {
				if (!file.delete())
					throw new Exception("El fichero "
							+ path.get(item.getItemId())
							+ " no puede ser borrado!");
			} catch (Exception e) {
			} // end try
			getDir(root);
			String deleteSuccessfully = " "
					+ getResources().getString(R.string.deleteSuccessfully);
			Toast.makeText(getApplicationContext(),
					file.getName() + deleteSuccessfully, Toast.LENGTH_SHORT)
					.show();
			return true;

		case R.id.CtxLstUplSer:
			try {
				// get Internet status
				isInternetPresent = cd.isConnectingToInternet();
				// check for Internet status
				if (isInternetPresent) {
					// Internet Connection is Present
					updateOnServer(file);
					lblMensaje.setText("Lista[" + info.position
							+ "]: Opcion 2 pulsada!");
					String UploadSuccessfully = " "
							+ getResources().getString(
									R.string.UploadSuccessfully);
					Toast.makeText(getApplicationContext(), UploadSuccessfully,
							Toast.LENGTH_SHORT).show();
				} else {
					String NoInternetConnection = getResources().getString(
							R.string.stringNoInternetConnection);
					Toast.makeText(getApplicationContext(),
							NoInternetConnection, Toast.LENGTH_SHORT).show();
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	private void updateOnServer(final File file) throws SocketException,
			UnknownHostException, IOException {

		new Thread(new Runnable() {
			public void run() {
				boolean status = false;
				status = ftpclient.ftpConnect("ftp.pfc-wsn.zz.mu",
						"u523556315", "u523556315", 21);
				if (status == true) {
					Log.d("consola", "Connection Success");
					ftpclient.ftpChangeDirectory("/public_html/WSN");
					ftpclient.ftpMakeDirectory(uEmail);
					ftpclient.ftpChangeDirectory("/public_html/WSN/" + uEmail);
					status = ftpclient.ftpUpload(root + file.getName(),
							file.getName(), "/public_html/WSN/" + uEmail,
							getApplicationContext());
					if (status == true) {
						Log.d(TAG, "Upload success");
					} else {
						Log.d(TAG, "Upload failed");
					}
				} else {
					Log.d(TAG, "Connection failed");
				}
			}
		}).start();
	}

	private void getDir(String dirPath) {
		myPath.setText(getString(R.string.stringLavelUser) + ": " + uEmail);
		item = new ArrayList<String>();
		path = new ArrayList<String>();
		File f = new File(dirPath);
		File[] files = f.listFiles();

		if (!dirPath.equals(root)) {
			item.add(root);
			path.add(root);
			item.add("../");
			path.add(f.getParent());
		}

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			if (!file.isHidden() && file.canRead()) {
				path.add(file.getPath());
				if (file.isDirectory()) {
					item.add(file.getName() + "/");
				} else {
					item.add(file.getName());
				}
			}
		}

		ArrayAdapter<String> fileList = new ArrayAdapter<String>(this,
				R.layout.rowbrowser, item);
		setListAdapter(fileList);
	}

	@Override
	public void onBackPressed() {
		// Closing browser screen
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), "text/plain");
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(intent);
	}
}

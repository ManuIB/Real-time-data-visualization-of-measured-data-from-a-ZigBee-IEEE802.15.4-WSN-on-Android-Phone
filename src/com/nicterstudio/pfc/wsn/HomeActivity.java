package com.nicterstudio.pfc.wsn;

import library.DatabaseHandler;
import library.UserFunctions;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class HomeActivity extends Activity {
	UserFunctions userFunctions;

	ImageButton btnSaveData;
	ImageButton btnBrowser;
	ImageButton btnGraphs;
	ImageButton btnAbout;
	ImageButton btnLogout;

	DatabaseHandler dbHandler;

	String uEmail;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle parametros = this.getIntent().getExtras();
		uEmail = parametros.getString("userEmail");

		/**
		 * Home Screen for the application
		 * */
		// Check login status in database
		userFunctions = new UserFunctions();
		if (userFunctions.isUserLoggedIn(getApplicationContext())) {
			// user already logged in show home.xml
			setContentView(R.layout.home);
			btnSaveData = (ImageButton) findViewById(R.id.iBtnsavedata);
			btnBrowser = (ImageButton) findViewById(R.id.iBtnBrowser);
			btnGraphs = (ImageButton) findViewById(R.id.iBtnGraphs);
			btnAbout = (ImageButton) findViewById(R.id.iBtnAbout);
			btnLogout = (ImageButton) findViewById(R.id.iBtnLogOut);

			btnSaveData.setOnClickListener(new View.OnClickListener() {

				public void onClick(View arg0) {
					userFunctions.logoutUser(getApplicationContext());

					// Defined bundle
					Bundle parametros = new Bundle();
					parametros.putString("userEmail", uEmail);
					// Defined activity
					Intent i = new Intent(getApplicationContext(),
							SaveDataActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.putExtras(parametros);
					// Start activity
					startActivity(i);

				}
			});

			btnBrowser.setOnClickListener(new View.OnClickListener() {

				public void onClick(View arg0) {

					// Defined bundle
					Bundle parametros = new Bundle();
					parametros.putString("userEmail", uEmail);
					// Defined activity
					Intent i = new Intent(getApplicationContext(),
							BrowserActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					i.putExtras(parametros);
					// Start activity
					startActivity(i);
				}
			});

			btnGraphs.setOnClickListener(new View.OnClickListener() {

				public void onClick(View arg0) {
					// Defined bundle
					Bundle parametros = new Bundle();
					parametros.putString("userEmail", uEmail);
					// Defined activity
					Intent graphs = new Intent(getApplicationContext(),
							ViewGraphsActivity.class);
					graphs.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					graphs.putExtras(parametros);
					// Start activity
					startActivity(graphs);
				}
			});

			btnAbout.setOnClickListener(new View.OnClickListener() {

				public void onClick(View arg0) {
					userFunctions.logoutUser(getApplicationContext());
					Intent about = new Intent(getApplicationContext(),
							AboutActivity.class);
					about.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(about);
				}
			});

			btnLogout.setOnClickListener(new View.OnClickListener() {

				public void onClick(View arg0) {
					userFunctions.logoutUser(getApplicationContext());
					Intent login = new Intent(getApplicationContext(),
							LogInActivity.class);
					login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(login);
					// Closing home screen
					finish();
				}
			});

		} else {
			// user is not logged in show login screen
			Intent login = new Intent(getApplicationContext(),
					LogInActivity.class);
			login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(login);
			// Closing home screen
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		userFunctions.logoutUser(getApplicationContext());
		Intent login = new Intent(getApplicationContext(), LogInActivity.class);
		login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(login);
		// Closing home screen
		finish();
	}
}
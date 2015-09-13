package com.nicterstudio.pfc.wsn;

import library.ConnectionDetector;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LogInActivity extends Activity {
	Button btnLogin;
	Button btnLoginToRegister;
	EditText LoginEmail;
	EditText LoginPassword;
	TextView loginErrorMsg;

	// flag for Internet connection status
	Boolean isInternetPresent = false;
	// Connection detector class
	ConnectionDetector cd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setting default screen to login.xml
		setContentView(R.layout.login);
		// Importing all assets like buttons, text fields
		LoginEmail = (EditText) findViewById(R.id.LoginEmail);
		LoginPassword = (EditText) findViewById(R.id.LoginPassword);
		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLoginToRegister = (Button) findViewById(R.id.btnLoginToRegister);
		loginErrorMsg = (TextView) findViewById(R.id.login_error);
		// creating connection detector class instance
		cd = new ConnectionDetector(getApplicationContext());

		// Login button Click Event
		btnLogin.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				// We verified that no blank fields
				if (checkLoginData(LoginEmail.getText().toString(),
						LoginPassword.getText().toString())) {
					// get Internet status
					isInternetPresent = cd.isConnectingToInternet();
					// check for Internet status
					if (isInternetPresent) {
						// Internet Connection is Present
						// make HTTP requests
						ProgressDialog progressDialog = new ProgressDialog(
								LogInActivity.this);
						progressDialog.setMessage(getResources().getString(
								R.string.PleaseWait));
						LogInTask loginTask = new LogInTask(LogInActivity.this,
								progressDialog);
						loginTask.execute();
						/**
						 * showAlertDialog(
						 * AndroidDetectInternetConnectionActivity. this,
						 * "Internet Connection",
						 * "You have internet connection", true);
						 */
					} else {
						// Internet connection is not present
						// Ask user to connect to Internet
						/**
						 * showAlertDialog(LogInActivity.this,
						 * "No Internet Connection",
						 * "You don't have internet connection.", false);
						 */
						showLoginError(2);
					}
				} else
					showLoginError(1);
			}
		});

		// Link to Register Screen
		btnLoginToRegister.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent i = new Intent(getApplicationContext(),
						RegisterActivity.class);
				startActivity(i);
				finish();
			}
		});
	}

	// Validate if there is no field blank
	public boolean checkLoginData(String username, String password) {

		if (username.equals("") || password.equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public void showLoginError(int responseCode) {
		int duration = Toast.LENGTH_LONG;
		Context context = getApplicationContext();
		Vibrator v = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(10);
		Toast toast;
		// Case 1 = There are blank fields
		if (responseCode == 1)
			toast = Toast.makeText(context,
					getResources().getString(R.string.stringFieldsBlank),
					duration);
		// Case 2: There is no internet connection
		else if (responseCode == 2)
			toast = Toast.makeText(
					context,
					getResources().getString(
							R.string.stringNoInternetConnection), duration);
		else
			toast = Toast.makeText(context,
					getResources().getString(R.string.stringLoginError),
					duration);
		toast.show();
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}
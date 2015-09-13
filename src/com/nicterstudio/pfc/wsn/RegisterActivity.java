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

public class RegisterActivity extends Activity {
	Button btnRegister;
	Button btnLinkToLogin;
	EditText RegisterFullName;
	EditText RegisterEmail;
	EditText RegisterPassword;
	TextView registerErrorMsg;

	// flag for Internet connection status
	Boolean isInternetPresent = false;
	// Connection detector class
	ConnectionDetector cd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setting default screen to register.xml
		setContentView(R.layout.register);
		// Importing all assets like buttons, text fields
		RegisterFullName = (EditText) findViewById(R.id.registerName);
		RegisterEmail = (EditText) findViewById(R.id.registerEmail);
		RegisterPassword = (EditText) findViewById(R.id.registerPassword);
		btnRegister = (Button) findViewById(R.id.btnRegister);
		btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);
		registerErrorMsg = (TextView) findViewById(R.id.register_error);

		// creating connection detector class instance
		cd = new ConnectionDetector(getApplicationContext());

		// Register Button Click event
		btnRegister.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				// We verified that no blank fields
				if (checkRegisterData(RegisterFullName.getText().toString(),
						RegisterEmail.getText().toString(), RegisterPassword
								.getText().toString())) {
					// get Internet status
					isInternetPresent = cd.isConnectingToInternet();
					// check for Internet status
					if (isInternetPresent) {
						// Internet Connection is Present
						// make HTTP requests
						ProgressDialog progressDialog = new ProgressDialog(
								RegisterActivity.this);
						progressDialog.setMessage(getResources().getString(
								R.string.Registering));
						RegisterTask registerTask = new RegisterTask(
								RegisterActivity.this, progressDialog);
						registerTask.execute();
					} else {
						showRegisterError(2);
					}
				} else
					showRegisterError(1);
			}
		});

		// Link to Login Screen
		btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent i = new Intent(getApplicationContext(),
						LogInActivity.class);
				startActivity(i);
				// Close Registration View
				finish();
			}
		});
	}

	// Validate if there is no field blank
	public boolean checkRegisterData(String username, String useremail,
			String userpassword) {

		if (username.equals("") || useremail.equals("")
				|| userpassword.equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public void showRegisterError(int responseCode) {
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
		Intent login = new Intent(getApplicationContext(), LogInActivity.class);
		login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(login);
		// Closing Register screen
		finish();
	}
}

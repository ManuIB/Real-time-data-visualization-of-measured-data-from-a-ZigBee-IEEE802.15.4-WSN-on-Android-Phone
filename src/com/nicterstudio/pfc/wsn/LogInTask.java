package com.nicterstudio.pfc.wsn;

import library.DatabaseHandler;
import library.UserFunctions;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

public class LogInTask extends AsyncTask<String, Void, Integer> {
	private ProgressDialog progressDialog;
	private LogInActivity activity;
	private static String KEY_SUCCESS = "success";
	private static String KEY_EMAIL = "email";
	private int responseCode = 0;

	String uEmail;

	public LogInTask(LogInActivity activity, ProgressDialog progressDialog) {
		this.activity = activity;
		this.progressDialog = progressDialog;
	}

	@Override
	protected void onPreExecute() {
		progressDialog.show();
	}

	protected Integer doInBackground(String... arg0) {
		EditText userEmail = (EditText) activity.findViewById(R.id.LoginEmail);
		EditText userPassword = (EditText) activity
				.findViewById(R.id.LoginPassword);
		String email = userEmail.getText().toString();
		String password = userPassword.getText().toString();
		UserFunctions userFunction = new UserFunctions();
		JSONObject json = userFunction.loginUser(email, password);

		// check for login response
		try {
			if (json.getString(KEY_SUCCESS) != null) {
				String res = json.getString(KEY_SUCCESS);

				if (Integer.parseInt(res) == 1) {
					// user successfully logged in
					// // Store user details in SQLite Database
					DatabaseHandler db = new DatabaseHandler(
							activity.getApplicationContext());
					JSONObject json_user = json.getJSONObject("user");
					// Clear all previous data in database
					userFunction.logoutUser(activity.getApplicationContext());
					db.addUser(json_user.getString(KEY_EMAIL));

					responseCode = 1;

				} else {
					responseCode = 0;
					// Error in login
				}
			}

		} catch (NullPointerException e) {
			e.printStackTrace();

		} catch (JSONException e) {
			e.printStackTrace();
		}

		uEmail = email;

		return responseCode;
	}

	@Override
	protected void onPostExecute(Integer responseCode) {

		if (responseCode == 1) {
			progressDialog.dismiss();

			// Defined bundle
			Bundle parametros = new Bundle();
			parametros.putString("userEmail", uEmail);
			// Defined activity
			Intent i = new Intent(activity.getApplicationContext(),
					HomeActivity.class);
			i.putExtras(parametros);
			// Start activity
			activity.startActivity(i);

		} else {
			progressDialog.dismiss();
			activity.showLoginError(responseCode);
		}
	}
}
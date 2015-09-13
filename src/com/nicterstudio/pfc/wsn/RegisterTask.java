package com.nicterstudio.pfc.wsn;

import library.DatabaseHandler;
import library.UserFunctions;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

import com.nicterstudio.pfc.wsn.R.string;

public class RegisterTask extends AsyncTask<String, Void, Integer> {

	private ProgressDialog progressDialog;
	private RegisterActivity activity;
	private static String KEY_SUCCESS = "success";
	private static String KEY_UID = "uid";
	private static String KEY_NAME = "name";
	private static String KEY_EMAIL = "email";
	private static String KEY_CREATED_AT = "created_at";
	private int responseCode = 0;

	/*
	 * Constructor that takes parameters passed by LoginFragment and stores them
	 * as class- wide fields so that all methods can access necessary variables.
	 */
	public RegisterTask(RegisterActivity activity, ProgressDialog progressDialog) {
		this.activity = activity;
		this.progressDialog = progressDialog;
	}

	/*
	 * A necessary but very simple method that launches a ProgressDialog to show
	 * the user that a background task is operating (registration).
	 */
	@Override
	protected void onPreExecute() {
		progressDialog.show();
	}

	@Override
	protected Integer doInBackground(String... arg0) {
		EditText nameEdit = (EditText) activity.findViewById(R.id.registerName);
		EditText userName = (EditText) activity
				.findViewById(R.id.registerEmail);
		EditText passwordEdit = (EditText) activity
				.findViewById(R.id.registerPassword);
		String name = nameEdit.getText().toString();
		String email = userName.getText().toString();
		String password = passwordEdit.getText().toString();

		UserFunctions userFunction = new UserFunctions();
		JSONObject json = userFunction.registerUser(name, email, password);
		// check for login response
		try {
			if (json.getString(KEY_SUCCESS) != null) {
				// registerErrorMsg.setText("");
				String res = json.getString(KEY_SUCCESS);
				if (Integer.parseInt(res) == 1) {
					// user successfully registred
					// Store user details in SQLite Database
					DatabaseHandler db = new DatabaseHandler(
							activity.getApplicationContext());
					JSONObject json_user = json.getJSONObject("user");
					// Clear all previous data in database
					userFunction.logoutUser(activity.getApplicationContext());
					db.addUser(json_user.getString(KEY_NAME),
							json_user.getString(KEY_EMAIL),
							json.getString(KEY_UID),
							json_user.getString(KEY_CREATED_AT));
					// successful registration
					responseCode = 1;
				} else {
					// Error in registration
					responseCode = 0;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return responseCode;
	}

	@Override
	protected void onPostExecute(Integer responseCode) {

		if (responseCode == 1) {
			progressDialog.dismiss();
			Intent i = new Intent();
			i.setClass(activity.getApplicationContext(), LogInActivity.class);
			activity.startActivity(i);
		}
		if (responseCode == 0) {
			Toast.makeText(activity, string.stringRegisterError,
					Toast.LENGTH_SHORT).show();
			progressDialog.dismiss();
		}
	}
}

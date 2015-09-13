package com.nicterstudio.pfc.wsn;

import android.app.Activity;
import android.os.Bundle;

public class AboutActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/**
		 * About Screen for the application
		 * */
		setContentView(R.layout.about);
	}

	@Override
	public void onBackPressed() {
		// Closing about screen
		finish();
	}
}

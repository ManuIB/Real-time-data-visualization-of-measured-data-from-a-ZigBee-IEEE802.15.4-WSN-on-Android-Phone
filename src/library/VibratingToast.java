package library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.widget.Toast;

@SuppressLint("ViewConstructor")
public class VibratingToast extends Toast {
	public VibratingToast(Context context, CharSequence text, int duration) {
		super(context);
		Vibrator v = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(10);
		super.makeText(context, text, duration).show();
	}
}

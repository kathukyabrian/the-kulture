package tech.kitucode.kulture;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		registerPlugin(LocationBroadcastPlugin.class);
		super.onCreate(savedInstanceState);
	}
}

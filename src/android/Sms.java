package com.mostlyepic.plugins.sms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import java.util.ArrayList;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class Sms extends CordovaPlugin {
  public final String ACTION_SEND_SMS = "send";

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals(ACTION_SEND_SMS)) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            //parsing arguments
            String separator = ";";
            if (android.os.Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
              // See http://stackoverflow.com/questions/18974898/send-sms-through-intent-to-multiple-phone-numbers/18975676#18975676
              separator = ",";
            }
            String phoneNumber = args.getJSONArray(0).join(separator).replace("\"", "");
            String message = args.getString(1);
            JSONObject options = args.getJSONObject(2);
            boolean replaceLineBreaks = Boolean.parseBoolean(options.getString("replaceLineBreaks"));

            // replacing \n by new line if the parameter replaceLineBreaks is set to true
            if (replaceLineBreaks) {
                message = message.replace("\\n", System.getProperty("line.separator"));
            }
            if (!checkSupport()) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "SMS not supported on this platform"));
                return;
            }
            invokeSMSIntent(phoneNumber, message);
            // always passes success back to the app
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return;
          } catch (JSONException ex) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
          }
        }
      });
      return true;
    }
    return false;
  }

  private boolean checkSupport() {
    Activity ctx = this.cordova.getActivity();
    return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
  }

	@SuppressLint("NewApi")
	private void invokeSMSIntent(String phoneNumber, String message) {
		Intent sendIntent;
		if ("".equals(phoneNumber) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this.cordova.getActivity());

			sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT, message);

			if (defaultSmsPackageName != null) {
				sendIntent.setPackage(defaultSmsPackageName);
			}
		} else {
			sendIntent = new Intent(Intent.ACTION_VIEW);
			sendIntent.putExtra("sms_body", message);
			// See http://stackoverflow.com/questions/7242190/sending-sms-using-intent-does-not-add-recipients-on-some-devices
			sendIntent.putExtra("address", phoneNumber);
			sendIntent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber)));
		}
		this.cordova.getActivity().startActivity(sendIntent);
	}
}

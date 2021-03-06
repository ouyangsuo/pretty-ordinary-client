package com.kitty.poclient.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kitty.poclient.common.WatchDog;
import com.kitty.poclient.http.HttpGetter;

public class UpdateUtil {

	private static final String TAG = UpdateUtil.class.getSimpleName() + ":";
	private Context context;
	private Handler handler;
	private int msgWhat;

	public UpdateUtil(Context context, Handler handler, int msgWhat) {
		this.context = context;
		this.handler = handler;
		this.msgWhat = msgWhat;
	}

//	public void updateControllerVersionIfNeccessary() {
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				String currentVersion = getCurrentVersion();
//				String latestVersion = getLatestVersion();
//				Log.e(TAG, "currentVersion=" + currentVersion);
//				Log.e(TAG, "latestVersion=" + latestVersion);
//
//				if ("未知最新版本".equals(latestVersion)) {
//					latestVersion = "1.99.99";
//				}
//
//				Log.e(TAG, "has Latest Version=" + new VersionNumberComparator().compare(latestVersion, currentVersion));
//				if (latestVersion.startsWith("未知最新版本") || !(new VersionNumberComparator().compare(latestVersion, currentVersion))) {
//					return;
//				}
//
//				Message msg = new Message();
//				msg.what = msgWhat;
//				Bundle bundle = new Bundle();
//				bundle.putString("currentVersion", currentVersion);
//				bundle.putString("latestVersion", latestVersion);
//				msg.setData(bundle);
//
//				handler.sendMessage(msg);
//			}
//		}).start();
//	}

//	private String getLatestVersion() {
//		String json = new HttpGetter(context).getLatestVersion();
//		Log.e("软件升级", TAG + "getLatestVersion():json=" + json);
//		new JsonUtil().getLatestVersion(json);
//
//		return WatchDog.latestControllerVersion;
//	}

	private String getCurrentVersion() {
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			WatchDog.currentControllerVersion = packInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e("软件升级", TAG + "getCurrentVersion():e=" + e);
			e.printStackTrace();
		}

		return WatchDog.currentControllerVersion;
	}

}

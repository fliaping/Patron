package com.deepwits.Patron.DetecteEvent;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/*
 * create by tlg on 20160120
 */
public class DetecteContentProvider extends ContentProvider {
	private static final String TAG = "DetecteContentProvider";
	private static UriMatcher uriMatcher = null;
	private static final int GET_PARK = 1;
	private static final int GET_COLLISION = 2;
	public static final String authorities = "com.deepwits.patron.detect";
	public static SharedPreferences sharedPreferences_park;
	public static SharedPreferences sharedPreferences_collision;
	// =================停车监控
	private final String PARK_DETECT = "parking_monitor";
	private final String LEVEL = "level";
	private final String PARK_STATUS = "park_status";
	private Editor editor_park, editor_collision;

	// =================碰撞检测
	private final String COLLISION_DETECT = "collision_detect";
	private final String COLLISION_SWITCH = "collision_switch";

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(authorities, "/getParkingMonitorValue", GET_PARK);
		uriMatcher.addURI(authorities, "/getCollisionValue", GET_COLLISION);
	}

	@Override
	public boolean onCreate() {
		Context mContext = getContext();
		return mContext != null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return null;
	}

	/**
	 * 获取停车监控和碰撞检测值
	 */
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case GET_PARK:
			return getParkingMonitorValue(getContext());
		case GET_COLLISION:
			return getCollisionDetectValue(getContext());
		default:
			throw new IllegalArgumentException("This is a unKnow Uri"
					+ uri.toString());
		}
	}

	/**
	 * 数据插入
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	// ================获取碰撞检测设置值===================
	private String getCollisionDetectValue(Context context) {
		
		if (sharedPreferences_collision == null) {
			sharedPreferences_collision = getContext().getSharedPreferences(
					COLLISION_DETECT, getContext().MODE_PRIVATE);
		}
		editor_collision = sharedPreferences_collision.edit();
		JSONObject settingvalue = new JSONObject();
		// default is off
//		boolean status = sharedPreferences_collision.getBoolean(
//				COLLISION_SWITCH, false); 
		//default level is 3
		int level = sharedPreferences_collision.getInt(LEVEL, 3); 
		
		try {
			//settingvalue.put(COLLISION_SWITCH, status);
			settingvalue.put(LEVEL, level);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(getContext().getPackageCodePath(), e.toString());
		}
		return settingvalue.toString();
	}

	// ==============获取停车监控的值=====================
	private String getParkingMonitorValue(Context context) {
		if (sharedPreferences_park == null) {
			sharedPreferences_park = getContext().getSharedPreferences(
					PARK_DETECT, getContext().MODE_PRIVATE);
		}
		editor_park = sharedPreferences_park.edit();
		JSONObject settingvalue = new JSONObject();

		boolean status = sharedPreferences_park.getBoolean(PARK_STATUS, false); // up
		// default levels is 2																		// off
		int level = sharedPreferences_park.getInt(LEVEL, 2); 

		try {
			settingvalue.put(PARK_STATUS, status);
			settingvalue.put(LEVEL, level);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(getContext().getPackageCodePath(), e.toString());
		}
		com.yysky.commons.Log.e(PARK_DETECT, settingvalue.toString());
		return settingvalue.toString();
	}

}

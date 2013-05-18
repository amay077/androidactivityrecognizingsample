package com.example.activityrecognizingsample;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * 行動認識結果を取得するための IntentService 
 * 
 * ActivityRecognitionClient.requestActivityUpdates に仕込んでおくと
 * 認識結果を受信する度にこれが呼ばれる。
 * 
 */
public class ReceiveRecognitionIntentService extends IntentService {
	private static final String TAG = "ReceiveRecognitionIntentService";

	public ReceiveRecognitionIntentService()  {
		super("ReceiveRecognitionIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (!ActivityRecognitionResult.hasResult(intent)) {
			// 行動認識結果持ってないよ
			return;
		}
		
		// 認識結果を取得する
		ActivityRecognitionResult result = 
				ActivityRecognitionResult.extractResult(intent);
		
		DetectedActivity mostProbableActivity = result.getMostProbableActivity();
		int activityType = mostProbableActivity.getType();
		int confidence = mostProbableActivity.getConfidence();
		
		Log.d(TAG, "Receive recognition.");
		Log.d(TAG, " activityType - " + activityType); // 行動タイプ
		Log.d(TAG, " confidence - " + confidence); // 確実性（精度みたいな）
		Log.d(TAG, " time - " + DateFormat.format("hh:mm:ss.sss", result.getTime())); // 時間
		Log.d(TAG, " elapsedTime - " + DateFormat.format("hh:mm:ss.sss", result.getElapsedRealtimeMillis())); // よく分からん

		// 画面に結果を表示するために、Broadcast で通知。
		//  MainActivity にしかけた BroadcastReceiver で受信する。
		Intent notifyIntent = new Intent("receive_recognition");
		notifyIntent.setPackage(getPackageName());
		notifyIntent.putExtra("activity_type", activityType);
		notifyIntent.putExtra("confidence", confidence);
		notifyIntent.putExtra("time", result.getTime());
		sendBroadcast(notifyIntent);
	}
}

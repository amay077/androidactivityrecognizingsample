package com.example.activityrecognizingsample;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ActivityRecognitionClient _recClient; // 行動認識のメインクラス
	private TextView _textResult; // 認識結果を表示するところ
	
	// 認識結果は PendingIntent で通知してくれる
	//  PendingIntent に、Service を起動する Intent を仕込んでおいて、
	//  認識結果の取得はそっちで行う。 > ReceiveRecognitionIntentService.java
    private PendingIntent _receiveRecognitionIntent; 
    
    // ReceiveRecognitionIntentService で取得した認識結果は、Broadcast で通知されるので、
    // それを受け取る Receiver 。ここで画面に認識結果を表示する。
	private final BroadcastReceiver _receiveFromIntentService = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int activityType = intent.getIntExtra("activity_type", 0);
			final int confidence = intent.getIntExtra("confidence", -1);
			final long time = intent.getLongExtra("time", 0);
			
			MainActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String text = _textResult.getText().toString();
					text = DateFormat.format("hh:mm:ss.sss", time) + " - " 
							+ getNameFromType(activityType) + "(" +
							+ confidence + ")" + "\n" + text;
					
					_textResult.setText(text);
				}
			});
		}
		
		// http://developer.android.com/training/location/activity-recognition.html
		// からパクってきた関数
		 /**
	     * Map detected activity types to strings
	     *@param activityType The detected activity type
	     *@return A user-readable name for the type
	     */
	    private String getNameFromType(int activityType) {
	        switch(activityType) {
	            case DetectedActivity.IN_VEHICLE:
	                return "in_vehicle";
	            case DetectedActivity.ON_BICYCLE:
	                return "on_bicycle";
	            case DetectedActivity.ON_FOOT:
	                return "on_foot";
	            case DetectedActivity.STILL:
	                return "still";
	            case DetectedActivity.UNKNOWN:
	                return "unknown";
	            case DetectedActivity.TILTING:
	                return "tilting";
	        }
	        return "unknown - " + activityType;
	    }
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        _textResult = (TextView)findViewById(R.id.text_results);
        
        // IntentService から Broadcast される認識結果を受け取るための Receiver を登録しておく
        LocalBroadcastManager.getInstance(this).registerReceiver(
        		_receiveFromIntentService, new IntentFilter("receive_recognition"));
        
        final Button buttonStart = (Button)findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new OnClickListener() {
        	private boolean _isStarted = false;
        	
			@Override
			public void onClick(View v) {
				
				if (!_isStarted) {
					startReckoning();
					buttonStart.setText("Stop");
				} else {
					stopReckoning();
					buttonStart.setText("Start");
				}
				
				_isStarted = !_isStarted;
			}
		});
    }
    
    @Override
    protected void onDestroy() {
    	stopReckoning();
		// ConnectionCallbacks.onDisconnected が呼ばれるまで待った方がいい気がする
    	unregisterReceiver(_receiveFromIntentService);
    	super.onDestroy();
    }
    
    private void startReckoning() {
    	_recClient = new ActivityRecognitionClient(this, new ConnectionCallbacks() {
			
			@Override
			public void onConnected(Bundle bundle) {
				Intent intent = new Intent(
		                MainActivity.this, ReceiveRecognitionIntentService.class);
				_receiveRecognitionIntent = PendingIntent.getService(
						MainActivity.this, 0, intent,
		                PendingIntent.FLAG_UPDATE_CURRENT);
				
				// 2. 行動認識開始！
				//  1秒間隔で認識間隔を通知。
				//  認識したら ReceiveRecognitionIntentService が呼び出されるようにしている。
				_recClient.requestActivityUpdates(1000, _receiveRecognitionIntent);
			}

			@Override
			public void onDisconnected() {
				_recClient = null; // NOTE disconnect してもここにこないよ？
			}

    	}, new OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(ConnectionResult result) {
				// 接続でエラーが発生したらここにくるらしい
			}
		});
    	
    	// 1. 行動認識サービスに接続！
    	_recClient.connect();
    }
    
    private void stopReckoning() {
    	if (_recClient == null || !_recClient.isConnected()) {
    		return;
    	}
    	
    	_recClient.removeActivityUpdates(_receiveRecognitionIntent);
		_recClient.disconnect(); 
		// ConnectionCallbacks.onDisconnected が呼ばれるまで待った方がいい気がする
    }
    
    
}

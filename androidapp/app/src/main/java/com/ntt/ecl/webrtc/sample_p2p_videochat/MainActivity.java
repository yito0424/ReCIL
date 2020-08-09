package com.ntt.ecl.webrtc.sample_p2p_videochat;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.Math;

import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.tan;

/**
 *
 * MainActivity.java
 * ECL WebRTC p2p video-chat sample
 *
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	//
	// Set your APIkey and Domain
	//
	private static final String API_KEY = "1b21eb31-978a-487d-9f1a-e117aa3a26a7";
	private static final String DOMAIN = "localhost";

	private Peer			_peer;
	private MediaStream		_localStream;
	private MediaStream		_remoteStream;
	private MediaConnection	_mediaConnection;
	private DataConnection _dataConnection;

	private String			_strOwnId;
	private boolean			_bConnected;

	private Handler			_handler;
	private TextView        _tvMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Window wnd = getWindow();
		wnd.addFlags(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		_handler = new Handler(Looper.getMainLooper());
		final Activity activity = this;

		//
		// Initialize Peer
		//
		PeerOption option = new PeerOption();
		option.key = API_KEY;
		option.domain = DOMAIN;
		_peer = new Peer(this,"eSRV2mFD4PpQZVpJ", option);

		//
		// Set Peer event callbacks
		//

		// OPEN
		_peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(Object object) {

				// Show my ID
				_strOwnId = (String) object;
				EditText tvOwnId = (EditText) findViewById(R.id.tvOwnId);
				_tvMessage  = (TextView) findViewById(R.id.tvMessage);
				tvOwnId.setText(_strOwnId);

				// Request permissions
				if (ContextCompat.checkSelfPermission(activity,
						Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity,
						Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},0);
				}
				else {

					// Get a local MediaStream & show it
					startLocalStream();
				}

			}
		});

		// CALL (Incoming call)
		_peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				if (!(object instanceof MediaConnection)) {
					return;
				}

				_mediaConnection = (MediaConnection) object;
				setMediaCallbacks();
				_mediaConnection.answer(_localStream);

				_bConnected = true;
				updateActionButtonTitle();
			}
		});

		// CONNECT
		_peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_dataConnection = (DataConnection)object;
				setDataCallbacks();
			}
		});

		_peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				Log.d(TAG, "[On/Close]");
			}
		});
		_peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				Log.d(TAG, "[On/Disconnected]");
			}
		});
		_peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/Error]" + error.message);
			}
		});


		//
		// Set GUI event listeners
		//

		Button btnAction = (Button) findViewById(R.id.btnAction);
		btnAction.setEnabled(true);
		btnAction.setOnClickListener(new View.OnClickListener()	{
			@Override
			public void onClick(View v)	{
				v.setEnabled(false);

				if (!_bConnected) {

					// Select remote peer & make a call
					showPeerIDs();
				}
				else {

					// Hang up a call
					closeRemoteStream();
					_mediaConnection.close();

				}

				v.setEnabled(true);
			}
		});

		Button switchCameraAction = (Button)findViewById(R.id.switchCameraAction);
		switchCameraAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)	{
				if(null != _localStream){
					Boolean result = _localStream.switchCamera();
					if(true == result)	{
						//Success
					}
					else {
						//Failed
					}
				}

			}
		});

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case 0: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startLocalStream();
				}
				else {
					Toast.makeText(this,"Failed to access the camera and microphone.\nclick allow when asked for permission.", Toast.LENGTH_LONG).show();
				}
				break;
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Disable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Set volume control stream type to WebRTC audio.
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	protected void onPause() {
		// Set default volume control stream type.
		setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
		super.onPause();
	}

	@Override
	protected void onStop()	{
		// Enable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		destroyPeer();
		super.onDestroy();
	}

	//
	// Get a local MediaStream & show it
	//
	void startLocalStream() {
		Navigator.initialize(_peer);
		MediaConstraints constraints = new MediaConstraints();
		constraints.maxWidth=640;
		constraints.maxHeight=480;
		_localStream = Navigator.getUserMedia(constraints);

		Canvas canvas = (Canvas) findViewById(R.id.svLocalView);
		_localStream.addVideoRenderer(canvas,0);
	}

	//
	// Set callbacks for MediaConnection.MediaEvents
	//
	void setMediaCallbacks() {

		_mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
			@Override
			public void onCallback(Object object) {
				_remoteStream = (MediaStream) object;
				Canvas canvas = (Canvas) findViewById(R.id.svRemoteView);
				_remoteStream.addVideoRenderer(canvas,0);
			}
		});

		_mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				closeRemoteStream();
				_bConnected = false;
				updateActionButtonTitle();
			}
		});

		_mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/MediaError]" + error);
			}
		});

	}

	void setDataCallbacks() {

		_dataConnection.on(DataConnection.DataEventEnum.OPEN, new OnCallback() {
			@Override
			public void onCallback(Object o) {
				appendLog("Connected.");
			}
		});

		_dataConnection.on(DataConnection.DataEventEnum.CLOSE, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				_bConnected = false;
				updateActionButtonTitle();
				unsetDataCallbacks();
				_dataConnection = null;
			}
		});

		_dataConnection.on(DataConnection.DataEventEnum.DATA, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				String strValue = null;
				String strValue2=null;
				double xradian=0, yradian=0;
				HashMap<String, Double> xyradian = new HashMap<String, Double>();


				HashMap map = (HashMap)object;

				ArrayList splitX=(ArrayList)map.get("msgX");
				ArrayList splitY=(ArrayList)map.get("msgY");
				//strValue=String.join(splitX);
				strValue=splitX.get(0).toString()+splitX.get(1).toString()+splitX.get(2).toString();
				strValue2=splitY.get(0).toString()+splitY.get(1).toString()+splitY.get(2).toString();
				double x=Double.parseDouble(strValue);
				double y=Double.parseDouble(strValue2);
				xyradian=CalculateRadian(x,y);
					//strValue=xyradian.get("xradian").toString();
					//strValue2=xyradian.get("yradian").toString();
					//strValue2=map.get("msgY").toString();
				appendLog("X:"+String.format("%.1f",xyradian.get("xradian") ) +" Y:"+String.format("%.1f",xyradian.get("yradian") ));
				//appendLog("X:"+x+" Y:"+strValue2);
			}
		});

		_dataConnection.on(DataConnection.DataEventEnum.ERROR, new OnCallback()	{
			@Override
			public void onCallback(Object object) {
				PeerError error = (PeerError) object;
				Log.d(TAG, "[On/MediaError]" + error);
			}
		});

	}

	//
	// Clean up objects
	//
	private void destroyPeer() {
		closeRemoteStream();

		if (null != _localStream) {
			Canvas canvas = (Canvas) findViewById(R.id.svLocalView);
			_localStream.removeVideoRenderer(canvas,0);
			_localStream.close();
		}

		if (null != _mediaConnection)	{
			if (_mediaConnection.isOpen()) {
				_mediaConnection.close();
			}
			unsetMediaCallbacks();
		}

		Navigator.terminate();

		if (null != _peer) {
			unsetPeerCallback(_peer);
			if (!_peer.isDisconnected()) {
				_peer.disconnect();
			}

			if (!_peer.isDestroyed()) {
				_peer.destroy();
			}

			_peer = null;
		}
	}

	//
	// Unset callbacks for PeerEvents
	//
	void unsetPeerCallback(Peer peer) {
		if(null == _peer){
			return;
		}

		peer.on(Peer.PeerEventEnum.OPEN, null);
		peer.on(Peer.PeerEventEnum.CONNECTION, null);
		peer.on(Peer.PeerEventEnum.CALL, null);
		peer.on(Peer.PeerEventEnum.CLOSE, null);
		peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
		peer.on(Peer.PeerEventEnum.ERROR, null);
	}

	//
	// Unset callbacks for MediaConnection.MediaEvents
	//
	void unsetMediaCallbacks() {
		if(null == _mediaConnection){
			return;
		}

		_mediaConnection.on(MediaConnection.MediaEventEnum.STREAM, null);
		_mediaConnection.on(MediaConnection.MediaEventEnum.CLOSE, null);
		_mediaConnection.on(MediaConnection.MediaEventEnum.ERROR, null);
	}

	//
	// Unset callbacks for DataConnection.DataEvents
	//
	void unsetDataCallbacks() {
		if(null == _dataConnection){
			return;
		}

		_dataConnection.on(DataConnection.DataEventEnum.OPEN, null);
		_dataConnection.on(DataConnection.DataEventEnum.CLOSE, null);
		_dataConnection.on(DataConnection.DataEventEnum.DATA, null);
		_dataConnection.on(DataConnection.DataEventEnum.ERROR, null);
	}

	//
	// Close a remote MediaStream
	//
	void closeRemoteStream(){
		if (null == _remoteStream) {
			return;
		}

		Canvas canvas = (Canvas) findViewById(R.id.svRemoteView);
		_remoteStream.removeVideoRenderer(canvas,0);
		_remoteStream.close();
	}

	//
	// Create a MediaConnection
	//
	void onPeerSelected(String strPeerId) {
		if (null == _peer) {
			return;
		}

		if (null != _mediaConnection) {
			_mediaConnection.close();
		}

		CallOption option = new CallOption();
		_mediaConnection = _peer.call(strPeerId, _localStream, option);

		if (null != _mediaConnection) {
			setMediaCallbacks();
			_bConnected = true;
		}

		updateActionButtonTitle();
	}

	//
	// Listing all peers
	//
	void showPeerIDs() {
		if ((null == _peer) || (null == _strOwnId) || (0 == _strOwnId.length())) {
			Toast.makeText(this, "Your PeerID is null or invalid.", Toast.LENGTH_SHORT).show();
			return;
		}

		// Get all IDs connected to the server
		final Context fContext = this;
		_peer.listAllPeers(new OnCallback() {
			@Override
			public void onCallback(Object object) {
				if (!(object instanceof JSONArray)) {
					return;
				}

				JSONArray peers = (JSONArray) object;
				ArrayList<String> _listPeerIds = new ArrayList<>();
				String peerId;

				// Exclude my own ID
				for (int i = 0; peers.length() > i; i++) {
					try {
						peerId = peers.getString(i);
						if (!_strOwnId.equals(peerId)) {
							_listPeerIds.add(peerId);
						}
					} catch(Exception e){
						e.printStackTrace();
					}
				}

				// Show IDs using DialogFragment
				if (0 < _listPeerIds.size()) {
					FragmentManager mgr = getFragmentManager();
					PeerListDialogFragment dialog = new PeerListDialogFragment();
					dialog.setListener(
							new PeerListDialogFragment.PeerListDialogFragmentListener() {
								@Override
								public void onItemClick(final String item) {
									_handler.post(new Runnable() {
										@Override
										public void run() {
											onPeerSelected(item);
										}
									});
								}
							});
					dialog.setItems(_listPeerIds);
					dialog.show(mgr, "peerlist");
				}
				else{
					Toast.makeText(fContext, "PeerID list (other than your ID) is empty.", Toast.LENGTH_SHORT).show();
				}
			}
		});

	}

	//
	// Append a string to tvMessage
	//
	void appendLog(String logText){
		_tvMessage.setText(logText+"\n");
	}

	//
	// Update actionButton title
	//
	void updateActionButtonTitle() {
		_handler.post(new Runnable() {
			@Override
			public void run() {
				Button btnAction = (Button) findViewById(R.id.btnAction);
				if (null != btnAction) {
					if (false == _bConnected) {
						btnAction.setText("Make Call");
					} else {
						btnAction.setText("Hang up");
					}
				}
			}
		});
	}

	HashMap CalculateRadian(double x, double y){
		double R=300; //mm distance from camera to surface
		double xradian, yradian;
		double Xr=R*tan(2*PI*68/360), Xt, Xq; //vertical angle is 68°
		double Yr=R*tan(2*PI*53/360), Yt, Yq; //horizontal angle is 53°
		HashMap<String,Double> xyradian=new HashMap<String,Double>();
		Xt=x-255/2;
		Xq=Xr*Xt/255;
		xradian = atan(Xq/R);
		Yt=y-255/2;
		Yq=Yr*Yt/255;
		yradian = -atan(Yq/R);
		xyradian.put("xradian",xradian);
		xyradian.put("yradian",yradian);
		return xyradian;
	}

}
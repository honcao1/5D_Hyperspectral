/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nanolambda.SpectrumMeter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.nanolambda.NSP32.DataChannel;
import com.nanolambda.NSP32.NSP32;
import com.nanolambda.NSP32.ReturnPacket;
import com.nanolambda.NSP32.ReturnPacketReceivedListener;
import com.nanolambda.NSP32.SpectrumInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
	private static final int REQUEST_SELECT_DEVICE	= 1;
	private static final int REQUEST_ENABLE_BT		= 2;
	private static final int REQUEST_APP_SETTINGS	= 3;
	private static final int REQUEST_PERMISSIONS	= 4;
	private static final String TAG					= "NSP32_Main";
	private static final String DISCONNECT_CMD_STR	= "disconnect";

	// app run mode enumeration
	private enum AppRunMode { Disconnected, Connected, Spectrum }
	
	private Button mBtnConnectDisconnect;
	private Button mBtnSpectrum;
	private Button mBtnResult;
	private Spinner mSpinnerIntegrationTime, mSpinnerFrameAvgNum, mSpinnerDataset;
	private CheckBox mChkbxEnableAE;
	private TextView mTextViewSensorId, mTextViewIntegrationTime, mTextViewExeTime, mTextViewLog;
	private TextView mTextViewX, mTextViewY, mTextViewZ;
	private LineChart mChartSpectrum;
	
	private NSP32 mNSP32;
	private short[] mWavelength = null;
	private boolean mStopSpectrum = true;
	private long mRoundTripTimeStart;
	private AppRunMode mCurAppMode = AppRunMode.Disconnected;
	private String mLogMessage = "";

	private SpectrumTransferService mService = null;
	private BluetoothAdapter mBtAdapter = null;
	
	// spectrum chart data
	private List<Entry> mChartPoints;
	private LineDataSet mChartDataSetA;
	private List<ILineDataSet> mChartDataSets;
	private LineData mChartData;

	boolean idItemMenu = false;
	ArrayList<ArrayList<Double>> unknowList = new ArrayList<ArrayList<Double>>();
    // UI update
	Handler guiUpdateHandler = new Handler();
	Runnable guiUpdateRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			mChartSpectrum.invalidate();	// update spectrum chart			
			guiUpdateHandler.postDelayed(this, 50);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
				
		mBtnConnectDisconnect = (Button) findViewById(R.id.btn_select);
		mBtnSpectrum = (Button)findViewById(R.id.buttonSpectrum);

		mSpinnerIntegrationTime = (Spinner)findViewById(R.id.spinnerIntegrationTime);
		SetSpinnerEntries(mSpinnerIntegrationTime, 1, 501, 31);

		mSpinnerFrameAvgNum = (Spinner)findViewById(R.id.spinnerFrameAvgNum);
		SetSpinnerEntries(mSpinnerFrameAvgNum, 1, 41, 2);

		mSpinnerDataset = findViewById(R.id.spinnerDataset);
		SpinnerDataset(mSpinnerDataset, 0);

		mChkbxEnableAE = (CheckBox)findViewById(R.id.chkbxEnableAE);

		mTextViewSensorId = (TextView)findViewById(R.id.textViewSensorId);
		mTextViewIntegrationTime = (TextView)findViewById(R.id.textViewLblIntegrationTime);
		mTextViewExeTime = (TextView)findViewById(R.id.textViewExeTime);
		mTextViewLog = (TextView)findViewById(R.id.textViewLog);

		mTextViewX = (TextView)findViewById(R.id.textViewX);
		mTextViewY = (TextView)findViewById(R.id.textViewY);
		mTextViewZ = (TextView)findViewById(R.id.textViewZ);

		mChartSpectrum = (LineChart)findViewById(R.id.chartSpectrum);
		mChartSpectrum.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
		mChartSpectrum.getDescription().setEnabled(false);

		// runtime permission
		GetPermission();

		// init bluetooth
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBtAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		ServiceInit();
				
		// set spectrum chart
		mChartPoints = new ArrayList<>();
		mChartPoints.add(new Entry(0, 0));
		mChartDataSetA = new LineDataSet(mChartPoints, "Spectrum");
		
		mChartDataSets = new ArrayList<>();
		mChartDataSets.add(mChartDataSetA);
		
		mChartData = new LineData(mChartDataSets);
		mChartData.setDrawValues(false);
		
		mChartSpectrum.setData(mChartData);
		
		// "Connect / Disconnect" button event handler
		mBtnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	// check permission
				if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					// remind user to grant permission
                    dialogPermission(2);
				}
				
				if (!mBtAdapter.isEnabled()) {
					// bluetooth not enabled yet
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
					return;
				}

				if (mBtnConnectDisconnect.getText().equals("Connect")) {
					// "Connect" button pressed, open DeviceListActivity class, with popup windows that scan for devices
					Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
					startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
				}
				else {
					// "Disconnect" button pressed
					if (mService != null && mService.isConnected()) {
						// stop spectrum acquisition
						mStopSpectrum = true;
						
						// send a disconnect request to BLE peripheral (we can only disconnect once receiving the admit from BLE peripheral)
						mService.sendCommand(DISCONNECT_CMD_STR.getBytes());
					}
				}
		    }
		});

		// "Spectrum" button event handler
		mBtnSpectrum.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCurAppMode == AppRunMode.Spectrum) {
					// stop spectrum acquisition
					mStopSpectrum = true;

					//show result
                    showResultRMES();

					WriteToLog("Stop Spectrum");
					System.out.println("Stop Spectrum");
					mNSP32.Standby((byte)0);

					SetGuiByAppMode(AppRunMode.Connected);
					//set enabled btn Result true

				}
				else {
					// start spectrum acquisition
					SetGuiByAppMode(AppRunMode.Spectrum);

					WriteToLog("Wakeup");	// the "wakeup" is automatically done by our API for MCU (i.e. our API on nRF52)
					WriteToLog("Start Spectrum");
					System.out.println("Start Spectrum");
					AcqSpectrum();
                    //set enabled btn Result true
				}
			}
		});

		// set initial UI state
		guiUpdateHandler.postDelayed(guiUpdateRunnable, 0);
		SetGuiByAppMode(AppRunMode.Disconnected);
	}

	// runtime permission
	private void GetPermission() {
		// check if required permissions are granted
		if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// ActivityCompat.shouldShowRequestPermissionRationale will return true if the user rejects permissions at the first time
			if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
				// tell user why the permissions are required
				dialogPermission(1);
			}
			else {
				// send request
				ActivityCompat.requestPermissions(MainActivity.this,
						new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, 
						REQUEST_PERMISSIONS);
			}
		}
	}

	// do some initialization when a new data channel session is connected
	private void OnDataChannelConnected() {
		// reset status
		mWavelength = null;
		
		// create a new NSP32 instance
		mNSP32 = new NSP32(mDataChannel, mReturnPacketReceivedListener);
		
		// get sensor id and wavelength first
		WriteToLog("Get Sensor ID");
		mNSP32.GetSensorId((byte)0);

		WriteToLog("Get Wavelength");
		mNSP32.GetWavelength((byte)0);

		mNSP32.Standby((byte)0);	// go standby for power saving
	}

	// set GUI by different app mode
	private void SetGuiByAppMode(AppRunMode appMode) {
		mCurAppMode = appMode;	// record the current app mode
		
		switch(appMode) {
			case Disconnected:
				mBtnConnectDisconnect.setText("Connect");
				mTextViewSensorId.setText("-");
				mBtnSpectrum.setEnabled(false);
				mSpinnerIntegrationTime.setEnabled(false);
				mSpinnerFrameAvgNum.setEnabled(false);
				UpdateSaturationStatus(false);
				break;

			case Connected:
				mBtnConnectDisconnect.setText("Disconnect");
				mBtnSpectrum.setEnabled(true);
				mBtnSpectrum.setText("Spectrum");
				mSpinnerIntegrationTime.setEnabled(true);
				mSpinnerFrameAvgNum.setEnabled(true);
				UpdateSaturationStatus(false);
				break;

			case Spectrum:
				mBtnSpectrum.setText("Stop");
				UpdateSaturationStatus(false);
				break;
	    }
	}

	// show log message on UI
	private void WriteToLog(String message) {
		mLogMessage = message + "<br />" + mLogMessage;
		mTextViewLog.setText(Html.fromHtml(mLogMessage));
	}

	// set spinner entries
	private void SetSpinnerEntries(Spinner spinner, int startIdx, int endIdx, int selectedIdx) {
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		for(int i = startIdx; i < endIdx; ++i)
		{
			dataAdapter.add(String.valueOf(i));
		}
		
		spinner.setAdapter(dataAdapter);
		spinner.setSelection(selectedIdx);
	}

	private void SpinnerDataset(Spinner spinner, int selectedIdx){
		ArrayAdapter<String> dataset = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
		dataset.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dataset.add("all_samples");
        spinner.setAdapter(dataset);
        spinner.setSelection(selectedIdx);
	}

	// update saturation status on UI
	private void UpdateSaturationStatus(boolean isSaturated) {
		// use different color to identify the saturation status
		mTextViewIntegrationTime.setBackgroundColor(isSaturated ? Color.RED : Color.GRAY);
	}

	// start spectrum acquisition
	private void AcqSpectrum() {
		int integrationTime = Integer.parseInt(mSpinnerIntegrationTime.getSelectedItem().toString());
		int frameAvgNum = Integer.parseInt(mSpinnerFrameAvgNum.getSelectedItem().toString());
		boolean enableAE = mChkbxEnableAE.isChecked();

		mStopSpectrum = false;	// clear the "stop spectrum" flag
		mRoundTripTimeStart = System.currentTimeMillis();	// record the single command round trip start time
		mNSP32.AcqSpectrum((byte)0, integrationTime, frameAvgNum, enableAE);	// start acquisition
	}

	// send data (commands) to NSP32 through BLE
	private final DataChannel mDataChannel = new DataChannel() {
		public void SendData(byte[] data) {
			try {
				if (mService != null && mService.isConnected()) {
					mService.sendCommand(data);
				}
			}
			catch (Exception excp) {
			}
		}
	};

	// process return packets
	private final ReturnPacketReceivedListener mReturnPacketReceivedListener = new ReturnPacketReceivedListener() {
		public void OnReturnPacketReceived(final ReturnPacket pkt) {
			runOnUiThread(new Runnable() {
				public void run() {
					// if invalid packet is received, show error message
					if(!pkt.IsPacketValid()) {
						WriteToLog("Invalid packet received.");
						return;
					}
							
					// process the return packet
					switch(pkt.CmdCode()) {
						case Standby:
							WriteToLog("Standby");
							break;
						
						case GetSensorId:
							mTextViewSensorId.setText(pkt.ExtractSensorIdStr());
							break;
						
						case GetWavelength:
							mWavelength = pkt.ExtractWavelengthInfo().Wavelength();
							break;
						
						case GetSpectrum:
							if(mStopSpectrum) {
								break;
							}

							SpectrumInfo info = pkt.ExtractSpectrumInfo();

							// calculate the round trip time and display
							long elapsedTime = System.currentTimeMillis() - mRoundTripTimeStart;
							mTextViewExeTime.setText(String.valueOf(elapsedTime));
						
							// if AE is enabled, let the spinner auto select the found integration time
							if(mChkbxEnableAE.isChecked()) {
								mSpinnerIntegrationTime.setSelection(info.IntegrationTime() - 1);
							}
							
							// update saturation status on UI
							UpdateSaturationStatus(info.IsSaturated());
								
							// update XYZ value on UI
							mTextViewX.setText(String.format("%.2f", info.X()));
							mTextViewY.setText(String.format("%.2f", info.Y()));
							mTextViewZ.setText(String.format("%.2f", info.Z()));

							// update spectrum chart data points
							float[] dataPoints = info.Spectrum();
							mChartPoints.clear();
							//file scanner
//							if (unknowLength<10) {
//							System.out.println(dataPoints.length + "\t"+unknowLength);
//								for (int i=0; i< dataPoints.length; i++) {
//									unknow[unknowLength][i] = (double) dataPoints[i];
//								}
//							} else {
//							    unknowLength=8;
//                            }
//							unknowLength++;
							ArrayList<Double> list = new ArrayList<>();
							for(int j=0; j<dataPoints.length; j++){
								list.add((double) dataPoints[j]);
							}
							unknowList.add(list);

							//hien thi chart
							for (int i = 0; i < info.NumOfPoints(); ++i) {

								if(mWavelength != null) {
									mChartPoints.add(new Entry(mWavelength[i], dataPoints[i]));

								}
								else {
									mChartPoints.add(new Entry(i, dataPoints[i]));

								}
							}

							mChartDataSetA.notifyDataSetChanged();
							mChartData.notifyDataChanged();
							mChartData.setDrawValues(false);
							mChartSpectrum.notifyDataSetChanged();
				
							// start a new spectrum acquisition
							AcqSpectrum();
							break;
					}
				}
			});
		}
	};

	// UART service connected/disconnected
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			mService = ((SpectrumTransferService.LocalBinder) rawBinder).getService();

			if (!mService.init()) {
				// Unable to initialize Bluetooth
				finish();
		    }
		}
		
		public void onServiceDisconnected(ComponentName classname)
		{
			mService = null;
		}
	};

	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			//*********************//
			if (action.equals(SpectrumTransferService.ACTION_GATT_CONNECTED)) {
				runOnUiThread(new Runnable() {
					public void run()
					{
						WriteToLog("Connected");
					}
				});
			}

			//*********************//
			if (action.equals(SpectrumTransferService.ACTION_GATT_DISCONNECTED)) {
				runOnUiThread(new Runnable() {
					public void run()
					{
						WriteToLog("Disconnected");
						SetGuiByAppMode(AppRunMode.Disconnected);
						mService.close();
					}
				});
			}

			//*********************//
			if (action.equals(SpectrumTransferService.ACTION_GATT_SERVICES_DISCOVERED)) {
				mService.enableTXNotification();
			}

			//*********************//
			if (action.equals(SpectrumTransferService.ACTION_TX_NOTIFICATION_SET)) {
				runOnUiThread(new Runnable() {
					public void run() {
						OnDataChannelConnected();
						SetGuiByAppMode(AppRunMode.Connected);
					}
				});
			}

			//*********************//
			if (action.equals(SpectrumTransferService.ACTION_DATA_AVAILABLE)) {
				byte[] data = intent.getByteArrayExtra(SpectrumTransferService.EXTRA_DATA);
				
				if(java.util.Arrays.equals(DISCONNECT_CMD_STR.getBytes(), data)) {
					// if this is a disconnect admit from BLE peripheral, then disconnect
					mService.disconnect();
				}
				else {
					try {
						// feed the received data to NSP32 API
						mNSP32.OnReturnBytesReceived(data);
					}
					catch (Exception excp) {
					}
				}
			}
			
			//*********************//
			if (action.equals(SpectrumTransferService.DEVICE_DOES_NOT_SUPPORT_SPECTRUM_TRANSFER)) {
				runOnUiThread(new Runnable() {
					public void run()
					{
						WriteToLog("Invalid BLE service, disconnecting!");
					}
				});

				mService.disconnect();
			}
		}
	};

	private void ServiceInit() {
		Intent bindIntent = new Intent(this, SpectrumTransferService.class);
		bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		
		LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SpectrumTransferService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(SpectrumTransferService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(SpectrumTransferService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(SpectrumTransferService.ACTION_TX_NOTIFICATION_SET);
		intentFilter.addAction(SpectrumTransferService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(SpectrumTransferService.DEVICE_DOES_NOT_SUPPORT_SPECTRUM_TRANSFER);
		return intentFilter;
	}

	private void showResultRMES() {
		InputStream isName = null;
        if(mSpinnerDataset.getSelectedItem().equals("all_samples")) {
            System.out.println("all_samples_611");

			isName = getResources().openRawResource(R.raw.all_samples_611);
        }
        int row_unknow = unknowList.size();
		int col_unknow = unknowList.get(0).size();

		if (row_unknow>0) idItemMenu = true;
		System.out.println("idItemMenu: "+idItemMenu);

		double[][] unknow = new double[row_unknow][col_unknow];
		for(int i=0;i<row_unknow;i++){
			for(int j=0;j<col_unknow;j++){
				unknow[i][j] = unknowList.get(i).get(j);
			}
		}
		Matrix.printMatrix(unknow);

		unknowList.clear();
        int result = RMES.result(isName, unknow, row_unknow);
        System.out.println("Result:\t"+result);
        dialogCustom(result);

    }

    private void dialogCustom(int result){
		Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.dailog_custom);
		dialog.setTitle("RESULT");
		TextView tvResult = dialog.findViewById(R.id.tvResult);
		tvResult.setText("Result: " + result +"%");
		dialog.show();
	}

	private void dialogPermission(int option){
		Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_permission);
		TextView tvDialog1 = dialog.findViewById(R.id.tvDialog1);
		Button btnOk = dialog.findViewById(R.id.btnOkDialog1);
		Button btnNo = dialog.findViewById(R.id.btnNoDialog1);

		if (option ==1 ) {
			tvDialog1.setText("Ứng dụng này yêu cầu Bluetooth để hoạt động tốt. Vui lòng cấp quyền 'vị trí truy cập' cho Bluetooth.");
			btnOk.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					ActivityCompat.requestPermissions(MainActivity.this,
							new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION },
							REQUEST_PERMISSIONS);

					dialog.dismiss();
				}
			});
			btnNo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dialog.dismiss();
				}
			});
		} else {
		    tvDialog1.setText("Bluetooth sẽ không hoạt động nếu không được phép 'vị trí truy cập'.");
		    btnOk.setText("Setting");
			btnOk.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
					myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
					myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivityForResult(myAppSettings, REQUEST_APP_SETTINGS);
				}
			});
			btnNo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dialog.dismiss();
				}
			});
		}
		dialog.show();
	}

	private void writeFileUri() {
		StringBuilder data = new StringBuilder();
		for (int i=0; i<unknowList.size();i++) {
			for (int j=0; j<unknowList.get(0).size();j++) {
				if (j != unknowList.get(0).size()-1)
					data.append(unknowList.get(i).get(j)+",");
				else
					data.append(unknowList.get(i).get(j));
			}
			data.append("\n");
		}
		System.out.println(data);

		LocalDateTime dtime = LocalDateTime.now();
		String name = String.valueOf(dtime).substring(0, 19);

		System.out.println(name);

		try {
			//saving the file into device
			FileOutputStream out = openFileOutput(name+".csv", Context.MODE_PRIVATE);
			out.write(data.toString().getBytes());
			out.close();
			//exporting
			Context context = getApplicationContext();
			File filelocation = new File(getFilesDir(), name+".csv");
			Uri path = FileProvider.getUriForFile(context, "com.nanolambda.SpectrumMeter.fileprovider", filelocation);
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/csv");
			intent.putExtra(Intent.EXTRA_SUBJECT, "data"+name);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.putExtra(Intent.EXTRA_STREAM, path);
			startActivity(Intent.createChooser(intent, "send mail"));
		} catch (Exception e){}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		try {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
		}
		catch (Exception ignore) {
		}
		
		unbindService(mServiceConnection);
		mService.stopSelf();
		mService= null;       
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!mBtAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SELECT_DEVICE:
				//When the DeviceListActivity return, with the selected device address
				if (resultCode == Activity.RESULT_OK && data != null) {
					String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
					mService.connect(deviceAddress);
				}

				break;

			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();				
				}
				else {
					// User did not enable Bluetooth or an error occurred
					Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
					finish();
				}
				
				break;

			default:
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.saveCSV:
				if (idItemMenu)
					writeFileUri();
				else {
					Toast.makeText(this, "No data avaible!", Toast.LENGTH_SHORT).show();
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}

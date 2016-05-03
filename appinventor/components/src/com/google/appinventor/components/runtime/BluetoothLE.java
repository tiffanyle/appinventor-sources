// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2016 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.SdkLevel;

import java.util.*;

// TODO: Cristhian
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import java.nio.charset.Charset;


/**
 * @author Tony Chan ( kwong3513@yahoo.com.hk )
 *         Beibei ZHANG ( beibei.zhang@connect.polyu.hk )
 *         Tiffany Le ( tiffanyl@mit.edu )
 *         Andrew F. McKinney ( mckinney@mit.edu )
 */

@DesignerComponent(version = YaVersion.BLUETOOTHLE_COMPONENT_VERSION,
    description = "This is a trial version of BluetoothLE component, blocks need to be specified later",
    category = ComponentCategory.CONNECTIVITY,
    nonVisible = true,
    iconName = "images/bluetooth.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.BLUETOOTH, " + "android.permission.BLUETOOTH_ADMIN")

public class BluetoothLE extends AndroidNonvisibleComponent implements Component {

  /**
   * Basic Variable
   */
  private static final String LOG_TAG = "BluetoothLEComponent";
  private final Activity activity;
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothGatt currentBluetoothGatt;
  private int device_rssi = 0;
  private final Handler uiThread;
  private boolean mLogEnabled = true;
  private String mLogMessage;

  // testing
  // private List<BluetoothGattCharacteristic> mGattCharList;
  // private List<BluetoothGattDescriptor> mGattDes;

  /**
   * BluetoothLE Info List
   */
  private HashMap<String, BluetoothGatt> gattList;
  private String deviceInfoList = "";
  private List<BluetoothDevice> mLeDevices;
  private List<BluetoothGattService> mGattService;
  private ArrayList<BluetoothGattCharacteristic> gattChars;
  private String serviceUUIDList;
  private String charUUIDList;
  private BluetoothGattCharacteristic mGattChar;
  private HashMap<BluetoothDevice, Integer> mLeDeviceRssi;

  /**
   * BluetoothLE Device Status
   */
  private boolean isEnabled = false;
  private boolean isScanning = false;
  private boolean isConnected = false;
  private boolean isCharRead = false;
  private boolean isCharWrite = false;
  private boolean isServiceRead = false;

  /**
   * GATT value
   */
  private int battery = -1;
  private String tempUnit = "";
  private byte[] bodyTemp;
  private byte[] heartRate;
  private int linkLoss_value = -1;
  private int txPower = -1;
  private byte[] data;
  private byte[] descriptorValue;
  private int intValue = 0;
  private float floatValue = 0;
  private String stringValue="";
  private String byteValue="";
  private int intOffset = 0;
  private int strOffset = 0;
  private int floatOffset = 0;

  private int charType = 0; //byte = 0; int = 1; string = 2; float = 3

  // TODO: Cristhian - BLE Advertisement testing
  private BluetoothLeScanner mBluetoothLeScanner;
  private Handler mHandler = new Handler();
  private AdvertiseCallback mAdvertiseCallback;
  private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
  private static final long SCAN_PERIOD = 5000;
  private String advertisementScanResult = "";

  // TODO: Cristhian - BLE Advertisemetn testing
  private ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        if( result == null
                || result.getDevice() == null) // || TextUtils.isEmpty(result.getDevice().getName())
            return;
 
        StringBuilder builder = new StringBuilder( result.getDevice().getName() );
 
        builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
        
        // TODO: return builder.toString() so that user can use it
        advertisementScanResult = builder.toString();
        // mText.setText(builder.toString());
    }
 
    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }
 
    @Override
    public void onScanFailed(int errorCode) {
        LogMessage("Discovery onScanFailed: " + errorCode, "e" );
        super.onScanFailed(errorCode);
    }
  };


  public BluetoothLE(ComponentContainer container) {
    super(container.$form());
    activity = (Activity) container.$context();
    mLeDevices = new ArrayList<BluetoothDevice>();
    mGattService = new ArrayList<BluetoothGattService>();
    gattChars = new ArrayList<BluetoothGattCharacteristic>();
    mLeDeviceRssi = new HashMap<BluetoothDevice, Integer>();
    gattList = new HashMap<String, BluetoothGatt>();
    uiThread = new Handler();

    if (SdkLevel.getLevel() < SdkLevel.LEVEL_JELLYBEAN_MR2) {
      mBluetoothAdapter = null;
    } else {
      mBluetoothAdapter = newBluetoothAdapter(activity);
    }

    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
      isEnabled = false;
      LogMessage("No Valid BTLE Device on platform", "e");
      form.dispatchErrorOccurredEvent(this, "BluetoothLE", ErrorMessages.ERROR_BLUETOOTH_NOT_ENABLED);
    } else {
      isEnabled = true;
      LogMessage("BluetoothLE Device found", "i");
    }
  }

  private void LogMessage(String message, String level) {
    if (mLogEnabled) {
      mLogMessage = message;
      String errorLevel = "e";
      String warningLevel = "w";

      // push to appropriate logging
      if (level.equals(errorLevel)) {
        Log.e(LOG_TAG, message);
      } else if (level.equals(warningLevel)) {
        Log.w(LOG_TAG, message);
      } else {
        Log.i(LOG_TAG, message);
      }
    }
  }

  public static BluetoothAdapter newBluetoothAdapter(Context context) {
    final BluetoothManager bluetoothManager = (BluetoothManager) context
        .getSystemService(Context.BLUETOOTH_SERVICE);
    return bluetoothManager.getAdapter();
  }

  @SimpleFunction(description="Start Scanning for BluetoothLE devices.")
  public void StartScanning() {
    if (!mLeDevices.isEmpty()) {
      mLeDevices.clear();
      mLeDeviceRssi.clear();
    }
    mBluetoothAdapter.startLeScan(mLeScanCallback);
    LogMessage("StartScanning Successfully.", "i");
  }

  @SimpleFunction(description="Stop Scanning for BluetoothLE devices.")
  public void StopScanning() {
    mBluetoothAdapter.stopLeScan(mLeScanCallback);
    LogMessage("StopScanning Successfully.", "i");
  }

  @SimpleFunction(description="Connect to a BluetoothLE device with index. Index specifies the position in BluetoothLE device list, starting from 0.")
  public void Connect(int index) {
    BluetoothGattCallback newGattCallback = null;
    currentBluetoothGatt = mLeDevices.get(index - 1).connectGatt(activity, false, initCallBack(newGattCallback));
    if(currentBluetoothGatt != null) {
      gattList.put(mLeDevices.get(index - 1).toString(), currentBluetoothGatt);
      LogMessage("Connect Successfully.", "i");
    } else {
      LogMessage("Connect Fail.", "e");
    }
  }

  @SimpleFunction(description="Connect to BluetoothLE device with address. Address specifies bluetooth address of the BluetoothLE device.")
  public void ConnectWithAddress(String address) {
    for (BluetoothDevice bluetoothDevice : mLeDevices) {
      if (bluetoothDevice.toString().equals(address)) {
        BluetoothGattCallback newGattCallback = null;
        currentBluetoothGatt = bluetoothDevice.connectGatt(activity, false, initCallBack(newGattCallback));
        if(currentBluetoothGatt != null) {
          gattList.put(bluetoothDevice.toString(), currentBluetoothGatt);
          LogMessage("Connect with Address Successfully.", "i");
          break;
        } else {
          LogMessage("Connect with Address Fail.", "e");
        }
      }
    }
  }

  @SimpleFunction(description="Disconnect from connected BluetoothLE device with address. Address specifies bluetooth address of the BluetoothLE device.")
  public void DisconnectWithAddress(String address) {
    if (gattList.containsKey(address)) {
      gattList.get(address).disconnect();
      isConnected = false;
      gattList.remove(address);
      LogMessage("Disconnect Successfully.", "i");
    } else {
      LogMessage("Disconnect Fail. No Such Address in the List", "e");
    }
  }

 
  @SimpleFunction(description="Write String value to a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and String value"
      + "are required.")
  public void WriteStringValue(String service_uuid, String characteristic_uuid, String value) {
    writeChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid), value);
  }
  
  
  @SimpleFunction(description="Write Integer value to a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID, Integer value"
      + " and offset are required. Offset specifies the start position of writing data.")
  public void WriteIntValue(String service_uuid, String characteristic_uuid, int value, int offset) {
    writeChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid), value, BluetoothGattCharacteristic.FORMAT_SINT32, offset);
  }
  
  
  @SimpleFunction(description="Read Integer value from a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and offset"
      + " are required. Offset specifies the start position of reading data.")
  public void ReadIntValue(String service_uuid, String characteristic_uuid, int intOffset) {
    charType = 1;
    this.intOffset = intOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
   
  }
  
  
  @SimpleFunction(description="Read String value from a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and offset"
      + " are required. Offset specifies the start position of reading data.")
  public void ReadStringValue(String service_uuid, String characteristic_uuid, int strOffset) {
    charType = 2;
    this.strOffset = strOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
    
  }
  
  
  @SimpleFunction(description="Read Float value from a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and offset"
      + " are required. Offset specifies the start position of reading data.")
  public void ReadFloatValue(String service_uuid, String characteristic_uuid, int floatOffset) {
    charType = 3;
    this.floatOffset = floatOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
  }
  
  
  @SimpleFunction(description="Read Byte value from a connected BluetoothLE device. Service Unique ID and Characteristic Unique ID are required.")
  public void ReadByteValue(String service_uuid, String characteristic_uuid) {
    charType = 0;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
  }

  
  @SimpleFunction(description="Get the RSSI (Received Signal Strength Indicator) of found device with index. Index specifies the position in BluetoothLE device list, starting from 0.")
  public int FoundDeviceRssi(int index) {
    if (index <= mLeDevices.size())
      return mLeDeviceRssi.get(mLeDevices.get(index-1));
    else
      return -1;
  }

  
  @SimpleFunction(description="Get the name of found device with index. Index specifies the position in BluetoothLE device list, starting from 0.")
  public String FoundDeviceName(int index) {
    if (index <= mLeDevices.size()) {
      LogMessage("Device Name is found", "i");
      return mLeDevices.get(index-1).getName();
    } else {
      LogMessage("Device Name isn't found", "e");
      return null;
    }
  }

  
  @SimpleFunction(description="Get the address of found device with index. Index specifies the position in BluetoothLE device list, starting from 0.")
  public String FoundDeviceAddress(int index) {
    if (index <= mLeDevices.size()) {
      LogMessage("Device Address is found", "i");
      return mLeDevices.get(index-1).getAddress();
    } else {
      LogMessage("Device Address is found", "e");
      return "";
    }
  }


  // TODO: Cristhian
  @SimpleFunction(description="Start BLE Advertising.")
  public void StartAdvertising() {
    //create a scan callback if it does not already exist. If it does, you're already scanning for ads.
    if(mBluetoothAdapter != null) {
      mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();  //newBLuetoothLeAdvertiser();


      AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
          super.onStartSuccess(settingsInEffect);
        }
     
        @Override
        public void onStartFailure(int errorCode) {
          LogMessage("Advertising onStartFailure: " + errorCode , "e");
          super.onStartFailure(errorCode);
        }
      };

      AdvertiseSettings advSettings = new AdvertiseSettings.Builder()
        .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
        .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
        .setConnectable( false )
        .build();

      ParcelUuid pUuid = new ParcelUuid( UUID.fromString( "0000b81d-0000-1000-8000-00805f9b34fb" ) );
 
      AdvertiseData advData = new AdvertiseData.Builder()
              .setIncludeDeviceName( true )
              .addServiceUuid( pUuid )
              .addServiceData( pUuid, "Data".getBytes( Charset.forName( "UTF-8" ) ) )
              .build();

      
      if (mAdvertiseCallback == null) {
              // AdvertiseSettings settings = buildAdvertiseSettings(); 
              AdvertiseSettings settings = advSettings;

              // AdvertiseData data = buildAdvertiseData();
              AdvertiseData data = advData;

              // mAdvertiseCallback = new SampleAdvertiseCallback(); 
              mAdvertiseCallback = advertisingCallback;

              if (mBluetoothLeAdvertiser != null) {
                  mBluetoothLeAdvertiser.startAdvertising(settings, data,
                          mAdvertiseCallback);
              }
        }
      
      LogMessage("StartScanningAdvertisements Successfully.", "i");
    }
    else {
      advertisementScanResult = "No bluetooth adapter";
    }
  }

  // TODO: Cristhian 
  @SimpleFunction(description="Stop BLE Advertising.")
  public void StopAdvertising() {
    LogMessage("Stopping BLE Advertising", "i");
    if (mBluetoothLeAdvertiser != null) {
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mAdvertiseCallback = null;
    }
  }

  // TODO: Cristhian
  @SimpleFunction(description="Scans for BLE advertisements.")
  public void ScanAdvertisements() {

    // Will stop the scanning after a set time.
    uiThread.postDelayed(new Runnable() {
        @Override
        public void run() {
            stopAdvertisementScanning();
        }
    }, SCAN_PERIOD);

    if(mBluetoothAdapter != null) {
      mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

      if(mScanCallback != null) {
      
        if(mBluetoothLeScanner != null) {
          // ScanSettings settings = buildScanSettings();
          ScanSettings settings = new ScanSettings.Builder()
            .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
            .build();

          // List<ScanFilter> filters = buildScanFilters();
          List<ScanFilter> filters = new ArrayList<ScanFilter>();
          ScanFilter filter = new ScanFilter.Builder()
            .setServiceUuid( new ParcelUuid(UUID.fromString( "0000b81d-0000-1000-8000-00805f9b34fb" ) ) )
            .build();
          filters.add( filter );

          if(settings != null && filters != null) {
            mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
          }
          else{
            advertisementScanResult = "settings or filters are null";
          }
        }
        else{
          advertisementScanResult = "le scanner is null";
        }
      }
      else {
        advertisementScanResult = "mScanCallback is null";
      }
    }
    else {
      advertisementScanResult = "No bluetooth adapter";
    }
  }

  // TODO: Cristhian
  @SimpleFunction(description="Stops scanning for BLE advertisements.")
  public void StopScanningAdvertisements() {
    LogMessage("Stopping BLE advertsiment scan.", "i");
    stopAdvertisementScanning();

  }




  
  //@SimpleProperty(description="Return the battery level.", category = PropertyCategory.BEHAVIOR)
  public String BatteryValue() {
    if (isCharRead) {
      return Integer.toString(battery);
    } else {
      return "Cannot Read Battery Level";
    }
  }

  
  //@SimpleProperty(description="Return the temperature.", category = PropertyCategory.BEHAVIOR)
  public String TemperatureValue() {
    if (isCharRead) {
      if ((int) bodyTemp[0] == 0) {
        tempUnit = "Celsius";
      } else {
        tempUnit = "Fahrenheit";
      }
      float mTemp = ((bodyTemp[2] & 0xff) << 8) + (bodyTemp[1] & 0xff);
      LogMessage("Temperature value is returned", "i");
      return mTemp + tempUnit;
    } else {
      LogMessage("Cannot read temperature value", "e");
      return "Cannot Read Temperature";
    }
  }

  
  //@SimpleProperty(description="Return the heart rate.", category = PropertyCategory.BEHAVIOR)
  public String HeartRateValue() {
    if (isCharRead) {
      int mTemp = 0;
      if (((int) (heartRate[0] & 0x1)) == 0) {
        mTemp = (heartRate[1] & 0xff);
      } else {
        mTemp = (heartRate[2] & 0xff);
      }
      LogMessage("Heart rate value is returned", "i");
      return mTemp + "times/sec";
    } else {
      LogMessage("Cannot read heart rate value", "e");
      return "Cannot Read Heart Rate";
    }
  }

  
  //@SimpleProperty(description="Return the Tx power.", category = PropertyCategory.BEHAVIOR)
  public int TxPower() {
    return txPower;
  }

  
  @SimpleProperty(description="Return true if a BluetoothLE device is connected; Otherwise, return false.", category = PropertyCategory.BEHAVIOR)
  public boolean IsDeviceConnected() {
    if (isConnected) {
      return true;
    } else {
      return false;
    }
  }
  

  @SimpleProperty(description="Return a sorted list of BluetoothLE devices as a String.", category = PropertyCategory.BEHAVIOR)
  public String DeviceList() {
    deviceInfoList = "";
    mLeDevices = sortDeviceList(mLeDevices);
    if (!mLeDevices.isEmpty()) {
      for (int i = 0; i < mLeDevices.size(); i++) {
        if (i != (mLeDevices.size() - 1)) {
          deviceInfoList += mLeDevices.get(i).getAddress() + " " + mLeDevices.get(i).getName() + " "
              + Integer.toString(mLeDeviceRssi.get(mLeDevices.get(i))) + ",";
        } else {
          deviceInfoList += mLeDevices.get(i).getAddress() + " " + mLeDevices.get(i).getName() + " "
              + Integer.toString(mLeDeviceRssi.get(mLeDevices.get(i)));
        }
      }
    }
    return deviceInfoList;
  }

  
  //@SimpleProperty(description="Return the RSSI (Received Signal Strength Indicator) of connected device.", category = PropertyCategory.BEHAVIOR)
  public String ConnectedDeviceRssi() {
    return Integer.toString(device_rssi);
  }

  
  //@SimpleProperty(description="Return Integer value of read value.", category = PropertyCategory.BEHAVIOR)
  public int IntValue() {
    return intValue;
  }

  
  //@SimpleProperty(description="Return String value of read value.", category = PropertyCategory.BEHAVIOR)
  public String StringValue() {
    return stringValue;
  }

  
  //@SimpleProperty(description="Return Byte value of read value.", category = PropertyCategory.BEHAVIOR)
  public String ByteValue() {
    return byteValue;
  }

  // TODO: Cristhian - BLE Advertisment testing
  @SimpleProperty(description="Returns value of ScanPeriod.")
  public long ScanPeriod() {
    return SCAN_PERIOD;
  }

  // TODO: Cristhian - BLE Advertisment testing
  @SimpleProperty(description="Returns result of advertisment scan.")
  public String AdvertisementScanResult() {
    return advertisementScanResult;
  }
  

  @SimpleEvent(description = "Trigger event when a BluetoothLE device is connected.")
  public void Connected() {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "Connected");
      }
    });
  }

  
  @SimpleEvent(description = "Trigger event when RSSI (Received Signal Strength Indicator) of found BluetoothLE device changes")
  public void RssiChanged(final int device_rssi) {
    uiThread.postDelayed(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "RssiChanged",device_rssi);
      }

    }, 1000);
  }

  
  @SimpleEvent(description = "Trigger event when a new BluetoothLE device is found.")
  public void DeviceFound() {
    EventDispatcher.dispatchEvent(this, "DeviceFound");
  }

  
  //@SimpleEvent(description = "Trigger event when value from connected BluetoothLE device is read. The value"
    //  + " can be byte, Integer, float, or String.")
  public void ValueRead(final String byteValue, final int intValue, final float floatValue, final String stringValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "ValueRead", byteValue, intValue, floatValue, stringValue);
      }
    });
  }
  
  @SimpleEvent(description = "Trigger event when byte value from connected BluetoothLE device is read.")
  public void ByteValueRead(final String byteValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "ByteValueRead", byteValue);
      }
    });
  }
  
  @SimpleEvent(description = "Trigger event when int value from connected BluetoothLE device is read.")
  public void IntValueRead(final int intValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "IntValueRead", intValue);
      }
    });
  }

  @SimpleEvent(description = "Trigger event when String value from connected BluetoothLE device is read.")
  public void StringValueRead(final String stringValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "StringValueRead", stringValue);
      }
    });
  }
  
  @SimpleEvent(description = "Trigger event when byte value from connected BluetoothLE device is read.")
  public void FloatValueRead(final float floatValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "FloatValueRead", floatValue);
      }
    });
  }
  
  //@SimpleEvent(description = "Trigger event when value from connected BluetoothLE device is changed. The value"
    //  + " can be byte, Integer, float, or String.")
  public void ValueChanged(final String byteValue, final int intValue, final float floatValue, final String stringValue) {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "ValueChanged", byteValue, intValue, floatValue, stringValue);
      }
    });
  }
  
  @SimpleEvent(description = "Trigger event when byte value from connected BluetoothLE device is changed.")
public void ByteValueChanged(final String byteValue) {
  uiThread.post(new Runnable() {
    @Override
    public void run() {
      EventDispatcher.dispatchEvent(BluetoothLE.this, "ByteValueChanged", byteValue);
    }
  });
}
  @SimpleEvent(description = "Trigger event when int value from connected BluetoothLE device is changed.")
public void IntValueChanged(final int intValue) {
  uiThread.post(new Runnable() {
    @Override
    public void run() {
      EventDispatcher.dispatchEvent(BluetoothLE.this, "IntValueChanged", intValue);
    }
  });
}
  
  @SimpleEvent(description = "Trigger event when int value from connected BluetoothLE device is changed.")
public void FloatValueChanged(final float floatValue) {
  uiThread.post(new Runnable() {
    @Override
    public void run() {
      EventDispatcher.dispatchEvent(BluetoothLE.this, "FloatValueChanged", floatValue);
    }
  });
}
  
  
  @SimpleEvent(description = "Trigger event when String value from connected BluetoothLE device is changed.")
public void StringValueChanged(final String stringValue) {
  uiThread.post(new Runnable() {
    @Override
    public void run() {
      EventDispatcher.dispatchEvent(BluetoothLE.this, "StringValueChanged", stringValue);
    }
  });
}
  
  @SimpleEvent(description = "Trigger event when value is successfully written to connected BluetoothLE device.")
  public void ValueWrite() {
    uiThread.post(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(BluetoothLE.this, "ValueWrite");
      }
    });
  }
  
  @SimpleFunction(description="Return list of supported services for connected device as a String")
  public String GetSupportedServices() {
    if (mGattService == null) return ",";
    serviceUUIDList = ", ";
    for (int i =0; i < mGattService.size(); i++){
        if (i==0){
          serviceUUIDList = "";
        }
        String serviceUUID = mGattService.get(i).getUuid().toString();
        String unknownServiceString = "Unknown Service";
        String serviceName = BluetoothLEGattAttributes.lookup(serviceUUID, unknownServiceString);
        serviceUUIDList += serviceUUID + " "+ serviceName + ",";
   
    }
    return serviceUUIDList;
}
  
  @SimpleFunction(description="Return Unique ID of selected service with index. Index specified by list of supported services for a connected device, starting from 0.")
  public String GetServicebyIndex(int index) {
    return mGattService.get(index).getUuid().toString();
  }
  
  @SimpleFunction(description="Return list of supported characteristics for connected device as a String")
  public String GetSupportedCharacteristics() {
    if (mGattService == null) return ",";
    charUUIDList = ", ";
    for (int i =0; i < mGattService.size(); i++){
        if (i==0){
          charUUIDList = "";
        }
        for(BluetoothGattCharacteristic characteristic: mGattService.get(i).getCharacteristics()){
          gattChars.add(characteristic);
        }
    }
        String unknownCharString = "Unknown Characteristic";
        for (int j = 0; j < gattChars.size(); j++){
          String charUUID = gattChars.get(j).getUuid().toString();
          String charName = BluetoothLEGattAttributes.lookup(charUUID, unknownCharString);
          charUUIDList += charUUID + " "+ charName + ",";
        }

    return charUUIDList;
}
  
  
  @SimpleFunction(description="Return Unique ID of selected characteristic with index. Index specified by list of supported characteristics for a connected device, starting from 0.")
  public String GetCharacteristicbyIndex(int index) {
    return gattChars.get(index).getUuid().toString();
  }
  
  /**
   * Functions
   */
  // sort the device list by RSSI
  private List<BluetoothDevice> sortDeviceList(List<BluetoothDevice> deviceList) {
    Collections.sort(deviceList, new Comparator<BluetoothDevice>() {
      @Override
      public int compare(BluetoothDevice device1, BluetoothDevice device2) {
        int result = mLeDeviceRssi.get(device1) - mLeDeviceRssi.get(device2);
        return result;
      }
    });
    Collections.reverse(deviceList);
    return deviceList;
  }

  
  // add device when scanning
  private void addDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (!mLeDevices.contains(device)) {
      mLeDevices.add(device);
      mLeDeviceRssi.put(device, rssi);
      DeviceFound();
    } else {
      mLeDeviceRssi.put(device, rssi);
    }
    RssiChanged(rssi);
  }

  
  // read characteristic based on UUID
  private void readChar(UUID ser_uuid, UUID char_uuid) {
    if (isServiceRead && !mGattService.isEmpty()) {
      for (int i = 0; i < mGattService.size(); i++) {
        if (mGattService.get(i).getUuid().equals(ser_uuid)) {
          
          BluetoothGattDescriptor desc = mGattService.get(i).getCharacteristic(char_uuid)
              .getDescriptor(UUID.fromString(BluetoothLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
          
          mGattChar = mGattService.get(i).getCharacteristic(char_uuid);
          
          if (desc != null) {
            if ((mGattService.get(i).getCharacteristic(char_uuid).getProperties() & 
               BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
              desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            } else {
              desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            currentBluetoothGatt.writeDescriptor(desc);
          }

          if(mGattChar != null) {
            currentBluetoothGatt.setCharacteristicNotification(mGattChar, true);
            isCharRead = currentBluetoothGatt.readCharacteristic(mGattChar);
          }
          break;
        }
      }
    }
    
    if(isCharRead == true) {
      LogMessage("Read Character Successfully.", "i");
    } else {
      LogMessage("Read Character Fail.", "i");
    }
  }

  
  // Write characteristic based on uuid
  private void writeChar(UUID ser_uuid, UUID char_uuid, int value, int format, int offset) {
    if (isServiceRead && !mGattService.isEmpty()) {
      for (int i = 0; i < mGattService.size(); i++) {
        if (mGattService.get(i).getUuid().equals(ser_uuid)) {
          mGattChar = mGattService.get(i).getCharacteristic(char_uuid);
          if (mGattChar != null) {
            mGattChar.setValue(value, format, offset);
            isCharWrite = currentBluetoothGatt.writeCharacteristic(mGattChar);
          }
          break;
        }
      }
    }
    
    if(isCharWrite == true) {
      LogMessage("Write Gatt Characteristic Successfully", "i");
    } else {
      LogMessage("Write Gatt Characteristic Fail", "e");
    }
  }

  private void writeChar(UUID ser_uuid, UUID char_uuid, String value) {
    if (isServiceRead && !mGattService.isEmpty()) {
      for (int i = 0; i < mGattService.size(); i++) {
        if (mGattService.get(i).getUuid().equals(ser_uuid)) {
          mGattChar = mGattService.get(i).getCharacteristic(char_uuid);
          if (mGattChar != null) {
            mGattChar.setValue(value);
            isCharWrite = currentBluetoothGatt.writeCharacteristic(mGattChar);
          }
          break;
        }
      }
    }
    
    if(isCharWrite == true) {
      LogMessage("Write Gatt Characteristic Successfully", "i");
    } else {
      LogMessage("Write Gatt Characteristic Fail", "e");
    }
  }

  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          isScanning = true;
          addDevice(device, rssi, scanRecord);
        }
      });
    }
  };

  public BluetoothGattCallback initCallBack(BluetoothGattCallback newGattCallback) {
    newGattCallback = this.mGattCallback;
    return newGattCallback;
  }

  BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        isConnected = true;
        gatt.discoverServices();
        gatt.readRemoteRssi();
        Connected();
      }
      
      if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        isConnected = false;
      }

    }

    @Override
    // New services discovered
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
      
        mGattService = (ArrayList<BluetoothGattService>) gatt.getServices();
        isServiceRead = true;
      }
    }

    @Override
    // Result of a characteristic read operation
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      
      if (status == BluetoothGatt.GATT_SUCCESS) {

          data = characteristic.getValue();
          intValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, intOffset);
          stringValue = characteristic.getStringValue(strOffset);
          floatValue = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, floatOffset);
          byteValue = "";
          for (byte i : data) {
            byteValue += i;
          }
          isCharRead = true;
          ByteValueRead(byteValue);
          IntValueRead(intValue);
          StringValueRead(stringValue);
          FloatValueRead(floatValue);
      }
    }

    @Override
    // Result of a characteristic read operation is changed
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      
 
        data = characteristic.getValue();
        LogMessage("dataLength: " + data.length,"i");
        switch (charType) {
        case 0: 
          byteValue = "";
          for (byte i : data) {
            byteValue += i;
          }
          LogMessage("byteValue: "+ byteValue,"i");
          ByteValueChanged(byteValue);
          break;
        case 1: 
          intValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, intOffset);
          LogMessage("intValue: " + intValue,"i");
          IntValueChanged(intValue);
          break;
        case 2:   
          stringValue = characteristic.getStringValue(strOffset);
          LogMessage("stringValue: "+ stringValue,"i");
          StringValueChanged(stringValue);
          break;
        case 3: 
          if(data.length==1){
            floatValue = (float)(data[0]*Math.pow(10,0));
          }
          else if (data.length==2 || data.length==3){
            floatValue = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, floatOffset);
          }
          else{
            floatValue = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, floatOffset);
          }
          LogMessage("floatValue: " + floatValue,"i");
          FloatValueChanged(floatValue);
       default: 
         byteValue = "";
         for (byte i : data) {
           byteValue += i;
         }
         LogMessage("byteValue: "+ byteValue,"i");
         ByteValueChanged(byteValue);
         break;
        }

        isCharRead = true;
     
    }

    @Override
    // set value of characteristic
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      LogMessage("Write Characteristic Successfully.", "i");
      ValueWrite();
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      descriptorValue = descriptor.getValue();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      LogMessage("Write Descriptor Successfully.", "i");
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
      device_rssi = rssi;
      RssiChanged(device_rssi);
    }
  };

  //TODO: Cristhian - BLE Advertismsent testing
  /**
   * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
   * and disable the built-in timeout since this code uses its own timeout runnable.
   */
  private AdvertiseSettings buildAdvertiseSettings() {
      AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
      settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
      settingsBuilder.setTimeout(0);
      return settingsBuilder.build();
  }

  
  //TODO: Cristhian - BLE Advertismsent testing
  /**
   * Returns an AdvertiseData object which includes the Service UUID and Device Name.
   */
  private AdvertiseData buildAdvertiseData() {

      /**
       * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
       *  This includes everything put into AdvertiseData including UUIDs, device info, &
       *  arbitrary service or manufacturer data.
       *  Attempting to send packets over this limit will result in a failure with error code
       *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
       *  onStartFailure() method of an AdvertiseCallback implementation.
       */

      AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
      dataBuilder.addServiceUuid(ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb"));
      dataBuilder.setIncludeDeviceName(true);

      /* For example - this will cause advertising to fail (exceeds size limit) */
      //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
      //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

      return dataBuilder.build();
  }

  // TODO: Cristhian - BLE Advertismsent testing
  private void stopAdvertisementScanning() {
    LogMessage("Stopping advertisement scanning.", "i");
    mBluetoothLeScanner.stopScan(mScanCallback);
    // mScanCallback = null;

    // TODO: maybe update data that potentially is returned?
  }

}



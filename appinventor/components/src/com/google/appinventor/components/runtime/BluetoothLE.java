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
  public int ReadIntValue(String service_uuid, String characteristic_uuid, int intOffset) {
    this.intOffset = intOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
    return intValue;
  }
  
  
  @SimpleFunction(description="Read String value from a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and offset"
      + " are required. Offset specifies the start position of reading data.")
  public String ReadStringValue(String service_uuid, String characteristic_uuid, int strOffset) {
    this.strOffset = strOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
    return stringValue;
  }
  
  
  //@SimpleFunction(description="Read Float value from a connected BluetoothLE device. Service Unique ID, Characteristic Unique ID and offset"
    //  + " are required. Offset specifies the start position of reading data.")
  public void ReadFloatValue(String service_uuid, String characteristic_uuid, int floatOffset) {
    this.floatOffset = floatOffset;
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
  }
  
  
  @SimpleFunction(description="Read Byte value from a connected BluetoothLE device. Service Unique ID and Characteristic Unique ID are required.")
  public String ReadByteValue(String service_uuid, String characteristic_uuid) {
    readChar(UUID.fromString(service_uuid), UUID.fromString(characteristic_uuid));
    return byteValue;
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

  
  @SimpleProperty(description="Return the RSSI (Received Signal Strength Indicator) of connected device.", category = PropertyCategory.BEHAVIOR)
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
      }
    }

    @Override
    // Result of a characteristic read operation is changed
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      
 
        data = characteristic.getValue();
        //xx no 32
        intValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, intOffset);
        stringValue = characteristic.getStringValue(strOffset);
        byteValue = "";
        for (byte i : data) {
          byteValue += i;
        }
        isCharRead = true;
        ByteValueChanged(byteValue);
        IntValueChanged(intValue);
        StringValueChanged(stringValue);
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
}



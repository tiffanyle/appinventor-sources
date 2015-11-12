// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Copyright 2014 - David Garrett - Broadcom Corporation
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;


import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.EventDispatcher;

import android.os.Handler;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

/**
 * The WICEDSense component connects to the BLTE device
 *
 * @author  David Garrett (not the violionist)
 */

@DesignerComponent(version = YaVersion.BLE_COMPONENT_VERSION,
  //category = ComponentCategory.CONNECTIVITY,
  category = ComponentCategory.SENSORS,
  description = "The WICEDSense component is still experimental",
  nonVisible = true)
@SimpleObject
@UsesPermissions(permissionNames =
  "android.permission.BLUETOOTH, " +
  "android.permission.BLUETOOTH_ADMIN")
public final class WICEDSense extends BLE implements Component {
  private final Activity activity;
  private final Handler androidUIHandler;
  private long startTime = 0;
  private long currentTime = 0;
  private long tempCurrentTime = 0;
  private float mXAccel = 0;
  private float mYAccel = 0;
  private float mZAccel = 0;
  private float mXGyro = 0;
  private float mYGyro = 0;
  private float mZGyro = 0;
  private float mXMagnetometer = 0;
  private float mYMagnetometer = 0;
  private float mZMagnetometer = 0;
  private float mHumidity = 0;
  private float mPressure = 0;
  private float mTemperature = 0;
  private boolean mUseFahrenheit = true;
  
  private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  private static final UUID SENSOR_SERVICE_UUID = UUID.fromString("739298B6-87B6-4984-A5DC-BDC18B068985");
  private static final UUID SENSOR_NOTIFICATION_UUID = UUID.fromString("33EF9113-3B55-413E-B553-FEA1EAADA459");
  private static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
  private static final UUID BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
  
  public WICEDSense (ComponentContainer container) {
    super(container.$form());
    activity = container.$context();
    androidUIHandler = new Handler();

    // names the function
    String functionName = "WICEDSense";

    // record the constructor time
    startTime  = System.nanoTime();
    currentTime  = startTime;
    tempCurrentTime  = startTime;
  }
  
  BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    if (SENSOR_NOTIFICATION_UUID.equals(characteristic.getUuid())) {
      byte[] value = characteristic.getValue();
      int bitMask = value[0];
      int index = 1;
      boolean envUpdatedFlag = false;
      boolean sensorsUpdatedFlag = false;

      // Update timestamp
      currentTime = System.nanoTime();
      if (bitMask == 0xFF) { 
        List<Float> accelProfile = new ArrayList<Float>();
        float nextValue;
        int unsignedByte;
  
        // Load up 10 unsigned bytes accelerometer data
        for (int i = 0; i < 10; i++) {
          unsignedByte = value[i+1] & 0xff;
          nextValue = (float)unsignedByte * (float)(16.0 / 255.0);
          accelProfile.add(nextValue);
        }

        // Load up 6 signed bytes gyro data
        for (int i = 0; i < 6; i++) {
          nextValue = (float)value[i+11] * (float)(1600.0 / 127.0);
          accelProfile.add(nextValue);
        }

        // load up peak accel
        unsignedByte = value[17] & 0xff;
        nextValue = (float)unsignedByte * (float)(16.0 / 255.0);
        accelProfile.add(nextValue);

        // load up sequence ID
        unsignedByte = value[18] & 0xff;
        nextValue = (float)unsignedByte;
        accelProfile.add(nextValue);

        // fire the event with new data
        AccelProfileUpdated(YailList.makeList(accelProfile));
      }  else {

        if ((bitMask & 0x1)>0) {
          sensorsUpdatedFlag = true;
          mXAccel = (value[index+1] << 8) + (value[  index] & 0xFF);
          mYAccel = (value[index+3] << 8) + (value[index+2] & 0xFF);
          mZAccel = (value[index+5] << 8) + (value[index+4] & 0xFF);
          index = index + 6;
        }
        if ((bitMask & 0x2)>0) {
          sensorsUpdatedFlag = true;
          mXGyro = (value[index+1] << 8) + (value[  index] & 0xFF);
          mYGyro = (value[index+3] << 8) + (value[index+2] & 0xFF);
          mZGyro = (value[index+5] << 8) + (value[index+4] & 0xFF);
          mXGyro = mXGyro / (float)100.0;
          mYGyro = mYGyro / (float)100.0;
          mZGyro = mZGyro / (float)100.0;
          index = index + 6;
        }
        if ((bitMask & 0x4)>0) {
          envUpdatedFlag = true;
          mHumidity =  ((value[index+1] & 0xFF) << 8) + (value[index] & 0xFF);
          mHumidity = mHumidity / (float)10.0;
          index = index + 2;
        }
        if ((bitMask & 0x8)>0) {
          sensorsUpdatedFlag = true;
          mXMagnetometer = (value[index+1] << 8) + (value[  index] & 0xFF);
          mYMagnetometer = (value[index+3] << 8) + (value[index+2] & 0xFF);
          mZMagnetometer = (value[index+5] << 8) + (value[index+4] & 0xFF);
          index = index + 6;
        }
        if ((bitMask & 0x10)>0) {
          envUpdatedFlag = true;
          mPressure =  ((value[index+1] & 0xFF) << 8) + (value[index] & 0xFF);
          mPressure = mPressure / (float)10.0;
          index = index + 2;
        }
        if ((bitMask & 0x20)>0) {
          envUpdatedFlag = true;
          mTemperature =  ((value[index+1] & 0xFF) << 8) + (value[index] & 0xFF);
          mTemperature = mTemperature / (float)10.0;
          index = index + 2;
          tempCurrentTime = System.nanoTime();
        }

        //LogMessage("Reading back sensor data with type " + bitMask + " packet", "i");
        // fire the response
        if (sensorsUpdatedFlag) { 
          SensorsUpdated();
        } else if (envUpdatedFlag) { 
          EnvSensorsUpdated();
        }
      }
      
      
    }
  }  

  };
  
  @SimpleEvent(description = "Sensor data updated.")
  public void SensorsUpdated() {
    androidUIHandler.post(new Runnable() {
        public void run() {
          EventDispatcher.dispatchEvent(WICEDSense.this, "SensorsUpdated");
        }
      });
  }
  
  @SimpleEvent(description = "Acceleration Profile updated.")
  public void AccelProfileUpdated(final YailList returnList) {
    androidUIHandler.post(new Runnable() {
        public void run() {
          EventDispatcher.dispatchEvent(WICEDSense.this, "AccelProfileUpdated", returnList);
        }
      });
  }
  
  @SimpleEvent(description = "Environment Sensor data updated.")
  public void EnvSensorsUpdated() {
    androidUIHandler.post(new Runnable() {
        public void run() {
          EventDispatcher.dispatchEvent(WICEDSense.this, "EnvSensorsUpdated");
        }
      });
  }
  
  
  /**
   * Return the X Accelerometer sensor data
   */
  @SimpleProperty(description = "Get X Accelerometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float XAccel() {
    return mXAccel;
  }
  
  /**
   * Return the Y Accelerometer sensor data
   */
  @SimpleProperty(description = "Get Y Accelerometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float YAccel() {
    return mYAccel;
  }

  /**
   * Return the Z Accelerometer sensor data
   */
  @SimpleProperty(description = "Get Z Accelerometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float ZAccel() {
    return mZAccel;
  }

  /**
   * Return the X Gyro sensor data
   */
  @SimpleProperty(description = "Get X Gyro data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float XGyro() {
    return mXGyro;
  }

  /**
   * Return the Y Gyro sensor data
   */
  @SimpleProperty(description = "Get Y Gyro data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float YGyro() {
    return mYGyro;
  }

  /**
   * Return the Z Gyro sensor data
   */
  @SimpleProperty(description = "Get Z Gyro data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float ZGyro() {
    return mZGyro;
  }

  /**
   * Return the X Magnetometer sensor data
   */
  @SimpleProperty(description = "Get X Magnetometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float XMagnetometer() {
    return mXMagnetometer;
  }

  /**
   * Return the Y Magnetometer sensor data
   */
  @SimpleProperty(description = "Get Y Magnetometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float YMagnetometer() {
    return mYMagnetometer;
  }

  /**
   * Return the Z Magnetometer sensor data
   */
  @SimpleProperty(description = "Get Z Magnetometer data", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float ZMagnetometer() {
    return mZMagnetometer;
  }

  /**
   * Return the Compass heading
   */
  @SimpleProperty(description = "Get the compass heading in degrees assuming device is flat", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public float Heading() {
    double mag = Math.sqrt(mXMagnetometer*mXMagnetometer + mYMagnetometer*mYMagnetometer);
    double heading;

    //LogMessage("Calculating heading from X+Y magnetometer data (" +
    //  mXMagnetometer + "," + mYMagnetometer + "), mag = " + mag, "i");

    if (mag > 0.0) {
      // convert x,y to radians to degrees
      double nX = mXMagnetometer/mag;
      double nY = mYMagnetometer/mag;
      heading = Math.atan2(nY, nX) * 57.295779578 + 180.0;
    } else {
      heading = 0.0;
    }

    //LogMessage("Heading = " + heading, "i");
    return (float)heading;
  }
  
  /**
   * Return the Humidity sensor data
   */
  @SimpleProperty(description = "Get Humidity data in %",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
      public float Humidity() {
      return mHumidity;
    }

  /**
   * Return the Pressure sensor data
   */
  @SimpleProperty(description = "Get Pressure data in millibar",
    category = PropertyCategory.BEHAVIOR,
    userVisible = true)
    public float Pressure() {
    return mPressure;
  }

  /**
   * Sets the temperature setting for Fahrenheit or Celsius
   *
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
    defaultValue = "True")
    @SimpleProperty(description = "Sets temperature data in Fahrenheit, not Celius",
      category = PropertyCategory.BEHAVIOR,
      userVisible = true)
      public void UseFahrenheit(boolean enableFlag) {
    mUseFahrenheit = enableFlag;
  }

  /**
   * Sets the temperature setting for Fahrenheit or Celsius
   *
   */
  @SimpleProperty(description = "Returns true if temperature format is in Fahrenheit",
    category = PropertyCategory.BEHAVIOR,
    userVisible = true)
    public boolean UseFahrenheit() {
    return mUseFahrenheit;
  }

  
  /**
   * Return the Temperature sensor data
   */
  @SimpleProperty(description = "Get Temperature data in Fahrenheit or Celsius",
    category = PropertyCategory.BEHAVIOR,
    userVisible = true)
    public float Temperature() {
    float tempConvert;

    // get temperature in celsius
    tempConvert = mTemperature;

    // Convert to Fahrenheit if selected
    if (mUseFahrenheit) {
      tempConvert = tempConvert* (float)(9.0/5.0) + (float)32.0;
    }

    return tempConvert;
  }
  
}
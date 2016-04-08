// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for supporting the
 * Bluetooth Low Energy (BluetoothLE) component.
 *
 * @author mckinney@mit.edu (Andrew F. McKinney)
 */

public class BluetoothLEGattAttributes {
  private static HashMap<String, String> attributes = new HashMap();
  public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
  public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

  static {
    // Services.
    attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
    attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
    attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Level Service");
   
    // Characteristics.
    attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
    attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level Characteristic");
  }

  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }
}
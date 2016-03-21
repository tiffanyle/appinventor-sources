// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import java.util.UUID;

/**
 * @author Tony Chan ( kwong3513@yahoo.com.hk )
 *         Beibei ZHANG ( beibei.zhang@connect.polyu.hk )
 *         Tiffany Le ( tiffanyle@gmail.com )
 *         Andrew F. McKinney ( mckinney@mit.edu )
 */

public final class BluetoothLEList {
  public final static UUID BATTERY_LEVEL_SER = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
  public final static UUID BATTERY_LEVEL_CHAR = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
  public final static UUID THERMOMETER_SER = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
  public final static UUID THERMOMETER_CHAR = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");
  public final static UUID FINDME_SER = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
  public final static UUID FINDME_CHAR = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
  public final static UUID HEART_RATE_SER = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
  public final static UUID HEART_RATE_MEASURE_CHAR = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
  public final static UUID LINKLOSS_SER=UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
  public final static UUID LINKLOSS_CHAR=UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
  public final static UUID TXPOWER_SER=UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
  public final static UUID TXPOWER_CHAR=UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
  public final static UUID CHAR_CONFIG_DES = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
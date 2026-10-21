package com.kavun.shared.util.core;

import java.net.UnknownHostException;
import java.net.InetAddress;
import static com.kavun.constant.LoggingConstants.*;

public final class NetworkUtils {

  private NetworkUtils() {
    throw new AssertionError("Cannot instantiate NetworkUtils");
  }

  public static String resolveHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return UNKNOWN;
    }
  }

  public static String resolveIp() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return UNKNOWN;
    }
  }
}

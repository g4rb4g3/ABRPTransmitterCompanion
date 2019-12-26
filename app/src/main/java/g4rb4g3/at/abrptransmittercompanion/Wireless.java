package g4rb4g3.at.abrptransmittercompanion;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class Wireless {
  private Context mContext;

  public Wireless(Context context) {
    mContext = context;
  }

  /**
   * Gets the device's internal LAN IP address associated with the cellular network
   *
   * @return Local cellular network LAN IP address
   */
  public static String getInternalMobileIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en != null && en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
            return inetAddress.getHostAddress();
          }
        }
      }
    } catch (SocketException ex) {
      return "Unknown";
    }

    return "Unknown";
  }

  /**
   * Gets the device's wireless address
   *
   * @return Wireless address
   */
  private InetAddress getWifiInetAddress() throws UnknownHostException, NoWifiManagerException {
    String ipAddress = getInternalWifiIpAddress(String.class);
    return InetAddress.getByName(ipAddress);
  }

  /**
   * Gets the device's internal LAN IP address associated with the WiFi network
   *
   * @param type
   * @param <T>
   * @return Local WiFi network LAN IP address
   */
  public <T> T getInternalWifiIpAddress(Class<T> type) throws UnknownHostException, NoWifiManagerException {
    int ip = getWifiInfo().getIpAddress();

    //Endianness can be a potential issue on some hardware
    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
      ip = Integer.reverseBytes(ip);
    }

    byte[] ipByteArray = BigInteger.valueOf(ip).toByteArray();


    if (type.isInstance("")) {
      return type.cast(InetAddress.getByAddress(ipByteArray).getHostAddress());
    } else {
      return type.cast(new BigInteger(InetAddress.getByAddress(ipByteArray).getAddress()).intValue());
    }
  }

  /**
   * Gets the Android WiFi information in the context of the current activity
   *
   * @return WiFi information
   */
  private WifiInfo getWifiInfo() throws NoWifiManagerException {
    return getWifiManager().getConnectionInfo();
  }

  /**
   * Gets the Android WiFi manager in the context of the current activity
   *
   * @return WifiManager
   */
  private WifiManager getWifiManager() throws NoWifiManagerException {
    WifiManager manager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if (manager == null) {
      throw new NoWifiManagerException();
    }

    return manager;
  }

  public static class NoWifiManagerException extends Exception {
  }
}

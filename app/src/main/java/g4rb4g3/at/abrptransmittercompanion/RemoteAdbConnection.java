package g4rb4g3.at.abrptransmittercompanion;

import android.util.Base64;
import android.util.Log;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class RemoteAdbConnection implements Runnable {
  private String mIp;
  private AdbStream mAdbStream;
  boolean mDownloadCompleted = false;
  boolean mInstallCompleted = false;

  public RemoteAdbConnection(String ip) {
    mIp = ip;
  }

  public void close() {
    try {
      mAdbStream.close();
    } catch (IOException e) {
      Log.e(MainActivity.TAG, e.getMessage(), e);
    }
  }

  @Override
  public void run() {
    try {
      Socket socket = new Socket(mIp, 5555);

      AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
        @Override
        public String encodeToString(byte[] data) {
          return Base64.encodeToString(data, Base64.NO_CLOSE);
        }
      });

      AdbConnection connection = AdbConnection.create(socket, crypto);
      connection.connect();

      mAdbStream = connection.open("shell:");

      // Start the receiving thread
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (!mAdbStream.isClosed())
            try {
              String msg = new String(mAdbStream.read(), "UTF-8");
              if(msg.contains("app-debug.apk        100%")) {
                mDownloadCompleted = true;
              }
              if(msg.equals("Success\r\n")) {
                mInstallCompleted = true;
              }
              Log.d(MainActivity.TAG, msg);
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
              return;
            } catch (InterruptedException e) {
              e.printStackTrace();
              return;
            } catch (IOException e) {
              e.printStackTrace();
              return;
            }
        }
      }).start();

      mAdbStream.write("busybox wget http://" + Wireless.getInternalMobileIpAddress() + ":8080 -O /sdcard/app-debug.apk\n");
      while(!mDownloadCompleted) {
        Thread.sleep(500);
      }
      mAdbStream.write("pm install -r /sdcard/app-debug.apk\n");
      while(!mInstallCompleted) {
        Thread.sleep(500);
      }
      mAdbStream.write("monkey -p g4rb4g3.at.abrptransmitter -c android.intent.category.LAUNCHER 1\n");
    } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
      Log.e(MainActivity.TAG, e.getMessage(), e);
    }
  }
}

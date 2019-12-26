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

  boolean mDownloadCompleted = false;
  boolean mInstallCompleted = false;
  private String mIp;
  private AdbStream mAdbStream;
  private RemoteAdbConnectionDelegate mRemoteAdbConnectionDelegate;

  public RemoteAdbConnection(String ip, RemoteAdbConnectionDelegate delegate) {
    mIp = ip;
    mRemoteAdbConnectionDelegate = delegate;
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
              if (msg.contains("app-debug.apk        100%")) {
                mDownloadCompleted = true;
              }
              if (msg.equals("Success\r\n")) {
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

      mRemoteAdbConnectionDelegate.setProgressDialogMessage(R.string.sending_apk);
      mAdbStream.write("busybox wget http://" + Wireless.getInternalMobileIpAddress() + ":8080 -O /sdcard/app-debug.apk\n");
      while (!mDownloadCompleted) {
        Thread.sleep(500);
      }

      mRemoteAdbConnectionDelegate.setProgressDialogMessage(R.string.installing_apk);
      mAdbStream.write("pm install -r /sdcard/app-debug.apk\n");
      while (!mInstallCompleted) {
        Thread.sleep(500);
      }

      mRemoteAdbConnectionDelegate.setProgressDialogMessage(R.string.launching_apk);
      mAdbStream.write("monkey -p g4rb4g3.at.abrptransmitter -c android.intent.category.LAUNCHER 1\n");
      Thread.sleep(500);
      mAdbStream.close();

      mRemoteAdbConnectionDelegate.completed();
    } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
      Log.e(MainActivity.TAG, e.getMessage(), e);
    }
  }

  public interface RemoteAdbConnectionDelegate {
    void setProgressDialogMessage(int id);

    void completed();
  }
}

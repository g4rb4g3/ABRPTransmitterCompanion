package g4rb4g3.at.abrptransmittercompanion;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import fi.iki.elonen.NanoHTTPD;

public class AbrpTransmitterServer extends NanoHTTPD {

  private String mApkPath;

  public AbrpTransmitterServer(int port, String apkPath) {
    super(port);
    mApkPath = apkPath;
  }

  @Override
  public Response serve(IHTTPSession session) {
    try {
      File file = new File(mApkPath);
      FileInputStream fileInputStream = new FileInputStream(file);
      Response response = newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fileInputStream, file.length());
      response.addHeader("Content-Disposition", "attachment; filename=\"app-debug.apk\"");
      return response;
    } catch (FileNotFoundException e) {
      Log.e(MainActivity.TAG, e.getMessage(), e);
    }
    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error serving " + mApkPath);
  }
}

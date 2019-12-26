package g4rb4g3.at.abrptransmittercompanion;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;

public class AbrpTransmitterReleaseLoader extends AsyncTask<String, String, String> {

  private Context mContext;
  private AbrpTransmitterReleaseLoaderDelegate mAbrpTransmitterReleaseLoaderDelegate;

  public AbrpTransmitterReleaseLoader(Context context, AbrpTransmitterReleaseLoaderDelegate delegate) {
    mContext = context;
    mAbrpTransmitterReleaseLoaderDelegate = delegate;
  }

  @Override
  protected String doInBackground(String... params) {
    HttpURLConnection connection = null;
    BufferedReader reader = null;

    try {
      URL url = new URL(params[0]);
      connection = (HttpURLConnection) url.openConnection();
      if (params[0].equals(MainActivity.ABRPTRANSMITTER_RELEASE_URL)) {
        mAbrpTransmitterReleaseLoaderDelegate.setProgressDialogMessage(R.string.please_wait_loading_releaes);
        connection.connect();
        InputStream stream = connection.getInputStream();
        reader = new BufferedReader(new InputStreamReader(stream));
        StringBuffer buffer = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
          buffer.append(line);

        }
        return buffer.toString();
      } else {
        mAbrpTransmitterReleaseLoaderDelegate.setProgressDialogMessage(R.string.please_wait_downloading);
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

          InputStream stream = connection.getInputStream();

          File outDir = mContext.getExternalFilesDir(null);
          File outFile = new File(outDir, MainActivity.APK_NAME);
          FileOutputStream fileOutputStream = new FileOutputStream(outFile);
          byte[] buffer = new byte[1024];
          int bufferLength;

          while ((bufferLength = stream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, bufferLength);
          }
          fileOutputStream.close();

          return outFile.getAbsolutePath();
        }
      }
    } catch (IOException e) {
      Log.e(MainActivity.TAG, e.getMessage(), e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        Log.e(MainActivity.TAG, e.getMessage(), e);
      }
    }
    return null;
  }

  @Override
  protected void onPostExecute(String result) {
    super.onPostExecute(result);
    if (result.startsWith("[") && result.endsWith("]")) {
      try {
        JSONArray jsonArray = new JSONArray(result);
        LinkedHashMap<String, String> releases = new LinkedHashMap<>(jsonArray.length() + 1, 1.0f);
        releases.put(mContext.getString(R.string.choose_release), "");
        String[] spinnerContent = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject jsonObject = jsonArray.getJSONObject(i);
          String name = jsonObject.getString("name");
          String url = jsonObject.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
          releases.put(name, url);
          spinnerContent[i] = name;
        }
        mAbrpTransmitterReleaseLoaderDelegate.releasesLoadComplete(releases);
      } catch (JSONException e) {
        Log.e(MainActivity.TAG, "error getting github releases", e);
      }
    } else {
      mAbrpTransmitterReleaseLoaderDelegate.downloadComplete(result);
    }
  }

  public interface AbrpTransmitterReleaseLoaderDelegate {
    void releasesLoadComplete(LinkedHashMap<String, String> result);

    void downloadComplete(String result);

    void setProgressDialogMessage(int id);
  }
}
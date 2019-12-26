package g4rb4g3.at.abrptransmittercompanion;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "ABRPTransmitter";
  public static final String ABETTERROUTEPLANNER_URL_TOKEN = "token";
  public static final String ABRPTRANSMITTER_RELEASE_URL = "https://api.github.com/repos/g4rb4g3/abrptransmitter/releases";
  public static final String APK_NAME = "app-debug.apk";

  private static final int EXCHANGE_PORT = 6942;

  private AbrpTransmitterServer mAbrpTransmitterServer = null;

  private ProgressDialog mProgressDialog;
  private Spinner mSpReleases;
  private EditText mEdCompanionIp;
  private LinkedHashMap<String, String> mReleases;
  private String mApkPath = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final Activity activity = this;
    final Context context = getApplicationContext();
    mEdCompanionIp = findViewById(R.id.ed_companion_ip);
    final EditText edToken = findViewById(R.id.ed_abrp_token);
    mSpReleases = findViewById(R.id.sp_releases);
    mSpReleases.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String url = mReleases.get(mSpReleases.getSelectedItem().toString());
        if("".equals(url)) {
          return;
        }
        new AbrpTransmitterReleaseLoader().execute(url);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    Button btSend = findViewById(R.id.btn_send);
    btSend.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          if (edToken.getText().length() == 0) {
            Toast.makeText(context, R.string.token_empty, Toast.LENGTH_LONG).show();
            return;
          }
          final JSONObject jsonObject = new JSONObject();
          jsonObject.put(ABETTERROUTEPLANNER_URL_TOKEN, edToken.getText().toString());

          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                Socket socket = new Socket(mEdCompanionIp.getText().toString(), EXCHANGE_PORT);
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(jsonObject.toString());

                out.close();
                socket.close();
              } catch (final Exception e) {
                Log.e(TAG, "error sending data to server", e);
                activity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                  }
                });
              }
            }
          }).start();
        } catch (JSONException e) {
          Log.e(TAG, e.getMessage(), e);
        }
      }
    });

    new AbrpTransmitterReleaseLoader().execute(ABRPTRANSMITTER_RELEASE_URL);
  }

  private void executeRemoteAdb() {
    try {
      Socket socket = new Socket(mEdCompanionIp.getText().toString(), 5555);

      AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
        @Override
        public String encodeToString(byte[] data) {
          return Base64.encodeToString(data, Base64.NO_CLOSE);
        }
      });

      AdbConnection connection = AdbConnection.create(socket, crypto);
      connection.connect();

      AdbStream stream = connection.open("shell:logcat");
    } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(mAbrpTransmitterServer != null) {
      mAbrpTransmitterServer.stop();
    }
  }

  private class AbrpTransmitterReleaseLoader extends AsyncTask<String, String, String> {

    protected void onPreExecute() {
      super.onPreExecute();

      mProgressDialog = new ProgressDialog(MainActivity.this);
      mProgressDialog.setMessage(getString(R.string.please_wait_loading_releaes));
      mProgressDialog.setCancelable(false);
      mProgressDialog.show();
    }

    protected String doInBackground(String... params) {
      HttpURLConnection connection = null;
      BufferedReader reader = null;

      try {
        URL url = new URL(params[0]);
        connection = (HttpURLConnection) url.openConnection();
        if (params[0].equals(ABRPTRANSMITTER_RELEASE_URL)) {
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
          mProgressDialog.setMessage(getString(R.string.please_wait_downloading) + " " + mSpReleases.getSelectedItem());
          connection.connect();

          if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

            InputStream stream = connection.getInputStream();

            File outDir = getExternalFilesDir(null);
            File outFile = new File(outDir, APK_NAME);
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
        Log.e(TAG, e.getMessage(), e);
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
        try {
          if (reader != null) {
            reader.close();
          }
        } catch (IOException e) {
          Log.e(TAG, e.getMessage(), e);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if(result.startsWith("[") && result.endsWith("]")) {
        try {
          JSONArray jsonArray = new JSONArray(result);
          mReleases = new LinkedHashMap<>(jsonArray.length()+1, 1.0f);
          mReleases.put(getString(R.string.choose_release), "");
          String[] spinnerContent = new String[jsonArray.length()];
          for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String name = jsonObject.getString("name");
            String url = jsonObject.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
            mReleases.put(name, url);
            spinnerContent[i] = name;
          }
          ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, mReleases.keySet().toArray(new String[mReleases.keySet().size()]));
          mSpReleases.setAdapter(adapter);
        } catch (JSONException e) {
          Log.e(TAG, "error getting github releases", e);
        }
      } else {
        mApkPath = result;
        mAbrpTransmitterServer = new AbrpTransmitterServer(8080, result);
        try {
          mAbrpTransmitterServer.start();
        } catch (IOException e) {
          Log.e(TAG, "error starting server", e);
        }
      }
      if (mProgressDialog.isShowing()) {
        mProgressDialog.dismiss();
      }
    }
  }
}
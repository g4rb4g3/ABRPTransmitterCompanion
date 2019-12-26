package g4rb4g3.at.abrptransmittercompanion;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity implements AbrpTransmitterReleaseLoader.AbrpTransmitterReleaseLoaderDelegate, RemoteAdbConnection.RemoteAdbConnectionDelegate {

  public static final String TAG = "ABRPTransmitter";
  public static final String ABETTERROUTEPLANNER_URL_TOKEN = "token";
  public static final String ABRPTRANSMITTER_RELEASE_URL = "https://api.github.com/repos/g4rb4g3/abrptransmitter/releases";
  public static final String APK_NAME = "app-debug.apk";

  private static final int EXCHANGE_PORT = 6942;

  private Spinner mSpReleases;
  private EditText mEdCompanionIp, mEdToken;
  private LinkedHashMap<String, String> mReleases = new LinkedHashMap<>();
  private ProgressDialog mProgressDialog;
  private AbrpTransmitterServer mAbrpTransmitterServer;

  @Override
  public void releasesLoadComplete(LinkedHashMap<String, String> result) {
    mReleases = result;
    ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, mReleases.keySet().toArray(new String[mReleases.keySet().size()]));
    mSpReleases.setAdapter(adapter);
    if (mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
  }

  @Override
  public void downloadComplete(String result) {
    if (mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
    String ip = getIp(getApplicationContext());
    if (ip == null) {
      return;
    }
    mAbrpTransmitterServer = new AbrpTransmitterServer(8080, result);
    try {
      mAbrpTransmitterServer.start();
      RemoteAdbConnection remoteAdbConnection = new RemoteAdbConnection(ip, MainActivity.this);
      new Thread(remoteAdbConnection).start();
    } catch (IOException e) {
      Log.e(MainActivity.TAG, "error starting server", e);
    }
  }

  @Override
  public void setProgressDialogMessage(final int id) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String msg = getString(id);
        if (id == R.string.please_wait_downloading) {
          msg += " " + mSpReleases.getSelectedItem().toString();
        }
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
      }
    });
  }

  @Override
  public void completed() {
    if(mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
    mAbrpTransmitterServer.stop();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mProgressDialog = new ProgressDialog(this);
    mProgressDialog.setCancelable(false);

    final Activity activity = this;
    final Context context = getApplicationContext();
    mEdCompanionIp = findViewById(R.id.ed_companion_ip);
    mEdToken = findViewById(R.id.ed_abrp_token);
    mSpReleases = findViewById(R.id.sp_releases);

    Button btSend = findViewById(R.id.btn_send);
    btSend.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          final String ip = getIp(context);
          if (ip == null) {
            return;
          }
          final String token = getToken(context);
          if (token == null) {
            return;
          }

          final JSONObject jsonObject = new JSONObject();
          jsonObject.put(ABETTERROUTEPLANNER_URL_TOKEN, token);

          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                Socket socket = new Socket(ip, EXCHANGE_PORT);
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

    Button btnInstall = findViewById(R.id.btn_install);
    btnInstall.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String ip = getIp(context);
        if (ip == null) {
          return;
        }
        String url = mReleases.get(mSpReleases.getSelectedItem().toString());
        if ("".equals(url)) {
          Toast.makeText(getApplicationContext(), getString(R.string.choose_release_first), Toast.LENGTH_LONG).show();
          return;
        }
        new AbrpTransmitterReleaseLoader(context, MainActivity.this).execute(url);
      }
    });
    new AbrpTransmitterReleaseLoader(context, MainActivity.this).execute(ABRPTRANSMITTER_RELEASE_URL);
  }

  private String getIp(Context context) {
    String ip = mEdCompanionIp.getText().toString();
    if (ip.length() == 0) {
      Toast.makeText(context, R.string.companion_ip_empty, Toast.LENGTH_LONG).show();
      return null;
    }
    return ip;
  }

  private String getToken(Context context) {
    String token = mEdToken.getText().toString();
    if (token.length() == 0) {
      Toast.makeText(context, R.string.token_empty, Toast.LENGTH_LONG).show();
      return null;
    }
    return token;
  }
}
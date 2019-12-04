package g4rb4g3.at.abrptransmittercompanion;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "ABRPTransmitter";
  public static final String ABETTERROUTEPLANNER_URL_TOKEN = "token";

  private static final int EXCHANGE_PORT = 6942;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final Activity activity = this;
    final Context context = getApplicationContext();
    final EditText edCompanionIp = findViewById(R.id.ed_companion_ip);
    final EditText edToken = findViewById(R.id.ed_abrp_token);
    Button btSend = findViewById(R.id.btn_send);
    btSend.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          if(edToken.getText().length() == 0) {
            Toast.makeText(context, R.string.token_empty, Toast.LENGTH_LONG).show();
            return;
          }
          final JSONObject jsonObject = new JSONObject();
          jsonObject.put(ABETTERROUTEPLANNER_URL_TOKEN, edToken.getText().toString());

          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
              Socket socket = new Socket(edCompanionIp.getText().toString(), EXCHANGE_PORT);
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
  }
}

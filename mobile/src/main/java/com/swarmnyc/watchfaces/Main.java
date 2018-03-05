package com.swarmnyc.watchfaces;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;


public class Main extends Activity {




    private static final String TAG = "swarm";
    public static final String PATH_CONFIG = "/WeatherWatchFace/Config/";



    ResultCallback<DataApi.DataItemResult> getDataCallback = new ResultCallback<DataApi.DataItemResult>() {
        @Override
        public void onResult(DataApi.DataItemResult result) {
            if (result.getStatus().isSuccess() && result.getDataItem() != null) {
                DataMap item = DataMapItem.fromDataItem(result.getDataItem()).getDataMap();

            }


            alreadyInitialize = true;




        }
    };



    private GoogleApiClient mGoogleApiClient;


    private String mPeerId;

    TextView temp;
    TextView icon;
    TextView weather;

    String mytemp;
    String myicon;
    String myweather;

    Button mManualUpdateButton;




    private boolean alreadyInitialize;

// -------------------------- OTHER METHODS --------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();



        mManualUpdateButton = (Button)findViewById(R.id.refreshweather);

        mManualUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main.this, WeatherService.class);
                intent.setAction(Main.class.getSimpleName());
                intent.putExtra("PeerId",mPeerId);
                startService(intent);
                getConfig();
                Toast.makeText(Main.this, "Refresh Succeeded!", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!alreadyInitialize) {

            Uri uri = new Uri.Builder()
                    .scheme("wear")
                    .path(PATH_CONFIG)
                    .authority(mPeerId)
                    .build();

            Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                    .setResultCallback(getDataCallback);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }





    private void sendConfigUpdateMessage(DataMap config) {
        if (mPeerId != null && alreadyInitialize) {
            Log.d(TAG, "Sending Config: " + config);
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_CONFIG, config.toByteArray())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "Send Config Result: " + sendMessageResult.getStatus());
                        }
                    });
        }
    }


    protected void fetchConfig(DataMap config) {

        if (config.containsKey(Settings.KEY_TEMP)) {
            String getweather = config.getString(Settings.KEY_TEMP);
            if (TextUtils.isEmpty(getweather)) {
                mytemp = "103";
            } else {
                mytemp = getweather;
            }

        }

        if (config.containsKey(Settings.KEY_ICON)) {
            String geticon = config.getString(Settings.KEY_ICON);
            if (TextUtils.isEmpty(geticon)) {
                myicon = "unknown";
            } else {
                myicon = geticon;
            }

        }

        if (config.containsKey(Settings.KEY_WEATHER)) {
            String getweather = config.getString(Settings.KEY_WEATHER);
            if (TextUtils.isEmpty(getweather)) {
                myweather = "unknown";
            } else {
                myweather = getweather;
            }
        }



        Log.i(TAG, "mytemp: " + mytemp);
        Log.i(TAG, "myweather: " + myweather);
        Log.i(TAG, "myicon: " + myicon);
        temp.setText(mytemp);
        icon.setText(myicon);
        weather.setText(myweather);


    }

    protected void getConfig() {
        Log.d(TAG, "Start getting Config");
        Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                Uri uri = new Uri.Builder()
                        .scheme("wear")
                        .path(Settings.PATH_CONFIG)
                        .authority(getLocalNodeResult.getNode().getId())
                        .build();

                getConfig(uri);

            }
        });
    }

    protected void getConfig(Uri uri) {

        Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                .setResultCallback(
                        new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.d(TAG, "Finish Config: " + dataItemResult.getStatus());
                                if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                    fetchConfig(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                }
                            }
                        }
                );
    }


}

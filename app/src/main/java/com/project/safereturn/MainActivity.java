package com.project.safereturn;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView textViewAddress;
    private ImageView buttonStart;
    private ImageView buttonSetting;

    public static double latitude = 0;
    public static double longitude = 0;

    private LocationManager locationManager;

    //해당 인터페이스 자체가 위치 정보와 관련되어있습니다.
    private LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) { //해당 부분이 위치가 변경됨을 감지했을 경우 계속해서 실행되는 메소드 이구요.

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    String latlng = String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude());
            //Log.d("main", latlng);
            getAddress(latlng);
        }

        /*
        아래 3개는 크게 상관없는 메소드 입니다.
        제공자(Provider라는 것이 Enable되거나 Disable 될때 실행되는것이기 때문에요
         */
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) { //1번

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) { //2번
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //아래 조건문은 Android 6.0인 마시멜로우 이상 버전부턴 앱에서 사용될 특정 부분에 대해 권한 요청을 하게됩니다.
        //아이폰의 경우도 알림이나 사진 접근 권한을 따로 사용자에게 받게 되어있죠
        //앱에서 사용될 총 5개에 대해 권한 승인이 되어있는지 확인하고 되어있지 않다면 승인 팝업창을 띄우는 코드입니다.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,  Manifest.permission.SEND_SMS}, 0);
        }
        else {
            initLocationManager();
        }

        textViewAddress = (TextView) findViewById(R.id.textView_address);
        buttonStart = (ImageView) findViewById(R.id.button_start);
        buttonSetting = (ImageView) findViewById(R.id.button_setting);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DetectActivity.class));
            }
        });

        buttonSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SettingActivity.class));
            }
        });

        findViewById(R.id.button_update_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String latlng = String.valueOf(latitude) + "," + String.valueOf(longitude);
                getAddress(latlng);
            }
        });
    }

    private void initLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //이 부분입니다. 현재 최소 시간 0 최소 거리가 0이기 때문에 그냥 무한정 현재 위치를 가져옵니다.
        /*
        minDistance 가 10이다 하면 10m가 이동했음을 감지했을 경우 위치를 가져오게 되구요.
         */
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /*
    GET에 대한 파라미터로 위도 경도 API키를 넘겨줘야 하기 때문에 아래와 같이 되어있구요.
     */
    private void getAddress(String latlng) { //3
        //아래는 Retrofit이라는 웹 요청 프로토콜 사용을 편하게 해주는 라이브러리 입니다.
        //3개의 총 변수를 보내고 받은 응답값을 처리하는곳이 onResponse입니다.

        Call<JsonObject> call = MyApplication.http.getAddress(latlng, "AIzaSyCJZC2Zk_Kvh2zBeF_rHr4SuY-708kr804", "ko");
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if(response.body() != null) {
                    /*
                    응답값은 json 형태로 오게되구요. 실직적으로 필요한 주소 데이터는
                    results 키에 대한 jsonarray 배열 내부의 formatted_address 키로 가지고 있습니다.
                     */
                    JsonObject jsonObject = response.body();

                    Log.e("request json data", jsonObject.toString());

                    //아래 코드들이 받은 json데이터를 나누는 과정이구요.
                    JsonArray jsonArray =  jsonObject.get("results").getAsJsonArray();

                    if(jsonArray.size() > 0) {
                        JsonObject object = jsonArray.get(0).getAsJsonObject(); //배열의 첫번째 값을 object형태로 가져와 해당 object내의 formatted_address 키에 대한 value를 가져옵니다.
                        String address = object.get("formatted_address").getAsString();

                        textViewAddress.setText(address);
                    }
                }
                else {
                    Log.e("ad", "Adsf");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    /*
    연결로 해당 메소드가 승인 팝업창이 뜬 후 처리를 하구요.

     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//4
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                /*
                승인 팝업창을 띄웠고 사용자가 모두 승인을 한 경우 위치관련 코드를 실행합니다.
                 */
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                    initLocationManager();
                }
                else {
                    //승인을 하나라도 하지 않은 경우
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }
}

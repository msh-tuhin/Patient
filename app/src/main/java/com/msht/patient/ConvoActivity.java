package com.msht.patient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ConvoActivity extends FragmentActivity {

    final int CAMERA_MIC_PERMISSION_REQUEST = 1;
    public TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convo);

        tv = findViewById(R.id.msg);

        if(!checkPermissionForCameraAndMicrophone()){
            requestPermissionForCameraAndMicrophone();
        }else{
            attachFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==CAMERA_MIC_PERMISSION_REQUEST){
            boolean result = true;
            for(int grantResult : grantResults){
                result &= grantResult== PackageManager.PERMISSION_GRANTED;
            }
            if(result){
                attachFragment();
            }else{
                Toast.makeText(this, "Permissions Denied!!!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                CAMERA_MIC_PERMISSION_REQUEST);
    }

    private void attachFragment(){
        RoomFragment roomFragment = new RoomFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.parent_layout, roomFragment);
        fragmentTransaction.commit();
    }
}

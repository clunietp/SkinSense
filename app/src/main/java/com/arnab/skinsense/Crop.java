package com.arnab.skinsense;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;

import com.lyft.android.scissors.CropView;

import java.io.FileInputStream;

public class Crop extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;

    CropView cropView;
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_crop);
        FloatingActionButton crop=findViewById(R.id.cropButton);
        Intent intent=getIntent();
        //String filename = getIntent().getStringExtra("bitmap");
        bitmap= BitmapHelper.getInstance().getBitmap();
        cropView=findViewById(R.id.crop_view);
        cropView.setImageBitmap(bitmap);
        crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap croppedBitmap=cropView.crop();
                croppedBitmap=Bitmap.createScaledBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE, false);
                Intent intent1=new Intent();
                intent1.putExtra("croppedBitmap",croppedBitmap);
                setResult(2,intent1);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Bitmap croppedBitmap=cropView.crop();
        croppedBitmap=Bitmap.createScaledBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE, false);
        Intent intent1=new Intent();
        intent1.putExtra("croppedBitmap",croppedBitmap);
        setResult(2,intent1);
        finish();
        super.onBackPressed();
    }
}

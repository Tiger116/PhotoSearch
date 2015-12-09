package com.zhuravlev.vitaliy.photosearch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int GET_IMAGE_REQUEST_CODE = 1;

    private GoogleGoggles goggles;
    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goggles = new GoogleGoggles(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageView = (ImageView) findViewById(R.id.main_imageview);
        textView = (TextView) findViewById(R.id.main_textview);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, GET_IMAGE_REQUEST_CODE);
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    goggles.validateCSSID(goggles.getCssid());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_IMAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        //get selected image
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) {
                            inputStream.close();
                        }

                        //set original size image to imageView
                        imageView.setImageBitmap(bitmap);

                        //reducing the size of image
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                        //current image size
                        int arraySize = outputStream.toByteArray().length;
                        //required image size
                        int reqSize = 1024 * 140;
                        //size divider
                        int inSampleSize = 2;

                        while (arraySize > reqSize) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = inSampleSize;
                            options.inPreferredConfig = Bitmap.Config.RGB_565;

                            inputStream = getContentResolver().openInputStream(data.getData());
                            outputStream.reset();

                            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            arraySize = outputStream.toByteArray().length;
                            inSampleSize += 2;
                        }

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        final byte[] array = outputStream.toByteArray();

                        //send image in thread
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    goggles.sendPhoto(goggles.getCssid(), array);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        thread.start();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //set response message to textview
    public void setResponseText(String text) {
        if (text != null)
            this.textView.setText(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

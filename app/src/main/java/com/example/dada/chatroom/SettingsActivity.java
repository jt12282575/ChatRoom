package com.example.dada.chatroom;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mUserRef;

    private CircleImageView mDisplayImage;
    private TextView mName;
    private TextView mStatus;
    private Button mSetStatusBtn;
    private Button mImageBtn;

    private static final int GALLERY_PCIK = 1;

    //Storage Firebase
    private StorageReference mImageStorage;
    private ProgressDialog mProgressDialog;
    private Bitmap thumb_bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        mDisplayImage = (CircleImageView) findViewById(R.id.settings_image);
        mName = (TextView) findViewById(R.id.settings_display_name);
        mStatus = (TextView) findViewById(R.id.settings_status);
        mDisplayImage = (CircleImageView) findViewById(R.id.settings_image);
        mSetStatusBtn = (Button)findViewById(R.id.settings_status_btn);
        mImageBtn = (Button) findViewById(R.id.settings_image_btn);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid();
        mUserRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(mCurrentUser.getUid());

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumb_images = dataSnapshot.child("thumb_image").getValue().toString();

                mName.setText(name);
                mStatus.setText(status);

                if (!image.equals("default")) {
//                    Picasso.with(SettingsActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mDisplayImage);
                    Picasso.with(SettingsActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.default_avatar).into(mDisplayImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            Picasso.with(SettingsActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mDisplayImage);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSetStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status_value = mStatus.getText().toString();
                Intent intent = new Intent(SettingsActivity.this,StatusActivity.class);
                intent.putExtra("status_value",status_value);
                startActivity(intent);

            }
        });

        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallaryIntent = new Intent();
                gallaryIntent.setType("image/*");
                gallaryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(gallaryIntent,"SELECT IMAGE"),GALLERY_PCIK);

                /*CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SettingsActivity.this);*/
            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode, data);

        if(requestCode==GALLERY_PCIK && resultCode == RESULT_OK ){
            Uri imageUri = data.getData();
            CropImage.activity()
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data); // result 是 剪裁 sdk 的結果
            if(resultCode == RESULT_OK){
                mProgressDialog = new ProgressDialog(SettingsActivity.this);
                mProgressDialog.setTitle("正在上傳圖片");
                mProgressDialog.setMessage("請等待我們完成上傳圖片");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.show();


                Uri resultUri = result.getUri();

                File thumb_filePath = new File(resultUri.getPath());

                String current_userID = mCurrentUser.getUid();

                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filePath);


                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos =  new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
                final byte[] thumb_byte = baos.toByteArray();


                StorageReference filepath = mImageStorage.child("profile_images").child(current_userID+".jpg");
                final StorageReference thumb_filepath = mImageStorage.child("profile_images").child("thumbs").child(current_userID+".jpg");

                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SettingsActivity.this,"成功運作",Toast.LENGTH_LONG).show();
                            final String download_url = task.getResult().getDownloadUrl().toString();

                            UploadTask uploadTask = thumb_filepath.putBytes(thumb_byte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {
                                    String thumb_downloadUrl = thumb_task.getResult().getDownloadUrl().toString();

                                    if(thumb_task.isSuccessful()){
                                        Map update_hashMap = new HashMap();
                                        update_hashMap.put("image",download_url);
                                        update_hashMap.put("thumb_image",thumb_downloadUrl);
                                        mUserDatabase.updateChildren(update_hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    mProgressDialog.dismiss();
                                                    Toast.makeText(SettingsActivity.this,"成功更新資料庫",Toast.LENGTH_LONG).show();
                                                }else{

                                                    Toast.makeText(SettingsActivity.this,"遇到錯誤",Toast.LENGTH_LONG).show();
                                                    mProgressDialog.dismiss();

                                                }
                                            }
                                        });






                                    }
                                }
                            });


                        }else{
                            Toast.makeText(SettingsActivity.this,"遇到錯誤",Toast.LENGTH_LONG).show();
                            mProgressDialog.dismiss();
                        }
                    }
                });

            }else if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error = result.getError();
            }


        }


    }
    @Override
    protected void onStart() {
        super.onStart();

        mUserRef.child("online").setValue("true");


    }
    @Override
    protected void onStop() {
        super.onStop();

        mUserRef.child("online").setValue(ServerValue.TIMESTAMP);

    }
    public static String random(){
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength ; i++) {
            tempChar = (char) (generator.nextInt(96)+32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

}

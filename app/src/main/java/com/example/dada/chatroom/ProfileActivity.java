package com.example.dada.chatroom;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private CircleImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileTotalFriends;
    private Button mProfileSendReq;
    private Button mProfileDeclineReq;

    private DatabaseReference mUserDatabase;

    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;

    private FirebaseUser mCurrent_user;

    private ProgressDialog mProgressDialog;
    private int mCurrent_state;
    final private int not_friends = 0;
    final private int req_sent = 1;
    final private int req_receive = 2;
    final private int friends = 3;

    private DatabaseReference mUserRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        mProfileImage = (CircleImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_displayName);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileTotalFriends = (TextView) findViewById(R.id.profile_totalFriends);
        mProfileSendReq = (Button) findViewById(R.id.profile_send_req_btn);
        mProfileDeclineReq = (Button) findViewById(R.id.profile_decline_req_btn);

        mCurrent_state = not_friends;





        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(mCurrent_user.getUid());



        final String user_ID = getIntent().getStringExtra("user_id");
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("讀取資料中");
        mProgressDialog.setMessage("請稍待片刻");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();


        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_ID);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friend");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef = FirebaseDatabase.getInstance().getReference();

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                String display_name = dataSnapshot.child("name").getValue().toString();
                String display_status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(display_status);
                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mProfileImage);

                //-----------Friend List, Request Feature----------   朋友請求表格中找的到的
                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(user_ID)){
                            String req_type = dataSnapshot.child(user_ID).child("request_type").getValue().toString();
                            if(req_type.equals("received")){
                                mCurrent_state = req_receive;
                                mProfileSendReq.setText("接受朋友請求");

                                mProfileDeclineReq.setVisibility(View.VISIBLE); // 收到別人的好友邀請 才給他拒絕選項
                                mProfileDeclineReq.setEnabled(true);

                            }else if(req_type.equals("sent")) {
                                mCurrent_state = req_sent;
                                mProfileSendReq.setText("取消邀請");

                                mProfileDeclineReq.setVisibility(View.INVISIBLE); // 如果是發送邀請方 就不讓他看到拒絕邀請的按鈕
                                mProfileDeclineReq.setEnabled(false);


                            }
                            mProgressDialog.dismiss();

        // 朋友請求資料表格中找不到的
                        }else{
                            // 接著看朋友資料表格中找不找的到
                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_ID)){ // 已經是好友
                                        mCurrent_state = friends;
                                        mProfileSendReq.setText("取消你們友誼");
                                        mProgressDialog.dismiss();
                                        mProfileDeclineReq.setVisibility(View.INVISIBLE); // 如果已經是朋友了 就不讓他看到拒絕邀請的按鈕
                                        mProfileDeclineReq.setEnabled(false);

                                    }else{ // 還不是朋友
                                        mProgressDialog.dismiss();
                                        mProfileDeclineReq.setVisibility(View.INVISIBLE); // 如果是發送邀請方 就不讓他看到拒絕邀請的按鈕
                                        mProfileDeclineReq.setEnabled(false);
                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    mProgressDialog.dismiss();
                                }
                            });


                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });



            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProfileSendReq.setEnabled(false);

                //-------------Not friend state------------
                if(mCurrent_state==not_friends){//原本非好友，按了寄request
                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_ID).push();
                    String newNotfificationId = newNotificationref.getKey();

                    HashMap<String,String> notificationData = new HashMap<>();
                    notificationData.put("from",mCurrent_user.getUid());
                    notificationData.put("type","request");

                    Map requestMap  = new HashMap<>();
                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" +user_ID+ "/" +"request_type","sent");
                    requestMap.put("Friend_req/"  +user_ID+ "/" + mCurrent_user.getUid() + "/" +"request_type","received");
                    requestMap.put("notifications/" + user_ID + "/" + newNotfificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null){

                                Toast.makeText(ProfileActivity.this, "request 寄送出現錯誤",Toast.LENGTH_SHORT).show();


                            }

                            mCurrent_state = req_sent;
                            mProfileSendReq.setText("取消好友邀請");

                            mProfileDeclineReq.setVisibility(View.INVISIBLE); // 接受對方成為好友之後 就不讓他看到拒絕邀請的按鈕
                            mProfileDeclineReq.setEnabled(false);
                            mProfileSendReq.setEnabled(true);//讀取時不讓使用者再按一次






                        }
                    });


                }

                //-------------Not friend state------------
                if(mCurrent_state==req_sent){//原本已經寄了request，現在要把request 取消
                    mProfileSendReq.setEnabled(false);
                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_ID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendReqDatabase.child(user_ID).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendReq.setEnabled(true);//讀取時不讓使用者再按一次
                                    mCurrent_state = not_friends;
                                    mProfileSendReq.setText("寄送朋友邀請");




                                }
                            });
                        }
                    });

                }

                if(mCurrent_state==req_receive){//收到邀請，按下後成為好友
                    final String current_Date = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friend/" + mCurrent_user.getUid() + "/" + user_ID + "/date",current_Date);// 好友在彼此底下留下時間戳記紀錄
                    friendsMap.put("Friend/" + user_ID + "/" + mCurrent_user.getUid() + "/date",current_Date);

                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_ID, null);// 將在 request 資料庫中資料清掉
                    friendsMap.put("Friend_req/" + user_ID + "/" + mCurrent_user.getUid(), null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError == null){ // 如果成功加入好友
                                mProfileSendReq.setEnabled(true);
                                mCurrent_state = friends;
                                mProfileSendReq.setText("解除好友關係");

                                mProfileDeclineReq.setVisibility(View.INVISIBLE); // 接受對方成為好友之後 就不讓他看到拒絕邀請的按鈕
                                mProfileDeclineReq.setEnabled(false);



                            }else{
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();


                            }





                        }
                    });


                }


                //------------ 取消好友 unfriend ---------------
                if(mCurrent_state == friends){// 代表兩人已經是好友關係
                    Map unfriendsMap = new HashMap();
                    unfriendsMap.put("Friends/" + mCurrent_user.getUid()+ "/" + user_ID, null);
                    unfriendsMap.put("Friends/" + user_ID+ "/" + mCurrent_user.getUid(), null);


                    mRootRef.updateChildren(unfriendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError == null){ // 如果成功加入好友

                                mCurrent_state = not_friends;
                                mProfileSendReq.setText("寄出好友邀請");

                                mProfileDeclineReq.setVisibility(View.INVISIBLE); // 接受對方成為好友之後 就不讓他看到拒絕邀請的按鈕
                                mProfileDeclineReq.setEnabled(false);



                            }else{
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();


                            }

                            mProfileSendReq.setEnabled(true);



                        }
                    });





                }

            }
        });




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
}

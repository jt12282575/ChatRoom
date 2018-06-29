package com.example.dada.chatroom;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.net.Uri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int GALLERY_PICK = 1;
    public String mChatUser;
    private Toolbar mChatToolbar;

    private DatabaseReference mRootRef;
    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;

    private ImageButton mChatAddBtn;
    private EditText mChatMesageView;
    private ImageButton mChatSendBtn;

    private RecyclerView mMessageList;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;


    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    private StorageReference mImageStorage;

    //New solution
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        mChatToolbar = (Toolbar)findViewById(R.id.chat_app_bar);

        setSupportActionBar(mChatToolbar);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mChatUser = getIntent().getStringExtra("user_id");
        String userName = getIntent().getStringExtra("user_name");
        getSupportActionBar().setTitle(userName);

        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        //--custom action bar items--

        mTitleView = (TextView)findViewById(R.id.custom_bar_title);
        mLastSeenView = (TextView)findViewById(R.id.custom_bar_seen);
        mProfileImage = (CircleImageView)findViewById(R.id.custom_bar_profile);

        mChatAddBtn = (ImageButton)findViewById(R.id.chat_add_btn);
        mChatMesageView = (EditText) findViewById(R.id.chat_message_view);
        mChatSendBtn = (ImageButton)findViewById(R.id.chat_send_btn );

        mAdapter = new MessageAdapter(messagesList);

        mMessageList = (RecyclerView)findViewById(R.id.messages_list);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);

        mLinearLayout = new LinearLayoutManager(this);

        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
        mMessageList.setAdapter(mAdapter);
        
        loadMessage();



        mTitleView.setText(userName);

        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String image = dataSnapshot.child("image").getValue().toString();
                if(dataSnapshot.hasChild("online")) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    if (online.equals("true")) {
                        mLastSeenView.setText("目前在線上");
                    } else {
                        GetTimeAgo getTimeAgo = new GetTimeAgo();

                        long lastTime = Long.parseLong(online);

                        String lastSeenTime = getTimeAgo.getTimeAgo(lastTime, getApplicationContext());

                        mLastSeenView.setText(lastSeenTime);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootRef.child("chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {// 讓對方知道自己的訊息已經被閱讀了
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(mChatUser)){
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen",false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/"+mCurrentUserId+"/"+mChatUser,chatAddMap);
                    chatUserMap.put("Chat/"+mChatUser+"/"+mCurrentUserId,chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null){
                                Log.d("CHAT_LOG",databaseError.getMessage().toString());

                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallaryIntent = new Intent();
                gallaryIntent.setType("image/*");
                gallaryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(gallaryIntent,"SELECT IMAGE"), GALLERY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage ++;

                itemPos = 0;

//                messagesList.clear();

                loadMoreMessages();
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){

            Uri imageUrl = data.getData();

            final String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
            final String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages")
                    .child(mCurrentUserId).child(mChatUser).push();

            final String push_id = user_message_push.getKey();

            StorageReference filepath = mImageStorage.child("message_images").child(push_id+".jpg");

            filepath.putFile(imageUrl).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()){
                        String download_url = task.getResult().getDownloadUrl().toString();


                        Map messageMap = new HashMap();
                        messageMap.put("message",download_url);
                        messageMap.put("seen",false);
                        messageMap.put("type","image");
                        messageMap.put("time",ServerValue.TIMESTAMP);
                        messageMap.put("from",mCurrentUserId);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref+"/"+push_id,messageMap);
                        messageUserMap.put(chat_user_ref+"/"+push_id,messageMap);

                        mChatMesageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError!= null){
                                    Log.d("CHAT_LOG",databaseError.getMessage().toString());

                                }


                            }
                        });



                    }



                }
            });




        }

    }

    private void loadMoreMessages() {
        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();


                if(!mPrevKey.equals(messageKey)){
                    messagesList.add(itemPos++,message);

                } else {

                    mPrevKey = mLastKey;




                }


                if(itemPos == 1){

                    mLastKey = messageKey;
                }




                Log.d("totalkey", "Lastkey: "+ mLastKey+"|  PrevKey: "+mPrevKey + " |  MessageKey: "+ messageKey);

                mAdapter.notifyDataSetChanged();

//                mMessageList.scrollToPosition(messagesList.size()-1); // only the first "loadMessages" load to the bottom

                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(10,0);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void loadMessage() {
        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);
        Query messageQuery = messageRef.limitToLast(TOTAL_ITEMS_TO_LOAD * mCurrentPage);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos ++;
                if(itemPos == 1){
                    String messageKey = dataSnapshot.getKey();

                    mLastKey = messageKey;
                    mPrevKey = messageKey;

                    Log.i("ok",mLastKey);// 想知道目前是指到哪一筆的key
                }

                messagesList.add(message);
                mAdapter.notifyDataSetChanged();

                mMessageList.scrollToPosition(messagesList.size()-1);

                mRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



    }

    private void sendMessage() {
        String message = mChatMesageView.getText().toString();
        if(!TextUtils.isEmpty(message)){
            String current_user_ref = "messages/"+mCurrentUserId+"/"+mChatUser;
            String chat_user_ref = "messages/"+mChatUser+"/"+mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages")
                    .child(mCurrentUserId).child(mChatUser).push();
            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message",message);
            messageMap.put("seen",false);
            messageMap.put("type","text");
            messageMap.put("time",ServerValue.TIMESTAMP);
            messageMap.put("from",mCurrentUserId);


            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref+"/"+push_id,messageMap);
            messageUserMap.put(chat_user_ref+"/"+push_id,messageMap);

            mChatMesageView.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Log.d("CHAT_LOG",databaseError.getMessage().toString());

                    }
                }
            });


        }


    }
}

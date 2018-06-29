package com.example.dada.chatroom;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    private View mMainView;



    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_friends, container, false);
        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView) mMainView.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        Log.i("ok","Current User ID: "+mCurrent_user_id);
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friend").child(mCurrent_user_id);
        mFriendsDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.i("ok","onCreateView");



        return mMainView;
    }

    public void onStart() {
        super.onStart();
        Log.i("ok","onStart");

        FirebaseRecyclerAdapter<Friend,FriendsViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friend, FriendsViewHolder>(

                Friend.class,
                R.layout.users_single_layout,
                FriendsViewHolder.class,
                mFriendsDatabase

        ) {
            @Override
            protected void populateViewHolder(final FriendsViewHolder friendsViewHolder, final Friend friend, int position) {
                Log.i("ok","before friend.getDate");
                friendsViewHolder.setDate(friend.getDate());
                Log.i("ok","after friend.getdate");
                final String list_user_id = getRef(position).getKey();
                Log.i("ok","getRefposition()");

                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.i("ok","before dataSnapshot");
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumb_image").getValue().toString();
                        if(dataSnapshot.hasChild("online")) {
                            String userOnline =  dataSnapshot.child("online").getValue().toString();
                            friendsViewHolder.setUserOnline(userOnline);
                        }

                        Log.i("ok","after dataSnapshot");
                        friendsViewHolder.setName(userName);
                        friendsViewHolder.setUserImage(userThumb,getContext());

                        friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence options[] = new CharSequence[]{"個人資料","寄送訊息"};

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select Options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Click event for each item

                                        if(which==0){
                                            Intent profileIntent = new Intent(getContext(),ProfileActivity.class);
                                            profileIntent.putExtra("user_id",list_user_id);
                                            startActivity(profileIntent);
                                        }

                                        if(which==1){
                                            Intent chatIntent = new Intent(getContext(),ChatActivity.class);
                                            chatIntent.putExtra("user_id",list_user_id);
                                            chatIntent.putExtra("user_name",userName);
                                            startActivity(chatIntent);
                                        }


                                    }
                                });

                                builder.show();

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mFriendsList.setAdapter(friendsRecyclerViewAdapter);

    }
    public static class FriendsViewHolder extends RecyclerView.ViewHolder{
        View mView;

        public FriendsViewHolder(View itemView){
            super(itemView);
            Log.i("ok","super(itemView)");

            mView = itemView;

        }
        public void setDate(String date){
            TextView userStatusView = (TextView) mView.findViewById(R.id.user_single_status);
            userStatusView.setText(date);
        }

        public void setName(String name){
            TextView userNameView = (TextView) mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String thumb_image, Context ctx) {
            CircleImageView userImageView = (CircleImageView)mView.findViewById(R.id.user_single_image);
            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_avatar).into(userImageView);
        }

        public void setUserOnline(String online_status){

            ImageView userOnlineView =(ImageView)mView.findViewById(R.id.user_single_online);
            if(online_status.equals("true")){
                userOnlineView.setVisibility(View.VISIBLE);
            }else{
                userOnlineView.setVisibility(View.INVISIBLE);
            }

        }




    }

}

package com.example.dada.chatroom;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment { // 顯示目前收到的好友Request

    private RecyclerView mReqList;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id;
    private View mReqView;



    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        //load request data from Notification

        mReqView = inflater.inflate(R.layout.fragment_request, container, false);

        mReqList = (RecyclerView) mReqView.findViewById(R.id.req_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        Log.i("ok","Current User ID: "+mCurrent_user_id);

        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req").child(mCurrent_user_id);
        mFriendReqDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mReqList.setHasFixedSize(true);
        mReqList.setLayoutManager(new LinearLayoutManager(getContext()));





        return mReqView;
    }

    public void onStart() {
        super.onStart();
//        Query reqQuery = mFriendReqDatabase.child("request_type").equalTo("received"); //only select received request

        FirebaseRecyclerAdapter<Req,ReqViewHolder> reqRecyclerViewAdapter = new FirebaseRecyclerAdapter<Req, ReqViewHolder>(
                Req.class,
                R.layout.req_bar,
                ReqViewHolder.class,
                mFriendReqDatabase//change to Query, only select request_type = "receive"


        ) {
            @Override
            protected void populateViewHolder(final ReqViewHolder reqViewHolder, Req req, int position) {
                final String list_user_id = getRef(position).getKey();
                Log.i("req","list_user_id: "+list_user_id);
                if (req.getRequest_type().equals("received")) {
                    reqViewHolder.mView.setVisibility(View.VISIBLE);
                    reqViewHolder.param.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    reqViewHolder.param.width = LinearLayout.LayoutParams.MATCH_PARENT;

                    mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final String userName = dataSnapshot.child("name").getValue().toString();
                            String userThumb = dataSnapshot.child("thumb_image").getValue().toString();
                            reqViewHolder.setContent(userName+ "向您寄了好友邀請");
                            reqViewHolder.setReqImage(userThumb,getContext());
                            reqViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent profileIntent = new Intent(getContext(),ProfileActivity.class);
                                    profileIntent.putExtra("user_id",list_user_id);
                                    startActivity(profileIntent);
                                }
                            });


                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }else {
                    reqViewHolder.mView.setVisibility(View.GONE);
                    reqViewHolder.param.height = 0;
                    reqViewHolder.param.width = 0;
                }


            }
        };
        mReqList.setAdapter(reqRecyclerViewAdapter);
//        mReqList.addItemDecoration(new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL));
        DividerItemDecoration divider = new DividerItemDecoration(getContext(),DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(getContext(),R.drawable.divideline));
        mReqList.addItemDecoration(divider);





    }

    public static class ReqViewHolder extends RecyclerView.ViewHolder{
        View mView;
        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams)itemView.getLayoutParams();
        public ReqViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

        }

        public void setContent(String Content){
            TextView reqContent = (TextView) mView.findViewById(R.id.req_bar_receivereq);
            reqContent.setText(Content);
        }

        public void setReqImage(String thumb_image, Context ctx) {// done not yet
            CircleImageView reqImageView = (CircleImageView)mView.findViewById(R.id.req_bar_profile);
            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.default_avatar).into(reqImageView);
        }

    }

}

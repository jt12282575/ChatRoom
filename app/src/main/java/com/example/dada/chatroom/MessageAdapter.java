package com.example.dada.chatroom;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by dada on 2018/5/20.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> mMessageList;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabese;

    public MessageAdapter(List<Messages> mMessageList){
        this.mMessageList = mMessageList;

    }

    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int ViewType){
        View v = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.message_single_layout,parent,false);


        return new MessageViewHolder(v);

    }


    public class MessageViewHolder extends RecyclerView.ViewHolder{

        public TextView messageText;
        public CircleImageView profileImage;
        public TextView displayName;
        public ImageView messageImage;


        public MessageViewHolder(View itemView) {
            super(itemView);

            messageText = (TextView)itemView.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView)itemView.findViewById(R.id.message_profile_layout);
            displayName = (TextView) itemView.findViewById(R.id.name_text_layout);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_layout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        mAuth = FirebaseAuth.getInstance();


        String mCurrentUserId = mAuth.getCurrentUser().getUid();



        Messages c = mMessageList.get(position);

        String from_user = c.getFrom();
        String message_type = c.getType();

        mUserDatabese = FirebaseDatabase.getInstance().getReference().child("Users").child(from_user);
        mUserDatabese.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();

                holder.displayName.setText(name);
                Picasso.with(holder.profileImage.getContext()).load(image).placeholder(R.drawable.default_avatar).into(holder.profileImage);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

       /* if(from_user.equals(mCurrentUserId)){
            holder.messageText.setBackgroundColor(Color.parseColor("#ADD8E6"));
            holder.messageText.setTextColor(Color.WHITE);


        }else{
            holder.messageText.setTextColor(Color.WHITE);

        }
        holder.messageText.setText(c.getMessage());*/

       if(message_type.equals("text")){
           holder.messageText.setText(c.getMessage());
           holder.messageImage.setVisibility(View.GONE);


       }else{
           holder.messageText.setVisibility(View.INVISIBLE);
           Picasso.with(holder.messageImage.getContext()).load(c.getMessage()).placeholder(R.drawable.default_avatar).into(holder.messageImage);

       }



    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }
}

package com.example.brushalpha;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private Toolbar ChatToolBar;

    private ImageButton SendMessageButton;
    private ImageButton SendImageFileButton;

    private EditText UserMessageInput;

    private RecyclerView UserMessagesList;
    private final List<Messages> MessagesList = new ArrayList<>();
    private LinearLayoutManager LinearLayoutManager;
    private MessagesAdapter MessageAdapter;

    private String MessageReceiverID;
    private String MessageReceiverName;
    private String MessageSenderID;
    private String MessageSenderName;
    private String saveCurrentDate;
    private String saveCurrentTime;

    private TextView ReceiverName;
    private CircleImageView ReceiverProfileImage;

    private DatabaseReference RootRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat2);

        mAuth = FirebaseAuth.getInstance();
        MessageSenderID = mAuth.getCurrentUser().getUid();

        RootRef = FirebaseDatabase.getInstance().getReference();

        MessageReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        MessageReceiverName = getIntent().getExtras().get("userName").toString();

        InitializeFields();
        DisplayReceiverInfo();
        SendMessageButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                SendMessage();
            }
        });

        FetchMessages();
    }

    private void FetchMessages() {

        RootRef.child("Messages").child(MessageSenderID).child(MessageReceiverID)
                .addChildEventListener(new ChildEventListener(){

                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s){

                        if(dataSnapshot.exists())
                        {
                            Messages messages = dataSnapshot.getValue(Messages.class);
                            MessagesList.add(messages);
                            MessageAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s){

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot){

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s){

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError){

                    }
                });
    }

    private void SendMessage(){

        String MessageText = UserMessageInput.getText().toString();

        if(TextUtils.isEmpty(MessageText))
        {
            Toast.makeText(this, "Please type a message first...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            String message_sender_ref = "Messages/" + MessageSenderID + "/" + MessageReceiverID;
            String message_receiver_ref = "Messages/" + MessageReceiverID + "/" + MessageSenderID;

            DatabaseReference user_message_key = RootRef.child("Messages").child(MessageSenderID).child(MessageReceiverID)
                    .child(MessageReceiverID).push();

            String message_push_id = user_message_key.getKey();

            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
            saveCurrentDate = currentDate.format(calForDate.getTime());
            Calendar calForTime = Calendar.getInstance();
            SimpleDateFormat currentTime = new SimpleDateFormat(("HH:mm"));
            saveCurrentTime = currentTime.format(calForTime.getTime());

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", MessageText);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", MessageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", MessageSenderID);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);
            messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if(task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                        UserMessageInput.setText("");
                    }
                    else
                    {
                        String message = task.getException().getMessage();
                        Toast.makeText(ChatActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        UserMessageInput.setText("");
                    }
                }
            });
        }
    }

    private void DisplayReceiverInfo(){

        ReceiverName.setText(MessageReceiverName);

        RootRef.child("Users").child(MessageReceiverID).addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot){

                if(dataSnapshot.exists())
                {
                    final String profileImage = dataSnapshot.child("Profile Picture").getValue().toString();
                    Picasso.with(ChatActivity.this).load(profileImage).placeholder(R.drawable.profile).into(ReceiverProfileImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError){

            }
        });
    }

    private void InitializeFields (){

        ChatToolBar = (Toolbar) findViewById(R.id.chat_bar_layout);
        setSupportActionBar(ChatToolBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = layoutInflater.inflate(R.layout.chat_custom_bar, null);
        actionBar.setCustomView(action_bar_view);

        ReceiverName = (TextView) findViewById(R.id.custom_profile_name);
        ReceiverProfileImage = (CircleImageView) findViewById(R.id.custom_profile_image);

        SendMessageButton = (ImageButton) findViewById(R.id.send_message_button);
        SendImageFileButton = (ImageButton) findViewById(R.id.send_image_file_button);

        UserMessageInput = (EditText) findViewById(R.id.input_message);

        MessageAdapter = new MessagesAdapter(MessagesList);
        UserMessagesList = (RecyclerView)findViewById(R.id.messages_list_users);
        LinearLayoutManager = new LinearLayoutManager(this);
        UserMessagesList.setHasFixedSize(true);
        UserMessagesList.setLayoutManager(LinearLayoutManager);
        UserMessagesList.setAdapter(MessageAdapter);
    }
}

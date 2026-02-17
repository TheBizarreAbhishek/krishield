package com.krishield.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.krishield.R;
import com.krishield.models.CommunityMessage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CommunityChatActivity extends AppCompatActivity {

    private String communityId;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<CommunityMessage> messageList;
    private EditText etMessage;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "FarmersUnionChat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat);

        communityId = getIntent().getStringExtra("community_id");
        String communityName = getIntent().getStringExtra("community_name");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(communityName);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadMessages();

        recyclerView = findViewById(R.id.recycler_chat);
        etMessage = findViewById(R.id.et_message);
        ImageButton btnSend = findViewById(R.id.btn_send);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        // Add fake welcome message if empty
        if (messageList.isEmpty()) {
            messageList.add(new CommunityMessage("System", "Welcome to the " + communityName + " group!",
                    System.currentTimeMillis(), false));
            adapter.notifyItemInserted(0);
        }
    }

    private void loadMessages() {
        String json = prefs.getString("chat_" + communityId, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<CommunityMessage>>() {
            }.getType();
            messageList = gson.fromJson(json, type);
        }
        if (messageList == null) {
            messageList = new ArrayList<>();
        }
    }

    private void saveMessages() {
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(messageList);
        editor.putString("chat_" + communityId, json);
        editor.apply();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            // Add user message
            CommunityMessage msg = new CommunityMessage("You", text, System.currentTimeMillis(), true);
            messageList.add(msg);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
            etMessage.setText("");
            saveMessages();

            // Simulate a reply after 2 seconds (Demo feature)
            new android.os.Handler().postDelayed(() -> {
                CommunityMessage reply = new CommunityMessage("Ramesh Farmer", "Great point! I agree with you.",
                        System.currentTimeMillis(), false);
                messageList.add(reply);
                adapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.smoothScrollToPosition(messageList.size() - 1);
                saveMessages();
            }, 2000);
        }
    }

    // Inner Adapter Class
    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<CommunityMessage> messages;

        public ChatAdapter(List<CommunityMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_message, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommunityMessage msg = messages.get(position);
            holder.tvMessage.setText(msg.getText());

            if (msg.isMe()) {
                holder.tvSender.setVisibility(View.GONE);
                holder.layout.setGravity(android.view.Gravity.END);
                holder.tvMessage.setBackgroundResource(R.drawable.bg_pill_blue); // Reuse existing drawable or color
            } else {
                holder.tvSender.setVisibility(View.VISIBLE);
                holder.tvSender.setText(msg.getSenderName());
                holder.layout.setGravity(android.view.Gravity.START);
                holder.tvMessage.setBackgroundResource(R.drawable.bg_pill_dark);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSender, tvMessage;
            android.widget.LinearLayout layout;

            ViewHolder(View itemView) {
                super(itemView);
                tvSender = itemView.findViewById(R.id.tv_sender);
                tvMessage = itemView.findViewById(R.id.tv_message);
                layout = (android.widget.LinearLayout) itemView;
            }
        }
    }
}

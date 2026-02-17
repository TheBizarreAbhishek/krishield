package com.krishield.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.krishield.R;
import com.krishield.models.Community;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FarmersUnionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<Community> communityList;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "FarmersUnionPrefs";
    private static final String KEY_COMMUNITIES = "communities";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmers_union);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadCommunities();

        // Setup Header Back Button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recycler_communities);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityAdapter(communityList);
        recyclerView.setAdapter(adapter);

        // Setup Floating Action Button
        FloatingActionButton fab = findViewById(R.id.fab_create_community);
        fab.setOnClickListener(v -> showCreateCommunityDialog());
    }

    private void loadCommunities() {
        String json = prefs.getString(KEY_COMMUNITIES, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Community>>() {
            }.getType();
            communityList = gson.fromJson(json, type);
        }

        if (communityList == null) {
            communityList = new ArrayList<>();
            // Add default communities if empty
            communityList.add(new Community(UUID.randomUUID().toString(), "Punjab Kisan Union",
                    "Official union for Punjab farmers.", 1250));
            communityList.add(new Community(UUID.randomUUID().toString(), "All India Kisan Sabha",
                    "National level farmers organization.", 5400));
            communityList.add(new Community(UUID.randomUUID().toString(), "Organic Farmers Group",
                    "Discuss organic farming techniques.", 320));
            saveCommunities();
        }
    }

    private void saveCommunities() {
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(communityList);
        editor.putString(KEY_COMMUNITIES, json);
        editor.apply();
    }

    private void showCreateCommunityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Community");

        // Simple approach: Use a LinearLayout programmatically for the dialog content
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Community Name");
        layout.addView(etName);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Description");
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (!name.isEmpty()) {
                Community newCommunity = new Community(UUID.randomUUID().toString(), name, desc, 1);
                newCommunity.setJoined(true); // Creator automatically joins
                communityList.add(0, newCommunity);
                adapter.notifyItemInserted(0);
                recyclerView.scrollToPosition(0);
                saveCommunities();
                Toast.makeText(this, "Community Created!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void openChat(Community community) {
        Intent intent = new Intent(this, CommunityChatActivity.class);
        intent.putExtra("community_id", community.getId());
        intent.putExtra("community_name", community.getName());
        startActivity(intent);
    }

    // Inner Adapter Class
    class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {
        private List<Community> communities;

        public CommunityAdapter(List<Community> communities) {
            this.communities = communities;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Community community = communities.get(position);
            holder.tvName.setText(community.getName());
            holder.tvDesc.setText(community.getDescription());
            holder.tvMembers.setText(community.getMemberCount() + " Members");

            if (community.isJoined()) {
                holder.btnJoin.setText("Open Chat");
                holder.btnJoin.setBackgroundColor(getColor(android.R.color.holo_green_dark));
            } else {
                holder.btnJoin.setText("Join");
                holder.btnJoin.setBackgroundColor(getColor(R.color.kri_primary));
            }

            holder.btnJoin.setOnClickListener(v -> {
                if (!community.isJoined()) {
                    community.setJoined(true);
                    community.setMemberCount(community.getMemberCount() + 1);
                    notifyItemChanged(position);
                    saveCommunities();
                    Toast.makeText(FarmersUnionActivity.this, "Joined " + community.getName(), Toast.LENGTH_SHORT)
                            .show();
                }
                openChat(community);
            });
        }

        @Override
        public int getItemCount() {
            return communities.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvMembers;
            Button btnJoin;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_community_name);
                tvDesc = itemView.findViewById(R.id.tv_community_description);
                tvMembers = itemView.findViewById(R.id.tv_member_count);
                btnJoin = itemView.findViewById(R.id.btn_join);
            }
        }
    }
}

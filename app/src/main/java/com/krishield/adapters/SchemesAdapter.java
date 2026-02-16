package com.krishield.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.krishield.R;
import com.krishield.models.Scheme;

import java.util.ArrayList;
import java.util.List;

public class SchemesAdapter extends RecyclerView.Adapter<SchemesAdapter.SchemeViewHolder> {

    private final List<Scheme> schemes = new ArrayList<>();
    private final Context context;

    public SchemesAdapter(Context context) {
        this.context = context;
    }

    public void setSchemes(List<Scheme> newSchemes) {
        schemes.clear();
        schemes.addAll(newSchemes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SchemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheme, parent, false);
        return new SchemeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SchemeViewHolder holder, int position) {
        Scheme scheme = schemes.get(position);
        holder.bind(scheme);
    }

    @Override
    public int getItemCount() {
        return schemes.size();
    }

    class SchemeViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvDesc, tvBenefits, btnReadMore;

        public SchemeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_scheme_icon);
            tvTitle = itemView.findViewById(R.id.tv_scheme_title);
            tvDesc = itemView.findViewById(R.id.tv_scheme_desc);
            tvBenefits = itemView.findViewById(R.id.tv_scheme_benefits);
            btnReadMore = itemView.findViewById(R.id.btn_read_more);
        }

        public void bind(Scheme scheme) {
            tvIcon.setText(scheme.iconEmoji != null ? scheme.iconEmoji : "🌾");
            tvTitle.setText(scheme.title);
            tvDesc.setText(scheme.description);
            tvBenefits.setText("✅ Benefits: " + scheme.benefits);

            itemView.setOnClickListener(v -> openUrl(scheme.url));
            btnReadMore.setOnClickListener(v -> openUrl(scheme.url));
        }

        private void openUrl(String url) {
            if (url != null && !url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No link available", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

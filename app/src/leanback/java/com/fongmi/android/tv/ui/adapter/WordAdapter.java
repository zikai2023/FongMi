package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.databinding.AdapterSearchWordBinding;

import java.util.ArrayList;
import java.util.List;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Hot.Data> mItems;

    public WordAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public void addAll(List<Hot.Data> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
    }

    public void appendAll(List<Hot.Data> items) {
        mItems.addAll(items.subList(0, Math.min(items.size(), 20)));
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSearchWordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hot.Data item = mItems.get(position);
        holder.binding.rank.setImageResource(Hot.getRankDrawable(item.getRank()));
        holder.binding.trend.setImageResource(Hot.getTrendDrawable(item.getTrend()));
        holder.binding.text.setText(item.getName());
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(item));
    }

    public interface OnClickListener {

        void onItemClick(Hot.Data text);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterSearchWordBinding binding;

        public ViewHolder(@NonNull AdapterSearchWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onItemClick(mItems.get(getLayoutPosition()));
        }
    }
}

package com.example.pharmamapapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pharmamapapp.R;
import com.example.pharmamapapp.model.Medicine;
import com.example.pharmamapapp.util.ImageLoader;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<Medicine> medicines = new ArrayList<>();
    private final OnMedicineClick clickListener;

    public interface OnMedicineClick {
        void onClick(Medicine medicine);
    }

    public MedicineAdapter(OnMedicineClick clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<Medicine> newList) {
        medicines = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicines.get(position);
        holder.bind(medicine);
        holder.itemView.setOnClickListener(v -> clickListener.onClick(medicine));
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    static class MedicineViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView image;
        private final TextView imagePlaceholder;
        private final TextView name, code, price, quantity, shelf, status, expiry;

        MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.medicine_card);
            image = itemView.findViewById(R.id.item_image);
            imagePlaceholder = itemView.findViewById(R.id.item_image_placeholder);
            name = itemView.findViewById(R.id.item_name);
            code = itemView.findViewById(R.id.item_code);
            price = itemView.findViewById(R.id.item_price);
            quantity = itemView.findViewById(R.id.item_quantity);
            shelf = itemView.findViewById(R.id.item_shelf);
            status = itemView.findViewById(R.id.item_status);
            expiry = itemView.findViewById(R.id.item_expiry);
        }

        void bind(Medicine m) {
            name.setText(m.getName());

            if (!m.getCode().isEmpty()) {
                code.setText(m.getCode());
                code.setVisibility(View.VISIBLE);
            } else {
                code.setVisibility(View.GONE);
            }

            price.setText(String.format("%.1f ₪", m.getPrice()));
            quantity.setText(itemView.getContext().getString(R.string.qty_label, m.getQuantity()));

            if (!m.getShelf().isEmpty()) {
                shelf.setText(itemView.getContext().getString(R.string.shelf_label, m.getShelf()));
                shelf.setVisibility(View.VISIBLE);
            } else {
                shelf.setVisibility(View.GONE);
            }

            if (!m.getExpiryDate().isEmpty()) {
                expiry.setText(m.getExpiryDate());
                expiry.setVisibility(View.VISIBLE);
            } else {
                expiry.setVisibility(View.GONE);
            }

            ImageLoader.load(m.getImageId(), image, imagePlaceholder);

            int bgColor;
            if (m.isExpired()) {
                status.setText(R.string.expired_label);
                status.setTextColor(0xFFC62828);
                bgColor = 0xFFFFEBEE;
            } else if (m.isExpiringSoon()) {
                status.setText(R.string.expiring_soon_label);
                status.setTextColor(0xFFE65100);
                bgColor = 0xFFFFF8E1;
            } else if (m.isLowStock()) {
                status.setText(R.string.low_stock_label);
                status.setTextColor(0xFFE65100);
                bgColor = 0xFFFFF3E0;
            } else {
                status.setText("");
                bgColor = 0xFFFFFFFF;
            }
            card.setCardBackgroundColor(bgColor);
        }
    }
}

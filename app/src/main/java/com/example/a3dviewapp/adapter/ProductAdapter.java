package com.example.a3dviewapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a3dviewapp.R;
import com.example.a3dviewapp.RealTextureActivity;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.utils.ImageLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;
    private Map<String, Boolean> favoriteMap = new HashMap<>();

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product, boolean isFavorite);
        void onAddToCart(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productTitle.setText(product.getTitle());
        holder.productId.setText("ID: " + product.getId());

        // List view ma khali preview image dekhadva mate
        ImageLoader.getInstance(context).loadImage(product.getImageUrl(), holder.productImage);

        // Favorite icon setup
        boolean isFavorite = favoriteMap.getOrDefault(product.getId(), false);
        holder.favoriteIcon.setImageResource(
                isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
        );

        // --- PRODUCT CLICK LOGIC ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);

                // RealTextureActivity open karvi
                Intent intent = new Intent(context, RealTextureActivity.class);
                intent.putExtra("product_name", product.getTitle());

                // --- MUKHYA SUDHARO ---
                // T-shirt texture mate 'image' (preview) pehla check karvu ke 'files' (template) available che ke nahi.
                // Jo product model ma files list hoy, to original texture pass karvu.
                if (product.getFiles() != null && !product.getFiles().isEmpty()) {
                    intent.putExtra("product_image", product.getFiles().get(0).getUrl());
                } else {
                    intent.putExtra("product_image", product.getImageUrl());
                }

                intent.putExtra("product_id", product.getId());
                intent.putExtra("product_type", product.getType());

                context.startActivity(intent);
            }
        });

        holder.favoriteIcon.setOnClickListener(v -> {
            boolean newFavoriteState = !favoriteMap.getOrDefault(product.getId(), false);
            favoriteMap.put(product.getId(), newFavoriteState);
            holder.favoriteIcon.setImageResource(
                    newFavoriteState ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
            );
            if (listener != null) listener.onFavoriteClick(product, newFavoriteState);
        });

        holder.addToCartBtn.setOnClickListener(v -> {
            if (listener != null) listener.onAddToCart(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateData(List<Product> newProductList) {
        this.productList = newProductList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, favoriteIcon;
        TextView productTitle, productId;
        Button addToCartBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            productTitle = itemView.findViewById(R.id.productTitle);
            productId = itemView.findViewById(R.id.productId);
            addToCartBtn = itemView.findViewById(R.id.addToCartBtn);
        }
    }
}
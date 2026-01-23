package com.example.a3dviewapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a3dviewapp.R;
import com.example.a3dviewapp.model.Product;
import com.example.a3dviewapp.utils.ImageLoader;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // તમારો નવો item_product.xml જેમાં માત્ર ઈમેજ છે
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        // માત્ર ઈમેજ લોડ કરવા માટેનો કોડ
        if (product.getImageUrl() != null) {
            ImageLoader.getInstance(context).loadImage(product.getImageUrl(), holder.productImage);
        }

        // ક્લિક લોજિક - આખા કાર્ડ પર ક્લિક કરવાથી પ્રોડક્ટ ખુલશે
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
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
        ImageView productImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // તમારા નવા XML મુજબ માત્ર ઈમેજનું ID
            productImage = itemView.findViewById(R.id.productImage);
        }
    }
}
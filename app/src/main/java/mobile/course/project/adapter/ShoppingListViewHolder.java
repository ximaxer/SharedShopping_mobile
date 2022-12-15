package mobile.course.project.adapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import mobile.course.project.R;
import mobile.course.project.Utils.Constants;

class ShoppingListViewHolder extends RecyclerView.ViewHolder {
    private final TextView ShoppingListItemView;
    private final ImageView ShoppingListImageView;

    private ShoppingListViewHolder(View itemView) {
        super(itemView);
        ShoppingListItemView = itemView.findViewById(R.id.textView);
        ShoppingListImageView = itemView.findViewById(R.id.ProfilePic);
    }

    public void bind(String text, String owner) {
        ShoppingListItemView.setText(text);
        if(owner==null)return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS).document(owner).get().addOnSuccessListener(UserHashMap -> {
            byte[] bytes = Base64.decode(UserHashMap.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            ShoppingListImageView.setImageBitmap(bitmap);
        });
    }

    static ShoppingListViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false);
        return new ShoppingListViewHolder(view);
    }
}

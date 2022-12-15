package mobile.course.project.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import mobile.course.project.db.ShoppingList;


public class CustomAdapter extends ListAdapter<ShoppingList, ShoppingListViewHolder> {
    public CustomAdapter(@NonNull DiffUtil.ItemCallback<ShoppingList> diffCallback) {
        super(diffCallback);
    }

    @Override
    public ShoppingListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ShoppingListViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(ShoppingListViewHolder holder, int position) {
        ShoppingList current = getItem(position);
        System.out.println(current.getListReference());
        System.out.println(current.getListTitle());
        System.out.println(current.getListContent());
        holder.bind(current.getListTitle(), current.getOwner());
    }

    static public class ShoppingListDiff extends DiffUtil.ItemCallback<ShoppingList> {

        @Override
        public boolean areItemsTheSame(@NonNull ShoppingList oldItem, @NonNull ShoppingList newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ShoppingList oldItem, @NonNull ShoppingList newItem) {
            return oldItem.getListId() == (newItem.getListId());
        }
    }



}
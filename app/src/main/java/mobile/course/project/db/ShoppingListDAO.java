package mobile.course.project.db;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
@Dao
public interface ShoppingListDAO {

    @Query("SELECT * FROM ShoppingLists")
    LiveData<List<ShoppingList>> getAllLists();

    @Query("SELECT * FROM ShoppingLists where listTitle= :title and listContent= :text")
    LiveData<ShoppingList> checkIfListExists(String title, String text);

    @Insert(onConflict = REPLACE)
    void insertList(ShoppingList shoppingList);

    @Query("UPDATE ShoppingLists Set listTitle = :title, listContent= :text where listId= :id and listOwner= :owner and listUsers= :listUsers")
    void updateList(int id, String title, String text, String owner, String listUsers);

    @Query("DELETE FROM ShoppingLists")
    void deleteAll();

    @Delete
    void deleteList(ShoppingList shoppingList);
}

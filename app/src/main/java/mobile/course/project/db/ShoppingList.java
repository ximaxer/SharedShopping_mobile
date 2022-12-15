package mobile.course.project.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ShoppingLists")
public class ShoppingList {

    public ShoppingList(@NonNull String listReference, @NonNull String listTitle, @NonNull String listContent, @NonNull String owner){
        this.listReference = listReference;
        this.listTitle = listTitle;
        this.listContent = listContent;
        this.owner = owner;
    }

    @PrimaryKey(autoGenerate = true)
    private int listId;

    @ColumnInfo(name = "listReference")
    private String listReference;

    @ColumnInfo(name = "listTitle")
    private String listTitle;

    @ColumnInfo(name = "listContent")
    private String listContent;

    @ColumnInfo(name = "listOwner")
    private String owner;

    public int getListId() {
        return listId;
    }

    public void setListId(int listId) {
        this.listId = listId;
    }

    public String getListReference() {
        return listReference;
    }

    public void setListReference(String listReference) {
        this.listReference = listReference;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getListTitle() {
        return listTitle;
    }

    public void setListTitle(String listTitle) {
        this.listTitle = listTitle;
    }

    public String getListContent() {
        return listContent;
    }

    public void setListContent(String listContent) {
        this.listContent = listContent;
    }




    //usar MQTT para topicos
}

package mobile.course.project.db;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import mobile.course.project.Utils.Converters;

public class ShoppingListRepository {
    private ShoppingListDAO shoppingListDao;
    private LiveData<List<ShoppingList>> allLists;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public ShoppingListRepository(Application application) {
        AppDatabase db = AppDatabase.getDbInstance(application);
        shoppingListDao = db.shoppingListDAO();
        allLists = shoppingListDao.getAllLists();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public LiveData<List<ShoppingList>> getAllLists() {
        return allLists;
    }

    public void updateList(int id, String _title, String _content, String _owner, String _listUsers) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            shoppingListDao.updateList(id, _title, _content, _owner, _listUsers);
        });
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    public void insert(ShoppingList shoppingList) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
                shoppingListDao.insertList(shoppingList);
        });
    }

    public void deleteList(ShoppingList shoppingList){
        AppDatabase.databaseWriteExecutor.execute(() -> {
            shoppingListDao.deleteList(shoppingList);
        });
    }

    public void deleteAllLists(){
        AppDatabase.databaseWriteExecutor.execute(() -> {
            shoppingListDao.deleteAll();
        });
    }
}

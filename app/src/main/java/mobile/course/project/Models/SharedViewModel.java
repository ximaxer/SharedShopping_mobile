package mobile.course.project.Models;



import android.app.Application;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttMessage;


import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;
import mobile.course.project.db.ShoppingListRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SharedViewModel extends AndroidViewModel {
    private Fragment parentFragment;
    private ShoppingList currentShoppingList;
    private ArrayList<ShoppingList> shoppingListList = new ArrayList<>();
    private ShoppingListRepository myRepository;
    private final LiveData<List<ShoppingList>> allShoppingLists;
    private ArrayList<String> topics = new ArrayList<>();
    private boolean isConnected = false;
    private MqttMessage message;
    private String myID =null;

    public void setPreferenceManager(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    private PreferenceManager preferenceManager;

    public String getMyID() {return myID;}
    public void setMyID(String myID) {this.myID = myID;}
    public SharedViewModel(Application application){
        super(application);
        myRepository = new ShoppingListRepository(application);
        allShoppingLists = myRepository.getAllLists();
    }

    public boolean getConnectionStatus(){return isConnected;}
    public void setConnectionStatus(boolean isConnected){this.isConnected=isConnected;}

    public void changeLocalListAttributes(ShoppingList _shoppingList){
        myRepository.updateList(_shoppingList.getListId(), _shoppingList.getListTitle(), _shoppingList.getListContent(), _shoppingList.getOwner(),_shoppingList.getListUsers());
    }

    public void changeListAttributes(ShoppingList _shoppingList) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {

            db.collection(Constants.KEY_COLLECTION_LISTS).document(_shoppingList.getListReference()).get().addOnSuccessListener(
                    documentSnapshot -> {
                        HashMap<String, Object> list = new HashMap<>();
                        list.put(Constants.KEY_LIST_ID, documentSnapshot.getString(Constants.KEY_LIST_ID));
                        list.put(Constants.KEY_LIST_TEXT, _shoppingList.getListContent());
                        String owner = documentSnapshot.getString(Constants.KEY_OWNER);
                        list.put(Constants.KEY_OWNER, owner);
                        String title = documentSnapshot.getString(Constants.KEY_LIST_TITLE);
                        if(owner.equals(preferenceManager.getString(Constants.KEY_USER_ID))){
                            title = _shoppingList.getListTitle();
                        }
                        list.put(Constants.KEY_LIST_TITLE, title);
                        myRepository.updateList(_shoppingList.getListId(), title, _shoppingList.getListContent(), _shoppingList.getOwner(),_shoppingList.getListUsers());
                        db.collection(Constants.KEY_COLLECTION_LISTS).document(_shoppingList.getListReference()).update(list);
                    }
            );
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void deleteList(ShoppingList _shoppingList){
        try{
            myRepository.deleteList(_shoppingList);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void deleteAllLists(){
        try{
            myRepository.deleteAllLists();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ShoppingList getList() {
        try {
            return currentShoppingList;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void setList(ShoppingList _shoppingList) {
        currentShoppingList = _shoppingList;
    }

    public LiveData<List<ShoppingList>> getLists(){
        LiveData<List<ShoppingList>> shoppingLists = null;
        try {
            if(allShoppingLists == null) {
                return shoppingLists;
            } else {
                return allShoppingLists;
            }
        }catch (Exception e){
            e.printStackTrace();
            return shoppingLists;
        }
    }

    public void createList(String listReference, String listTitle, String listContent, String owner, String listUsers){
        ShoppingList newShoppingList = new ShoppingList(listReference, listTitle, listContent, owner,listUsers);
        try{
            myRepository.insert(newShoppingList);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getTopics(){ return topics; }
    public void addTopic(String topic){
        if(topics.contains(topic));
        else topics.add(topic);
    }
    public void removeTopic(String topic){ topics.remove(topic); }


    public MqttMessage getMessage() {
        return message;
    }

    public void setMessage(MqttMessage message) {
        this.message = message;
    }

    public Fragment getParentFragment() {
        return parentFragment;
    }

    public void setParentFragment(Fragment parentFragment) {
        this.parentFragment = parentFragment;
    }
}

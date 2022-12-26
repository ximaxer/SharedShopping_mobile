package mobile.course.project.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;


import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;


import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import mobile.course.project.Activities.MainActivity;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.QR.QRDialogFragment;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;
import mobile.course.project.firebase.FcmNotificationsSender;
import mobile.course.project.network.ApiClient;
import mobile.course.project.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoteFragment extends Fragment{
    private ShoppingList myShoppingList;
    private SharedViewModel viewModel;
    private EditText TextField;
    private Toolbar toolbar;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    public NoteFragment() {
        // Required empty public constructor
    }

    public static NoteFragment newInstance() {
        return new NoteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        View v = inflater.inflate(R.layout.note_fragment, container, false);
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        db = FirebaseFirestore.getInstance();
        myShoppingList = viewModel.getList();
        TextField = v.findViewById(R.id.NoteText);
        if(myShoppingList !=null) TextField.setText(myShoppingList.getListContent());

        toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.note_fragment_menu);
        toolbar.setTitle(myShoppingList.getListTitle());
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_save:
                        if(myShoppingList != null) {
                            myShoppingList.setListContent(TextField.getText().toString());
                            viewModel.changeListAttributes(myShoppingList);
                            ((MainActivity) requireActivity()).sendNotification("Note changed!","Note "+myShoppingList.getListTitle()+" was change by "+preferenceManager.getString(Constants.KEY_NAME)+".");
                           /* try {
                                JSONArray tokens = new JSONArray();
                                tokens.put(preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                                String key_message = "message";
                                String message = "notification text";
                                JSONObject data = new JSONObject();
                                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                                data.put(key_message,message);
                                JSONObject body = new JSONObject();
                                body.put(Constants.REMOTE_MSG_DATA, data);
                                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
                                sendNotification(body.toString());
                            }catch(Exception exception){
                                Toast toast = Toast.makeText(getContext(), exception.getMessage()+" howdy",Toast.LENGTH_SHORT);
                                toast.show();
                        }*/

                    }
                        return true;
                    case R.id.share_note_QR:
                        QRDialogFragment qrDialogFragment = QRDialogFragment.newInstance(new QRDialogFragment(){});
                        qrDialogFragment.show(getParentFragmentManager(),"QRDialogFragment");
                        return true;
                }
                return true;
            }
        });
        listenToListChanges();
        return v;
    }


    private void listenToListChanges() {
        Query query = db.collection(Constants.KEY_COLLECTION_LISTS).whereEqualTo(Constants.KEY_LIST_ID, myShoppingList.getListReference());
        ListenerRegistration registration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null){
                    return;
                }
                if(value != null){
                    for(DocumentChange dc: value.getDocumentChanges()) {
                        if(dc.getType()== DocumentChange.Type.MODIFIED) {
                            HashMap<String, Object> fireBaseList = (HashMap<String, Object>) dc.getDocument().getData();
                            if(myShoppingList.getListTitle().equals((String) fireBaseList.get(Constants.KEY_LIST_TITLE))){
                                TextField.setText((String) fireBaseList.get(Constants.KEY_LIST_TEXT));
                                myShoppingList.setListContent((String) fireBaseList.get(Constants.KEY_LIST_TEXT));
                            }else {
                                toolbar.setTitle((String) fireBaseList.get(Constants.KEY_LIST_TITLE));
                                myShoppingList.setListTitle((String) fireBaseList.get(Constants.KEY_LIST_TITLE));
                            }
                            viewModel.changeLocalListAttributes(myShoppingList);
                        }else if(dc.getType() == DocumentChange.Type.REMOVED){
                            db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).get().addOnSuccessListener(
                                    userHashMap -> {
                                        ArrayList<String> myPreferedLists = preferenceManager.getStringArray();
                                        myPreferedLists.remove(myShoppingList.getListReference());
                                        preferenceManager.putStringArray(myPreferedLists);
                                        HashMap<String, Object> myLists = (HashMap<String, Object>) userHashMap.getData();
                                        myLists.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(myPreferedLists));
                                        db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).update(myLists);
                                        viewModel.deleteList(myShoppingList);
                                    }
                            );

                            FragmentManager fm = getParentFragmentManager();
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(((ViewGroup)(getView().getParent())).getId(), new ListFragment());
                            ft.commit();
                            Toast.makeText(requireActivity().getApplicationContext(),"This list was just deleted by its creator",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        registration.remove();
       //db.collection(Constants.KEY_COLLECTION_LISTS).whereEqualTo(Constants.KEY_LIST_ID, myShoppingList.getListReference()).addSnapshotListener(eventListener);
    }
/*
    private final EventListener<QuerySnapshot> eventListener = (value, error) ->{
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange dc: value.getDocumentChanges()) {
                if(dc.getType()== DocumentChange.Type.MODIFIED) {
                    HashMap<String, Object> fireBaseList = (HashMap<String, Object>) dc.getDocument().getData();
                    if(myShoppingList.getListTitle().equals((String) fireBaseList.get(Constants.KEY_LIST_TITLE))){
                        TextField.setText((String) fireBaseList.get(Constants.KEY_LIST_TEXT));
                        myShoppingList.setListContent((String) fireBaseList.get(Constants.KEY_LIST_TEXT));
                    }else {
                        toolbar.setTitle((String) fireBaseList.get(Constants.KEY_LIST_TITLE));
                        myShoppingList.setListTitle((String) fireBaseList.get(Constants.KEY_LIST_TITLE));
                    }
                    viewModel.changeLocalListAttributes(myShoppingList);
                }else if(dc.getType() == DocumentChange.Type.REMOVED){
                    db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).get().addOnSuccessListener(
                            userHashMap -> {
                                ArrayList<String> myPreferedLists = preferenceManager.getStringArray();
                                myPreferedLists.remove(myShoppingList.getListReference());
                                preferenceManager.putStringArray(myPreferedLists);
                                HashMap<String, Object> myLists = (HashMap<String, Object>) userHashMap.getData();
                                myLists.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(myPreferedLists));
                                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).update(myLists);
                                viewModel.deleteList(myShoppingList);
                            }
                    );
                    FragmentManager fm = getParentFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(((ViewGroup)(getView().getParent())).getId(), new ListFragment());
                    ft.commit();
                    Toast.makeText(requireActivity().getApplicationContext(),"This list was just deleted by its creator",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
   */
   /* private void sendNotification(String title, String body) {
       /* db.collection(Constants.KEY_COLLECTION_USERS).document(viewModel.getList().getOwner()).get().addOnSuccessListener(
                userHashMap -> {
                    HashMap<String, Object> myUser = (HashMap<String, Object>) userHashMap.getData();
                    String currentUsertoken = (String) myUser.get(Constants.KEY_FCM_TOKEN);
                    System.out.println("owner token: "+ currentUsertoken);
                    myShoppingList.getListUsers()
                    // FirebaseMessaging.getInstance().subscribeToTopic("all");
                    FcmNotificationsSender notificationsSender = new FcmNotificationsSender(currentUsertoken,title,body, requireActivity().getApplicationContext(), getActivity());
                    notificationsSender.SendNotifications();
                }
        );*/
      /*  ArrayList<String> currentListUsers = Converters.JsonStringToArrayList(myShoppingList.getListUsers());
        for (String listUser : currentListUsers) {
            if (!listUser.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                System.out.println("-->listuser: " + listUser + "\n-->userId: " + preferenceManager.getString(Constants.KEY_USER_ID));
                db.collection(Constants.KEY_COLLECTION_USERS).document(listUser).get().addOnSuccessListener(
                        userHashMap -> {
                            HashMap<String, Object> myUser = (HashMap<String, Object>) userHashMap.getData();
                            String usertoken = (String) myUser.get(Constants.KEY_FCM_TOKEN);
                            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(usertoken, title, body, requireActivity().getApplicationContext(), getActivity());
                            notificationsSender.SendNotifications();
                        }
                );
            }
        }
    }*/
   /* private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                        Constants.getRemoteMsgHeaders(),
                        messageBody).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if(response.isSuccessful()){
                            try {
                                if(response.body() != null){
                                    JSONObject responseJson = new JSONObject(response.body());
                                    JSONArray results = responseJson.getJSONArray("results");
                                    if(responseJson.getInt("failure")==1) {
                                        JSONObject error = (JSONObject) results.get(0);
                                        Toast toast = Toast.makeText(getContext(), "error: "+error.getString("error"),Toast.LENGTH_SHORT);
                                        toast.show();
                                        return;
                                    }
                                }
                            }catch(JSONException e){
                                e.printStackTrace();
                            }
                            Toast toast = Toast.makeText(getContext(), "Notification sent sucefully",Toast.LENGTH_SHORT);
                            toast.show();
                        }else{
                            Toast toast = Toast.makeText(getContext(), "error"+response.code()+"responde",Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                        Toast toast = Toast.makeText(getContext(), t.getMessage(),Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
    }*/


}

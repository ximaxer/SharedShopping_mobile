package mobile.course.project.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;


public class EditDialogFragment2 extends DialogFragment{
    PreferenceManager preferenceManager;
    public EditDialogFragment2() {}

    public static EditDialogFragment2 newInstance(EditDialogFragment2 editDialogFragment) {
        EditDialogFragment2 fragment = new EditDialogFragment2();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit2_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        ShoppingList myShoppingList = viewModel.getList();
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        super.onViewCreated(view, savedInstanceState);
        Button deleteButton = view.findViewById(R.id.deleteButton2);
        deleteButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection(Constants.KEY_COLLECTION_LISTS).document(myShoppingList.getListReference()).get().addOnSuccessListener(
                        listObject -> {
                            if (listObject.exists()) {
                                ArrayList<String> listsUsers = Converters.JsonStringToArrayList(listObject.getString(Constants.KEY_LIST_USERS));
                                listsUsers.remove(preferenceManager.getString(Constants.KEY_USER_ID));

                                HashMap<String, Object> myFirebaseListUser = (HashMap<String, Object>) listObject.getData();
                                myFirebaseListUser.put(Constants.KEY_LIST_USERS, Converters.ArrayListToJsonString(listsUsers));
                                db.collection(Constants.KEY_COLLECTION_LISTS).document(myShoppingList.getListReference()).update(myFirebaseListUser);
                            }
                        }
                );
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

                dismiss();
            }
        });
    }
}
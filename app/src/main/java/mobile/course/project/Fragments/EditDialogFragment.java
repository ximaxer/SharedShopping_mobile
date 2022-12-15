package mobile.course.project.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

import mobile.course.project.Activities.MainActivity;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;


public class EditDialogFragment extends DialogFragment{
    PreferenceManager preferenceManager;
    public EditDialogFragment() {}

    public static EditDialogFragment newInstance(EditDialogFragment editDialogFragment) {
        EditDialogFragment fragment = new EditDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        ShoppingList myShoppingList = viewModel.getList();
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        super.onViewCreated(view, savedInstanceState);
        Button saveButton = view.findViewById(R.id.saveButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        EditText newTitle = view.findViewById(R.id.newTitle);
        EditText topic = view.findViewById(R.id.topic);
        saveButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                String newTitleString = newTitle.getText().toString();
                if(!newTitleString.isEmpty()) {
                    myShoppingList.setListTitle(newTitleString);
                    viewModel.changeListAttributes(myShoppingList);
                    dismiss();
                }
                else{
                    Toast toast = Toast.makeText(getContext(), "Title field can't be empty ;)",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                if(preferenceManager.getString(Constants.KEY_USER_ID).equals(myShoppingList.getOwner())) {
                    db.collection(Constants.KEY_COLLECTION_LISTS).document(myShoppingList.getListReference()).delete();
                }
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
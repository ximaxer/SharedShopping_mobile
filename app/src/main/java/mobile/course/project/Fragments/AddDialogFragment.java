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

import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;

public class AddDialogFragment extends DialogFragment {

    PreferenceManager preferenceManager;

    public AddDialogFragment() {
    }

    public static AddDialogFragment newInstance(AddDialogFragment addDialogFragment) {
        AddDialogFragment fragment = new AddDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        return inflater.inflate(R.layout.fragment_add_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        EditText listTitle = view.findViewById(R.id.listTitle);
        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String listTitleString = listTitle.getText().toString();
                if (!listTitleString.isEmpty()) {
                    addListToFirebase(listTitleString, "", viewModel);
                    dismiss();
                } else {
                    Toast toast = Toast.makeText(getContext(), "Title field can't be empty ;)", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private void addListToFirebase(String _title, String _text, SharedViewModel viewModel) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> shoppingList = new HashMap<>();
        shoppingList.put(Constants.KEY_OWNER, preferenceManager.getString(Constants.KEY_USER_ID));
        shoppingList.put(Constants.KEY_LIST_TITLE, _title);
        shoppingList.put(Constants.KEY_LIST_TEXT, _text);
        db.collection(Constants.KEY_COLLECTION_LISTS).add(shoppingList)
            .addOnSuccessListener(listDocumentReference -> {
                shoppingList.put(Constants.KEY_LIST_ID, listDocumentReference.getId());
                db.collection(Constants.KEY_COLLECTION_LISTS).document(listDocumentReference.getId()).update(shoppingList);

                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).get().addOnSuccessListener(
                        userDocumentReference -> {
                            ArrayList<String> myPreferedLists = preferenceManager.getStringArray();
                            myPreferedLists.add(listDocumentReference.getId());
                            preferenceManager.putStringArray(myPreferedLists);
                            HashMap<String, Object> myLists = (HashMap<String, Object>) userDocumentReference.getData();
                            myLists.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(myPreferedLists));
                            db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).update(myLists);

                            viewModel.createList(listDocumentReference.getId(),_title, _text, preferenceManager.getString(Constants.KEY_USER_ID));
                        }).addOnFailureListener(exception -> {
                    exception.printStackTrace();
                });
            });
    }
}
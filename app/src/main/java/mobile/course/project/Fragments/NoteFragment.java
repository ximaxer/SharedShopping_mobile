package mobile.course.project.Fragments;

import android.os.Bundle;

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
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.QR.QRDialogFragment;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;

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
        db.collection(Constants.KEY_COLLECTION_LISTS).whereEqualTo(Constants.KEY_LIST_ID, myShoppingList.getListReference()).addSnapshotListener(eventListener);
    }

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
}

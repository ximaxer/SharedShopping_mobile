package mobile.course.project.Fragments;
//fragment 1

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;

import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import mobile.course.project.Activities.MainActivity;
import mobile.course.project.Interfaces.DrawerLocker;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.MQTTHelper;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.Utils.RecyclerItemClickListener;
import mobile.course.project.adapter.CustomAdapter;
import mobile.course.project.db.ShoppingList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ListFragment extends Fragment {
    private SharedViewModel viewModel;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private MQTTHelper mqtt;
    private CustomAdapter adapter;
    private String filter;
    private LiveData<List<ShoppingList>> noteList;
    public ListFragment() {}

    public static ListFragment newInstance  () {
        ListFragment fragment = new ListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        //((MainActivity)getActivity()).connectingToMqttServer(requireActivity().getApplicationContext());
        adapter = new CustomAdapter(new CustomAdapter.ShoppingListDiff());
        viewModel.getLists().observe(requireActivity(), shoppingLists -> {
            // Update the cached copy of the words in the adapter.
            adapter.submitList(shoppingLists);
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(requireActivity(), recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
            @Override public void onItemClick(View view, int position) {
                viewModel.setList(adapter.getCurrentList().get(position));
                FragmentManager fm = getParentFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(((ViewGroup)(getView().getParent())).getId(), new NoteFragment());
                ft.commit();
            }

            @Override public void onLongItemClick(View view, int position) {
                if(adapter.getCurrentList().get(position).getOwner().equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                    viewModel.setList(adapter.getCurrentList().get(position));
                    EditDialogFragment editDialogFragment = EditDialogFragment.newInstance(new EditDialogFragment() {
                    });
                    editDialogFragment.show(getParentFragmentManager(), "EditDialogFragment");
                }
                else{
                    viewModel.setList(adapter.getCurrentList().get(position));
                    EditDialogFragment2 editDialogFragment2 = EditDialogFragment2.newInstance(new EditDialogFragment2() {
                    });
                    editDialogFragment2.show(getParentFragmentManager(), "EditDialogFragment2");
                }
                // do whatever
            }
        }));

        ((DrawerLocker)getActivity()).setDrawerEnabled(true);
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.list_fragment_menu);
        toolbar.setTitle("Shopping Lists");
        DrawerLayout drawer = requireActivity().findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(requireActivity(), drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.action_search:
                        SearchView searchView = (SearchView) item.getActionView();
                        searchView.setQueryHint("Search by note title");
                        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
                            @Override
                            public boolean onQueryTextSubmit(String s) {
                                return false;
                            }

                            @Override
                            public boolean onQueryTextChange(String s) {
                                filter(s);
                                return false;
                            }
                        });
                        return true;
                    /*
                    case R.id.action_sub_topic:
                        viewModel.setParentFragment(getParentFragmentManager().findFragmentById(R.id.listFrag));
                        SubDialogFragment subDialogFragment = SubDialogFragment.newInstance(new SubDialogFragment(){});
                        subDialogFragment.show(getParentFragmentManager(),"SubDialogFragment");
                        return true;*/
                }

                return true;
            }
        });
        listenToListChanges();
        return view;
    }
    private void listenToListChanges() {
        for(String listId : preferenceManager.getStringArray()){
            db.collection(Constants.KEY_COLLECTION_LISTS).whereEqualTo(Constants.KEY_LIST_ID,listId).addSnapshotListener(eventListener);
        }
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) ->{
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange dc: value.getDocumentChanges()) {
                if(dc.getType()== DocumentChange.Type.MODIFIED) {
                    HashMap<String, Object> fireBaseList = (HashMap<String, Object>) dc.getDocument().getData();
                    String listId = (String) fireBaseList.get(Constants.KEY_LIST_ID);
                    for (ShoppingList list : Objects.requireNonNull(viewModel.getLists().getValue())) {
                        if (list.getListReference().equals(listId)) {
                            list.setListTitle((String) fireBaseList.get(Constants.KEY_LIST_TITLE));
                            list.setListContent((String) fireBaseList.get(Constants.KEY_LIST_TEXT));
                            list.setListUsers((String) fireBaseList.get(Constants.KEY_LIST_USERS));
                            viewModel.changeLocalListAttributes(list);
                        }
                    }
                }else if(dc.getType() == DocumentChange.Type.REMOVED){
                    HashMap<String, Object> fireBaseList = (HashMap<String, Object>) dc.getDocument().getData();
                    db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).get().addOnSuccessListener(
                            userHashMap -> {
                                String listId = (String) fireBaseList.get(Constants.KEY_LIST_ID);
                                ArrayList<String> myPreferedLists = preferenceManager.getStringArray();
                                myPreferedLists.remove(listId);
                                preferenceManager.putStringArray(myPreferedLists);
                                HashMap<String, Object> myLists = (HashMap<String, Object>) userHashMap.getData();
                                myLists.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(myPreferedLists));
                                db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).update(myLists);
                                for (ShoppingList list : Objects.requireNonNull(viewModel.getLists().getValue())) {
                                    if (list.getListReference().equals(listId)) {
                                        viewModel.deleteList(list);
                                    }
                                }
                            }
                    );
                }
            }
        }
    };
    public void filter(String text) {
        // creating a new array list to filter our data.
        List<ShoppingList> shoppingListList = viewModel.getLists().getValue();
        List<ShoppingList> filteredlist = new ArrayList<>();
        assert shoppingListList != null;
        for (ShoppingList shoppingList : shoppingListList) {
            if (shoppingList.getListTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredlist.add(shoppingList);
            }
        }
        if (filteredlist.isEmpty()) {
            adapter.submitList(filteredlist);
            Toast.makeText(getContext(), "No Data Found.", Toast.LENGTH_SHORT).show();
        } else {
            adapter.submitList(filteredlist);
        }
    }


}
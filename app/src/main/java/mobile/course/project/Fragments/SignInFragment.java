package mobile.course.project.Fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.util.Base64;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import mobile.course.project.Interfaces.DrawerLocker;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;

public class SignInFragment extends Fragment {
    private View SignInScreenView;
    private PreferenceManager preferenceManager;
    public SignInFragment() {
        // Required empty public constructor
    }
    public static SignInFragment newInstance(String param1, String param2) {
        SignInFragment fragment = new SignInFragment();
        Bundle args = new Bundle();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SignInScreenView = inflater.inflate(R.layout.fragment_sign_in, container, false);
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        SetListeners();

        ((DrawerLocker)getActivity()).setDrawerEnabled(false);
        return SignInScreenView;
    }
    private void SetListeners(){
        SignInScreenView.findViewById(R.id.textCreateNewAccount).setOnClickListener(v ->{
            FragmentManager fm = getParentFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(((ViewGroup)(getView().getParent())).getId(), new SignUpFragment());
            ft.commit();
        });
        SignInScreenView.findViewById(R.id.SignInButton).setOnClickListener(v ->{
            if(isValidSignInDetails()) {
                SignIn();
            }
        });
    }
    private void SignIn(){
        loading(true);
        EditText Name = SignInScreenView.findViewById(R.id.inputName);
        EditText Email = SignInScreenView.findViewById(R.id.inputEmail);
        EditText Password = SignInScreenView.findViewById(R.id.inputPassword);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL,Email.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,Password.getText().toString())
                .get().addOnCompleteListener(task ->{
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                        DocumentSnapshot MyHashMap = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                        preferenceManager.putString(Constants.KEY_USER_ID, MyHashMap.getId());
                        preferenceManager.putString(Constants.KEY_EMAIL, MyHashMap.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_NAME, MyHashMap.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, MyHashMap.getString(Constants.KEY_IMAGE));
                        //get the shopping lists
                        ArrayList<String> myLists = Converters.JsonStringToArrayList(MyHashMap.getString(Constants.KEY_MY_LISTS));
                        preferenceManager.putStringArray(myLists);
                        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
                        for (String listId: myLists) {
                            db.collection(Constants.KEY_COLLECTION_LISTS).document(listId).get().addOnSuccessListener(
                                    listObject -> {
                                        if(listObject.exists()) {
                                            String reference = listObject.getString(Constants.KEY_LIST_ID);
                                            String title = listObject.getString(Constants.KEY_LIST_TITLE);
                                            String content = listObject.getString(Constants.KEY_LIST_TEXT);
                                            viewModel.createList(reference, title, content,listObject.getString(Constants.KEY_OWNER));

                                        }else{
                                            myLists.remove(listId);
                                            db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).get().addOnSuccessListener(
                                                    userDocumentReference -> {
                                                        preferenceManager.putStringArray(myLists);
                                                        HashMap<String, Object> myFirebaseLists = (HashMap<String, Object>) userDocumentReference.getData();
                                                        myFirebaseLists.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(myLists));
                                                        db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID)).update(myFirebaseLists);
                                                    }).addOnFailureListener(exception -> {
                                                exception.printStackTrace();
                                            });
                                        }
                                    }
                            );
                        }


                        FragmentManager fm = getParentFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(((ViewGroup)(getView().getParent())).getId(), new ListFragment());
                        ft.commit();
                        loadUserDetails();
                        getToken();
                    }else{
                        loading(false);
                        ShowToast("Login Failed.");
                    }
                });
    }
    private void ShowToast(String displayMessage){
        Toast.makeText(getContext(),displayMessage, Toast.LENGTH_SHORT).show();
    }
    private Boolean isValidSignInDetails(){
        EditText Email = SignInScreenView.findViewById(R.id.inputEmail);
        EditText Password = SignInScreenView.findViewById(R.id.inputPassword);
        if(Email.getText().toString().trim().isEmpty()){
            ShowToast("Enter Email.");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(Email.getText().toString()).matches()){
            ShowToast("This is not a valid email format.");
            return false;
        }else if(Password.getText().toString().trim().isEmpty()){
            ShowToast("Enter Password.");
            return false;
        }else{
            return true;
        }
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            SignInScreenView.findViewById(R.id.SignInButton).setVisibility(View.INVISIBLE);
            SignInScreenView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }else{
            SignInScreenView.findViewById(R.id.SignInButton).setVisibility(View.VISIBLE);
            SignInScreenView.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        }
    }


    private void loadUserDetails(){
        NavigationView navigationView = requireActivity().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        TextView text = header.findViewById(R.id.nameProfile);
        ImageView image = header.findViewById(R.id.imageProfile);
        text.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        image.setImageBitmap(bitmap);
    }


    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnSuccessListener(
                        unused -> {/*do something for success*/})
                .addOnFailureListener(
                        e -> ShowToast("Unable to update token"));
    }


}
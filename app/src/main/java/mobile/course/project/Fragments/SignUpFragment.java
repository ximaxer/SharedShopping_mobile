package mobile.course.project.Fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.provider.MediaStore;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.Encoders;
import mobile.course.project.Utils.PreferenceManager;

public class SignUpFragment extends Fragment {
    private View SignUpScreenView;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    public SignUpFragment() {
        // Required empty public constructor
    }

    public static SignUpFragment newInstance() {
        SignUpFragment fragment = new SignUpFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SignUpScreenView = inflater.inflate(R.layout.fragment_register, container, false);
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        SetListeners();
        return SignUpScreenView;
    }
    private void SetListeners(){
        SignUpScreenView.findViewById(R.id.SignInButton).setOnClickListener(v ->{
            FragmentManager fm = getParentFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(((ViewGroup)(getView().getParent())).getId(), new SignInFragment());
            ft.commit();
        });
        SignUpScreenView.findViewById(R.id.SignUpButton).setOnClickListener(v->{
           if(isValidSignUpDetails()) SignUp();
        });
        SignUpScreenView.findViewById(R.id.imageProfile).setOnClickListener(v->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void SignUp(){
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        EditText Name = SignUpScreenView.findViewById(R.id.inputName);
        EditText Email = SignUpScreenView.findViewById(R.id.inputEmail);
        EditText Password = SignUpScreenView.findViewById(R.id.inputPassword);
        user.put(Constants.KEY_NAME, Name.getText().toString());
        user.put(Constants.KEY_EMAIL, Email.getText().toString());
        user.put(Constants.KEY_PASSWORD, Password.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        user.put(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(new ArrayList<String>()));
        db.collection(Constants.KEY_COLLECTION_USERS).add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME,Name.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE,encodedImage);
                    preferenceManager.putStringArray(new ArrayList<String>());
                    FragmentManager fm = getParentFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(((ViewGroup)(getView().getParent())).getId(), new ListFragment());
                    ft.commit();
                    loadUserDetails();
                    getToken();
                }).addOnFailureListener(exception -> {
                    loading(false);
                    ShowToast(exception.getMessage());
                });
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),result->{
            if(result.getResultCode() == RESULT_OK){
                if(result.getData() != null){
                    Uri imageUri = result.getData().getData();
                    try{
                        InputStream inputStream = requireActivity().getApplicationContext().getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        ImageView imageView= SignUpScreenView.findViewById(R.id.imageProfile);
                        imageView.setImageBitmap(bitmap);
                        TextView textView= SignUpScreenView.findViewById(R.id.textAddImage);
                        textView.setVisibility(View.GONE);
                        encodedImage = Encoders.encodeImage(bitmap);
                    }catch(FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    );
    private void ShowToast(String displayMessage){
        Toast.makeText(getContext(),displayMessage, Toast.LENGTH_SHORT).show();
    }
    private Boolean isValidSignUpDetails(){
        EditText Name = SignUpScreenView.findViewById(R.id.inputName);
        EditText Email = SignUpScreenView.findViewById(R.id.inputEmail);
        EditText Password = SignUpScreenView.findViewById(R.id.inputPassword);
        EditText ConfirmPassword = SignUpScreenView.findViewById(R.id.inputConfirmPassword);
        if(encodedImage == null){
            ShowToast("Select profile image.");
            return false;
        }else if(Name.getText().toString().trim().isEmpty()){
            ShowToast("Enter Name.");
            return false;
        }else if(Email.getText().toString().trim().isEmpty()){
            ShowToast("Enter Email.");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(Email.getText().toString()).matches()){
            ShowToast("This is not a valid email format.");
            return false;
        }else if(Password.getText().toString().trim().isEmpty()){
            ShowToast("Enter Password.");
            return false;
        }else if(ConfirmPassword.getText().toString().trim().isEmpty()){
            ShowToast("Confirm your Password.");
            return false;
        }else if(!Password.getText().toString().equals(ConfirmPassword.getText().toString())){
            ShowToast("Your Passwords do not match.");
            return false;
        }else{
            return true;
        }
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            SignUpScreenView.findViewById(R.id.SignUpButton).setVisibility(View.INVISIBLE);
            SignUpScreenView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }else{
            SignUpScreenView.findViewById(R.id.SignUpButton).setVisibility(View.VISIBLE);
            SignUpScreenView.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
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
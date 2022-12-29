package mobile.course.project.Fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;

public class ProfileFragment extends Fragment {
    PreferenceManager preferenceManager;

    public ProfileFragment() {}

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        ImageView userImageView = view.findViewById(R.id.profilePic);
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        userImageView.setImageBitmap(bitmap);

        TextView userName = view.findViewById(R.id.textUserName);
        TextView userEmail = view.findViewById(R.id.textUserEmail);
        TextView userLists = view.findViewById(R.id.textUserLists);
        preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        userName.setText(preferenceManager.getString(Constants.KEY_NAME));
        userEmail.setText("Email: " + preferenceManager.getString(Constants.KEY_EMAIL));
        String myListsTitles= "Colaborator on the following lists: \n";
        for(ShoppingList list : Objects.requireNonNull(viewModel.getLists().getValue())) {
            myListsTitles = myListsTitles.concat(list.getListTitle()+"; ");
        }
        userLists.setText(myListsTitles);
        return view;
    }
}


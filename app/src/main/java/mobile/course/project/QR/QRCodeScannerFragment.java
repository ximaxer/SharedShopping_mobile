package mobile.course.project.QR;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.PreferenceChangeEvent;

import mobile.course.project.Fragments.ListFragment;
import mobile.course.project.Fragments.NoteFragment;
import mobile.course.project.Interfaces.DrawerLocker;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;

public class QRCodeScannerFragment extends Fragment {

    private CodeScanner mCodeScanner;
    boolean CameraPermission = false;
    final int CAMERA_PERM = 1;
    private SharedViewModel viewModel;
    private PreferenceManager preferenceManager;

    public QRCodeScannerFragment() {
        // Required empty public constructor
    }

    public static QRCodeScannerFragment newInstance() { return new QRCodeScannerFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View QRCodeScannerFragmentView = inflater.inflate(R.layout.fragment_qrcode_scanner, container, false);
        final Activity activity = getActivity();
        CodeScannerView scannerView = QRCodeScannerFragmentView.findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(activity, scannerView);
        ((DrawerLocker)getActivity()).setDrawerEnabled(false);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleResults(result);
                        FragmentManager fm = getParentFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(((ViewGroup)(getView().getParent())).getId(), new ListFragment());
                        ft.commit();
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
        return QRCodeScannerFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    public void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

    private void handleResults(Result result){
        if(result.getText()!=null){
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                ShoppingList scannedShoppingList = Converters.JsonStringToListObject(result.getText());
                db.collection(Constants.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL))
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                ArrayList<String> myLists = Converters.JsonStringToArrayList(documentSnapshot.getString(Constants.KEY_MY_LISTS));
                                if (!myLists.contains(scannedShoppingList.getListReference())) {
                                    myLists.add(scannedShoppingList.getListReference());
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
                                db.collection(Constants.KEY_COLLECTION_LISTS).document(scannedShoppingList.getListReference()).get().addOnSuccessListener(
                                        listObject -> {
                                            if (listObject.exists()) {
                                                String reference = listObject.getString(Constants.KEY_LIST_ID);
                                                String title = listObject.getString(Constants.KEY_LIST_TITLE);
                                                String content = listObject.getString(Constants.KEY_LIST_TEXT);
                                                String owner = scannedShoppingList.getOwner();
                                                viewModel.createList(reference, title, content, owner);
                                            }
                                        }
                                );

                            }
                        });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
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

import org.json.JSONException;
import org.json.JSONObject;

import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;

public class AcceptDialogFragment extends DialogFragment{
    public AcceptDialogFragment() {}

    public static AcceptDialogFragment newInstance(AcceptDialogFragment acceptDialogFragment) {
        AcceptDialogFragment fragment = new AcceptDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accept_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        Button accButton = view.findViewById(R.id.acceptButton);
        accButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                JSONObject newNote = null;
                try {
                    newNote = new JSONObject(new String(viewModel.getMessage().getPayload()));
                    System.out.println(viewModel.getMessage()+" has arrived!");
                    String noteTitle = newNote.getString("Title");
                    String noteText = newNote.getString("Text");
                    //viewModel.createList(noteTitle,noteText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dismiss();
            }
        });
    }



}

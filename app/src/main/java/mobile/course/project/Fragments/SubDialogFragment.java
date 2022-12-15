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

import mobile.course.project.Activities.MainActivity;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;


public class SubDialogFragment extends DialogFragment {


    public SubDialogFragment() {}


    public static SubDialogFragment newInstance(SubDialogFragment subDialogFragment) {
        SubDialogFragment fragment = new SubDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        EditText topic = view.findViewById(R.id.topic);
        Button subButton = view.findViewById(R.id.subButton);
        subButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String topicString = topic.getText().toString();
                if(!topicString.isEmpty()) {
                    ((MainActivity)getActivity()).subscribeToTopic(topicString);
                    dismiss();
                }
                else{
                    Toast toast = Toast.makeText(getContext(), "Topic field can't be empty ;)",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            });
        Button unsubButton = view.findViewById(R.id.unsubButton);
        unsubButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String topicString = topic.getText().toString();
                if(!topicString.isEmpty()) {
                    ((MainActivity)getActivity()).unsubscribeToTopic(topicString);
                    dismiss();
                }
                else{
                    Toast toast = Toast.makeText(getContext(), "Topic field can't be empty ;)",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }
}
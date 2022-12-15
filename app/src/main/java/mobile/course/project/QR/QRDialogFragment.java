package mobile.course.project.QR;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;

public class QRDialogFragment extends DialogFragment {

    private ImageView qrCodeIV;
    private Bitmap bitmap;
    private QRGEncoder qrgEncoder;
    private SharedViewModel viewModel;

    public QRDialogFragment() {
        // Required empty public constructor
    }


    public static QRDialogFragment newInstance(QRDialogFragment qrDialogFragment) {
        QRDialogFragment fragment = new QRDialogFragment();
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

        return inflater.inflate(R.layout.fragment_qr_dialog, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        qrCodeIV = view.findViewById(R.id.idIVQrcode);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        int width = qrCodeIV.getMinimumWidth();
        int height = qrCodeIV.getMinimumHeight();
        System.out.println("min Width: "+width);
        System.out.println("min Height: "+height);
        // generating dimension from width and height.
        int dimen = Math.min(width, height);
        System.out.println(Converters.ListObjectToJsonString(viewModel.getList()));
        bitmap = createQR(Converters.ListObjectToJsonString(viewModel.getList()),200);
        qrCodeIV.setImageBitmap(bitmap);
    }

    public Bitmap createQR(String data,int dimension){
        qrgEncoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, dimension);
        qrgEncoder.setColorBlack(Color.BLACK);
        qrgEncoder.setColorWhite(Color.WHITE);
        try {
            bitmap = qrgEncoder.getBitmap();
            return bitmap;
        }catch(Exception e){
            System.out.print(e);
            return null;
        }
    }
    public Bitmap createQR(Bundle data,int dimension){
        qrgEncoder = new QRGEncoder(null, data, QRGContents.Type.CONTACT, dimension);
        qrgEncoder.setColorBlack(Color.RED);
        qrgEncoder.setColorWhite(Color.BLUE);
        try {
            bitmap = qrgEncoder.getBitmap();
            return bitmap;
        }catch(Exception e){
            System.out.print(e);
            return null;
        }
    }
}
package mobile.course.project.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import mobile.course.project.Fragments.AcceptDialogFragment;
import mobile.course.project.Fragments.AddDialogFragment;
import mobile.course.project.Fragments.ListFragment;
import mobile.course.project.Fragments.NoteFragment;
import mobile.course.project.Fragments.ProfileFragment;
import mobile.course.project.Fragments.SignUpFragment;
import mobile.course.project.Fragments.SignInFragment;
import mobile.course.project.Interfaces.DrawerLocker;
import mobile.course.project.QR.QRCodeScannerFragment;
import mobile.course.project.R;
import mobile.course.project.Utils.Constants;
import mobile.course.project.Utils.Converters;
import mobile.course.project.Utils.PreferenceManager;
import mobile.course.project.db.ShoppingList;
import mobile.course.project.Utils.MQTTHelper;
import mobile.course.project.Models.SharedViewModel;
import mobile.course.project.firebase.FcmNotificationsSender;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DrawerLocker {
    private SharedViewModel viewModel;
    private MQTTHelper mqtt;
    private DrawerLayout drawer;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);
        preferenceManager = new PreferenceManager(getApplicationContext());
        preferenceManager.clear();
         viewModel.setPreferenceManager(preferenceManager);
        setContentView(R.layout.activity_main);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            getSupportFragmentManager().beginTransaction()
                    //.add(R.id.fragment_b, new NoteFragment(), null)
                    .add(R.id.fragment_container, new ListFragment(), null)
                    .commit();

            setDrawerEnabled(true);
            loadUserDetails();
            getToken();
            navigationView.setCheckedItem(R.id.nav_myShoppingLists);
        }else {
            viewModel.deleteAllLists();
            getSupportFragmentManager().beginTransaction()
                    //.add(R.id.fragment_b, new NoteFragment(), null)
                    .add(R.id.fragment_container, new SignInFragment(), null)
                    .commit();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onBackPressed(){
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        }else{
            if(currentFragment instanceof ListFragment) {
                super.onBackPressed();
            }
            else if(currentFragment instanceof NoteFragment) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ListFragment()).commit();
            }
            else if(currentFragment instanceof SignUpFragment) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SignInFragment()).commit();
            }
        }
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.nav_myShoppingLists:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ListFragment()).commit();
                break;
            case R.id.nav_add_note_QR:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new QRCodeScannerFragment()).commit();
                break;
            case R.id.nav_add_note:
                AddDialogFragment addDialogFragment = AddDialogFragment.newInstance(new AddDialogFragment(){});
                addDialogFragment.show(getSupportFragmentManager(), "AddDialogFragment");
                break;
            case R.id.nav_logout:
                viewModel.deleteAllLists();
                SignOut();
                break;
            case R.id.nav_profile:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void connectingToMqttServer(Context context) {
        String clientID;
        if(viewModel.getMyID()!=null)clientID = viewModel.getMyID();
        else clientID = MqttClient.generateClientId();
        mqtt = new MQTTHelper(context, clientID);
        mqtt.connect();
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                viewModel.setConnectionStatus(true);
                viewModel.setMyID(clientID);
                Toast.makeText(getApplicationContext(), clientID,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void connectionLost(Throwable cause) {
                viewModel.setConnectionStatus(false);
                System.out.println("No connection with mqtt");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                viewModel.setMessage(message);
                AcceptDialogFragment acceptDialogFragment = AcceptDialogFragment.newInstance(new AcceptDialogFragment(){});
                acceptDialogFragment.show(getSupportFragmentManager(),"AcceptDialogFragment");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    };


    public void publishMessage(ShoppingList shoppingList, String[] topics){
        for(String topic : topics) {
            if(topic.isEmpty())continue;
            try {
                MqttMessage message = new MqttMessage();
                message.setQos(1);
                message.setPayload(("{id: " + shoppingList.getListId() + ", Title: " + jsonEscape(shoppingList.getListTitle()) + ", Text: " + jsonEscape(shoppingList.getListContent()) + "}").getBytes(StandardCharsets.UTF_8));
                mqtt.mqttAndroidClient.publish(topic, message);
                System.out.println(message + " Send to " + topic + " as " + message.getPayload());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static String jsonEscape(String str)  {
        return str.replace("\n","\\n").replace("\t","\\t").replace("\r","\\r");
    }

    public void subscribeToTopic(String topic){
        viewModel.addTopic(topic);
        System.out.println("Topics sub:"+viewModel.getTopics().toString());
        mqtt.subscribeToTopic(topic);
    }

    public void unsubscribeToTopic(String topic){
        viewModel.removeTopic(topic);
        System.out.println("Topics sub:"+viewModel.getTopics().toString());
        mqtt.mqttAndroidClient.unsubscribe(topic);
    }


    private void loadUserDetails(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        TextView text = header.findViewById(R.id.nameProfile);
        ImageView image = header.findViewById(R.id.imageProfile);
        text.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        image.setImageBitmap(bitmap);
    }

    private void ShowToast(String displayMessage){
        Toast.makeText(getApplicationContext(),displayMessage, Toast.LENGTH_SHORT).show();
    }

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
       // preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnSuccessListener(
                        unused -> ShowToast("Token updated successfully"))
                .addOnFailureListener(
                        e -> ShowToast("Unable to update token"));
    }

    private void SignOut(){
        ShowToast("Signing Out");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap <String,Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates).addOnSuccessListener(unused->{
            preferenceManager.clear();
            getSupportFragmentManager().beginTransaction()
                    //.add(R.id.fragment_b, new NoteFragment(), null)
                    .replace(R.id.fragment_container, new SignInFragment(), null)
                    .commit();
        }).addOnFailureListener(e->ShowToast("Unable to Sign Out."));
    }

    @Override
    public void setDrawerEnabled(boolean enabled) {
        int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        drawer.setDrawerLockMode(lockMode);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if(!enabled){
            toolbar.getMenu().clear();
            toolbar.setTitle(null);
            toolbar.getBackground().setAlpha(0);
        }else{
            toolbar.getBackground().setAlpha(255);
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.setDrawerIndicatorEnabled(enabled);
    }

    public void sendNotification(String title, String body) {
        ShoppingList myShoppingList = viewModel.getList();
        FirebaseFirestore db = FirebaseFirestore.getInstance();;
        ArrayList<String> currentListUsers = Converters.JsonStringToArrayList(myShoppingList.getListUsers());
        for (String listUser : currentListUsers) {
            if (!listUser.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                System.out.println("-->listuser: " + listUser + "\n-->userId: " + preferenceManager.getString(Constants.KEY_USER_ID));
                db.collection(Constants.KEY_COLLECTION_USERS).document(listUser).get().addOnSuccessListener(
                        userHashMap -> {
                            HashMap<String, Object> myUser = (HashMap<String, Object>) userHashMap.getData();
                            String usertoken = (String) myUser.get(Constants.KEY_FCM_TOKEN);
                            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(usertoken, title, body, getApplicationContext(), this);
                            notificationsSender.SendNotifications();
                        }
                );
            }
        }
    }
}
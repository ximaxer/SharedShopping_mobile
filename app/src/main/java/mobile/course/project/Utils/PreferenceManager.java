package mobile.course.project.Utils;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;
    // Constructor
    public PreferenceManager(Context context){
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    // Handle Boolean Values
    public void putBoolean(String key, Boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key,value);
        editor.apply();
    }
    public Boolean getBoolean(String key){
        return sharedPreferences.getBoolean(key,false);
    }

    // Handle String Values
    public void putString(String key, String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }
    public String getString(String key){
        return sharedPreferences.getString(key,null);
    }


    public ArrayList<String> getStringArray(){
        return Converters.JsonStringToArrayList(sharedPreferences.getString(Constants.KEY_MY_LISTS,null));
    }

    public void putStringArray(ArrayList<String> textList){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_MY_LISTS, Converters.ArrayListToJsonString(textList));
        editor.apply();
    }

    // Clear shared preferences
    public void clear(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}

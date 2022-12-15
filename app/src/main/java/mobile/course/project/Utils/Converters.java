package mobile.course.project.Utils;

import com.google.gson.Gson;

import java.util.ArrayList;

import mobile.course.project.db.ShoppingList;

public class Converters {
    public static String ArrayListToJsonString(ArrayList<String> list){
        Gson gson = new Gson();
        return gson.toJson(list);
    }
    public static ArrayList<String> JsonStringToArrayList(String listString){
        Gson gson = new Gson();
        return gson.fromJson(listString, ArrayList.class);
    }
    public static String ListObjectToJsonString(ShoppingList list){
        Gson gson = new Gson();
        return gson.toJson(list);
    }
    public static ShoppingList JsonStringToListObject(String jsonString){
        Gson gson = new Gson();
        return gson.fromJson(jsonString, ShoppingList.class);
    }
}

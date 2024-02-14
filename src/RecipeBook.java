import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class RecipeBook {

    private Map<String, Tuple<String, String>> recipeBook;

    public RecipeBook() {
        this.recipeBook = new HashMap<>();
    }

    public RecipeBook(JSONObject in) {
        this.recipeBook = new HashMap<>();
        for (String k : in.keySet()) {
            JSONArray tupJson = in.getJSONArray(k);
            this.recipeBook.put(k, new Tuple<String, String>(
                    (String) tupJson.get(0), (String) tupJson.get(1)));
        }
    }

    public void addEntry(String result, String ingredient1,
            String ingredient2) {
        Tuple<String, String> ingredients = new Tuple<>(ingredient1,
                ingredient2);

        this.recipeBook.put(result, ingredients);
    }

    public Tuple<String, String> getRecipe(String elem) {
        return this.recipeBook.get(elem);

    }

    public JSONObject exportJson() {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, Tuple<String, String>> e : this.recipeBook
                .entrySet()) {
            Tuple<String, String> t = e.getValue();
            String k = e.getKey();
            JSONArray elem = new JSONArray();
            elem.put(t.x);
            elem.put(t.y);

            obj.put(k, elem);
        }
        return obj;
    }

}

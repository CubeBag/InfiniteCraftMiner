import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebsiteBuilder {

    public WebsiteBuilder() {
        // TODO Auto-generated constructor stub
    }

    public static void createWebsite(String folderName, JSONArray knownElements,
            JSONObject recipes) {
        new File(folderName + "/recipes").mkdirs();

        try {
            BufferedWriter indexWriter = new BufferedWriter(
                    new FileWriter(folderName + "/index.html", false));
            indexWriter.append("<!DOCTYPE html>\n" + "<html><head>" + "<title>"
                    + "Neal.fun Infinite Craft Recipe Book" + "</title></head>"
                    + "<body>"
                    + "<h1>Neal.fun Infinite Craft Recipe Book</h1><hr/>"
                    + "<p>Currently serving " + knownElements.length()
                    + " recipes</p><hr/><br><p style=\"margin-left: 25px\">");

            List<String> elements = Main.knownJsonToList(knownElements);
            elements.sort(String.CASE_INSENSITIVE_ORDER);

            for (int i = 0; i < elements.size(); i++) {

                String elem = elements.get(i);

                if (recipes.has(elem)) {
                    BufferedWriter currentElementWriter = new BufferedWriter(
                            new FileWriter(folderName + "/recipes/" + i + "-"
                                    + elements.get(i).replace("/", "／")
                                    + ".html", false));
                    JSONArray combo = recipes.getJSONArray(elem);

                    indexWriter.append("<a href=\"recipes/" + i + "-"
                            + elem.replace("?", "%3F").replace("/", "／")
                                    .replace("%", "%25")
                            + ".html\">" + elem + "</a><br>");
                    currentElementWriter.append("<!DOCTYPE html>\n"
                            + "<html><head>" + "<title>Recipe " + elem
                            + "</title></head><body><h2>Recipe for " + elem
                            + ":</h2>" + "<br/><hr/>" + "<h3>Summary</h3><p>"
                            + combo.getString(0) + " + " + combo.getString(1)
                            + "</p><hr/><br><h3>Expanded Recipe</h3><br/><p style=\"font-family:monospace\"><pre>");
                    currentElementWriter.append(expandedRecipe(elem, recipes));

                    currentElementWriter.append("</pre></p></body></html>");
                    currentElementWriter.close();
                }

            }

            indexWriter.append("</p></body></html>");

            indexWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static String expandedRecipe(String elem, JSONObject recipes) {

        List<String> alreadyUsed = new ArrayList<>();
        return expandedRecipe(elem, recipes, alreadyUsed, 0);

    }

    private static String expandedRecipe(String elem, JSONObject recipes,
            List<String> alreadyUsed, int depth) {
        StringBuilder depthSpacesB = new StringBuilder();

        for (int j = 0; j < depth; j++) {
            depthSpacesB.append("| ");
        }

        String depthSpaces = depthSpacesB.toString();
        String ret = depthSpaces + elem;
        if (alreadyUsed.contains(elem)) {
            return ret;
        } else {
            alreadyUsed.add(elem);
        }
        if (recipes.has(elem)) {
            JSONArray combo = recipes.getJSONArray(elem);

            ret += "\n" + expandedRecipe(combo.getString(0), recipes,
                    alreadyUsed, depth + 1);

            ret += "\n" + expandedRecipe(combo.getString(1), recipes,
                    alreadyUsed, depth + 1);

        } else {
            //ret += "\n" + depthSpaces + " " + elem + " is a primitive.";
        }
        return ret;

    }

}

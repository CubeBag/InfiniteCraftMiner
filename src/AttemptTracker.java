import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

public class AttemptTracker {

    List<Tuple<String, String>> pastAttempts;

    public AttemptTracker() {
        this.pastAttempts = new ArrayList<>();
    }

    public AttemptTracker(JSONArray in) {
        this.pastAttempts = new ArrayList<>();
        int inLen = in.length();
        for (int i = 0; i < inLen; i++) {
            JSONArray att = (JSONArray) in.get(i);
            this.pastAttempts.add(new Tuple<String, String>((String) att.get(0),
                    (String) att.get(1)));
        }
    }

    public boolean isNewCombination(String a, String b) {

        boolean ret = true;

        if (a.compareTo(b) > 0) {
            String c = b;
            b = a;
            a = c;
        }

        Tuple<String, String> newTuple = new Tuple<>(a, b);

        if (!this.pastAttempts.contains(newTuple)) {
            this.pastAttempts.add(newTuple);
        } else {
            ret = false;
        }

        return ret;
    }

    public JSONArray exportJson() {
        JSONArray obj = new JSONArray();
        for (Tuple<String, String> t : this.pastAttempts) {
            JSONArray elem = new JSONArray();
            elem.put(t.x);
            elem.put(t.y);
            obj.put(elem);
        }
        return obj;
    }

}

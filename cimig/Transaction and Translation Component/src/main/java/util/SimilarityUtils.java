package util;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimilarityUtils {
    public static Map<String, Double> textToMap(List<String> textLines) {
        Map<String, Double> map = new HashMap<>();
        for (String text : textLines) {
            if (text.startsWith("#")){
                continue;
            }
            //Split the text by spaces
            String[] words = text.split(" ");
            //For each word
            for (String word : words) {
                //Trim the word and make it lowercase
                word = word.trim().toLowerCase();
                //If the word is not empty
                if (word.equals(":")) {
                    continue;
                }
                word = word.replace("-", "").replace("\n", "").replace("'","").replace("\"","");
                if (!word.isEmpty()) {
                    //Get the current frequency of the word or 0 if not present
                    double freq = map.getOrDefault(word, 0.0);
                    //Increment the frequency by 1
                    freq++;
                    //Put the word and frequency back to the map
                    map.put(word, freq);
                }
            }
        }
//        System.out.println(map);
        return map;
    }

    public static double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        //Get the common keys of both maps
        Set<String> both = Sets.newHashSet(v1.keySet());
        both.retainAll(v2.keySet());
        //Initialize the dot product, norm1 and norm2
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        //For each common key
        for (String k : both) {
            //Get the values from both maps
            double val1 = v1.get(k);
            double val2 = v2.get(k);
            //Add to the dot product
            dotProduct += val1 * val2;
        }
        //For each key in v1
        for (String k : v1.keySet()) {
            //Get the value and add to norm1
            double val = v1.get(k);
            norm1 += val * val;
        }
        //For each key in v2
        for (String k : v2.keySet()) {
            //Get the value and add to norm2
            double val = v2.get(k);
            norm2 += val * val;
        }
        //Return the cosine similarity or 2.0 if invalid
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 2.0;
        } else {
            return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        }
    }
}

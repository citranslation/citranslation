package gamigration.travis2ga.translation_engine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;

public class TarFix {
    public static void main(String[] args) {
        HashSet<TarRule> tar_github_rules = new HashSet<>();
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        try {
            FileWriter myWriter = new FileWriter("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-travis-v2-cleaned.jsonl");
            BufferedReader br = new BufferedReader(new FileReader("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-travis-v2.jsonl"));
            String line = "";
            br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.contains("OBJ-NODE")) {
                    try {
                        JSONObject TAR = new JSONObject(line);
                        cleanAndOutput(TAR, myWriter);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else{
                    myWriter.write(line +"\n");
                }
            }
            myWriter.flush();
            myWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanAndOutput(JSONObject tar, FileWriter myWriter) throws Exception {

        if (tar.getString("type").equals("OBJ-NODE")) {
            tar.put("type", "travis-file");
//            System.out.println(tar);
        }
            System.out.println(tar);
            JSONObject cleanedTar = cleanTAR(tar);
            System.out.println(cleanedTar);
            if(cleanedTar.toString().contains("OBJ-NODE")){
                System.exit(1);
                System.out.println("ERROR");
            }
            myWriter.write(cleanedTar +"\n");

//            System.out.println("Cleaned");

    }

    private static JSONObject cleanTAR(JSONObject tar) {
        JSONArray children = tar.getJSONArray("children");
        if (children.length() == 0) {
            return tar;
        }
        JSONArray new_children = new JSONArray();
        for (int index = 0; index < children.length(); index++) {
            JSONObject child_json = children.getJSONObject(index);
            if (child_json.getString("type").equals("OBJ-NODE")) {
                JSONArray children_of_obj_node = child_json.getJSONArray("children");
                for (int index_k = 0; index_k < children_of_obj_node.length(); index_k++) {
                    JSONObject child_child_json = children_of_obj_node.getJSONObject(index_k);
                    new_children.put(cleanTAR(child_child_json));
                }
            } else {
                new_children.put(cleanTAR(child_json));
            }
        }
        tar.put("children", new_children);
        return tar;
    }

}

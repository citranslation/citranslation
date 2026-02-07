package gamigration.travis2ga.translation_engine;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import util.TreeUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class TarRule implements Serializable {
    transient JSONObject node;

    public TarRule(JSONObject node) {
        this.node = node;
    }

    private JSONObject find_injection_point(JSONObject root) {
        JSONObject injectionPoint;
        String type = node.getString("type");
        if (node.getString("type").equals("github-file")) {
            if (!is_partial_match(root)) {
                return null;
            }
            return root;
        } else {
            Vector<JSONObject> injectionsPoints = TreeUtils.BFSAllWithType(root, type);
            for (JSONObject ip : injectionsPoints) {
                // find first Node that's a partial match
                if (is_partial_match(ip)) {
                    return ip;
                }
            }
        }
        return null;
    }


    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(node.toString());
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        JSONObject node = new JSONObject((String) ois.readObject());
        this.node = node;
    }

    public boolean IsInJSON(JSONObject travisJSON) {
        if(node.get("type").toString().equals("travis-file")){
            if(is_full_match_ignore_root(travisJSON)){
                return true;
            }
        }
        Vector<JSONObject> matchingNodes = TreeUtils.findAllWithType(travisJSON, node.getString("type"));
        for (JSONObject node : matchingNodes) {
            if (is_full_match(node)) {
                return true;
            }
        }
        return false;
    }

    private boolean is_full_match_ignore_root(JSONObject root) {
        Vector<Pair<String, JSONObject>> children_vector = new Vector<>();
        JSONArray arrayChildrenCurrentTree = root.getJSONArray("children");
        JSONArray arrayChildrenTreeToMatch = this.node.getJSONArray("children");
        if (arrayChildrenTreeToMatch.length() > arrayChildrenCurrentTree.length()) {
            return false;
        }
        // collect children and their values in a hashmap
        for (Object child : arrayChildrenCurrentTree) {
            JSONObject tempObject = (JSONObject) child;
            if (tempObject.getString("type").equals("OBJ-NODE")) {
                JSONArray arrayChildrenTemp = root.getJSONArray("children");
                for (Object child_temp : arrayChildrenTemp) {
                    JSONObject child_json_temp = (JSONObject) child_temp;
                    children_vector.add(new ImmutablePair<>(child_json_temp.getString("type"), child_json_temp));

                }
            }
            children_vector.add(new ImmutablePair<>(tempObject.getString("type"), tempObject));
        }
        int nb_children_matched = 0;
        for (Object child : arrayChildrenTreeToMatch) {
            JSONObject tempObject = (JSONObject) child;
            String childType = tempObject.getString("type");
            for (Pair<String, JSONObject> type_child_pair : children_vector) {
                if (type_child_pair.getLeft().equals(childType) && FindSubTree(type_child_pair.getRight(), tempObject)) {
                    nb_children_matched++;
                }
            }
        }
        return nb_children_matched == (arrayChildrenTreeToMatch.length());
    }

    public boolean is_full_match(JSONObject root) {
        if (node.getString("type").equals(root.get("type"))) {
            Vector<Pair<String, JSONObject>> children_vector = new Vector<>();
            JSONArray arrayChildrenCurrentTree = root.getJSONArray("children");
            JSONArray arrayChildrenTreeToMatch = this.node.getJSONArray("children");
            if (arrayChildrenTreeToMatch.length() > arrayChildrenCurrentTree.length()) {

                return false;
            }
            // collect children and their values in a hashmap
            for (Object child : arrayChildrenCurrentTree) {
                JSONObject tempObject = (JSONObject) child;
                if (tempObject.getString("type").equals("OBJ-NODE")) {
                    JSONArray arrayChildrenTemp = root.getJSONArray("children");
                    for (Object child_temp : arrayChildrenTemp) {
                        JSONObject child_json_temp = (JSONObject) child_temp;
                        children_vector.add(new ImmutablePair<>(child_json_temp.getString("type"), child_json_temp));

                    }
                }
                children_vector.add(new ImmutablePair<>(tempObject.getString("type"), tempObject));
            }
            int nb_children_matched = 0;
            for (Object child : arrayChildrenTreeToMatch) {
                JSONObject tempObject = (JSONObject) child;
                String childType = tempObject.getString("type");
                for (Pair<String, JSONObject> type_child_pair : children_vector) {
                    if (type_child_pair.getLeft().equals(childType) && FindSubTree(type_child_pair.getRight(), tempObject)) {
                        nb_children_matched++;
                    }
                }
            }
            return nb_children_matched == (arrayChildrenTreeToMatch.length());
        } else {
            return false;
        }

    }

    public boolean is_partial_match(JSONObject root, double match_percentage) {
        Vector<Pair<String, JSONObject>> children_vector = new Vector<>();
        JSONArray arrayChildrenCurrentTree = root.getJSONArray("children");
        JSONArray arrayChildrenTreeToMatch = this.node.getJSONArray("children");
        if (arrayChildrenTreeToMatch.length() * match_percentage > arrayChildrenCurrentTree.length()) {
            return false;
        }
        // collect children and their values in a hashmap
        for (Object child : arrayChildrenCurrentTree) {
            JSONObject tempObject = (JSONObject) child;
            if (tempObject.getString("type").equals("OBJ-NODE")) {
                JSONArray arrayChildrenTemp = root.getJSONArray("children");
                for (Object child_temp : arrayChildrenTemp) {
                    JSONObject child_json_temp = (JSONObject) child_temp;
                    children_vector.add(new ImmutablePair<>(child_json_temp.getString("type"), child_json_temp));

                }
            }
            children_vector.add(new ImmutablePair<>(tempObject.getString("type"), tempObject));
        }
        int nb_children_matched = 0;
        for (Object child : arrayChildrenTreeToMatch) {
            JSONObject tempObject = (JSONObject) child;
            String childType = tempObject.getString("type");
            for (Pair<String, JSONObject> type_child_pair : children_vector) {
                if (type_child_pair.getLeft().equals(childType) && FindSubTree(type_child_pair.getRight(), tempObject)) {
                    nb_children_matched++;
                }
            }
        }
        return nb_children_matched >= (arrayChildrenTreeToMatch.length() * match_percentage);

    }



    private boolean is_partial_match(JSONObject root) {
        double partial = 0.5;
        Vector<Pair<String, JSONObject>> children_vector = new Vector<>();
        JSONArray arrayChildrenCurrentTree = root.getJSONArray("children");
        JSONArray arrayChildrenTreeToMatch = this.node.getJSONArray("children");
        if (arrayChildrenTreeToMatch.length() * partial > arrayChildrenCurrentTree.length()) {
            return false;
        }
        // collect children and their values in a hashmap
        for (Object child : arrayChildrenCurrentTree) {
            JSONObject tempObject = (JSONObject) child;
            if (tempObject.getString("type").equals("OBJ-NODE")) {
                JSONArray arrayChildrenTemp = root.getJSONArray("children");
                for (Object child_temp : arrayChildrenTemp) {
                    JSONObject child_json_temp = (JSONObject) child_temp;
                    children_vector.add(new ImmutablePair<>(child_json_temp.getString("type"), child_json_temp));

                }
            }
            children_vector.add(new ImmutablePair<>(tempObject.getString("type"), tempObject));
        }
        int nb_children_matched = 0;
        for (Object child : arrayChildrenTreeToMatch) {
            JSONObject tempObject = (JSONObject) child;
            String childType = tempObject.getString("type");
            for (Pair<String, JSONObject> type_child_pair : children_vector) {
                if (type_child_pair.getLeft().equals(childType) && FindSubTree(type_child_pair.getRight(), tempObject)) {
                    nb_children_matched++;
                }
            }
        }
        return nb_children_matched >= (arrayChildrenTreeToMatch.length() * partial);

    }

    private boolean FindSubTree(JSONObject root, JSONObject treeToMatch) {
        if (root == null && treeToMatch == null) {
            return true;
        }
        if (root == null || treeToMatch == null) {
            return false;
        }
        JSONArray arrrayOfChildrenofCurrentTree = root.getJSONArray("children");
        JSONArray arrrayOfChildrenofTreeToMatch = treeToMatch.getJSONArray("children");
        if (arrrayOfChildrenofCurrentTree.length() == 0) {
            if (arrrayOfChildrenofTreeToMatch.length() == 0) {
                return Objects.equals(treeToMatch.get("type").toString(), root.get("type").toString());
            }
        } else {
            if (arrrayOfChildrenofTreeToMatch.length() != 0) {
                HashMap<String, JSONObject> children_map = new HashMap<>();
                // collect children and their values in a hashmap
                for (Object child : arrrayOfChildrenofTreeToMatch) {
                    JSONObject tempObject = (JSONObject) child;
                    children_map.put(tempObject.get("type").toString(), tempObject);
                }

                for (Object child : arrrayOfChildrenofCurrentTree) {
                    JSONObject tempObject = (JSONObject) child;
                    String childType = tempObject.get("type").toString();
                    if (children_map.containsKey(childType)) {
                        if (!FindSubTree(children_map.get(childType), tempObject)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;

            } else {
                return false;
            }
        }
        return false;

    }


    public boolean applyTar(JSONObject root) {
        JSONObject injectionPoint;
        injectionPoint = this.find_injection_point(root);
        if (injectionPoint == null) {
            return false;
        }
        JSONObject injectionPoint_copy = new JSONObject(injectionPoint, JSONObject.getNames(injectionPoint));

//        System.out.println(" match");
//        System.out.println(" Before TAR");
//        System.out.println(root);
        TreeUtils.insertMissingChildren(injectionPoint, node);
        TreeUtils.BFSandReplaceWithNode(root, injectionPoint_copy, injectionPoint);
//        System.out.println("After TAR");
//        System.out.println(root);
//        System.out.println("OKK");
        return true;
    }

    private Collection<? extends Pair<JSONObject, JSONObject>> extract_pairs(Set<JSONObject> ruleChildren, Set<JSONObject> nodeChildren) {
        Set<Pair<JSONObject, JSONObject>> temp_node_Pairs = new HashSet<>();
        for (JSONObject rule_child : ruleChildren) {
            for (JSONObject node_child : nodeChildren) {
                if (rule_child.getString("type").equals(node_child.getString("type"))) {
                    temp_node_Pairs.add(new MutablePair<>(rule_child, node_child));

                }
            }
        }
//        System.out.println(temp_node_Pairs.size());
        return temp_node_Pairs;
    }
}

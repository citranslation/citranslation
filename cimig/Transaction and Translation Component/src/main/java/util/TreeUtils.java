package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import gamigration.travis2ga.translation_engine.AprioriRule;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

@SuppressWarnings("VulnerableCodeUsages")
public class TreeUtils {
    public static List<String> getLeaves(Tree root) {
        List<String> leaves = new ArrayList<String>();
        for (Tree node : root.breadthFirst()) {
            if (isLeaf(node)) {
                leaves.add(node.getLabel());
            }
        }
        return leaves;
    }

    public static boolean isNodeParentJobs(Tree node) {
        Tree temp = node.getParent();
        if (temp == null) return false;
        temp = node.getParent();
        if (temp == null) return false;
        temp = node.getParent();
        if (temp == null) return false;
        temp = temp.getChild(0);
        if (temp == null) return false;
        return temp.getLabel().replaceAll("^\"|\"$", "").equals("jobs");
    }

    public static boolean isNodeBranchName(Tree node) {
        Tree temp = node.getParent();
        if (temp == null || !temp.getType().equals(TypeSet.type("STRING"))) return false;
        temp = temp.getParent();
        if (temp == null || !temp.getType().equals(TypeSet.type("ARRAY"))) return false;
        temp = temp.getParent();
        if (temp == null || !temp.getType().equals(TypeSet.type("FIELD"))) return false;
        temp = temp.getChild(0);
        if (temp == null || !temp.hasLabel()) return false;
        return temp.getLabel().equals("branches");
    }

    public static JSONObject treeToJson(Tree node) {
        if (node.getType().equals(TypeSet.type("OBJECT")) || node.getType().equals(TypeSet.type("ARRAY"))) {
            List<JSONObject> temp = new ArrayList<>();
            for (Tree child : node.getChildren()) {
                if (!child.getType().equals(TypeSet.type("NULL"))) {
                    temp.add(treeToJson(child));
                }
            }
            JSONObject retObject = new JSONObject();
            if (node.getType().equals(TypeSet.type("OBJECT"))) {
                if (!retObject.keySet().contains("value"))
                    retObject.put("type", "OBJ-NODE");
            }
            return retObject.put("children", temp);
        }
        if (node.getType().equals(TypeSet.type("FIELD"))) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("type", node.getChild(0).getLabel());
            if (node.getChild(1).getType().equals(TypeSet.type("STRING")) || node.getChild(1).getType().equals(TypeSet.type("NUMBER"))) {
                List<JSONObject> temp = new ArrayList<>();
                temp.add(new JSONObject().put("type", node.getChild(1).getChild(0).getLabel()).put("children", new ArrayList<>()));
                tempJson.put("children", temp);
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("NULL"))) {
                tempJson.put("children", new ArrayList<>());
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("ARRAY")) || node.getChild(1).getType().equals(TypeSet.type("OBJECT"))) {
                List<JSONObject> temp = new ArrayList<>();
                for (Tree child : node.getChild(1).getChildren()) {
                    if (!child.getType().equals(TypeSet.type("NULL"))) {
                        temp.add(treeToJson(child));
                    }
                }
                tempJson.put("children", temp);
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("TRUE")) || node.getChild(1).getType().equals(TypeSet.type("FALSE"))) {
                List<JSONObject> temp = new ArrayList<>();
                temp.add(new JSONObject().put("type", node.getChild(1).getType()).put("children", new ArrayList<>()));
                return tempJson.put("children", temp);
            } else {
                //this part should never be reached as the above if statements should cover all the cases
                //as such it will return null (we could have raised an exception instead)
//                System.out.println("inside if: " + node.getType());
//                System.out.println("inside if child type: " + node.getChild(1).getType());
                return null;
            }
        }
        if (node.getType().equals(TypeSet.type("STRING")) || node.getType().equals(TypeSet.type("NUMBER"))) {
            return new JSONObject().put("type", node.getChild(0).getLabel()).put("children", new ArrayList<>());

        }
        if ((node.getType().equals(TypeSet.type("TRUE")) || node.getType().equals(TypeSet.type("FALSE")))) {
            return new JSONObject().put("type", node.getType()).put("children", new ArrayList<>());
        }
        if (node.getType().equals(TypeSet.type("String"))) {
            return new JSONObject().put("type", node.getLabel()).put("children", new ArrayList<>());
        }
        if (node.getType().equals(TypeSet.type("NULL"))) {
            return null;
        }
//        System.out.println(node.getType());
        return null;
    }

    public static boolean isLeaf(Tree node) {
        if (node.isLeaf() &&
                (node.getParent().getType().equals(TypeSet.type("STRING")) || node.getParent().getType().equals(TypeSet.type("NUMBER"))
                ))
            return true;
        else return false;
    }

    public static String getParentsURI(Tree targetNode) {
        StringBuilder sb = new StringBuilder();
        for (Tree node : targetNode.getParents()) {
            if (node.getType().equals(TypeSet.type("FIELD")) || node.getType().equals(TypeSet.type("ARRAY")) && node.hasLabel()) {
                String temp = node.getChild(0).getLabel();
                sb.insert(0, temp.substring(1, temp.length() - 1) + ".");
            }
        }
        return sb.toString();
    }

    public static String appendChildToParentURI(Tree leafNode, String parents) {
        String normalizedLeafNodeLabel = leafNode.getLabel().replaceAll("^\"|\"$", "");
        StringBuilder sb = new StringBuilder(parents);
        // default case final parent is an object or field
        sb.append('[');
        sb.append(normalizedLeafNodeLabel);
        sb.append(']');

        return sb.toString();
    }

    public static List<String> generateAllSequences(Tree targetTree) {
        List<String> sequences = new ArrayList<String>();
        for (Tree node : targetTree.postOrder()) { // very important to be post order to get leafs in order
            if (node.isLeaf() && node.hasLabel() && !node.getParent().getType().equals(TypeSet.type("FIELD"))) {
                sequences.add(appendChildToParentURI(node, getParentsURI(node)));
            }

        }
        return sequences;
    }

    public static boolean isLeaf(JSONObject node) {
        try {
            node.get("children");
        } catch (Exception e) {
            return true;
        }
        return node.getJSONArray("children").isEmpty();
    }

    public static boolean isLeaf(JsonNode node) {
        if (!node.get("children").isArray()) {
            return false;
        }
        return !node.get("children").elements().hasNext();
    }

    public static void traverseTree(JSONObject node, Set<JSONObject> setOfNodes) {
        if (isLeaf(node)) {
            setOfNodes.add(node);
            return;
        } else {
            node.getJSONArray("children").forEach(elem -> {
                if (!((JSONObject) elem).isEmpty()) {
                    setOfNodes.add((JSONObject) elem);
                    traverseTree((JSONObject) elem, setOfNodes);
                }
            });
        }
    }

    public static void traverseTree(JsonNode node, Set<JsonNode> setOfNodes) {
        if (isLeaf(node)) {
            setOfNodes.add(node);
            return;
        } else {
            node.get("children").elements().forEachRemaining(elem -> {
                if (!elem.isEmpty()) {
                    setOfNodes.add(elem);
                    traverseTree(elem, setOfNodes);
                }
            });
        }
    }

    public static int getNodeLevel(JsonNode root, JsonNode node) {
        if (node.equals(root)) return 1;
        Iterator<JsonNode> iter = root.get("children").elements();
        while (iter.hasNext()) {
            int nodeLevel = getNodeLevel(node, iter.next());
            if (nodeLevel != 0) return 1 + nodeLevel;
        }
        return 0;
    }

    public static void pruneLevel3Node_alt(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            if (!isNodeLevel2(jsn)) {
                pruneLevel2Node(jsn);
            }
            childrenOfParent.add(jsn);
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);
    }

    public static void pruneLevel3Node(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            if (isNodeLevel2(jsn)) {
                childrenOfParent.add(jsn);
            }
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);

    }

    public static boolean isNodeLevel3(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        while (iter.hasNext()) {
            if (isNodeLevel2(iter.next())) return true;
        }
        return false;
    }

    public static boolean isNodeLevel2(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            if (isLeaf(jsn)) return true;
        }
        return false;
    }

    public static void pruneLevel2Node_alt(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            if (isLeaf(jsn)) {
                childrenOfParent.add(jsn);
            }
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);
    }

    public static void pruneLevel2Node(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            if (!isLeaf(jsn)) {
                ((ObjectNode) jsn).putArray("children");
            }
            childrenOfParent.add(jsn);
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);
    }

    public static boolean isNodeNMinus1(JsonNode root, JsonNode node) {
        Iterator<JsonNode> iter = root.get("children").elements();
        while (iter.hasNext()) {
            if (node.equals(iter.next())) return true;
        }
        return false;
    }

    public static void pruneN1NodeToDepth2(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            ((ObjectNode) jsn).putArray("children");
            childrenOfParent.add(jsn);
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);
    }

    public static void pruneN1NodeToDepth3(JsonNode node) {
        Iterator<JsonNode> iter = node.get("children").elements();
        List<JsonNode> childrenOfParent = new ArrayList<>();
        while (iter.hasNext()) {
            JsonNode jsn = iter.next();
            pruneN1NodeToDepth2(jsn);
            childrenOfParent.add(jsn);
        }
        ((ObjectNode) node).putArray("children").addAll(childrenOfParent);
    }

    public static String cleanJsonVal(String val) {
        if (val.startsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        if (val.endsWith("\"")) {
            val = val.substring(0, val.length() - 2);
        }
        return val;
    }

    static Queue<JSONObject> TravisNodes = new LinkedList<>();
    static Queue<JSONObject> GithubNodes = new LinkedList<>();
    static Queue<JSONObject> GithubNodes_for_parent = new LinkedList<>();
    static Queue<JSONObject> TempBFSNodes = new LinkedList<>();

    public static void resetQueues(){
        TravisNodes.clear();
        GithubNodes.clear();
        GithubNodes_for_parent.clear();
        TempBFSNodes.clear();
        mapper = new ObjectMapper();
    }

    static int type_index = 0;
    static int max_index = 0;

    public static boolean DFSandInsertLastWithType(JSONObject root, String type, JSONObject gh_node) {
//        System.out.println("DFS");
//        System.out.println(type);
//        System.out.println(gh_node);
        Map<JSONObject, Integer> MatchingTravisNodes = new HashMap<>();
        DFSandInsertLastWithTypeHelper(root, type, gh_node, 0, MatchingTravisNodes);
        if (MatchingTravisNodes.size() != 0) {
            JSONObject MaxNode = null;
            int max = -1;
            for (JSONObject node : MatchingTravisNodes.keySet()) {
                if (MatchingTravisNodes.get(node) > max) {
                    max = MatchingTravisNodes.get(node);
                    MaxNode = node;
                }
            }
//            JSONObject maxNode_copy = new JSONObject(MaxNode, JSONObject.getNames(MaxNode));
            JSONObject maxNode_copy = new JSONObject(MaxNode.toMap());
            JSONArray arr = (JSONArray) MaxNode.get("children");
            arr.put(gh_node);
            MaxNode.put("children", arr);
            if (!MaxNode.get("type").toString().equals("github-file"))
                BFSandReplaceWithNode(root, maxNode_copy, MaxNode);
            return true;
        }
        return false;
    }

    private static void DFSandInsertLastWithTypeHelper(JSONObject root, String type, JSONObject gh_node, int depth, Map<JSONObject, Integer> MatchingTravisNodes) {
//        System.out.println("DFSH");
//        System.out.println(type);
        if (root == null)
            return;
        if (!root.toString().contains(type)) {
            return;
        }
        if (cleanJsonVal((String) root.get("type")).equals(type)) {
            MatchingTravisNodes.put(root, depth);
        } else {
            for (Object child_node : (JSONArray) root.get("children")) {
                if (child_node != null && child_node.toString().contains(type))
                    DFSandInsertLastWithTypeHelper((JSONObject) child_node, type, gh_node, depth + 1, MatchingTravisNodes);
            }
        }

    }

    public static void BFSandInsertWithType(JSONObject root, String type, int depth, JSONObject gh_node) {
        // depth not currently used, to be implemented
        if (root == null)
            return;
        if (!root.toString().contains(type)) {
            return;
        }

        String type_temp = type;
        if (type.contains(";")) {
            if (max_index == 0) {
                max_index = type.split(";").length - 1;
            }
            type_temp = type.split(";")[type_index];
        }
        if (cleanJsonVal((String) root.get("type")).equals(type_temp)) {
            if (type_index == max_index) {
                JSONArray arr = (JSONArray) root.get("children");
                if (arr.length() == 0) {
                    arr = new JSONArray();
                }
                arr.put(arr.length(), gh_node);
                max_index = 0;
                type_index = 0;
                TravisNodes.clear();
            } else {
                type_index += 1;
            }
        }
        for (Object child_node : (JSONArray) root.get("children")) {
            if (child_node != null && child_node.toString().contains(type))
                TravisNodes.add((JSONObject) child_node);
        }
        while (!TravisNodes.isEmpty()) {
            JSONObject currNode = TravisNodes.remove();
            BFSandInsertWithType(currNode, type, depth + 1, gh_node);
        }
    }

    public static JSONObject convertJsonNodeToJSONObject(JsonNode githubNode) {
        String json = githubNode.toString();
        JSONObject jo = new JSONObject(json);
        return jo;
    }


    public static void addChildToNode(Tree node, Tree convertJsonNodeToTree) {

    }

    public static Tree findNodeInTree(Tree node, Tree original) {

        for (Tree temp_n : original.preOrder()) {
            if (temp_n.getPos() == node.getPos() && temp_n.getLength() == node.getLength() && temp_n.getLabel().equals(node.getLabel())) {
                return temp_n;
            }
        }
        return null;
    }

    public static Tree findParentOfNodeInTree(Tree node) {
        Tree firstParent = node.getParent();
        while (firstParent != null) {
            for (Tree child : firstParent.getChildren()) {
                if (child == node) {
                    continue;
                }
                if (!Objects.equals(child.getLabel(), "")) {
                    return child;
                }
            }
            firstParent = firstParent.getParent();
        }
        return firstParent;
    }

    public static void transferParameters(JSONObject travisNode, JSONObject githubNode, HashMap<Tree, Tree> normalizationMap, HashMap<JSONObject, Tree> transformationMap) {
        Vector<JSONObject> leaves = new Vector<>();
        if (isNodeLevel2(travisNode)) {
            Iterator<Object> iter = travisNode.getJSONArray("children").iterator();
            for (Iterator<Object> it = iter; it.hasNext(); ) {
                JSONObject tmp = (JSONObject) it.next();
                leaves.add(tmp);
            }
        }
        for (JSONObject tmp_travis_leaf : leaves) {
            Tree normalized_node = transformationMap.get(tmp_travis_leaf);

            Tree original_node = normalizationMap.get(normalized_node);
            if (original_node == null) {
                continue;
            }
            Tree ParentNode = findParentOfNodeInTree(original_node);
            JSONObject original_json = treeToJson(original_node);
            if (githubNode.has("children")) {
                for (Object tmp_github_leaf : githubNode.getJSONArray("children")) {
                    JSONObject tmp_jo_github_leaf = (JSONObject) tmp_github_leaf;
                    if (((String) tmp_jo_github_leaf.get("type")).equals((String) tmp_travis_leaf.get("type"))) {
                        tmp_jo_github_leaf.put("type", cleanJsonVal((String) original_json.get("type")));
                    }
                }
            }
        }
        return;
    }

    public static String transferParametersAndGetParentSet(JSONObject travisNode, JSONObject githubNode, HashMap<Tree, Tree> normalizationMap, HashMap<JSONObject, Tree> transformationMap) {
        HashMap<String, Integer> parentsCount = new HashMap<>();
        Vector<JSONObject> leaves = new Vector<>();
        if (isNodeLevel2(travisNode)) {
            Iterator<Object> iter = travisNode.getJSONArray("children").iterator();
            for (Iterator<Object> it = iter; it.hasNext(); ) {
                JSONObject tmp = (JSONObject) it.next();
                leaves.add(tmp);
            }
        }
        for (JSONObject tmp_travis_leaf : leaves) {
            Tree normalized_node = transformationMap.get(tmp_travis_leaf);
            Tree original_node = normalizationMap.get(normalized_node);
            if (original_node == null) {
                continue;
            }
            Tree ParentNode = findParentOfNodeInTree(original_node);
            String ParentType = ParentNode.getLabel();
            int old_val = 0;
            if (parentsCount.containsKey(ParentType)) {
                old_val = parentsCount.get(ParentType);
            }
            parentsCount.put(ParentType, old_val + 1);
            JSONObject original_json = treeToJson(original_node);
            if (githubNode.has("children")) {
                for (Object tmp_github_leaf : githubNode.getJSONArray("children")) {
                    JSONObject tmp_jo_github_leaf = (JSONObject) tmp_github_leaf;
                    if (((String) tmp_jo_github_leaf.get("type")).equals((String) tmp_travis_leaf.get("type"))) {
                        if (original_json != null)
                            tmp_jo_github_leaf.put("type", cleanJsonVal((String) original_json.get("type")));
                    }
                }
            }
        }
        if (parentsCount.values().size() == 0) {
            return "";
        }
        int max = Collections.max(parentsCount.values());
        String max_parent = "";
        for (Map.Entry<String, Integer> temp_parent : parentsCount.entrySet()) {
            if (temp_parent.getValue() == max) {
                max_parent = temp_parent.getKey();
            }
        }
        return max_parent.replace("\"", "");
    }

    public static JSONObject treeToJsonWithMap(Tree node, HashMap<JSONObject, Tree> transformationMap) {
        if (node.getType().equals(TypeSet.type("OBJECT")) || node.getType().equals(TypeSet.type("ARRAY"))) {
            List<JSONObject> temp = new ArrayList<>();
            for (Tree child : node.getChildren()) {
                if (!child.getType().equals(TypeSet.type("NULL"))) {
                    JSONObject temp_json = treeToJsonWithMap(child, transformationMap);
//                    transformationMap.put(temp_json,child);
                    temp.add(temp_json);
                }
            }
            JSONObject retObject = new JSONObject();
            if (node.getType().equals(TypeSet.type("OBJECT"))) {
                if (!retObject.keySet().contains("value"))
                    retObject.put("type", "OBJ-NODE");
            }
            return retObject.put("children", temp);
        }
        if (node.getType().equals(TypeSet.type("FIELD"))) {
            JSONObject tempJson = new JSONObject();
            tempJson.put("type", node.getChild(0).getLabel());
            if (node.getChild(1).getType().equals(TypeSet.type("STRING")) || node.getChild(1).getType().equals(TypeSet.type("NUMBER"))) {
                List<JSONObject> temp = new ArrayList<>();

                JSONObject temp_json = new JSONObject().put("type", node.getChild(1).getChild(0).getLabel()).put("children", new ArrayList<>());
                transformationMap.put(temp_json, node.getChild(1).getChild(0));

                temp.add(temp_json);

                tempJson.put("children", temp);
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("NULL"))) {
                tempJson.put("children", new ArrayList<>());
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("ARRAY")) || node.getChild(1).getType().equals(TypeSet.type("OBJECT"))) {
                List<JSONObject> temp = new ArrayList<>();
                for (Tree child : node.getChild(1).getChildren()) {
                    if (!child.getType().equals(TypeSet.type("NULL"))) {

                        temp.add(treeToJsonWithMap(child, transformationMap));

                    }
                }
                tempJson.put("children", temp);
                return tempJson;
            } else if (node.getChild(1).getType().equals(TypeSet.type("TRUE")) || node.getChild(1).getType().equals(TypeSet.type("FALSE"))) {

                List<JSONObject> temp = new ArrayList<>();
                JSONObject temp_json = new JSONObject().put("type", node.getChild(1).getType()).put("children", new ArrayList<>());
                transformationMap.put(temp_json, node.getChild(1));
                temp.add(temp_json);
                return tempJson.put("children", temp);
            } else {
                //this part should never be reached as the above if statements should cover all the cases
                //as such it will return null (we could have raised an exception instead)
//                System.out.println("inside if: " + node.getType());
//                System.out.println("inside if child type: " + node.getChild(1).getType());
                return null;
            }
        }


        if (node.getType().equals(TypeSet.type("STRING")) || node.getType().equals(TypeSet.type("NUMBER"))) {
            JSONObject temp_json = new JSONObject().put("type", node.getChild(0).getLabel()).put("children", new ArrayList<>());
            transformationMap.put(temp_json, node.getChild(0));
            return temp_json;

        }
        if ((node.getType().equals(TypeSet.type("TRUE")) || node.getType().equals(TypeSet.type("FALSE")))) {
            JSONObject temp_json = new JSONObject().put("type", node.getType()).put("children", new ArrayList<>());
            transformationMap.put(temp_json, node);
            return temp_json;
        }
        if (node.getType().equals(TypeSet.type("String"))) {
            JSONObject temp_json = new JSONObject().put("type", node.getLabel()).put("children", new ArrayList<>());
            transformationMap.put(temp_json, node);
            return temp_json;
        }
        if (node.getType().equals(TypeSet.type("NULL"))) {
            return null;
        }
//        System.out.println(node.getType());
        return null;
    }

    public static ObjectMapper mapper = new ObjectMapper();

    public static boolean isNodeLevel2(JSONObject Node) {
        try {
            JsonNode temp_node = mapper.readTree(Node.toString(1));
            return isNodeLevel2(temp_node);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isNodeLevel3(JSONObject Node) {
        try {
            JsonNode temp_node = mapper.readTree(Node.toString(1));
            return isNodeLevel3(temp_node);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean isNodeNMinus1(JSONObject travisNormalizedJSON, JSONObject travisNode) {
        Iterator<Object> iter = travisNormalizedJSON.getJSONArray("children").iterator();
        while (iter.hasNext()) {
            if (travisNode.equals(iter.next())) return true;
        }
        return false;

    }

    public static void BFSandInsertWithNode(JSONObject root, JSONObject originalGithubNode, JSONObject newGithubNode) {
        if (root == originalGithubNode) {
            root.put("type", newGithubNode.get("type"));
            GithubNodes.clear();
        }
        for (Object child_node : (JSONArray) root.get("children")) {
            GithubNodes.add((JSONObject) child_node);
        }
        while (!GithubNodes.isEmpty()) {
            JSONObject currNode = GithubNodes.remove();
            BFSandInsertWithNode(currNode, originalGithubNode, newGithubNode);
        }
    }

    public static String ConvertJsonToYaml(JSONObject root, int depth, boolean childOfBuild, boolean sameLine) {
        String type = root.get("type").toString();
        JSONArray children = root.getJSONArray("children");
        StringBuilder tmp_ret = new StringBuilder();
        int tmp_depth = depth;
        if (childOfBuild && type.equals("name")) {
            tmp_depth -= 1;
            tmp_ret.append("- ");
        }
        if (!sameLine) {
            for (int i = 0; i < tmp_depth; i++) {
                tmp_ret = tmp_ret.insert(0, "  ");
            }
        }

        if (!type.equals("github-file")) {
            tmp_ret.append(type);

        } else {
            depth -= 1;
        }
        if (type.equals("push")) {
            tmp_ret.append(":");
        }
        if (!children.isEmpty()) {
            if (!type.equals("github-file")) {
                tmp_ret.append(":");
            }
            boolean childOfBuild_child = childOfBuild;
            if (type.equals("build")) {
                childOfBuild_child = true;
            }
//            System.out.println( (JSONObject) children.get(0));
//            System.out.println(TreeUtils.getDepth((JSONObject) children.get(0)));
            if (children.length() > 1 || TreeUtils.getDepth((JSONObject) children.get(0)) > 1) {
                if (!type.equals("github-file")) {
                    tmp_ret.append("\n");
                }
                for (Object child : children) {
                    tmp_ret.append(
                            ConvertJsonToYaml((JSONObject) child, depth + 1, childOfBuild_child, false));
                    if (!tmp_ret.toString().endsWith("\n")) {
                        tmp_ret.append("\n");
                    }
                    if (type.equals("github-file")) {
                        tmp_ret.append("\n");
                    }

                }
            } else {
                tmp_ret.append(" ");
                tmp_ret.append(
                        ConvertJsonToYaml((JSONObject) children.get(0), depth + 1, childOfBuild_child, true));
                if (!tmp_ret.toString().endsWith("\n")) {
                    tmp_ret.append("\n");
                }
            }
        }
        return tmp_ret.toString();
    }

    private static int getDepth(JSONObject o) {
        int max = 0;
        JSONArray tmp_children = o.getJSONArray("children");
        if (!tmp_children.isEmpty()) {
            for (Object child : tmp_children) {
                JSONObject tmp_jo = (JSONObject) child;
                max = Integer.max(max, getDepth(tmp_jo));
            }
        }
        return 1 + max;
    }


    public static Vector<JSONObject> BFSAllWithType(JSONObject node, String type) {
        TempBFSNodes.clear();
        String curr_type = node.get("type").toString();
        Vector<JSONObject> matchingNodes = new Vector<>();
        Set<JSONObject> matchingNodesSet = new HashSet<>();
        JSONObject temp_node = null;
        if (curr_type.equals(type)) {
            if (!matchingNodesSet.contains(node)) {
                matchingNodes.add(node);
                matchingNodesSet.add(node);
            }
        }
        for (Object child_node : node.getJSONArray("children")) {
            TempBFSNodes.add((JSONObject) child_node);
        }
        while (!TempBFSNodes.isEmpty()) {
            JSONObject currNode = TempBFSNodes.remove();
            JSONObject temp = BFSWithType(currNode, type);
            if (temp != null) {
                if (!matchingNodesSet.contains(node)) {
                    matchingNodes.add(node);
                    matchingNodesSet.add(node);
                }
            }
        }

        return matchingNodes;
    }

    public static JSONObject BFSWithType(JSONObject node, String type) {
        String curr_type = node.get("type").toString();
        JSONObject temp_node = null;
        if (curr_type.equals(type)) {
            return node;
        }
        for (Object child_node : node.getJSONArray("children")) {
            TempBFSNodes.add((JSONObject) child_node);
        }
        while (!TempBFSNodes.isEmpty()) {
            JSONObject currNode = TempBFSNodes.remove();
            JSONObject temp = BFSWithType(currNode, type);
            if (temp != null) {
                temp_node = temp;
                TempBFSNodes.clear();
            }
        }
        return temp_node;
    }


    public static void insertMissingChildren(JSONObject injectionPoint, JSONObject treeToInsert) {
//        System.out.println("Inserting "+treeToInsert.toString()+" in "+injectionPoint.toString());
        if (injectionPoint == null) {
            return;
        }
        HashMap<String, JSONObject> InjectionPointChildrenMap = new HashMap<>();
        HashMap<String, JSONObject> InjectionPointTypesOfGrandChildrenMap = new HashMap<>();

        for (Object ob_child : injectionPoint.getJSONArray("children")) {
            JSONObject jo_child = (JSONObject) ob_child;
            InjectionPointChildrenMap.put(jo_child.get("type").toString(), jo_child);
            for (Object ob_child_of_child : jo_child.getJSONArray("children")) {
                JSONObject temp_child = (JSONObject) ob_child_of_child;
                InjectionPointTypesOfGrandChildrenMap.put(temp_child.get("type").toString(), jo_child);
            }

        }

        for (Object ob_child : treeToInsert.getJSONArray("children")) {
            JSONObject treeToInsertChild = (JSONObject) ob_child;
            String typeToInsert = treeToInsertChild.get("type").toString();
            if (!InjectionPointChildrenMap.containsKey(typeToInsert) && !InjectionPointTypesOfGrandChildrenMap.containsKey(typeToInsert)) {
                JSONArray jarr = injectionPoint.getJSONArray("children");  // revisit insertion location
                jarr.put(treeToInsertChild);
                for (int i = 0; i < jarr.toList().size() - 1; i++) {
                    JSONObject temp_ob = (JSONObject) jarr.remove(0);
                    jarr.put(temp_ob);
                }
            } else if (typeToInsert.equals("uses") || typeToInsert.equals("with")) {
                JSONArray jarr = injectionPoint.getJSONArray("children");
                if (findJsonNodeInJsonArray(jarr, treeToInsertChild) == -1) {
                    jarr.put(treeToInsertChild);
                    for (int i = 0; i < jarr.toList().size() - 1; i++) {
                        JSONObject temp_ob = (JSONObject) jarr.remove(0);
                        jarr.put(temp_ob);
                    }
                }
            } else if (InjectionPointTypesOfGrandChildrenMap.containsKey(typeToInsert)) {
                insertMissingChildren(InjectionPointTypesOfGrandChildrenMap.get(typeToInsert), treeToInsertChild);
            } else {
                insertMissingChildren(InjectionPointChildrenMap.get(typeToInsert), treeToInsertChild);
            }
        }


    }

    public static void insertMissingParent(JSONObject Github_Tree, JSONObject ghNode, AprioriRule maxRule) {
        int temp_index = findJsonNodeInJsonArray(Github_Tree.getJSONArray("children"), ghNode);
        if (Github_Tree.get("type").toString().equals(maxRule.github_node.get("type").toString())) {
            return;
        }
        if (ghNode.get("type").toString().equals(maxRule.github_node.get("type").toString())) {
            return;
        }
        if (temp_index != -1) {
            JSONArray temp_array = Github_Tree.getJSONArray("children");
            JSONObject new_node = new JSONObject(maxRule.github_node, JSONObject.getNames(maxRule.github_node));
            JSONArray child_array = new JSONArray();
            child_array.put(0, ghNode);
            new_node.put("children", child_array);
            temp_array.put(temp_index, new_node);
            Github_Tree.put("children", temp_array);
            GithubNodes_for_parent.clear();
        } else {
            for (Object child_node : (JSONArray) Github_Tree.get("children")) {
                GithubNodes_for_parent.add((JSONObject) child_node);

            }
            while (!GithubNodes_for_parent.isEmpty()) {
                JSONObject currNode = GithubNodes_for_parent.remove();
                insertMissingParent(currNode, ghNode, maxRule);
            }
        }

    }

    private static int findJsonNodeInJsonArray(JSONArray children, JSONObject ghNode) {

        for (int i = 0; i < children.length(); i++) {
            JSONObject child_json = children.getJSONObject(i);
            if (child_json.get("type").toString().equals(ghNode.get("type").toString())) {
                if (nodesAreEqual(child_json, ghNode)) {
                    return i;
                }
            }
        }
        return -1;

    }

    public static boolean nodesAreEqual(JSONObject node_1, JSONObject node_2) {

        if (!node_1.get("type").toString().equals(node_2.get("type").toString())) {
            return false;
        }
        JSONArray node_1_array = node_1.getJSONArray("children");
        JSONArray node_2_array = node_2.getJSONArray("children");
        if (node_1_array.length() != node_2_array.length()) {
            return false;
        }
        for (int i = 0; i < node_1_array.length(); i++) {
            if (!nodesAreEqual(node_1_array.getJSONObject(i), node_2_array.getJSONObject(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean BFSForNode(JSONObject root, JSONObject originalInjectionPoint) {
        Queue<JSONObject> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            JSONObject temp_root = q.remove();
            if (TreeUtils.nodesAreEqual(temp_root, originalInjectionPoint)) {
                return true;
            } else {
                for (Object child : temp_root.getJSONArray("children")) {
                    q.add((JSONObject) child);
                }
            }
        }
        return false;
    }

    public static void BFSandReplaceWithNode(JSONObject root, JSONObject originalInjectionPoint, JSONObject injectionPoint) {
        Queue<JSONObject> queue = new LinkedList<>();
        queue.add(root);
        boolean inserted = false;
        while (!queue.isEmpty()) {
            JSONObject curr_root = queue.remove();
            JSONArray curr_array = curr_root.getJSONArray("children");
            JSONArray temp_array = new JSONArray();
            for (Object child : curr_array) {
                if (TreeUtils.nodesAreEqual((JSONObject) child, originalInjectionPoint)) {
                    temp_array.put(injectionPoint);
                    inserted = true;
                    queue.clear();
                } else {
                    temp_array.put((JSONObject) child);
                    if (!inserted)
                        queue.add((JSONObject) child);
                }
            }
            curr_root.put("children", temp_array);
        }


//        JSONArray curr_array = root.getJSONArray("children");
//        JSONArray temp_array = new JSONArray();
//        for (Object child : curr_array) {
//            if (TreeUtils.nodesAreEqual((JSONObject) child, originalInjectionPoint)) {
//                temp_array.put(injectionPoint);
//            } else {
//                BFSandReplaceWithNode((JSONObject) child, originalInjectionPoint, injectionPoint);
//                temp_array.put(child);
//            }
//        }
//        root.put("children", temp_array);
    }

    public static void CleanNodes(JSONObject root) {
        String type = root.get("type").toString();
        switch (type) {
            case ("run") -> {
                if ( root.toString().contains("-cmd") && root.getJSONArray("children").length() > 1) {
                    JSONArray new_arr = new JSONArray();
                    for (int i = 0; i < root.getJSONArray("children").length(); i++) {
                        JSONObject child = (JSONObject) root.getJSONArray("children").get(i);
                        if (!child.get("type").toString().strip().toLowerCase().endsWith("-cmd")) {
                            new_arr.put(child);
                        }
                    }
                    root.put("children", new_arr);
                }
            }
            case ("name") -> {
                if (root.getJSONArray("children").length() > 1) {
                    JSONObject name_obj = new JSONObject();
                    name_obj.put("type", "placeholder_Name");
                    name_obj.put("children", new JSONArray());
                    root.put("children", new JSONArray().put(name_obj));
                }
            }
            case ("jdk") -> {
                List<String> javaTypes = new ArrayList<>();
                for (int i = 0; i < root.getJSONArray("children").length(); i++) {
                    JSONObject child = (JSONObject) root.getJSONArray("children").get(i);
                    String child_type = child.get("type").toString();
                    if (child_type.matches(".*\\d.*")) {
                        javaTypes.add(child_type);
                    }
                }
                StringBuilder java_types = new StringBuilder();
                if (javaTypes.size() > 1) {
                    java_types.append('[');
                    String prefix = "";
                    for (String javaType : javaTypes) {
                        java_types.append(prefix);
                        prefix = ",";
                        java_types.append(javaType);
                    }
                    java_types.append(']');
                } else if (javaTypes.size() == 1) {
                    java_types.append(javaTypes.get(0));
                }
                JSONObject child_object = new JSONObject();
                child_object.put("type", java_types.toString());
                child_object.put("children", new JSONArray());
                root.put("children", new JSONArray().put(child_object));
            }
            case ("fail-fast") -> {
                JSONArray failFastType = new JSONArray();
                int falseCount = 0;
                int trueCount = 0;
                for (int i = 0; i < root.getJSONArray("children").length(); i++) {
                    JSONObject child = (JSONObject) root.getJSONArray("children").get(i);
                    String child_type = child.get("type").toString();
                    if (child_type.equals("FALSE")) {
                        falseCount++;
                    } else if (child_type.equals("TRUE")) {
                        trueCount++;
                    }
                }
                if (falseCount >= trueCount) {
                    JSONObject child_object = new JSONObject();
                    child_object.put("type", "FALSE");
                    child_object.put("children", new JSONArray());
                    failFastType.put(child_object);
                } else {
                    JSONObject child_object = new JSONObject();
                    child_object.put("type", "TRUE");
                    child_object.put("children", new JSONArray());
                    failFastType.put(child_object);
                }
                root.put("children", failFastType);
            }

            default -> {
                JSONArray tempArray = new JSONArray();
                for (int i = 0; i < root.getJSONArray("children").length(); i++) {
                    JSONObject child = (JSONObject) root.getJSONArray("children").get(i);
                    String child_type = child.get("type").toString();
                    switch (child_type) {
                        case ("name"), ("uses"), ("with") -> {
                            if (child.getJSONArray("children").length() == 0) {
                                continue;
                            }
                        }
                        case ("run") -> {
//                            System.out.println(root);
//                            System.out.println(child);
                            if (child.getJSONArray("children").length() == 0) {
                                continue;
                            }
                            if (child.getJSONArray("children").length() == 1) {
                                if (((JSONObject)child.getJSONArray("children").get(0)).get("type").toString().equals("mvn-cmd"))
                                    continue;
                            }
                        }
                    }
                    CleanNodes(child);
                    tempArray.put(child);
                }
                root.put("children", tempArray);
            }


        }

    }

    public static void BFSandRemoveNode(JSONObject root, String type) {
        Queue<JSONObject> queue = new LinkedList<>();
        queue.add(root);
        boolean removed = false;
        while (!queue.isEmpty()) {
            JSONObject curr_root = queue.remove();
            JSONArray curr_array = curr_root.getJSONArray("children");
            JSONArray temp_array = new JSONArray();
            for (Object child : curr_array) {
                if (!((JSONObject) child).get("type").toString().equals(type)) {
                    temp_array.put(child);
                    removed = true;
                    queue.clear();
                } else {
                    if (!removed)
                        queue.add((JSONObject) child);
                }
            }
            curr_root.put("children", temp_array);
        }
//
//        JSONArray curr_array = root.getJSONArray("children");
//        JSONArray temp_array = new JSONArray();
//        for (Object child : curr_array) {
//            if (!((JSONObject) child).get("type").toString().equals(type)) {
//                JSONArray child_array = ((JSONObject) child).getJSONArray("children");
//                if (child_array.length() == 1) {
//                    JSONObject child_of_child = (JSONObject) child_array.get(0);
//                    if (child_of_child.get("type").toString().equals(type)) {
//                        continue;
//                    }
//                }
//                BFSandRemoveNode((JSONObject) child, type);
//                temp_array.put(child);
//            }
//        }
//        root.put("children", temp_array);
    }


    public static void BFSandRemoveNode(JSONObject root, JSONObject originalInjectionPoint) {
        Queue<JSONObject> queue = new LinkedList<>();
        queue.add(root);
        boolean removed = false;
        while (!queue.isEmpty()) {
            JSONObject curr_root = queue.remove();
            JSONArray curr_array = curr_root.getJSONArray("children");
            JSONArray temp_array = new JSONArray();
            for (Object child : curr_array) {
                if (!TreeUtils.nodesAreEqual((JSONObject) child, originalInjectionPoint)) {
                    temp_array.put(child);
                    if (!removed)
                        queue.add((JSONObject) child);
                } else {
                    removed = true;
                    queue.clear();
                }
            }
            curr_root.put("children", temp_array);
        }


//        JSONArray curr_array = root.getJSONArray("children");
//        JSONArray temp_array = new JSONArray();
//        for (Object child : curr_array) {
//            if (!TreeUtils.nodesAreEqual((JSONObject) child, originalInjectionPoint)) {
//                BFSandRemoveNode((JSONObject) child, originalInjectionPoint);
//                temp_array.put(child);
//            }
//        }
//        root.put("children", temp_array);
    }


    public static Vector<JSONObject> findAllWithType(JSONObject travisJSON, String type) {
        Vector<JSONObject> matchingNodes = new Vector<>();
        Queue<JSONObject> nodesQueue = new LinkedList<>();
        nodesQueue.add(travisJSON);
        while (!nodesQueue.isEmpty()) {
            JSONObject temp = nodesQueue.remove();
            if (temp.get("type").toString().equals(type)) {
                matchingNodes.add(temp);
            }
            for (Object ob_child : temp.getJSONArray("children")) {
                JSONObject jo_child = (JSONObject) ob_child;
                nodesQueue.add(jo_child);
            }
        }
        return matchingNodes;
    }
}

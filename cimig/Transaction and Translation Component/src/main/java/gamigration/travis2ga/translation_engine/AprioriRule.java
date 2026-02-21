package gamigration.travis2ga.translation_engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import util.TreeUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class AprioriRule implements Serializable {
    public transient JSONObject travis_node;
    public transient JSONObject github_node;
    double Conf;
    double Lift;
    double Supp;
    double Conv;
    double Conf_2;
    double Lift_2;
    double Supp_2;
    double Conv_2;

    public AprioriRule(JSONObject travis_node, JSONObject github_node, double confidence, double lift, double support, double conv, double confidence_2, double lift_2, double support_2, double conv_2) {
        this.travis_node = travis_node;
        travis_node.remove("origin");
        this.github_node = github_node;
        github_node.remove("origin");
        Conf = confidence;
        Lift = lift;
        this.Supp = support;
        Conv = conv;
        Conf_2 = confidence_2;
        this.Lift_2 = lift_2;
        Supp_2 = support_2;
        Conv_2 = conv_2;
    }

    private void writeObject(ObjectOutputStream oos)   throws IOException {
        oos.defaultWriteObject();
        String githubAndTravisNodes = github_node.toString()+"===="+travis_node.toString();
        oos.writeObject(githubAndTravisNodes);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        String githubAndTravisNodes = (String) ois.readObject();
        JSONObject githubNode = new JSONObject (githubAndTravisNodes.split("====")[0]);
        JSONObject travisNode = new JSONObject (githubAndTravisNodes.split("====")[1]);
        this.github_node = githubNode ;
        this.travis_node = travisNode ;
    }


    public boolean Rule_matches_rhs(JSONObject rhs_node) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (TreeUtils.nodesAreEqual(this.github_node,rhs_node)){
                return true;
            }
            else{
                return false;
            }
//
//            JsonNode temp_node_1 = mapper.readTree(this.github_node.toString(1));
//            JsonNode temp_node_2 = mapper.readTree(githubNode.toString(1));
//           System.out.println(temp_node_1);
//            System.out.println(temp_node_2);
//            if (temp_node_1.equals(temp_node_2)) {
//                return true;
//            }
//            System.out.println("False");
//            return false;
        }
        catch(Exception e) {
            return false;
        }
    }

    public boolean Rule_matches_lhs(JSONObject lhs_node) {
        ObjectMapper mapper = new ObjectMapper();
        try {

            if (TreeUtils.nodesAreEqual(this.travis_node,lhs_node)){
                return true;
            }
            else{
                return false;
            }

//            JsonNode temp_node_1 = mapper.readTree(this.travis_node.toString(1));
//            JsonNode temp_node_2 = mapper.readTree(travis_node.toString(1));
////            System.out.println(temp_node_1);
////            System.out.println(temp_node_2);
//            if (temp_node_1.equals(temp_node_2)) {
//                return true;
//            }
////            System.out.println("False");
//            return false;
        }
        catch(Exception e) {
            return false;
        }
    }
}

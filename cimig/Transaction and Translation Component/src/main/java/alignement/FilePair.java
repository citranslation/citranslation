package alignement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.simmetrics.StringMetric;
import org.simmetrics.StringMetrics;

import com.github.gumtreediff.tree.Tree;

import db.PsqlConnection;
import travisdiff.TravisCITree;
import util.NormalizationUtils;
import util.TreeUtils;

public class FilePair {
    List<String> travisFileContents;
    List<String> githubActionsFileContents;
    Tree travisTree;
    Tree githubActionsTree;
    List<String> travisSequences;
    List<String> githubSequences;
    String travisJsonString;
    String githubJsonString;
    public String path;
    static String conversionType;
    public String githubFileName;


    public String getTravisJsonString() {
        return travisJsonString;
    }

    public FilePair(List<String> travisFileContents, List<String> githubActionsFileContents, String travisFilePath, String githubActionsFilePath) {
        this.travisFileContents = travisFileContents;
        this.githubActionsFileContents = githubActionsFileContents;
        TravisCITree travis = new TravisCITree();
        this.travisTree = travis.getTravisCITree(travisFilePath);
        TravisCITree github = new TravisCITree();
        System.out.println(githubActionsFilePath);
        this.githubActionsTree = github.getTravisCITree(githubActionsFilePath);
        travisSequences = TreeUtils.generateAllSequences(travisTree);
        githubSequences = TreeUtils.generateAllSequences(githubActionsTree);
        NormalizationUtils.normalizeTreeContents(this.travisTree);
        NormalizationUtils.normalizeTreeContents(this.githubActionsTree);
        conversionType = "Travis";
        String pathArray[]=githubActionsFilePath.split("/");
        githubFileName=pathArray[pathArray.length-1];
        path = travisFilePath.split("/")[2];
        System.out.println("converting travis of " + path);
        travisJsonString = TreeUtils.treeToJson(travisTree).put("type", "travis-file").toString(1);
        conversionType = "Github";
        System.out.println("converting github of " + path);
        githubJsonString = TreeUtils.treeToJson(githubActionsTree).put("type", "github-file").toString(1);
        conversionType = "";


    }

    public String getGithubJsonString() {
        return githubJsonString;
    }


    //this function is not working as expected


    public void matchLeaves() throws SQLException {
        PsqlConnection con = new PsqlConnection();
        StringMetric metric = StringMetrics.cosineSimilarity();
        for (String travisSeq : this.getTravisSequences()) {
            String travisLeaf = StringUtils.substringBetween(travisSeq, ".[", "]");
            String travisParent = travisSeq.split("\\.\\[")[0];
            for (String githubSeq : this.getGithubSequences()) {
                String githubLeaf = StringUtils.substringBetween(githubSeq, ".[", "]");
                String githubParent = githubSeq.split("\\.\\[")[0];
                if (metric.compare(travisLeaf, githubLeaf) > 0.5) {
                    con.updateIfExistElseInsert(travisLeaf, githubLeaf, travisParent, githubParent);
                }
            }
        }
    }


    public Tree getTravisTree() {
        return travisTree;
    }

    public Tree getGithubActionsTree() {
        return githubActionsTree;
    }

    public List<String> getTravisSequences() {
        return travisSequences;
    }

    public List<String> getGithubSequences() {
        return githubSequences;
    }

    //filter sequences that never appears in the database
    //filters out statements such as script:mvn -...........
    public List<String> filterUncommonSequences(List<String> listOfSequences) {
        List<String> returnList = new ArrayList<String>();
        PsqlConnection db = new PsqlConnection();
        for (String sequences : listOfSequences) {
            if (db.pathExists(sequences)) {
                returnList.add(sequences);
            }
        }
        return returnList;
    }

    public void applyNormalizationOnTravis(Function<String, String> method) {
        this.travisSequences = this.travisSequences.stream().map(method::apply).collect(Collectors.toList());
    }

    public void applyNormalizationOnGithub(Function<String, String> method) {
        this.githubSequences = this.githubSequences.stream().map(method::apply).collect(Collectors.toList());
    }

    public void filterSequencesContaining(String str) {
        this.travisSequences = this.travisSequences.stream().filter(elem -> !elem.contains(str)).collect(Collectors.toList());
        this.githubSequences = this.githubSequences.stream().filter(elem -> !elem.contains(str)).collect(Collectors.toList());
    }
}

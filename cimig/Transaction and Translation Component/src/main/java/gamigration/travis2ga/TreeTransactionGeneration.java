package gamigration.travis2ga;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;
import util.TreeUtils;

public class TreeTransactionGeneration {

    CSVWriter csvWriterL2;
    CSVWriter csvWriterL2_alt;
    CSVWriter csvWriterL3;
    CSVWriter csvWriterL3_alt;
    CSVWriter csvWriterN1;
    CSVWriter csvWriterAll;
    CSVWriter csvWriterTopLevel2;
    CSVWriter csvWriterTopLevel3;

    CSVWriter csvWriterMixed;
    CSVWriter csvWriterMixed_alt;

    public TreeTransactionGeneration() throws IOException {
        String[] header = {"travis", "github"};
        this.csvWriterL2 = getCsvWriterObject("L2");
        this.csvWriterL2_alt = getCsvWriterObject("L2_alt");
        this.csvWriterL3 = getCsvWriterObject("L3");
        this.csvWriterL3_alt = getCsvWriterObject("L3_alt");
        this.csvWriterN1 = getCsvWriterObject("N-1");
        this.csvWriterAll = getCsvWriterObject("all_to_all");
        this.csvWriterTopLevel2 = getCsvWriterObject("top_l_2");
        this.csvWriterTopLevel3 = getCsvWriterObject("top_l_3");

        this.csvWriterMixed = getCsvWriterObject("github_l_3_and_travis_l_2");
        this.csvWriterMixed_alt = getCsvWriterObject("github_l_3_alt_and_travis_l_2_alt");

        this.csvWriterL2.writeNext(header);
        this.csvWriterL2_alt.writeNext(header);
        this.csvWriterL3.writeNext(header);
        this.csvWriterL3_alt.writeNext(header);
        this.csvWriterN1.writeNext(header);
        this.csvWriterAll.writeNext(header);
        this.csvWriterTopLevel2.writeNext(header);
        this.csvWriterTopLevel3.writeNext(header);
        this.csvWriterMixed.writeNext(header);
        this.csvWriterMixed_alt.writeNext(header);
        // a;

    }

    private static CSVWriter getCsvWriterObject(String fileNameSuffix) throws IOException {
        return new CSVWriter(new FileWriter("transactions/transactions_json_ast_" + fileNameSuffix + ".csv"), ';',
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
    }

//    private static boolean isLeaf(JsonNode node) {
//        if (!node.get("children").isArray()) {
//            return false;
//        }
//        return !node.get("children").elements().hasNext();
//    }

    private void writeTreePairToCsv(HashSet<JsonNode> travisNodes, HashSet<JsonNode> githubNodes, CSVWriter csvWriter, Predicate<JsonNode> isNodeLevel, Consumer<JsonNode> pruneNode) {
        travisNodes.forEach(travisNode -> {
            if (isNodeLevel.test(travisNode)) {
                githubNodes.forEach(githubNode -> {
                    if (isNodeLevel.test(githubNode)) {
                        JsonNode tempTravisNode = travisNode.deepCopy();
                        JsonNode tempGithubNode = githubNode.deepCopy();
                        pruneNode.accept(tempTravisNode);
                        pruneNode.accept(tempGithubNode);
                        ObjectNode tempTravis = tempTravisNode.deepCopy();
                        ObjectNode tempGithub = tempGithubNode.deepCopy();
                        tempTravis.put("origin", "travis");
                        tempGithub.put("origin", "github");
                        String[] data = {tempTravis.toString(), tempGithub.toString()};
                        csvWriter.writeNext(data);
                        try {
                            csvWriter.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        });
    }

    private void writeMixedTreePairToCsv(HashSet<JsonNode> travisNodes, HashSet<JsonNode> githubNodes, CSVWriter csvWriter, Predicate<JsonNode> isNodeLevel, Predicate<JsonNode> isNodeLevel_other, Consumer<JsonNode> pruneNode, Consumer<JsonNode> pruneNode_other) {
        travisNodes.forEach(travisNode -> {
            if (isNodeLevel.test(travisNode)) {
                githubNodes.forEach(githubNode -> {
                    if (isNodeLevel_other.test(githubNode)) {
                        JsonNode tempTravisNode = travisNode.deepCopy();
                        JsonNode tempGithubNode = githubNode.deepCopy();
                        pruneNode.accept(tempTravisNode);
                        pruneNode_other.accept(tempGithubNode);
                        ObjectNode tempTravis = tempTravisNode.deepCopy();
                        ObjectNode tempGithub = tempGithubNode.deepCopy();
                        tempTravis.put("origin", "travis");
                        tempGithub.put("origin", "github");
                        String[] data = {tempTravis.toString(), tempGithub.toString()};
                        csvWriter.writeNext(data);
                        try {
                            csvWriter.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        });
    }

    private void writeTreePairToCsv(JsonNode travisRoot, JsonNode githubRoot, HashSet<JsonNode> travisNodes, HashSet<JsonNode> githubNodes, CSVWriter csvWriter, BiPredicate<JsonNode, JsonNode> isNodeLevel, Consumer<JsonNode> pruneNode) {
        travisNodes.forEach(travisNode -> {
            if (isNodeLevel.test(travisRoot, travisNode)) {
                githubNodes.forEach(githubNode -> {
                    if (isNodeLevel.test(githubRoot, githubNode)) {
                        JsonNode tempTravisNode = travisNode.deepCopy();
                        JsonNode tempGithubNode = githubNode.deepCopy();
                        pruneNode.accept(tempTravisNode);
                        pruneNode.accept(tempGithubNode);
                        ObjectNode tempTravis = tempTravisNode.deepCopy();
                        ObjectNode tempGithub = tempGithubNode.deepCopy();
                        tempTravis.put("origin", "travis");
                        tempGithub.put("origin", "github");
                        String[] data = {tempTravis.toString(), tempGithub.toString()};
                        csvWriter.writeNext(data);
                        try {
                            csvWriter.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        });
    }

    private final static Consumer<JsonNode> NOOP = (whatever) -> {
    };
    private final static Predicate<JsonNode> alwaysTrue = (whatever) -> {
        return true;
    };

    public void genEverything(JsonNode travisRoot, JsonNode githubRoot, HashSet<JsonNode> travisNodes, HashSet<JsonNode> githubNodes) {
        writeTreePairToCsv(travisNodes, githubNodes, this.csvWriterL2, TreeUtils::isNodeLevel2, TreeUtils::pruneLevel2Node);
        writeTreePairToCsv(travisNodes, githubNodes, this.csvWriterL2_alt, TreeUtils::isNodeLevel2, TreeUtils::pruneLevel2Node_alt);
        writeTreePairToCsv(travisNodes, githubNodes, this.csvWriterL3, TreeUtils::isNodeLevel3, TreeUtils::pruneLevel3Node);
        writeTreePairToCsv(travisNodes, githubNodes, this.csvWriterL3_alt, TreeUtils::isNodeLevel3, TreeUtils::pruneLevel3Node_alt);
        writeTreePairToCsv(travisRoot, githubRoot, travisNodes, githubNodes, this.csvWriterN1, TreeUtils::isNodeNMinus1, NOOP);
        writeTreePairToCsv(travisNodes, githubNodes, this.csvWriterAll, alwaysTrue, NOOP);

        writeMixedTreePairToCsv(travisNodes, githubNodes, this.csvWriterMixed, TreeUtils::isNodeLevel2, TreeUtils::isNodeLevel3, TreeUtils::pruneLevel2Node, TreeUtils::pruneLevel3Node);
        writeMixedTreePairToCsv(travisNodes, githubNodes, this.csvWriterMixed_alt, TreeUtils::isNodeLevel2, TreeUtils::isNodeLevel3, TreeUtils::pruneLevel2Node_alt, TreeUtils::pruneLevel3Node_alt);
        //consumer do nothing
        writeTreePairToCsv(travisRoot, githubRoot, travisNodes, githubNodes, this.csvWriterTopLevel2, TreeUtils::isNodeNMinus1, TreeUtils::pruneN1NodeToDepth2);
        writeTreePairToCsv(travisRoot, githubRoot, travisNodes, githubNodes, this.csvWriterTopLevel3, TreeUtils::isNodeNMinus1, TreeUtils::pruneN1NodeToDepth3);

    }

    public void closeRessources() throws IOException {
        this.csvWriterL2.close();
        this.csvWriterL2_alt.close();
        this.csvWriterL3.close();
        this.csvWriterL3_alt.close();
        this.csvWriterN1.close();
        this.csvWriterAll.close();
        this.csvWriterTopLevel2.close();
        this.csvWriterTopLevel3.close();
        this.csvWriterMixed.close();
        this.csvWriterMixed_alt.close();
    }
}

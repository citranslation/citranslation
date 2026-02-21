package gamigration.travis2ga.translation_engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.tree.Tree;

import com.opencsv.CSVReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import travisdiff.TravisCITree;
import util.*;


import java.nio.file.Files;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static util.SimilarityUtils.*;
import static util.TreeUtils.*;

public class MainTranslator {
    private static Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> alignAprioirRulesWithTars(Set<AprioriRule> nonSimRules, Set<TarRule> travisRuleSet, Set<TarRule> ghRuleSet)
            throws InterruptedException, ExecutionException {

        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of threads: " + threads);
        ExecutorService service = Executors.newFixedThreadPool(threads);

        List<Future<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>>> futures = new ArrayList<>();
        for (final AprioriRule input : nonSimRules) {
            Callable<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>> callable = new Callable<>() {
                public Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> call() throws Exception {
                    Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> output = null;
                    Vector<TarRule> travisTars = new Vector<>();
                    Vector<TarRule> ghTars = new Vector<>();
                    for (TarRule travisTar : travisRuleSet) {
                        HashSet<JSONObject> temp_L2_Nodes = new HashSet<>();
                        HashSet<JSONObject> tempNodesSet = new HashSet<>();
                        TreeUtils.traverseTree(travisTar.node, tempNodesSet);
                        tempNodesSet.forEach(travisNode -> {
                            if (TreeUtils.isNodeLevel2(travisNode)) {
                                temp_L2_Nodes.add(travisNode);
                            }
                        });

                        for (JSONObject l2_node : temp_L2_Nodes) {
                            if (input.Rule_matches_lhs(l2_node)) {
                                travisTars.add(travisTar);
                                break;
                            }
                        }
                    }
                    for (TarRule ghTar : ghRuleSet) {
                        HashSet<JSONObject> temp_L2_Nodes = new HashSet<>();
                        HashSet<JSONObject> tempNodesSet = new HashSet<>();
                        TreeUtils.traverseTree(ghTar.node, tempNodesSet);
                        tempNodesSet.forEach(travisNode -> {
                            if (TreeUtils.isNodeLevel2(travisNode)) {
                                temp_L2_Nodes.add(travisNode);
                            }
                        });

                        for (JSONObject l2_node : temp_L2_Nodes) {
                            if (input.Rule_matches_rhs(l2_node)) {
                                ghTars.add(ghTar);
                                break;
                            }
                        }
                    }
                    if (ghTars.size() > 0 && travisTars.size() > 0) {
                        output = new ImmutablePair<>(input, new ImmutablePair<>(travisTars, ghTars));
                    }
                    return output;
                }
            };
            futures.add(service.submit(callable));
        }

        service.shutdown();

        Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> outputs = new HashMap<>();
        for (Future<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>> future : futures) {
            if (future.get() != null)
                outputs.put(future.get().getLeft(), future.get().getRight());

        }
        return outputs;
    }

    private static Map<AprioriRule, AprioriRule> alignAprioirRules(Set<AprioriRule> travisRuleSet, Set<AprioriRule> ghRuleSet)
            throws InterruptedException, ExecutionException {

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);

        List<Future<Pair<AprioriRule, AprioriRule>>> futures = new ArrayList<>();
        for (final AprioriRule input : travisRuleSet) {
            Callable<Pair<AprioriRule, AprioriRule>> callable = new Callable<Pair<AprioriRule, AprioriRule>>() {
                public Pair<AprioriRule, AprioriRule> call() throws Exception {
                    Pair<AprioriRule, AprioriRule> output = null;
                    for (AprioriRule gh_apr : ghRuleSet) {
                        if (TranslationUtils.AprioriRulesAreEqual(input, gh_apr)) {
                            output = new ImmutablePair<>(input, gh_apr);
                            break;
                        }
                    }
                    return output;
                }
            };
            futures.add(service.submit(callable));
        }

        service.shutdown();

        Map<AprioriRule, AprioriRule> outputs = new HashMap<>();
        for (Future<Pair<AprioriRule, AprioriRule>> future : futures) {
            if (future.get() != null)
                outputs.put(future.get().getLeft(), future.get().getRight());
        }
        return outputs;
    }


    public static void main(String[] Args) {

        try {


            File csvOutputFile = new File("stats-out.csv");
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                String header = "fileName,NbL2NodesTotal,NbL2NodesTranslatedWithSim,NbL2NodesTranslatedWithNonSim,NbTARsAdded,NbChildToParentRulesApplied";
                pw.println(header);
            }
            File csvOfFilesOutputFile = new File("files-out.csv");
            try (PrintWriter pw = new PrintWriter(csvOfFilesOutputFile)) {
                String header = "GeneratefileName;OriginalFilesList";
                pw.println(header);
            }



            String directory = "./data/";
            final String csvPath = "projects-file-tuples.csv";
            Vector<String> pathList = new Vector<>();
            CSVReader reader = new CSVReader(new FileReader(csvPath));
            String[] nextLine;
            try {
                reader.readNext();
                List<String[]> unparsedPairs = new ArrayList<>();
                String old_proj_name = "";
                while ((nextLine = reader.readNext()) != null) {
                    String train = nextLine[3];
                    if (train.equals("True")) {
                        continue;
                    }
                    String project_name = nextLine[0];
                    String maxGHAFile = nextLine[2];
                    if (project_name.equals(old_proj_name)) {
                        continue;
                    } else {
                        old_proj_name = project_name;
                    }

                    String fileName = nextLine[1];
                    String travisFilePath = directory + project_name + "/Max/0/travis.yml";
                    File f = new File(travisFilePath);
                    pathList.add(travisFilePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            int nbTr = 0;
            for (String travisFile : pathList) {

                // load TAR rules
                HashSet<TarRule> tar_github_rules = new HashSet<>();
                BufferedReader br = new BufferedReader(new FileReader("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-github.jsonl"));
                String line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    try {
                        tar_github_rules.add(new TarRule(new JSONObject(line)));
                    } catch (Exception e) {
                    }
                }
                HashSet<TarRule> tar_travis_rules = new HashSet<>();

                br = new BufferedReader(new FileReader("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-travis.jsonl"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    try {
                        tar_travis_rules.add(new TarRule(new JSONObject(line)));
                    } catch (Exception e) {
                    }
                }

                // load apriori rules
                HashSet<AprioriRule> l_2_Apriori_rules = new HashSet<>();
                br = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/sim 0.5/rules_H2.csv"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
                    String travis_str = rule_str[0].split("->")[0].substring(1, rule_str[0].split("->")[0].length());
                    if (rule_str.length > 1) {
                        try {
                            l_2_Apriori_rules.add(new AprioriRule(new JSONObject(travis_str), new JSONObject(rule_str[0].split("->")[1]), Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                }
                //load non-sim rules
                HashSet<AprioriRule> l_2_Apriori_rules_non_sim = new HashSet<>();
                br = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/nonsimrules/rules_H2.csv"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
                    String travis_str = rule_str[0].split("->")[0].substring(1, rule_str[0].split("->")[0].length());
                    String gh_str = rule_str[0].split("->")[1];
//                System.out.println(travis_str);
//                System.out.println(gh_str);
                    if (rule_str.length > 1) {
                        try {
                            l_2_Apriori_rules_non_sim.add(new AprioriRule(new JSONObject(travis_str), new JSONObject(gh_str), Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                }

                Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> aprioriRuleTarMap = new HashMap<>();
                Instant start = Instant.now();

                if (Files.exists(Paths.get("apr_and_tars_map.ser"))) {
                    FileInputStream fileIn = new FileInputStream("apr_and_tars_map.ser");
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    try {
                        aprioriRuleTarMap = (Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>) in.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("AprioriRules and Matching TARs Loaded successfully");
                } else {
                    aprioriRuleTarMap = new HashMap<>();
                    System.out.println(l_2_Apriori_rules_non_sim.size());
                    System.out.println(tar_travis_rules.size());
                    System.out.println(tar_github_rules.size());
                    System.out.println("aligning AprioriRules and TARs");
                    aprioriRuleTarMap = alignAprioirRulesWithTars(l_2_Apriori_rules_non_sim, tar_travis_rules, tar_github_rules);

                    try {
                        FileOutputStream fileOut =
                                new FileOutputStream("apr_and_tars_map.ser");
                        ObjectOutputStream out = new ObjectOutputStream(fileOut);
                        out.writeObject(aprioriRuleTarMap);
                        out.close();
                        fileOut.close();
                        System.out.println("AprioriRules and Matching TARs  generated and serialized successfully");
                        Instant end = Instant.now();
                        Duration timeElapsed = Duration.between(start, end);
                        System.out.println("Time taken for alignment: " + timeElapsed.toMillis() + " milliseconds");
                        System.exit(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                HashSet<AprioriRule> child_to_parent_rules = new HashSet<>();
                try {
                    BufferedReader br_temp = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/parents/rules_H2_parent_child_github.csv"));
                    br_temp.readLine();
                    while ((line = br_temp.readLine()) != null) {
                        String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
                        String github_child_str = rule_str[0].split("->")[0];
                        String github_parent_str = rule_str[0].split("->")[1];
                        String temp_str = """
                             {"children": [],
                              "type": "PLACEHOLDER"
                             }
                            """;
//                    JSONObject travis_parent = new JSONObject(temp_str.replace("PLACEHOLDER", travis_str));
                        JSONObject github_parent = new JSONObject(temp_str.replace("PLACEHOLDER", github_parent_str));
                        if (rule_str.length > 1) {
                            try {
                                child_to_parent_rules.add(new AprioriRule(new JSONObject(github_child_str), github_parent, Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                                child_to_parent_rules.add(new AprioriRule(new JSONObject(github_child_str), github_parent, Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }



                TreeUtils.resetQueues();
                // Get the current time in milliseconds before the task
                long startTime = System.currentTimeMillis();

                String FolderPath = travisFile.substring(0, travisFile.lastIndexOf("/")) + "/GHActions/";
                File f = new File(FolderPath);
                File[] matchingFiles = f.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith("yml");
                    }
                });

                nbTr++;


                // TODO fix bug related to install and - in L-2 nodes. JanusGraph_janusgraph-foundationdb ;zhanhb_thymeleaf-layout-dialect

//                if (!travisFile.contains("selenide") ) {
//                    continue;
//                }
//                if (nbTr > 10) {
//                    break;
//                }
                try {

                    //  : Parse file into tree
                    JSONObject GithubCompleteJsonRootNode = null;
                    TravisCITree travis = new TravisCITree();
                    Tree travisTreeOriginal = travis.getTravisCITree(travisFile);

                    //  make normalized copy of tree
                    Tree travisTreeNormalized = travisTreeOriginal.deepCopy();
                    HashMap<Tree, Tree> normalizationMap = new HashMap<>();

                    NormalizationUtils.normalizeTreeContentsWithMap(travisTreeNormalized, travisTreeOriginal, normalizationMap);
//            NormalizationUtils.normalizeTreeContentsWithMapAndParents(travisTreeNormalized, travisTreeOriginal, normalizationMap,parentsMap);
                    HashMap<JSONObject, Tree> transformationMap = new HashMap<>();
//            NormalizationUtils.normalizeTreeContents(travisTreeNormalized);
//            String travisJsonString = TreeUtils.treeToJson(travisTreeNormalized).toString(1);

                    JSONObject travisNormalizedJSON = TreeUtils.treeToJsonWithMap(travisTreeNormalized, transformationMap);
                    HashSet<JSONObject> travisNodesNormalizedJSON = new HashSet<JSONObject>();
                    TreeUtils.traverseTree(travisNormalizedJSON, travisNodesNormalizedJSON);


                    // Find node levels that can be processed


                    HashSet<Pair<JSONObject, JSONObject>> L2_Nodes = new HashSet<>();
                    HashSet<JSONObject> L3_Nodes = new HashSet<>();
                    travisNodesNormalizedJSON.forEach(travisNode -> {
                                if (TreeUtils.isNodeLevel2(travisNode)) {
                                    L2_Nodes.add(new MutablePair<>(null, travisNode));
                                } else if (TreeUtils.isNodeLevel3(travisNode)) {
                                    L3_Nodes.add(travisNode);
                                }
                            }
                    );

                    for (JSONObject L3_node : L3_Nodes) {
                        Iterator<Object> iter = L3_node.getJSONArray("children").iterator();
                        for (Iterator<Object> it = iter; it.hasNext(); ) {
                            JSONObject l_2_node = (JSONObject) it.next();
                            if (L2_Nodes.contains(new MutablePair<>(null, l_2_node))) {
                                L2_Nodes.remove(new MutablePair<>(null, l_2_node));
                                L2_Nodes.add(new ImmutablePair<>(L3_node, l_2_node));
                            }
                        }
                    }
                    //  Use translation rules

                    // match aprior simbased translation rules
                    HashMap<Pair<JSONObject, JSONObject>, Vector<AprioriRule>> new_github_nodes_dict = new HashMap<>();
                    for (Pair<JSONObject, JSONObject> pair : L2_Nodes) {
                        for (AprioriRule aprioriRule : l_2_Apriori_rules) {
                            JSONObject tmp_l2 = pair.getRight();
                            if (aprioriRule.Rule_matches_lhs(tmp_l2)) {
                                if (new_github_nodes_dict.containsKey(pair)) {
                                    new_github_nodes_dict.get(pair).add(aprioriRule);
                                } else {
                                    Vector<AprioriRule> vr = new Vector<>();
                                    vr.add(aprioriRule);
                                    new_github_nodes_dict.put(pair, vr);
                                }
                            }
                        }
                    }
                    Set<Pair<JSONObject, JSONObject>> L2NodesNotTranslatableWithSim = new HashSet<>(L2_Nodes);
                    L2NodesNotTranslatableWithSim.removeAll(new_github_nodes_dict.keySet());

                    // find best sim-based translation rule for translatable nodes
                    Vector<Pair<Pair<JSONObject, JSONObject>, JSONObject>> translationNodes = new Vector<>();
                    for (Pair<JSONObject, JSONObject> pair : new_github_nodes_dict.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_github_nodes_dict.get(pair);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_github_nodes_dict.get(pair)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }
                        translationNodes.add(new ImmutablePair<>(pair, max_rule.github_node));
                    }

                    String basicGitHubStructure =
                            """
                                    {
                                        "children": [
                                            {
                                                "children": [
                                                  {
                                                     "children": [],
                                                     "type": "placeholder"
                                                 }
                                                ],
                                                "type": "name"
                                            },
                                            {
                                                "children": [
                                                    {
                                                        "children": [],
                                                        "type": "push"
                                                    }
                                                ],
                                                "type": "on"
                                            },
                                            {
                                                "children": [
                                                    {
                                                        "children": [
                                                            {
                                                                "children": [],
                                                                "type": "runs-on"
                                                            }
                                                        ],
                                                        "type": "build"
                                                    }
                                                ],
                                                "type": "jobs"
                                            }
                                        ],
                                        "type": "github-file"
                                    }
                                    """;
                    // apply and save results of best sim-based rules
                    HashSet<Pair<JSONObject, JSONObject>> translatedNodes = new HashSet<>();
                    JSONObject GithubNormalizedJsonRootNode = new JSONObject(basicGitHubStructure);
                    HashMap<JSONObject, String> GitHubNodesAndParentsMap_SimBased = new HashMap<>();
                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes) {
                        JSONObject travis_parent = element.getLeft().getLeft();
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = element.getRight();
                        translatedNodes.add(element.getLeft());
//                JSONObject github_node_obj = TreeUtils.convertJsonNodeToJSONObject(github_node);
//                TreeUtils.transferParameters(travis_node,github_node,normalizationMap,transformationMap);
                        String travis_parent_val = "";

                        // use travis node to find matching translation parent in github
                        travis_parent_val = String.valueOf(travis_node.get("type"));
                        switch (cleanJsonVal(travis_parent_val)) {
                            case "language" -> {
                                TreeUtils.BFSandInsertWithType(GithubNormalizedJsonRootNode, "runs-on", 0, github_node);
                                GitHubNodesAndParentsMap_SimBased.put(github_node, "runs-on");
                            }
                            case "script" -> {
                                TreeUtils.BFSandInsertWithType(GithubNormalizedJsonRootNode, "build", 0, github_node);
                                GitHubNodesAndParentsMap_SimBased.put(github_node, "build");
                            }
                            case "after_success" -> {
                                TreeUtils.BFSandInsertWithType(GithubNormalizedJsonRootNode, "build", 0, github_node);
                                GitHubNodesAndParentsMap_SimBased.put(github_node, "build");
                            }
                            default -> {
                                String github_parent_val = github_node.get("type").toString();
                                MutableBoolean inserted = new MutableBoolean(false);
                                switch (cleanJsonVal(github_parent_val)) {
                                    default -> {
                                        if (!GithubNormalizedJsonRootNode.toString().contains(github_parent_val)) {
                                            TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, "github-file", github_node);
                                            GitHubNodesAndParentsMap_SimBased.put(github_node, "github-file");
                                        } else {
                                            if (!BFSForNode(GithubNormalizedJsonRootNode, github_node)) {
                                                for (int i = 0; i < github_node.getJSONArray("children").length(); i++) {
                                                    JSONObject temp_child = github_node.getJSONArray("children").getJSONObject(i);
                                                    TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, github_parent_val, temp_child);
                                                    GitHubNodesAndParentsMap_SimBased.put(temp_child, github_parent_val);
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }

                    // apply non-sim rules if not already translated

//                    TarRule temp_tar = new TarRule(new JSONObject(temp_line));
//                    if (temp_tar.IsInJSON(travisNormalize-dJSON)){
//                        System.out.println("Found!");
//                    }

                    // find rules that apply to non-translated nodes:

                    new_github_nodes_dict = new HashMap<>();
                    for (AprioriRule nonsimRule : aprioriRuleTarMap.keySet()) {
                        for (Pair<JSONObject, JSONObject> pair : L2NodesNotTranslatableWithSim) {
                            JSONObject tmp_l2 = pair.getRight();
                            if (nonsimRule.Rule_matches_lhs(tmp_l2)) {
                                for (TarRule tar : aprioriRuleTarMap.get(nonsimRule).getLeft()) {
                                    if (tar.IsInJSON(travisNormalizedJSON)) {
                                        for (TarRule gh_tar : aprioriRuleTarMap.get(nonsimRule).getRight()) {
                                            if (gh_tar.is_partial_match(GithubNormalizedJsonRootNode, 0.5)) { //TODO experiment
                                                if (new_github_nodes_dict.containsKey(pair)) {
                                                    new_github_nodes_dict.get(pair).add(nonsimRule);
                                                } else {
                                                    Vector<AprioriRule> vr = new Vector<>();
                                                    vr.add(nonsimRule);
                                                    new_github_nodes_dict.put(pair, vr);
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }

                    Vector<Pair<Pair<JSONObject, JSONObject>, JSONObject>> translationNodes_2 = new Vector<>();
                    for (Pair<JSONObject, JSONObject> pair : new_github_nodes_dict.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_github_nodes_dict.get(pair);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_github_nodes_dict.get(pair)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }
                        translationNodes_2.add(new ImmutablePair<>(pair, max_rule.github_node));
                    }
                    HashSet<Pair<JSONObject, JSONObject>> translatedNodes_2 = new HashSet<>();
                    HashMap<JSONObject, String> GitHubNodesAndParentsMap_NonSimBased = new HashMap<>();
                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes_2) {
                        JSONObject travis_parent = element.getLeft().getLeft();
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = element.getRight();
                        translatedNodes_2.add(element.getLeft());
//                JSONObject github_node_obj = TreeUtils.convertJsonNodeToJSONObject(github_node);
//                TreeUtils.transferParameters(travis_node,github_node,normalizationMap,transformationMap);
                        String val = "";
                        // use travis node to find matching translation parent in github
                        val = String.valueOf(travis_node.get("type"));
                        switch (cleanJsonVal(val)) {
                            case "language" -> {
                                TreeUtils.BFSandInsertWithType(GithubNormalizedJsonRootNode, "runs-on", 0, github_node);
                                GitHubNodesAndParentsMap_NonSimBased.put(github_node, "runs-on");
                            }
                            case "after_success" -> {
                                TreeUtils.BFSandInsertWithType(GithubNormalizedJsonRootNode, "build", 0, github_node);
                                GitHubNodesAndParentsMap_NonSimBased.put(github_node, "build");
                            }

                            default -> {
                                String github_parent_val = github_node.get("type").toString();
                                switch (cleanJsonVal(github_parent_val)) {

                                    default -> {

                                        if (!GithubNormalizedJsonRootNode.toString().contains(github_parent_val)) {
                                            TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, "github-file", github_node);
                                            GitHubNodesAndParentsMap_NonSimBased.put(github_node, "github-file");
                                        } else {
                                            if (!BFSForNode(GithubNormalizedJsonRootNode, github_node)) {
                                                for (Object child : github_node.getJSONArray("children")) {
                                                    TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, github_parent_val, (JSONObject) child);
                                                    GitHubNodesAndParentsMap_NonSimBased.put(github_node, github_parent_val);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }
                    //  : Augment translation with TARs


                    int nb_tars_applied = 0;

                    for (TarRule tar : tar_github_rules) {
                        if (tar.applyTar(GithubNormalizedJsonRootNode)) {
                            nb_tars_applied++;
                        }
                    }

                    //Apply child to parent rules for sim-rules nodes

                    HashMap<JSONObject, Vector<AprioriRule>> new_parent_nodes_dict_2 = new HashMap<>();

                    for (Map.Entry<JSONObject, String> temp_parent : GitHubNodesAndParentsMap_SimBased.entrySet()) {
                        JSONObject child_node = temp_parent.getKey();
                        String current_gh_parent = temp_parent.getValue();
                        if (current_gh_parent.equals("")) {
                            continue;
                        }
                        for (AprioriRule aprioriRule : child_to_parent_rules) {
                            if (aprioriRule.Rule_matches_lhs(child_node)) {
                                if (new_parent_nodes_dict_2.containsKey(child_node)) {
                                    new_parent_nodes_dict_2.get(child_node).add(aprioriRule);
                                } else {
                                    Vector<AprioriRule> vr = new Vector<>();
                                    vr.add(aprioriRule);
                                    new_parent_nodes_dict_2.put(child_node, vr);
                                }
                            }
                        }
                    }

                    for (JSONObject gh_node : new_parent_nodes_dict_2.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_parent_nodes_dict_2.get(gh_node);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_parent_nodes_dict_2.get(gh_node)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }

                        String new_parent_type = max_rule.github_node.get("type").toString();
                        String current_parent_type = GitHubNodesAndParentsMap_SimBased.get(gh_node);
                        if (!GitHubNodesAndParentsMap_SimBased.containsKey(gh_node) || !current_parent_type.equals(new_parent_type)) {
                            GitHubNodesAndParentsMap_SimBased.put(gh_node, new_parent_type);
                            if (!GithubNormalizedJsonRootNode.toString().contains(new_parent_type)) {
                                TreeUtils.insertMissingParent(GithubNormalizedJsonRootNode, gh_node, max_rule);
                            } else {
                                if (TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, new_parent_type, gh_node)) {
                                    BFSandRemoveNode(GithubNormalizedJsonRootNode, gh_node);
                                }
                            }
                        }
                    }

                    //Apply child to parent rules for non-sim-rules nodes

                    HashMap<JSONObject, Vector<AprioriRule>> new_parent_nodes_dict_3 = new HashMap<>();

                    for (Map.Entry<JSONObject, String> temp_parent : GitHubNodesAndParentsMap_NonSimBased.entrySet()) {
                        JSONObject child_node = temp_parent.getKey();
                        String new_parent_type = temp_parent.getValue();
                        if (new_parent_type.equals("")) {
                            continue;
                        }
                        for (AprioriRule aprioriRule : child_to_parent_rules) {
                            if (aprioriRule.Rule_matches_lhs(child_node)) {
                                if (new_parent_nodes_dict_3.containsKey(child_node)) {
                                    new_parent_nodes_dict_3.get(child_node).add(aprioriRule);
                                } else {
                                    Vector<AprioriRule> vr = new Vector<>();
                                    vr.add(aprioriRule);
                                    new_parent_nodes_dict_3.put(child_node, vr);
                                }
                            }
                        }
                    }

                    for (JSONObject gh_node : new_parent_nodes_dict_3.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_parent_nodes_dict_3.get(gh_node);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_parent_nodes_dict_3.get(gh_node)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }
                        String new_parent_type = max_rule.github_node.get("type").toString();
                        String current_parent_type = GitHubNodesAndParentsMap_NonSimBased.get(gh_node);
                        if (!GitHubNodesAndParentsMap_NonSimBased.containsKey(gh_node) || !current_parent_type.equals(new_parent_type)) {
                            GitHubNodesAndParentsMap_NonSimBased.put(gh_node, new_parent_type);
                            if (GithubNormalizedJsonRootNode.toString().contains(new_parent_type)) {
                                TreeUtils.insertMissingParent(GithubNormalizedJsonRootNode, gh_node, max_rule);
                            } else {
                                if (TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, new_parent_type, gh_node))
                                    BFSandRemoveNode(GithubNormalizedJsonRootNode, gh_node);
                            }
                        }
                    }


                    // Copy parameters from non-normed tree.


                    HashMap<JSONObject, String> GithubNodesAndTravisParentsMap = new HashMap<>();
                    GithubCompleteJsonRootNode = GithubNormalizedJsonRootNode;
                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes) {
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = new JSONObject(element.getRight().toMap());
                        GithubNodesAndTravisParentsMap.put(github_node, TreeUtils.transferParametersAndGetParentSet(travis_node, github_node, normalizationMap, transformationMap));
                        JSONObject new_github = (JSONObject) ((JSONArray) github_node.get("children")).get(0);
                        JSONObject original_github = (JSONObject) ((JSONArray) element.getRight().get("children")).get(0);
                        TreeUtils.BFSandInsertWithNode(GithubCompleteJsonRootNode, original_github, new_github);
                    }

                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes_2) {
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = new JSONObject(element.getRight().toMap());
                        GithubNodesAndTravisParentsMap.put(github_node, TreeUtils.transferParametersAndGetParentSet(travis_node, github_node, normalizationMap, transformationMap));
                    }
                    HashMap<JSONObject, Vector<AprioriRule>> new_parent_nodes_dict = new HashMap<>();

                    CleanNodes(GithubCompleteJsonRootNode);


                    // convert Json to Yaml
                    String yaml = TreeUtils.ConvertJsonToYaml(GithubCompleteJsonRootNode, 0, false, false);
                    FileWriterUtil fwriter = new FileWriterUtil();
                    String ghFileName = travisFile.split("/")[2] + "_" + travisFile.split("/")[3] + ".yml";
                    fwriter.writetoFile("./test_outputs/" + ghFileName, yaml);


                    // Calculate statistics and output to csv

                    File file1 = new File("./test_outputs/" + ghFileName);
                    List<String> lines1 = FileUtils.readLines(file1, "UTF-8");
                    Map<String, Double> map1 = textToMap(lines1);


                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOutputFile, true))) {
                        pw.println(ghFileName + ',' + L2_Nodes.size() + ',' + translatedNodes.size() + ',' + translatedNodes_2.size() + ',' + nb_tars_applied + ',' + (new_parent_nodes_dict_2.keySet().size() + new_parent_nodes_dict_3.keySet().size())  );
                    }

                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOfFilesOutputFile, true))) {
                        pw.print("./test_outputs/" + ghFileName + ';');
                        for (File original_gh_file : matchingFiles) {
                            pw.print(original_gh_file.getPath() + ',');
                        }
                        pw.print("\n");
                    }
                    System.out.println(travisFile.split("/")[2] + "  Done");
//                    System.exit(0);
                } catch (Exception e) {
                    System.out.println(travisFile.split("/")[2] + "  Error");
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


    }

}

package gamigration.travis2ga.translation_engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.tree.Tree;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import travisdiff.TravisCITree;
import util.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static util.SimilarityUtils.textToMap;
import static util.TreeUtils.*;

public class GHAToTravisTranslator {
    private static Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> alignAprioirRulesWithTars(Set<AprioriRule> nonSimRules, Set<TarRule> travisRuleSet, Set<TarRule> ghRuleSet) throws InterruptedException, ExecutionException {

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);

        List<Future<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>>> futures = new ArrayList<>();
        for (final AprioriRule input : nonSimRules) {
            Callable<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>> callable = new Callable<>() {
                public Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> call() throws Exception {
                    Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> output = null;
                    Vector<TarRule> ghTARs = new Vector<>();
                    Vector<TarRule> travisTARs = new Vector<>();
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
                            if (input.Rule_matches_lhs(l2_node)) {
                                ghTARs.add(ghTar);
                                break;
                            }
                        }
                    }
                    for (TarRule trTar : travisRuleSet) {
                        HashSet<JSONObject> temp_L2_Nodes = new HashSet<>();
                        HashSet<JSONObject> tempNodesSet = new HashSet<>();
                        TreeUtils.traverseTree(trTar.node, tempNodesSet);
                        tempNodesSet.forEach(travisNode -> {
                            if (TreeUtils.isNodeLevel2(travisNode)) {
                                temp_L2_Nodes.add(travisNode);
                            }
                        });

                        for (JSONObject l2_node : temp_L2_Nodes) {
                            if (input.Rule_matches_rhs(l2_node)) {
                                travisTARs.add(trTar);
                                break;
                            }
                        }
                    }
                    if (travisTARs.size() > 0 && ghTARs.size() > 0) {
                        output = new ImmutablePair<>(input, new ImmutablePair<>(ghTARs, travisTARs));
                    }
                    return output;
                }
            };
            futures.add(service.submit(callable));
        }

        service.shutdown();

        Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> outputs = new HashMap<>();
        for (Future<Pair<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>> future : futures) {
            if (future.get() != null) outputs.put(future.get().getLeft(), future.get().getRight());

        }
        return outputs;
    }


    public static void main(String[] Args) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String directory = "./OriginalAndNewGHAFiles/";
            final String csvPath = "train_test_split.csv";
//            String directory = "./dataForAprioriMiningMixed/";
//            final String csvPath = "combinedOldAndNew.csv";
            Vector<String> pathList = new Vector<>();
            CSVReader reader = new CSVReader(new FileReader(csvPath));
            String[] nextLine;
            try {
                reader.readNext(); // read and skip header
                // redefine so that it reads stuff following the new csv format
                List<String[]> unparsedPairs = new ArrayList<>();
                while ((nextLine = reader.readNext()) != null) {
                    String project_name = nextLine[0];
                    String train = nextLine[3];
                    if (train.equalsIgnoreCase("true")) {
                        continue;
                    }
                    String ghFileDir = directory + project_name + "/new/";
                    pathList.add(ghFileDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            int nbTr = 0;
            File csvOutputFile = new File("reverse-stats-out.csv");
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                String header = "fileName,NbL2NodesTotal,NbL2NodesTranslatedWithSim,NbL2NodesTranslatedWithNonSim,NbTARsAdded,NbChildToParentRulesApplied,TimeInMs,CosineSimilarityAvg,CosineSimilarityMax";
                pw.println(header);
            }
            File csvOfFilesOutputFile = new File("reverse-files-out.csv");
            try (PrintWriter pw = new PrintWriter(csvOfFilesOutputFile)) {
                String header = "GeneratefileName;OriginalFileName";
                pw.println(header);
            }


            for (String ghDir : pathList) {
                TreeUtils.resetQueues();
                Instant start = Instant.now();
                //
//                nbTr++;


                if (!ghDir.contains("dogtagpki_jss")) {
                    continue;
                }
//                if (nbTr > 10) {
//                    break;
//                }

                // Step 0:

                // load TAR rules
                HashSet<TarRule> tar_github_rules = new HashSet<>();
                BufferedReader br = new BufferedReader(new FileReader("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-github-v2-cleaned.jsonl"));
                String line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    try {
                        TarRule temp = new TarRule(new JSONObject(line));
                        tar_github_rules.add(temp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                HashSet<TarRule> tar_travis_rules = new HashSet<>();

                br = new BufferedReader(new FileReader("./src/main/java/gamigration/travis2ga/translation_engine/Current rules set/TAR/5_support/mined-rules-exp-travis-v2.jsonl"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    try {
                        tar_travis_rules.add(new TarRule(new JSONObject(line)));
                    } catch (Exception e) {
//                    e.printStackTrace();
                    }
                }

                // load apriori rules
                HashSet<AprioriRule> l_2_Apriori_rules = new HashSet<>();
                br = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/GHAToTravis/sim/rules_L2.csv"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
                    String travis_str = rule_str[0].split("->")[0].substring(1, rule_str[0].split("->")[0].length());
                    String gh_str = rule_str[0].split("->")[1].substring(2, rule_str[0].split("->")[1].length() - 1);
                    if (rule_str.length > 1) {
                        try {
                            l_2_Apriori_rules.add(new AprioriRule(new JSONObject(travis_str), new JSONObject(gh_str), Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                }
                //load non-sim rules
                HashSet<AprioriRule> l_2_Apriori_rules_non_sim = new HashSet<>();
                br = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/GHAToTravis/nonsim/rules_L2.csv"));
                line = "";
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
                    String travis_str = rule_str[0].split("->")[0];
                    if (travis_str.startsWith("\"")) travis_str = travis_str.substring(1);
                    String gh_str = rule_str[0].split("->")[1];
                    if (gh_str.endsWith("\"")) gh_str = gh_str.substring(2, gh_str.length() - 1);
//                System.out.println(travis_str);
//                System.out.println(gh_str);
                    if (rule_str.length > 1) {
                        try {
                            l_2_Apriori_rules_non_sim.add(new AprioriRule(new JSONObject(travis_str), new JSONObject(gh_str), Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(0);
                        }
                    } else {
                        continue;
                    }
                }

                Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>> aprioriRuleTarMap = new HashMap<>();
                start = Instant.now();

                if (Files.exists(Paths.get("apr_and_tars_map_reverse.ser"))) {
                    FileInputStream fileIn = new FileInputStream("apr_and_tars_map_reverse.ser");
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    try {
                        aprioriRuleTarMap = (Map<AprioriRule, Pair<Vector<TarRule>, Vector<TarRule>>>) in.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("AprioriRules and Matching TARs Loaded successfully");
                } else {
                    System.out.println("AprioriRules and Matching TARs are being aligned, this may take a while");
                    aprioriRuleTarMap = new HashMap<>();
                    aprioriRuleTarMap = alignAprioirRulesWithTars(l_2_Apriori_rules_non_sim, tar_travis_rules, tar_github_rules);

                    try {
                        FileOutputStream fileOut = new FileOutputStream("apr_and_tars_map_reverse.ser");
                        ObjectOutputStream out = new ObjectOutputStream(fileOut);
                        out.writeObject(aprioriRuleTarMap);
                        out.close();
                        fileOut.close();
                        System.out.println("AprioriRules and Matching TARs  generated and serialized successfully");
                        Instant end = Instant.now();
                        Duration timeElapsed = Duration.between(start, end);
                        System.out.println("Time taken for alignment: " + timeElapsed.toMinutes() + " minutes");
                        System.exit(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


//                HashSet<AprioriRule> parent_to_parent_rules = new HashSet<>();

//                BufferedReader br_temp = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/parents/rules_L2_parent_to_parent.csv"));
//                br_temp.readLine();
//
//                while ((line = br_temp.readLine()) != null) {
//                    String[] rule_str = line.replaceAll("\"\"", "\"").split("@;@");
//                    String travis_str = rule_str[0].split("->")[0].toLowerCase();
//                    String gh_str = rule_str[0].split("->")[1].toLowerCase();
//
//                    String temp_str = """
//                             {"children": [],
//                              "type": "PLACEHOLDER"
//                             }
//                            """;
//                    JSONObject travis_parent = new JSONObject(temp_str.replace("PLACEHOLDER", travis_str.strip()));
//                    JSONObject github_parent = new JSONObject(temp_str.replace("PLACEHOLDER", gh_str.strip()));
//                    if (rule_str.length > 1) {
//                        try {
//                            parent_to_parent_rules.add(new AprioriRule(travis_parent, github_parent, Double.valueOf(rule_str[1]), Double.valueOf(rule_str[2]), Double.valueOf(rule_str[3]), Double.valueOf(rule_str[4]), Double.valueOf(rule_str[5]), Double.valueOf(rule_str[6]), Double.valueOf(rule_str[7]), Double.valueOf(rule_str[8])));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        continue;
//                    }
//                }

                HashSet<AprioriRule> child_to_parent_rules = new HashSet<>();
                try {
                    BufferedReader br_temp = new BufferedReader(new FileReader("src/main/java/gamigration/travis2ga/translation_engine/Current rules set/Apriori/GHAToTravis/parents/rules_L2_parent_child_travis.csv"));
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
                        JSONObject github_parent = new JSONObject(temp_str.replace("PLACEHOLDER", github_parent_str.strip()));
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


//                String FolderPath = ghDir.substring(0, ghDir.lastIndexOf("/")) + "/../new/";
                File f = new File(ghDir);
                File[] matchingFiles = f.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith("yml");
                    }
                });
//                System.out.println(ghDir);

                try {
                    File mainGHFile = null;
                    for (File file : matchingFiles) {
                        if (file.toString().contains("build") || file.toString().contains("compile") || file.toString().contains("main") || file.toString().contains("maven") || file.toString().contains("ci") ) {
                            mainGHFile = file;
                        }
                    }
                    if (mainGHFile == null) {
                        int max_size = 0;
                        for (File file : matchingFiles) {
                            if (Files.size(file.toPath()) > max_size) {
                                mainGHFile = file;
                                max_size = (int) file.length();
                            }
                        }
                    }
                    // Step 1 : Parse file into tree
                    JSONObject TravisCompleteJsonRootNode = null;
                    TravisCITree github_actions = new TravisCITree();
                    Tree ghaTreeOriginal = github_actions.getTravisCITree(mainGHFile.getAbsolutePath());

                    // Step 2: make normalized copy of tree
                    Tree ghaTreeNormalized = ghaTreeOriginal.deepCopy();
                    HashMap<Tree, Tree> normalizationMap = new HashMap<>();

                    NormalizationUtils.normalizeTreeContentsWithMap(ghaTreeNormalized, ghaTreeOriginal, normalizationMap);
//            NormalizationUtils.normalizeTreeContentsWithMapAndParents(travisTreeNormalized, travisTreeOriginal, normalizationMap,parentsMap);
                    HashMap<JSONObject, Tree> transformationMap = new HashMap<>();
//            NormalizationUtils.normalizeTreeContents(travisTreeNormalized);
//            String travisJsonString = TreeUtils.treeToJson(travisTreeNormalized).toString(1);

                    JSONObject ghaNormalizedJSON = TreeUtils.treeToJsonWithMap(ghaTreeNormalized, transformationMap);
                    HashSet<JSONObject> ghaNodesNormalizedJSON = new HashSet<JSONObject>();
                    TreeUtils.traverseTree(ghaNormalizedJSON, ghaNodesNormalizedJSON);

                    // Step 3 : Find node levels that can be processed
                    //L-2 , L-3 Nodes
                    //L2 , L3 Nodes

                    HashSet<Pair<JSONObject, JSONObject>> L2_Nodes = new HashSet<>();
                    HashSet<JSONObject> L3_Nodes = new HashSet<>();
//                    HashSet<JSONObject> L_Top2_Nodes = new HashSet<>();
//                    HashSet<JSONObject> L_Top3_Nodes = new HashSet<>();
                    ghaNodesNormalizedJSON.forEach(travisNode -> {
                        if (TreeUtils.isNodeLevel2(travisNode)) {
                            L2_Nodes.add(new MutablePair<>(null, travisNode));
                        } else if (TreeUtils.isNodeLevel3(travisNode)) {
                            L3_Nodes.add(travisNode);
                        }
//                                else if (TreeUtils.isNodeNMinus1(travisNormalizedJSON, travisNode)) {
//                            Iterator<JsonNode> iter = travisNode.get("children").elements();
//                                    Iterator<Object> iter = travisNode.getJSONArray("children").iterator();
//                                    while (iter.hasNext()) {
//                                        JSONObject tmpJsn = (JSONObject) iter.next();
//                                        L_Top2_Nodes.add(tmpJsn);
//                                        Iterator<Object> iter_2 = tmpJsn.getJSONArray("children").iterator();
//                                        while (iter_2.hasNext()) {
//                                            L_Top3_Nodes.add((JSONObject) iter_2.next());
//                                        }
//                                    }
//                                }
                    });

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
                    // Step 4: Use translation rules

                    // match aprior simbased translation rules
                    HashMap<Pair<JSONObject, JSONObject>, Vector<AprioriRule>> new_travis_nodes_dict = new HashMap<>();
                    for (Pair<JSONObject, JSONObject> pair : L2_Nodes) {
                        for (AprioriRule aprioriRule : l_2_Apriori_rules) {
                            JSONObject tmp_l2 = pair.getRight();
                            if (aprioriRule.Rule_matches_lhs(tmp_l2)) {
                                if (new_travis_nodes_dict.containsKey(pair)) {
                                    new_travis_nodes_dict.get(pair).add(aprioriRule);
                                } else {
                                    Vector<AprioriRule> vr = new Vector<>();
                                    vr.add(aprioriRule);
                                    new_travis_nodes_dict.put(pair, vr);
                                }
                            }
                        }
                    }
                    Set<Pair<JSONObject, JSONObject>> L2NodesNotTranslatableWithSim = new HashSet<>(L2_Nodes);
                    L2NodesNotTranslatableWithSim.removeAll(new_travis_nodes_dict.keySet());

                    // find best sim-based translation rule for translatable nodes
                    Vector<Pair<Pair<JSONObject, JSONObject>, JSONObject>> translationNodes = new Vector<>();
                    for (Pair<JSONObject, JSONObject> pair : new_travis_nodes_dict.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_travis_nodes_dict.get(pair);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_travis_nodes_dict.get(pair)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }
                        translationNodes.add(new ImmutablePair<>(pair, max_rule.github_node));
                    }

                    String basicTravisStructure = """
                                 {
                                     "children": [
                                         {
                                             "children": [
                                                 {
                                                     "children": [],
                                                     "type": "java"
                                                 }
                                             ],
                                             "type": "language"
                                         },
                                         {
                                             "children": [],
                                             "type": "jdk"
                                         },
                                         {
                                             "children": [],
                                             "type": "android"
                                         },
                                         {
                                             "children": [],
                                             "type": "branches"
                                         },
                                         {
                                             "children": [],
                                             "type": "before_install"
                                         },
                                         {
                                             "children": [],
                                             "type": "install"
                                         },
                                         {
                                             "children": [],
                                             "type": "before_script"
                                         },
                                         {
                                             "children": [],
                                             "type": "script"
                                         },
                                         {
                                             "children": [],
                                             "type": "after_success"
                                         },
                                         {
                                             "children": [],
                                             "type": "after_failure"
                                         },
                                         {
                                             "children": [],
                                             "type": "before_deploy"
                                         },
                                         {
                                             "children": [],
                                             "type": "deploy"
                                         },
                                         {
                                             "children": [],
                                             "type": "after_deploy"
                                         },
                                         {
                                             "children": [],
                                             "type": "after_script"
                                         },
                                     ],
                                     "type": "travis-file"
                                 }
                            """;
                    // apply and save results of best sim-based rules
                    HashSet<Pair<JSONObject, JSONObject>> translatedNodes = new HashSet<>();
                    JSONObject TravisNormalizedJsonRootNode = new JSONObject(basicTravisStructure);
                    HashMap<JSONObject, String> TravisNodesAndParentsMap_SimBased = new HashMap<>();
                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes) {
                        JSONObject travis_parent = element.getLeft().getLeft();
                        JSONObject gha_node = element.getLeft().getRight();
                        JSONObject travis_node = element.getRight();
                        translatedNodes.add(element.getLeft());
//                JSONObject github_node_obj = TreeUtils.convertJsonNodeToJSONObject(github_node);
//                TreeUtils.transferParameters(travis_node,github_node,normalizationMap,transformationMap);
                        String travis_parent_val = "";

                        // use travis node to find matching translation parent in github
                        travis_parent_val = String.valueOf(gha_node.get("type"));
                        switch (cleanJsonVal(travis_parent_val)) {
                            case "runs-on" -> {
                                TreeUtils.BFSandInsertWithType(TravisNormalizedJsonRootNode, "language", 0, travis_node);
                                TravisNodesAndParentsMap_SimBased.put(travis_node, "language");
                            }
                            case "build" -> {
                                TreeUtils.BFSandInsertWithType(TravisNormalizedJsonRootNode, "script", 0, travis_node);
                                TravisNodesAndParentsMap_SimBased.put(travis_node, "script");
                            }
                            default -> {
                                  travis_parent_val = travis_node.get("type").toString();
//                                MutableBoolean inserted = new MutableBoolean(false);
                                switch (cleanJsonVal(travis_parent_val)) {
                                    case "script" -> {
                                        TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, "script", travis_node);
                                        TravisNodesAndParentsMap_SimBased.put(travis_node, "script");
                                    }
                                    default -> {
                                        if (!TravisNormalizedJsonRootNode.toString().contains(travis_parent_val)) {
                                            TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, "travis-file", travis_node);
                                            TravisNodesAndParentsMap_SimBased.put(travis_node, "travis-file");
                                        } else {
                                            if (!BFSForNode(TravisNormalizedJsonRootNode, travis_node)) {
                                                for (int i = 0; i < travis_node.getJSONArray("children").length(); i++) {
                                                    JSONObject temp_child = travis_node.getJSONArray("children").getJSONObject(i);
                                                    TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, travis_parent_val, temp_child);
                                                    TravisNodesAndParentsMap_SimBased.put(temp_child, travis_parent_val);
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

                    new_travis_nodes_dict = new HashMap<>();
                    for (AprioriRule nonsimRule : aprioriRuleTarMap.keySet()) {
                        for (Pair<JSONObject, JSONObject> pair : L2NodesNotTranslatableWithSim) {
                            JSONObject tmp_l2 = pair.getRight();
                            if (nonsimRule.Rule_matches_lhs(tmp_l2)) {
                                for (TarRule tar : aprioriRuleTarMap.get(nonsimRule).getLeft()) {
                                    if (tar.IsInJSON(ghaNormalizedJSON)) {
                                        for (TarRule gh_tar : aprioriRuleTarMap.get(nonsimRule).getRight()) {
                                            if (gh_tar.is_partial_match(TravisNormalizedJsonRootNode, 0.5)) {
                                                if (new_travis_nodes_dict.containsKey(pair)) {
                                                    new_travis_nodes_dict.get(pair).add(nonsimRule);
                                                } else {
                                                    Vector<AprioriRule> vr = new Vector<>();
                                                    vr.add(nonsimRule);
                                                    new_travis_nodes_dict.put(pair, vr);
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }

                    Vector<Pair<Pair<JSONObject, JSONObject>, JSONObject>> translationNodes_2 = new Vector<>();
                    for (Pair<JSONObject, JSONObject> pair : new_travis_nodes_dict.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_travis_nodes_dict.get(pair);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_travis_nodes_dict.get(pair)) {
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
                            case "runs-on" -> {
                                TreeUtils.BFSandInsertWithType(TravisNormalizedJsonRootNode, "language", 0, github_node);
                                GitHubNodesAndParentsMap_NonSimBased.put(github_node, "language");
                            }
                            case "build" -> {
                                TreeUtils.BFSandInsertWithType(TravisNormalizedJsonRootNode, "script", 0, github_node);
                                GitHubNodesAndParentsMap_NonSimBased.put(github_node, "script");
                            }
//                        case "x3":
//                            TreeUtils.BFSandInsertType(GithubNormalizedJsonRootNode,"runs-on",0,github_node_obj);
//                            break;
                            default -> {
                                String github_parent_val = github_node.get("type").toString();
                                switch (cleanJsonVal(github_parent_val)) {
                                    case "script" -> {
                                        TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, "script", github_node);
                                        GitHubNodesAndParentsMap_NonSimBased.put(github_node, "script");
                                    }
                                    default -> {

                                        if (!TravisNormalizedJsonRootNode.toString().contains(github_parent_val)) {
                                            TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, "travis-file", github_node);
                                            GitHubNodesAndParentsMap_NonSimBased.put(github_node, "travis-file");
                                        } else {
                                            if (!BFSForNode(TravisNormalizedJsonRootNode, github_node)) {
                                                for (Object child : github_node.getJSONArray("children")) {
                                                    TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, github_parent_val, (JSONObject) child);
                                                    GitHubNodesAndParentsMap_NonSimBased.put(github_node, github_parent_val);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }
                    // Step 5 : Augment translation with TARs

//                    System.out.println(tar_github_rules.size());
                    int nb_tars_applied = 0;
                    //TODO clean tar application (no -name, no mvn-cmd insertion, etc.)
                    Comparator<TarRule> TarComp = (TarRule o1, TarRule o2) -> (Integer.compare(o2.node.getJSONArray("children").length(), o1.node.getJSONArray("children").length()));
                    List<TarRule> OrderedRules = tar_travis_rules.stream().sorted(TarComp).collect(Collectors.toList());
                    for (TarRule tar : OrderedRules) {
//                        System.out.println(tar.node.toString(1).replace("\n", ""));
                        if (tar.applyTar(TravisNormalizedJsonRootNode)) {
                            nb_tars_applied++;
                        }
                    }
//                    System.exit(0);
                    //Step 6:
                    //Apply child to parent rules for sim-rules nodes
                    HashMap<JSONObject, Vector<AprioriRule>> new_parent_nodes_dict_2 = new HashMap<>();

                    for (Map.Entry<JSONObject, String> temp_parent : TravisNodesAndParentsMap_SimBased.entrySet()) {
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
//                        System.out.println(max_rule.travis_node);
//                        System.out.println(max_rule.github_node);
                        String new_parent_type = max_rule.github_node.get("type").toString();
                        String current_parent_type = TravisNodesAndParentsMap_SimBased.get(gh_node);
                        if (!TravisNodesAndParentsMap_SimBased.containsKey(gh_node) || !current_parent_type.equals(new_parent_type)) {
                            TravisNodesAndParentsMap_SimBased.put(gh_node, new_parent_type);
                            if (!TravisNormalizedJsonRootNode.toString().contains(new_parent_type)) {
                                TreeUtils.insertMissingParent(TravisNormalizedJsonRootNode, gh_node, max_rule);
                            } else {
                                if (TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, new_parent_type, gh_node)) {
//                                TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, new_parent_type, gh_node);
                                    BFSandRemoveNode(TravisNormalizedJsonRootNode, gh_node);
                                }
                            }
                        }
                    }
                    //TODO add reinsertion of nodes at root via DFS

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
                            if (!TravisNormalizedJsonRootNode.toString().contains(new_parent_type)) {
                                TreeUtils.insertMissingParent(TravisNormalizedJsonRootNode, gh_node, max_rule);
                            } else {
                                if (TreeUtils.DFSandInsertLastWithType(TravisNormalizedJsonRootNode, new_parent_type, gh_node)) {
//                                TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, new_parent_type, gh_node);
                                    BFSandRemoveNode(TravisNormalizedJsonRootNode, gh_node);
                                }
                            }
                        }
                    }

                    //TODO add reinsertion of nodes at root via DFS

                    // Step 7: Copy parameters from non-normed tree.


                    HashMap<JSONObject, String> GithubNodesAndTravisParentsMap = new HashMap<>();
                    TravisCompleteJsonRootNode = TravisNormalizedJsonRootNode;
                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes) {
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = new JSONObject(element.getRight().toMap());
                        GithubNodesAndTravisParentsMap.put(github_node, TreeUtils.transferParametersAndGetParentSet(travis_node, github_node, normalizationMap, transformationMap));
//                        System.out.println(github_node);
                        for (int i = 0; i < github_node.getJSONArray("children").length(); i++) {
                            JSONObject new_github = (JSONObject) ((JSONArray) github_node.get("children")).get(i);
                            JSONObject original_github = (JSONObject) ((JSONArray) element.getRight().get("children")).get(i);
                            TreeUtils.BFSandInsertWithNode(TravisCompleteJsonRootNode, original_github, new_github);
                        }
                    }

                    for (Pair<Pair<JSONObject, JSONObject>, JSONObject> element : translationNodes_2) {
                        JSONObject travis_node = element.getLeft().getRight();
                        JSONObject github_node = new JSONObject(element.getRight().toMap());
                        GithubNodesAndTravisParentsMap.put(github_node, TreeUtils.transferParametersAndGetParentSet(travis_node, github_node, normalizationMap, transformationMap));

                        for (int i = 0; i < github_node.getJSONArray("children").length(); i++) {
                            JSONObject new_github = (JSONObject) ((JSONArray) github_node.get("children")).get(i);
                            JSONObject original_github = (JSONObject) ((JSONArray) element.getRight().get("children")).get(i);
                            TreeUtils.BFSandInsertWithNode(TravisCompleteJsonRootNode, original_github, new_github);
                        }
                    }
//                    HashMap<JSONObject, Vector<AprioriRule>> new_parent_nodes_dict = new HashMap<>();

                    //Step 6.5 add intermediary nodes with parent to parent :
                    // Use NodesAndParentsMap along with parent to parent

/*
                    for (Map.Entry<JSONObject, String> temp_parent : GithubNodesAndTravisParentsMap.entrySet()) {
                        JSONObject for_parent_node = temp_parent.getKey();
                        String travis_parent_type = temp_parent.getValue();
                        if (travis_parent_type.equals("")) {
                            continue;
                        }
                        String temp_str = """
                                 {"children": [],
                                  "type": "PLACEHOLDER"
                                 }
                                """;
//                        System.out.println(temp_str.replace("PLACEHOLDER", travis_parent_type));
                        JSONObject temp_travis_parent = new JSONObject(temp_str.replace("PLACEHOLDER", travis_parent_type));
                        for (AprioriRule aprioriRule : parent_to_parent_rules) {
                            if (aprioriRule.Rule_matches_lhs(temp_travis_parent)) {
                                if (new_parent_nodes_dict.containsKey(for_parent_node)) {
                                    new_parent_nodes_dict.get(for_parent_node).add(aprioriRule);
                                } else {
                                    Vector<AprioriRule> vr = new Vector<>();
                                    vr.add(aprioriRule);
                                    new_parent_nodes_dict.put(for_parent_node, vr);
                                }
                            }
                        }
                    }
//
                    for (JSONObject gh_node : new_parent_nodes_dict.keySet()) {
                        double max_Stat = 0.0;
                        Vector<AprioriRule> apri_rules = new_parent_nodes_dict.get(gh_node);
                        if (apri_rules.size() == 0) {
                            continue;
                        }
                        AprioriRule max_rule = apri_rules.get(0);
                        for (AprioriRule rule : new_parent_nodes_dict.get(gh_node)) {
                            if (rule.Conf_2 > max_Stat) {
                                max_rule = rule;
                                max_Stat = rule.Conf_2;
                            }
                        }
                        String new_parent_type = max_rule.github_node.get("type").toString();
                        if (GitHubNodesAndParentsMap_SimBased.containsKey(gh_node)) {
                            String current_parent_type = GitHubNodesAndParentsMap_SimBased.get(gh_node);
                            if (!GitHubNodesAndParentsMap_SimBased.containsKey(gh_node) || !current_parent_type.equals(new_parent_type)) {
                                if (GithubNormalizedJsonRootNode.toString().contains(new_parent_type)) {
                                    TreeUtils.insertMissingParent(GithubNormalizedJsonRootNode, gh_node, max_rule);
                                } else {
                                    TreeUtils.DFSandInsertLastWithType(GithubNormalizedJsonRootNode, new_parent_type, gh_node);
                                    BFSandRemoveNode(GithubNormalizedJsonRootNode, gh_node);
                                }
                            }
                        }
                    }


                    // add dash name

//                    System.out.println(GithubNormalizedJsonRootNode);


                    // hashmap travis tar --> [apriori] rules

                    // hashmap apriori rules --> github_tar

//                    System.out.println(GithubCompleteJsonRootNode);

*/
                    // Step 8: Clean tree
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "mvn-cmd");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "notifications");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "gradle-cmd");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "jdk");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "android");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "branches");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "before_install");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "install");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "before_script");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "script");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "dist");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "after_success");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "after_failure");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "before_deploy");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "deploy");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "after_deploy");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "after_script");
                    // CleanTreeOfLeafs(TravisCompleteJsonRootNode, "env");
                    // AssignDefaultValueToLeaf(TravisCompleteJsonRootNode, "name", "Placeholder");
                    CleanNodes(TravisCompleteJsonRootNode);
                    // RemoveRedundancies(TravisCompleteJsonRootNode);

                    //Step 9 convert Json to Yaml

                    String yaml = TreeUtils.ConvertJsonToYaml(TravisCompleteJsonRootNode, 0, false, false);
                    FileWriterUtil fwriter = new FileWriterUtil();
                    String ghFileName = ghDir.split("/")[2] + "_travis.yml";
                    String outFolder = ghDir.substring(0, ghDir.lastIndexOf("/")) + "/../generated_travis/";
                    Files.createDirectories(Paths.get(outFolder));
                    fwriter.writetoFile(outFolder + ghFileName, yaml);

                    // Calculate statistics and output to csv
//                    File file1 = new File("./test_outputs/" + ghFileName);
                    BufferedReader bufferedReader;
                    List<String> lines1 = new ArrayList<>();
                    try {
                        bufferedReader = new BufferedReader(new FileReader(outFolder + ghFileName));
                        String TempLine = bufferedReader.readLine();
                        while (TempLine != null) {
                            if (!TempLine.startsWith("#")) {
                                if (TempLine.contains("#")) {
                                    TempLine = TempLine.split("#")[0];
                                }
                                lines1.add(TempLine);
                            }
//                            System.out.println(TempLine);
                            // read next line
                            TempLine = bufferedReader.readLine();
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    Map<String, Double> map1 = textToMap(lines1);
                    ArrayList<Double> cosine_sim_list = new ArrayList<>();
                    File original_tr_file = new File(ghDir+"/../original/travis.yml");
                        List<String> lines2 = new ArrayList<>();
                        try {
                            bufferedReader = new BufferedReader(new FileReader(original_tr_file));
                            String TempLine = bufferedReader.readLine();
                            while (TempLine != null) {
                                if (!TempLine.startsWith("#")) {
                                    if (TempLine.contains("#")) {
                                        TempLine = TempLine.split("#")[0];
                                    }
//                                System.out.println(TempLine);
                                    lines2.add(TempLine);
                                }
                                // read next line
                                TempLine = bufferedReader.readLine();
                            }
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Map<String, Double> map2 = textToMap(lines2);
                        double cosine_sim = SimilarityUtils.cosineSimilarity(map1, map2);
                        cosine_sim_list.add(cosine_sim);

                    OptionalDouble average_cosine = cosine_sim_list.stream().mapToDouble(a -> a).average();
                    OptionalDouble max_cosine = cosine_sim_list.stream().mapToDouble(a -> a).max();

                    double average_cosine_sim = average_cosine.getAsDouble();
                    double max_cosine_sim = max_cosine.getAsDouble();
                    String average_cosine_sim_string = String.format("%.2f", average_cosine_sim);
                    String max_cosine_sim_string = String.format("%.2f", max_cosine_sim);
                    Instant end = Instant.now();
                    Duration timeElapsed = Duration.between(start, end);
                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOutputFile, true))) {
//                 String header = "fileName,NbL2NodesTotal,NbL2NodesTranslatedWithSim,NbL2NodesTranslatedWithNonSim,NbTARsAdded,NbChildToParentRulesApplied,NbParentToParentRulesApplied";
                        pw.println(ghFileName + ',' + L2_Nodes.size() + ',' + translatedNodes.size() + ',' + translatedNodes_2.size() + ',' + nb_tars_applied + ',' + (new_parent_nodes_dict_2.keySet().size() + new_parent_nodes_dict_3.keySet().size()) + "," +timeElapsed.toMillis() +","+ average_cosine_sim_string + "," + max_cosine_sim_string);
                        pw.flush();
                    }


                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOfFilesOutputFile, true))) {
                        pw.print(outFolder + ghFileName + ';');

                            pw.print(ghDir+"/../original/travis.yml" );

                        pw.print("\n");
                        pw.flush();
                    }
                    System.out.println(ghDir.split("/")[2] + "  Done");
//                    System.exit(1);
//                    System.exit(0);
                } catch (Exception e) {
                    System.out.println(ghDir.split("/")[2] + "  Error");
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


    }

}

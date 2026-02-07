package util;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;

public class NormalizationUtils {
    public static String rebalanceBrackets(String target) {
        int openingBracketCount = target.length() - target.replace("[", "").length();
        int closingBracketCount = target.length() - target.replace("]", "").length();
        if (openingBracketCount > closingBracketCount) {
            target = target + ']';
        }
        return target;
    }

    public static String normalizeForLeafMatching(String target) {
        String normalized = target.replaceAll("jobs\\.(([A-z0-9]|(_|-))+)\\.", "jobs.build.");
        return normalized;
    }

    public static String normalizeNodeLabel(String target) {
        target = target.toLowerCase();
        target = target.replaceAll("^\"|\"$", "");
        target = target.replaceAll("  ", " "); //double space to single
        //actions/checkout@v2
        target = target.replaceAll("(.*/.*)@.*$", "$1@version");
        target = target.replaceAll("docker.*$", "docker-cmd");
        target = target.replaceAll("apt-get .*$", "apt-cmd");
        target = target.replaceAll("if .*$", "shell-cmd");
        target = target.replaceAll("wget .*$", "wget-cmd");
        target = target.replaceAll("export .*$", "export-cmd");
        target = target.replaceAll("(\\.\\/)?mvnw .*$", "mvn-cmd");
        target = target.replaceAll("mvn .*$", "mvn-cmd");
        target = target.replaceAll("for .*$", "for-cmd");
        target = target.replaceAll("cd .*$", "cd-cmd");
        target = target.replaceAll("(\\.\\/)?gradlew .*$", "gradle-cmd");
        target = target.replaceAll("(\\.\\/)?gradle .*$", "gradle-cmd");
        target = target.replaceAll("chmod .*$", "chmod-cmd");
        target = target.replaceAll("curl .*$", "curl-cmd");
        target = target.replaceAll("mkdir .*$", "mkdir-cmd");
        target = target.replaceAll("npm .*$", "npm-cmd");
        target = target.replaceAll("coverage .*$", "coverage-cmd");
        target = target.replaceAll("python .*$", "python-cmd");
        target = target.replaceAll("checkstyle .*$", "checkstyle-cmd");
        target = target.replaceAll("jacoco .*$", "jacoco-cmd");
        target = target.replaceAll("echo .*$", "echo-cmd");
        target = target.replaceAll("apt-get .*$", "apt-cmd");
        target = target.replaceAll("ant .*$", "ant-cmd");
        target = target.replaceAll("codecov .*$", "codecov-cmd");
        target = target.replaceAll("\\$\\{\\{ hashfiles\\(.*\\) \\}\\}", "\\${{ hashfiles(arg) }}");
        target = target.replaceAll("\\n", "");
        target = target.replaceAll(".*(jdk[0-9\\-\\.ea]+)", "$1");
        //from docker script
        target = target.replaceAll("^[a-zA-Z0-9_!#$%&'*+/=?'{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)", "EMAIL");
        target = target.replaceAll("(?i)\\b((?:https?:(?:/{1,3}|[a-z0-9%])|[a-z0-9.\\-]+[.](?:com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)/)(?:[^\\s()<>{}\\[\\]]+|\\([^\\s()]*?\\([^\\s()]+\\)[^\\s()]*?\\)|\\([^\\s]+?\\))+(?:\\([^\\s()]*?\\([^\\s()]+\\)[^\\s()]*?\\)|\\([^\\s]+?\\)|[^\\s`!()\\[\\]{};:\\'\".,<>?«»“”‘’])|(?:(?<!@)[a-z0-9]+(?:[.\\-][a-z0-9]+)*[.](?:com|net|org|edu|gov|mil|aero|asia|biz|cat|coop|info|int|jobs|mobi|museum|name|post|pro|tel|travel|xxx|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|Ja|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw)\\b/?(?!@)))", "PATH-OR-URL");
        //implement more normalizations
        target = target.replaceAll("ubuntu\\-[0-9]{2}\\.[0-9]{2}", "ubuntu-latest");
        target = target.replaceAll("matrix\\.(java-version|java_version|jdk)", "matrix.java");
        target = target.replaceAll("\\$home\\/\\.m2[\\/a-z]*", "~/.m2");
        target = target.replaceAll("~\\/\\.m2[\\/a-z]*", "~/.m2");
        target = target.replaceAll("\\$home\\/\\.gradle[\\/a-z]*", "~/.gradle");
        target = target.replaceAll("~\\/\\.gradle[\\/a-z]*", "~/.gradle");
        target = target.replaceAll("\\$home\\/", "~/");

        target = target.replaceAll("matrix\\.((java(\\-|\\_)version)|jdk)", "matrix.java");
        target = target.replaceAll("(maven|m2)[\\-\\\\n]*", "maven-");
        //target = target.replaceAll("(^([a-z]|[A-Z]):(?=\\\\(?![\\0\\37<>:\"/\\\\|?*])|\\/(?![\\0\\37<>:\"/\\\\|?*])|$)|^\\\\(?=[\\\\\\/][^\\0-\\37<>:\"/\\\\|?*]+)|^(?=(\\\\|\\/)$)|^\\.(?=(\\\\|\\/)$)|^\\.\\.(?=(\\\\|\\/)$)|^(?=(\\\\|\\/)[^\\0\\37<>:\"/\\\\|?*]+)|^\\.(?=(\\\\|\\/)[^\\0\\37<>:\"/\\\\|?*]+)|^\\.\\.(?=(\\\\|\\/)[^\\0\\37<>:\"/\\\\|?*]+))((\\\\|\\/)[^\\0\\37<>:\"/\\\\|?*]+|(\\\\|\\/)$)*()$", "PATH");
        //target = target.replaceAll("^\\/$|(^(?=\\/)|^\\.|^\\.\\.)(\\/(?=[^/\\0])[^/\\0]+)*\\/?$","PATH");normalizeTreeString

        return target;
    }

    private static String normalizeBothTypes(String target) {
        target = target.toLowerCase();
        target = target.replaceAll("docker.*$", "docker-cmd");
        target = target.replaceAll("apt-get .*$", "apt-cmd");
        target = target.replaceAll("if .*$", "shell-cmd");
        target = target.replaceAll("wget .*$", "wget-cmd");
        target = target.replaceAll("export .*$", "export-cmd");
        target = target.replaceAll("mvnw .*$", "mvnw-cmd");
        target = target.replaceAll("mvn .*$", "mvn-cmd]");
        target = target.replaceAll("for .*$", "for-cmd]");
        target = target.replaceAll("cd .*$", "cd-cmd]");
        target = target.replaceAll("gradlew .*$", "gradle-cmd]");
        target = target.replaceAll("\\.\\/gradlew .*$", "gradle-cmd]");

        target = target.replaceAll("git .*$", "git-cmd]");
        target = target.replaceAll("if: .*$", "if-cmd]");
        target = target.replaceAll("bash .*$", "bash-cmd]");
        // target = target.replaceAll("export .*$", "bash-cmd]");
        target = target.replaceAll("run:", "");
        target = target.replaceAll("key: .*$", "key_secret");
        target = target.replaceAll("\\.\\/mvnw: .*$", "mvnw_cmd]");
        target = target.replaceAll("chmod .*$", "chmod_cmd]");
        target = target.replaceAll("curl .*$", "curl_cmd]");
        target = target.replaceAll("docker-compose .*$", "docker-comp-cmd]");
        target = target.replaceAll("mkdir .*$", "mkdir-cmd]");
        target = target.replaceAll("npm .*$", "npm-cmd]");
        target = target.replaceAll("coverage .*$", "coverage-cmd]");
        target = target.replaceAll("install.sh .*$", "install.sh-cmd]");
        target = target.replaceAll("python .*$", "python-cmd]");
        target = target.replaceAll("checkstyle .*$", "checkstyle-cmd]");
        target = target.replaceAll("jacoco .*$", "jacoco-cmd]");
        target = target.replaceAll("\\.run\\.\\[.*java .*$", ".run.[java-cmd]");
        target = target.replaceAll("echo .*$", "echo-cmd]");
        target = target.replaceAll("apt-get .*$", "apt-cmd]");
        target = target.replaceAll("bazel .*$", "bazel-cmd]");
        target = target.replaceAll("checkstyle .*$", "checkstyle-cmd]");
        target = target.replaceAll("ant .*$", "ant-cmd]");
        target = target.replaceAll("ruby .*$", "ruby-cmd]");
        target = target.replaceAll("pip .*$", "pip-cmd]");
        target = target.replaceAll("pip3 .*$", "pip-cmd]");
        target = target.replaceAll("run.sh .*$", "run-cmd]");
        target = target.replaceAll("checkinstall .*$", "check-cmd]");
        target = target.replaceAll("codecov .*$", "codecov-cmd]");
        target = target.replaceAll("\\$\\{\\{ hashfiles\\(.*\\) \\}\\}", "\\${{ hashfiles(arg) }}");
        if (target.matches(".*key\\.\\[.*\\$.*\\$.*\\]")) {
            target = target.replaceAll("\\}\\}\\-[a-z0-9\\-]+\\$\\{\\{", "}}-build-system-\\${{");
        }
        if (target.matches(".*key\\.\\[.*\\$.*\\]")) {
            target = target.replaceAll("\\[[a-z0-9\\-]+\\$\\{\\{", "[build-system-\\${{");
        }
        target = target.replaceAll("\\n", "");


        return target;
    }

    public static String normalizeTravisStatements(String target) {
        //Continue replacement of cmds

        target = normalizeBothTypes(target);
        target = target.replaceAll("\\.secure\\.\\[.*\\]", ".secure.[X]");

        if (target.matches(".*\\.branch((es)?)\\..*\\[[A-z]+\\]")) {
            target = target.replaceAll("\\[[A-z]+\\]", "[master]");
        }
        //Continue replacement of cmds


        if (target.matches(".*\\.\\[[A-z]+jdk(([0-9]+)|\\-ea)\\]")) {
            target = target.replaceAll("\\.\\[[A-z]+jdk", ".[jdk");
            //target = target.replaceAll(".\\[[A-z]+jdk(([0-9]+)|\\-ea)\\]", ".[jdkX]");
        }
        target = NormalizationUtils.rebalanceBrackets(target);
        return target;
    }

    public static String normalizeGithubStatements(String target) {
        //Continue replacement of cmds
        target = normalizeBothTypes(target);
        if (target.matches(".*\\/.*@.*")) { //actions/checkout@v2
            target = target.replaceAll("@.*", "@version");
        }
        if (target.matches(".*\\.branches\\.\\[[A-z]+\\]")) {
            target = target.replaceAll("\\.\\[[A-z]+\\]", ".[master]");
        }
        target = target.replaceAll("jobs\\.(([A-z0-9]|(_|-))+)\\.", "jobs.build.");


        if (target.matches(".*strategy\\.matrix\\.((java)([\\-\\_]version)?|jdk)\\.\\[[0-9\\.]+\\]")) {
            target = target.replaceAll("strategy\\.matrix\\.((java)([\\-\\_]version)?|jdk)\\.", "strategy.matrix.java.");

        }
        //target = target.replaceAll("strategy\\.matrix\\.((java)([\\-\\_]version)?|jdk)\\.\\[[0-9\\.]+\\]", "strategy.matrix.java.[X]");
        //target = target.replaceFirst("\\.java\\-version\\.\\[.*\\]", ".java-version.[X]");

        target = NormalizationUtils.rebalanceBrackets(target);
        return target;
    }


    public static void normalizeTreeContents(Tree root) {
        root.preOrder().forEach(node -> {
            if (node.hasLabel()) {
                node.setLabel(normalizeNodeLabel(node.getLabel()));
            }
            if (TreeUtils.isNodeParentJobs(node)) {
                node.setLabel("build");
            }
            if (TreeUtils.isNodeBranchName(node)) {
                node.setLabel("branch-name");
            }

            if (node.getLabel().equals("matrix")) {
                if (node.getParent().getChild(1).getType().equals(TypeSet.type("OBJECT"))) {
                    Tree temp = node.getParent().getChild(1).getChild(0).getChild(0);
                    String tempLabel = temp.getLabel().replaceAll("^\"|\"$", "");
                    if ((tempLabel.equals("java-version") || tempLabel.equals("java_version")) || tempLabel.equals("jdk")) {
                        temp.setLabel("java");
                    }
                }
            }
            if (node.getLabel().equals("secure") && node.getParent().getChild(1).getType().equals(TypeSet.type("STRING"))) {
                node.getParent().getChild(1).getChild(0).setLabel("TOKEN");
            }

        });
    }



    public static void normalizeTreeContentsWithMap(Tree new_tree, Tree original_tree, HashMap<Tree, Tree> normalizationMap) {
        new_tree.preOrder().forEach(node -> {
            Tree origin_node = TreeUtils.findNodeInTree(node, original_tree);

            if (node.hasLabel()) {
                normalizationMap.put(node,origin_node);
                node.setLabel(normalizeNodeLabel(node.getLabel()));
            }
            if (TreeUtils.isNodeParentJobs(node)) {
                normalizationMap.put(node,origin_node);
                node.setLabel("build");
            }
            if (TreeUtils.isNodeBranchName(node)) {
                normalizationMap.put(node,origin_node);
                node.setLabel("branch-name");
            }

            if (node.getLabel().equals("matrix")) {
                if (node.getParent().getChild(1).getType().equals(TypeSet.type("OBJECT"))) {
                    Tree temp = node.getParent().getChild(1).getChild(0).getChild(0);
                    String tempLabel = temp.getLabel().replaceAll("^\"|\"$", "");
                    if ((tempLabel.equals("java-version") || tempLabel.equals("java_version")) || tempLabel.equals("jdk")) {
                        origin_node =   TreeUtils.findNodeInTree(temp, original_tree);
                        normalizationMap.put(temp,origin_node);
                        temp.setLabel("java");
                    }
                }
            }
            if (node.getLabel().equals("secure") && node.getParent().getChild(1).getType().equals(TypeSet.type("STRING"))) {
                Tree token_node =  node.getParent().getChild(1).getChild(0);
                origin_node = TreeUtils.findNodeInTree(token_node,original_tree);
                normalizationMap.put(token_node,origin_node);
                token_node.setLabel("TOKEN");
            }

        });
    }

}

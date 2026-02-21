package gamigration.travis2ga;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.gumtreediff.tree.Tree;

import travisdiff.TravisCITree;
import static util.NormalizationUtils.normalizeTreeContents;
import util.TreeUtils;

public class App_generate_Jsons {








// TODO use fa and fp for json generation

    public static void main(String[] args) {
        // 确保输出目录存在
        File outputDir = new File("JsonAstsForTAR/py");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            System.out.println("创建输出目录: " + outputDir.getAbsolutePath());
        }
        
        String dataPath = "./data_projects_for_TAR_mining/py/github_files/";
        List<Path> pathList = new ArrayList<>();





        try (Stream<Path> stream = Files.walk(Paths.get(dataPath))) {
            pathList = stream.map(Path::normalize)
                    .filter(Files::isRegularFile) //.filter(path -> path.getFileName().toString().equalsIgnoreCase(".travis.yml"))
                    .collect(Collectors.toList());
            pathList.forEach(file -> {
                        try {
                            Path p = file.toAbsolutePath();
                            System.out.println("Processing: " + p);

                            // 保证目录足够深
                            if (p.getNameCount() < 4) {
                                System.err.println("Skipping (path too short): " + p);
                                return;
                            }

                            // 正确的路径解析方式
                            String owner = p.getName(p.getNameCount() - 3).toString();
                            String repo = p.getName(p.getNameCount() - 2).toString();
                            String proj = owner + "_" + repo;

                            System.out.println("Project: " + proj);

                            TravisCITree travis = new TravisCITree();
                            Tree travisTree = travis.getTravisCITree(file.toString());
                            normalizeTreeContents(travisTree);
                            System.out.println(file);


                            String travisJsonString = TreeUtils.treeToJson(travisTree).put("type", "github-file").toString(1);
                            com.google.common.io.Files.write(travisJsonString.getBytes(), new File("JsonAstsForTAR/githubJson-" + proj + "-" + file.getFileName().toString().toLowerCase() + ".json"));
//                            System.exit(0);
                        } catch (Exception e) {
                            System.err.println("处理GitHub文件失败: " + file);
                            e.printStackTrace();

                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }


        dataPath = "./data_projects_for_TAR_mining/py/travis_files/";
        pathList = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get(dataPath))) {
            pathList = stream.map(Path::normalize)
                    .filter(Files::isRegularFile).filter(path -> path.getFileName().toString().equalsIgnoreCase("travis.yml"))
                    .collect(Collectors.toList());
            pathList.forEach(file -> {
                        try {
                            Path p = file.toAbsolutePath();
                            System.out.println("Processing: " + p);

                            // 保证目录足够深
                            if (p.getNameCount() < 4) {
                                System.err.println("Skipping (path too short): " + p);
                                return;
                            }

                            // 正确的路径解析方式
                            String owner = p.getName(p.getNameCount() - 3).toString();
                            String repo = p.getName(p.getNameCount() - 2).toString();
                            String proj = owner + "_" + repo;

                            System.out.println("Project: " + proj);
                            TravisCITree travis = new TravisCITree();
                            Tree travisTree = travis.getTravisCITree(file.toString());
                            normalizeTreeContents(travisTree);
                            System.out.println(file);
                            // String proj = file.toString().split("/")[2] + '_' + file.toString().split("/")[3];
                            System.out.println(proj);
                            String travisJsonString = TreeUtils.treeToJson(travisTree).put("type", "travis-file").toString(1);
                            com.google.common.io.Files.write(travisJsonString.getBytes(), new File("JsonAstsForTAR/travisjson-" + proj + ".json"));
//                            System.exit(0);
                        } catch (Exception e) {
                            System.err.println("处理Travis文件失败: " + file);
                            e.printStackTrace();

                        }
                    }
            );
        } catch (IOException e) {
            System.err.println("读取Travis文件目录失败: " + dataPath);
            e.printStackTrace();
        }
        
        System.out.println("\n=== 生成完成 ===");
        System.out.println("输出目录: " + outputDir.getAbsolutePath());
    }

}

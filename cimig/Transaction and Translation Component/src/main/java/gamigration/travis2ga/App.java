package gamigration.travis2ga;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.utils.Pair;
import com.google.common.io.Files;
import com.opencsv.CSVWriter;

import alignement.FilePair;
import alignement.FilesAlignement;
import util.TreeUtils;

public class App {
    final static String dataPath = "./data_pairs/";
    final static String csvPath = "./error.csv";
    final static String outputCsv = "./gen_file.csv";

    
    public static void write2csv(List<Pair<String, String>> entries, CSVWriter writer) {
        for (Pair<String, String> entry : entries) {
            String[] data = {entry.first, entry.second};
            writer.writeNext(data, true);
        }
    }


    public static void main(String[] args) throws  IOException {
        File inputCsv = new File(csvPath);
        List<String> lines = Files.readLines(inputCsv,java.nio.charset.StandardCharsets.UTF_8);
        String[] header = lines.get(0).split(",");
        List<String> dataLines = lines.subList(1, lines.size());
        for (int i = 0; i < dataLines.size(); i++) {
            String[] currentRow = dataLines.get(i).split(",");
            String repo_name = currentRow[0];
            String language = currentRow[1];
            try (CSVWriter writer = new CSVWriter(new FileWriter(outputCsv))) {

                // 写表头
                writer.writeNext(header);

                // 写除当前行外的所有数据行
                for (int j = 0; j < dataLines.size(); j++) {
                    if (j != i) {
                        writer.writeNext(dataLines.get(j).split(","));
                    }
                }
            }

            System.out.println("已排除第 " + (i + 1) + " 行并覆盖生成 output.csv");
            try {

                FilesAlignement fa = new FilesAlignement(dataPath, outputCsv);
                
                TreeTransactionGeneration ttrg = new TreeTransactionGeneration();
                for (FilePair fp : fa.getAlignedFiles()) {
                    String proj = fp.path;
                    String s1 = fp.getTravisJsonString();
                    String s2 = fp.getGithubJsonString();
                
                    Files.write(fp.getTravisTree().toTreeString().getBytes(), new File("plainAst/travisAst-"+proj+".txt"));
                    Files.write(fp.getGithubActionsTree().toTreeString().getBytes(), new File("plainAst/githubAst-"+proj+"-"+fp.githubFileName+".txt"));
                    Files.write(s1.getBytes(), new File("JsonAsts/travisJson-"+proj+".json"));
                    Files.write(s2.getBytes(), new File("JsonAsts/githubJson-"+proj+"-"+fp.githubFileName+".json"));
            
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNodeTravis = mapper.readTree(s1);
                    JsonNode rootNodeGithub = mapper.readTree(s2);


                    HashSet<JsonNode> travisNodes = new HashSet<JsonNode>();
                    TreeUtils.traverseTree(rootNodeTravis, travisNodes);
                    HashSet<JsonNode> githubNodes = new HashSet<JsonNode>();
                    TreeUtils.traverseTree(rootNodeGithub, githubNodes);
                    
                    ttrg.genEverything(rootNodeTravis, rootNodeGithub, travisNodes, githubNodes);


                }
                ttrg.closeRessources();

                String sourcePath1 = "D:\\vscode\\3\\CItranslation\\cimig\\Transaction and Translation Component\\transactions\\transactions_json_ast_L2_alt.csv";
                String sourcePath2 = "D:\\vscode\\3\\CItranslation\\cimig\\Transaction and Translation Component\\transactions\\transactions_json_ast_L2.csv";
                String targetPath = "D:\\vscode\\3\\CItranslation\\cimig\\Transaction and Translation Component\\genrules\\rules_h2\\"+language+"\\"+repo_name;
                String targetPath1 = targetPath+"\\"+"transactions_json_ast_L2_alt.csv";
                String targetPath2 = targetPath+"\\"+"transactions_json_ast_L2.csv";
                copyCsvUsingGuava(sourcePath1,targetPath1,';');
                copyCsvUsingGuava(sourcePath2, targetPath2,';');

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } /*catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
            // break;
        }


    }
    public static void copyCsvUsingGuava(String sourcePath, String targetPath, char separator) throws IOException {
        File inputCsv = new File(sourcePath);
        File targetCsv = new File(targetPath);

        // 确保目标目录存在
        File targetDir = targetCsv.getParentFile();
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 使用 OpenCSV 读取源文件
        try (BufferedReader reader = new BufferedReader(new FileReader(inputCsv));
             BufferedWriter writer = new BufferedWriter(new FileWriter(targetCsv))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 去掉换行符
                line = line.replace("\n", "").replace("\r", "");
                writer.write(line);
                writer.newLine(); // 写入换行符
            }

            System.out.println("文件处理完成: " + targetCsv);

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("CSV 文件已成功写入到: " + targetCsv.getAbsolutePath());
    }
}

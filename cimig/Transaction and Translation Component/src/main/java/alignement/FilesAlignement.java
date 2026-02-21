package alignement;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.opencsv.CSVReader;

import com.opencsv.CSVWriter;
import db.PsqlConnection;
import util.Combinatorials;
import util.ListComparator;
import util.ListUtils;

public class FilesAlignement {
	
    private List<FilePair> alignedFiles = new ArrayList<>();

    public FilesAlignement(String dataPath, String csvStats) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(csvStats));
        String[] nextLine;
        reader.readNext(); // read and skip header
        // redefine so that it reads stuff following the new csv format
        List<String[]> unparsedPairs = new ArrayList<>();

        while ((nextLine = reader.readNext()) != null) {
            String project_name = nextLine[0];
            String language_name = nextLine[1];
            // String fileName=nextLine[1];
            try{
                String travisFilePath=dataPath+"/"+language_name+"/"+project_name+"/travis.yml";
                System.out.println(travisFilePath);
                String githubFilePath=dataPath+"/"+language_name+"/"+project_name+"/actions.yml";
                List<String> travisFileContents = Files.readAllLines(Paths.get(travisFilePath));
                List <String> githubFileContents = Files.readAllLines(Paths.get(githubFilePath));
                FilePair fp = new FilePair(travisFileContents,githubFileContents,travisFilePath,githubFilePath);
                alignedFiles.add(fp);
            }catch (Exception e){
                System.out.println("problem with file");
                String[]   unparsedPair = new String[2];
                unparsedPair[0]=project_name;
                // unparsedPair[1]=fileName;
                unparsedPairs.add(unparsedPair);
            }

            }
        reader.close();
        //write unparsedPairs to csv
        CSVWriter writer = new CSVWriter(new FileWriter("unparsedPairs.csv"));
        writer.writeAll(unparsedPairs);
        writer.close();
    }

    public List<FilePair> getAlignedFiles() {
        return alignedFiles;
    }

    public void matchLeavesOfAllFiles() {
        for (FilePair fp : getAlignedFiles()) {
            try {
                fp.matchLeaves();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private HashSet<PathMappingData> generateMappings(List<List<String>> sourceCombination, List<List<String>> targetCombination){
    	HashSet<PathMappingData> mappings = new HashSet<PathMappingData>();
    	 for (List<String> source : sourceCombination) {
             for (List<String> target : targetCombination) {
            	 mappings.add(new PathMappingData(source, target));
             }
    	 }
    	 return mappings;
    }
    //generates mapping from every element of source to every element of target
    // if filterUncommonElements == true remove mappings with uncommon individual mappings [a,b]->[c,d] incorrect if P([a]->[c]) = 0
    // otherwise consider all mappings
    //filterUncommonElements set to false to generate 1 to 1 mappings
    
    //generate every combination of elements from a travis tree and every combination of elements from a github tree
    //performed on all the data set at once => more time efficient , very memory hungry
    private HashSet<PathMappingData> generateCombinationsAndMappings(int sourceCombinationSize, int targetCombinationSize) {
        HashSet<PathMappingData> pathMappingDataSet = new HashSet<PathMappingData>();
        System.out.printf("generating combinations of size %d from travis and size %d from github \n", sourceCombinationSize, targetCombinationSize);
        for (FilePair fp : getAlignedFiles()) {
            List<List<String>> travisCombinations;
            List<List<String>> githubCombinations;
            if (sourceCombinationSize > 1 && targetCombinationSize > 1) {
                //generate combinations of string representation of leaves for both travis and github
                travisCombinations = Combinatorials.combinationIterative(fp.filterUncommonSequences(fp.getTravisSequences()), sourceCombinationSize);
                githubCombinations = Combinatorials.combinationIterative(fp.filterUncommonSequences(fp.getGithubSequences()), targetCombinationSize);
                pathMappingDataSet.addAll(generateMappings(travisCombinations, githubCombinations));
            } else {
                travisCombinations = Combinatorials.combinationIterative(fp.getTravisSequences(), sourceCombinationSize);
                githubCombinations = Combinatorials.combinationIterative(fp.getGithubSequences(), targetCombinationSize);
                pathMappingDataSet.addAll(generateMappings(travisCombinations, githubCombinations ));
            }


        }
        return pathMappingDataSet;
    }

 
    //generate combinations and mappings of a single file pair
    //performed on a single file pair => time costly, memory efficient
    public HashSet<PathMappingData> generateFilePairCombinationAndMappings(FilePair fp, int sourceCombinationSize, int targetCombinationSize) {
        List<List<String>> travisCombinations;
        List<List<String>> githubCombinations;
        HashSet<PathMappingData> pathMappingDataSet = new HashSet<PathMappingData>();
        if (sourceCombinationSize > 1 && targetCombinationSize > 1) {
            //generate combinations of string representation of leaves for both travis and github
            travisCombinations = Combinatorials.combinationIterative(fp.filterUncommonSequences(fp.getTravisSequences()), sourceCombinationSize);
            githubCombinations = Combinatorials.combinationIterative(fp.filterUncommonSequences(fp.getGithubSequences()), targetCombinationSize);
            pathMappingDataSet.addAll(generateMappings(travisCombinations, githubCombinations));
        } else {
            travisCombinations = Combinatorials.combinationIterative(fp.getTravisSequences(), sourceCombinationSize);
            githubCombinations = Combinatorials.combinationIterative(fp.getGithubSequences(), targetCombinationSize);


            pathMappingDataSet.addAll(generateMappings(travisCombinations, githubCombinations));
        }
        return pathMappingDataSet;


    }

    //compute the frequencies of a mapping [a,b]->[c,d]
    //everytime [a,b] is encountered in a file increment TRAVIS variable
    public PathMappingData computeProbability(PathMappingData pathMapping) {
        for (FilePair fp : getAlignedFiles()) {
            //everytime [a,b] is encountered in a travis tree increment travis variable
            if (ListComparator.containsAllAndOrderPreserved(fp.getTravisSequences(), pathMapping.travis)) {
                pathMapping.totalTravis++;
                //everytime [c,d] is encountered in github tree where [a,b] was also encountered in its corresponding travis tree increment BOTH_EXIST variable
                if (ListComparator.containsAllAndOrderPreserved(fp.getGithubSequences(), pathMapping.github)) {
                    pathMapping.totalGithub++;
                    pathMapping.bothExists++;
                }

            }//everytime [c,d] is encountered in a github tree increment GITHUB variable
            else if (ListComparator.containsAllAndOrderPreserved(fp.getGithubSequences(), pathMapping.github)) {
                pathMapping.totalGithub++;
            }
        }
        return pathMapping;
    }

    //generate all combinations and mappings from size source to size target
    //performed on all data set at once then written to DB(memory hungry)
    public void generateCorrelatedSequences(int sourceCombinationSize, int targetCombinationSize) {
        PsqlConnection db = new PsqlConnection();
        HashSet<PathMappingData> pathMappingDataSet;

        pathMappingDataSet = generateCombinationsAndMappings(sourceCombinationSize, targetCombinationSize);
        //compute probabilities associated with each mappings
        long nextTime = 0;
        int index = 0;
        int size = pathMappingDataSet.size();
        System.out.println(size);
        for (PathMappingData pathMappingData : pathMappingDataSet) {
            long time = System.currentTimeMillis();
            if (time >= nextTime) {
                System.out.printf("%.2f%%\r", index * 100d / size);
                nextTime = time + 1000;
            }
            for (FilePair fp : getAlignedFiles()) {
                if (ListComparator.containsAllAndOrderPreserved(fp.getTravisSequences(), pathMappingData.travis)) {
                    pathMappingData.totalTravis++;
                    if (ListComparator.containsAllAndOrderPreserved(fp.getGithubSequences(), pathMappingData.github)) {
                        pathMappingData.totalGithub++;
                        pathMappingData.bothExists++;
                    }
                } else if (ListComparator.containsAllAndOrderPreserved(fp.getGithubSequences(), pathMappingData.github)) {
                    pathMappingData.totalGithub++;
                }
            }
            index++;

        }
        System.out.println("Computed all probabilities");
        //insert in db
        for (PathMappingData pathMappingData : pathMappingDataSet) {
            String stringyfiedTravisCombinations = ListUtils.fromListToString(pathMappingData.travis);
            String stringyfiedGithubCombinations = ListUtils.fromListToString(pathMappingData.github);
            try {
//                if (pathMappingData.totalGithub > 2 && pathMappingData.totalTravis > 2 && pathMappingData.bothExists > 1) {
                db.insertPathMappingData(stringyfiedTravisCombinations, stringyfiedGithubCombinations, pathMappingData.totalTravis, pathMappingData.totalGithub, pathMappingData.bothExists);
//                } else {
//                    db.insertIncorrectMapping(stringyfiedTravisCombinations, stringyfiedGithubCombinations, pathMappingData.totalTravis, pathMappingData.totalGithub, pathMappingData.bothExists);
//                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

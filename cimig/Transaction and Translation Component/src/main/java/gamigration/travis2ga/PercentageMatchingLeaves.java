package gamigration.travis2ga;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.simmetrics.StringMetric;
import org.simmetrics.StringMetrics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import alignement.FilePair;
import util.TreeUtils;

public class PercentageMatchingLeaves {
	public static boolean isFileMatch(List<String> correct_files,String fileName) {
		for (String string : correct_files) {
			if(fileName.equalsIgnoreCase(string)) return true;
		}
		return false;
		
	}
	public static String stripComments(List<String> lines) {
		StringBuilder sb = new StringBuilder();
		for(String line : lines) {
			if(!line.startsWith("#"))
				sb.append(line).append("\n");
		}
		return sb.toString();
	}
	public static void main(String[] args) {
		  String dataDirPath="C:\\Users\\alaa\\research\\GhActions-Travis-Migration\\Temp";
		  String csvFilePath= "C:\\Users\\alaa\\research\\GhActions-Travis-Migration\\allFilesBySim.csv";
		  CSVReader reader = null;
		  int numberOfException=0;
		  try {
			reader = new CSVReader(new FileReader(csvFilePath));
			 String[] nextLine;
		     reader.readNext(); // read and skip header
		     List<FilePair> filePairs = new ArrayList<FilePair>();
		     StringMetric metric = StringMetrics.cosineSimilarity();
		     File file = new File("percentageLeaves.csv");
		     FileWriter outputfile = new FileWriter(file);
		     
		     //write csv header
		     String[] header= {"project_name","github_file_name","max_sim","match","number_of_matching_leafs","percentage_travis","percentage_github","file_sim"};
		     CSVWriter writer = new CSVWriter(outputfile);
		     writer.writeNext(header);
		     //read csv file lines
		     while ((nextLine = reader.readNext()) != null) {
		    	 String project_name = nextLine[0];	
		    	 double MaxSim = Double.parseDouble(nextLine[1]);
		    	 String match=nextLine[3];
		    	 //if similarity smaller than 0.1 disregard
		    	 if(MaxSim < 0.1) {
		    		 continue;
		    	 }
		    	 List<String> correct_project_files = new ArrayList<String>();
		    	 
		    	 //fill correct_project_files with data from csv
		    	 for(int i=4;i<=9;i++) {
		            	if(!nextLine[i].isEmpty()) {
		            		correct_project_files.add(nextLine[i]);
		            	}
		            }
		    	 
		    	 //get all files in GHActions folder
		    	 File githubFilesFolder=new File(dataDirPath+"\\"+project_name+"\\Max\\0\\GHActions");
		    	 File[] githubFiles = githubFilesFolder.listFiles();
		    	

		    	 File travisFile=new File(dataDirPath+"\\"+project_name+"\\Max\\0\\travis.yml");
		    	 System.out.println(travisFile);
		    	 
		    	 //createFilePairs
		    	 //parse their trees
		    	 //store content in bucket FilePair class
		    	 List<String> travisFileContents = Files.readAllLines(Paths.get(travisFile.getPath()));
		    	 String travisFileString = stripComments(travisFileContents);
		    	 //repeat for every githubFile
		    	 for(File githubFile:githubFiles) {
		    		 List<String> githubActionsFileContents = Files.readAllLines(Paths.get(githubFile.getPath()));
		    		 String githubFileString=stripComments(githubActionsFileContents);
		    		 try {
		    			 FilePair fp=new FilePair(travisFileContents,githubActionsFileContents,travisFile.getPath(),githubFile.getPath());
		    			 filePairs.add(fp);
		    			 List<String> travisLeaves = TreeUtils.getLeaves(fp.getTravisTree());
				    	 List<String> githubLeaves = TreeUtils.getLeaves(fp.getGithubActionsTree());
				    	 int matchingLeaves = 0;
				    	 for(String travisLeaf:travisLeaves) {
				    		 for(String githubLeaf:githubLeaves) {
				    			 if(metric.compare(travisLeaf, githubLeaf)>0.5) {
				    				 //only consider long leaves to not count leaves such as "master" and "java"
				    				 if(travisLeaf.length()<8 || githubLeaf.length()<8) continue;
				    				 else {
				    					 matchingLeaves++;
				    					 System.out.println(travisLeaf+"=>"+githubLeaf);
				    					 break;
				    				 }
				    			 }
				    		 }
				    	 }
				    	 Double percentTravis=(matchingLeaves*1.0)/(travisLeaves.size()*1.0);
				    	 Double percentGithub=(matchingLeaves*1.0)/(githubLeaves.size()*1.0);
				    	 Float fileSim=metric.compare(travisFileString, githubFileString); 
				    	 if("no".equalsIgnoreCase(match)) {
				    		 String[] data = {project_name,githubFile.getName(),Double.toString(MaxSim) ,
				    				 match , Integer.toString(matchingLeaves) , Double.toString(percentTravis),Double.toString(percentGithub)
				    				 ,Float.toString(fileSim)};
				    		 writer.writeNext( data , false);
				    	 }else {
				    		 //if match from csv is partial or perfect
				    		 //check if the file being inspected is in the list of matching files from the csv
				    		 if(isFileMatch(correct_project_files,githubFile.getName())) {
				    			 String[] data = {project_name,githubFile.getName(),Double.toString(MaxSim) , match ,
				    					 Integer.toString(matchingLeaves), Double.toString(percentTravis),Double.toString(percentGithub)
				    					 ,Float.toString(fileSim)};
				    			 writer.writeNext( data , false);
				    		 } else {
				    			 String[] data = {project_name,githubFile.getName(),Double.toString(MaxSim) , "No" ,
				    					 Integer.toString(matchingLeaves), Double.toString(percentTravis),Double.toString(percentGithub)
				    					 ,Float.toString(fileSim)};	
				    			 writer.writeNext( data , false);
				    		 }
				    		
				    	 }
				    	 
				    	
		    		 }catch(Exception e) {
		    			 numberOfException++;
		    			 System.out.println(numberOfException);
		    			 System.err.println(e);
		    		 }
		    	 }
		    	 
		    	 
		    	 
		     }
		     writer.close();
		   
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}  
	
		  
	}
}

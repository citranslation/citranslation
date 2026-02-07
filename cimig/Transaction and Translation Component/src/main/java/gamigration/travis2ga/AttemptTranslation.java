package gamigration.travis2ga;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import alignement.PathMappingData;
import db.PsqlConnection;
import travisdiff.TravisCITree;
import util.NormalizationUtils;
import util.TreeUtils;

public class AttemptTranslation {
	public static PsqlConnection db = new PsqlConnection();
	 public static List<Pair<String,String>> extractLeavesAndBranches(Tree root) {
		 List<Pair<String,String>> banchesAndLeafs = new ArrayList<Pair<String,String>>();
		 for(Tree node:root.preOrder()) {
			 if(TreeUtils.isLeaf(node)) {
				 banchesAndLeafs.add(new Pair<String,String>(TreeUtils.getParentsURI(node),node.getLabel().replaceAll("^\"|\"$", "")));
			 }
		 }
			return  banchesAndLeafs;
	 }
	
	
	public static String appendLeafBackToBranch(String branch,String leaf){
		if(branch.endsWith(".[]")) {
			return branch.replace("[]", "["+leaf+"]");
		}else {
			//malformed string
			return null;
		}
		
	}
	public static String attemptSimTranslate(String branch,String leaf) throws SQLException {
		System.out.println(branch);
		String translation = db.getSimiliratyTranslation(branch);
		if(translation == null)
			return null;
		
		
		return appendLeafBackToBranch(translation,leaf); 
		
	}
	

	
	public static List<String> normalizeTravisStatements(List<String> travisStatements){
		 List<String> travis_sequences_cleaned = new ArrayList<>();
		   for (String temp_str : travisStatements) {
                 travis_sequences_cleaned.add( NormalizationUtils.normalizeTravisStatements(temp_str));
             }
		   return travis_sequences_cleaned;
	}

	
	
		
	
	
	
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
		//read file to translate
		//parse file to translate
		//normalize file to translate 
		//fetch translation rules
		//attempt 1 to 1 translations 
		String fileToTranslate="/home/alaa/eclipse-workspace/GhActions-Travis-Migration/Temp/apache_commons-cli/Max/0/travis.yml";
		TravisCITree travis = new TravisCITree();
		Tree travisTree=travis.getTravisCITree(fileToTranslate);
		List<String> leaves= TreeUtils.getLeaves(travisTree);
		List<String> travisStatements= TreeUtils.generateAllSequences(travisTree);
		System.out.println("travisStatements-----------------");
		travisStatements.forEach(System.out::println);
		  HashSet<String> translation = new HashSet<String>();
		  List<String> unmatchedStatement = new ArrayList<String>();
		  List<Pair<String,String>> branchesAndLeaves = extractLeavesAndBranches(travisTree);
		  
		  for(Pair<String,String> branchAndLeafPair:branchesAndLeaves) {
			  String res=attemptSimTranslate(branchAndLeafPair.first,branchAndLeafPair.second);
			  if(res!=null) {
					translation.add(res);
				} else {
					unmatchedStatement.add(appendLeafBackToBranch(branchAndLeafPair.first,branchAndLeafPair.second));
				}
		  }
		  
		  
		System.out.println("unmatchedStatements with sim------------------------");
		unmatchedStatement.forEach(System.out::println);
		
		List<String> unmatchedTravisStatements=normalizeTravisStatements(unmatchedStatement);
	   //travisStatements.forEach(s->System.out.println(s));
	    List<String> probabilyGenericTravisStatement = new ArrayList<String>();
		for(String travisStmt:unmatchedTravisStatements) {
			List<PathMappingData> result = new ArrayList<PathMappingData>();
			result.addAll(db.getTranslationRule(travisStmt,true));
			result.addAll(db.getTranslationRule(travisStmt, false));
			
			List<String> stringTrans=result.stream().map(elem->elem.getGithub().get(0)).collect(Collectors.toList());
			if(stringTrans.size()==0) {
				probabilyGenericTravisStatement.add(travisStmt);
			}else {
				HashSet<String> removeDupes = new HashSet<String>(stringTrans);
				translation.addAll(removeDupes);
			}
		}
		
		System.out.println("probably generic------------------------");
		probabilyGenericTravisStatement.forEach(System.out::println);
		System.out.println("GHA translataion------------------------");
		translation.forEach(System.out::println);
		
//		translation=(HashSet<String>) translation.stream().map(NormalizationUtils::normalizeGithubStatementForApriori).collect(Collectors.toSet());
		
		
	}

}

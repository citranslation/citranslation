package gamigration.travis2ga;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.utils.Pair;
import com.opencsv.CSVWriter;

import db.PsqlConnection;

public class GenerateTranslationRules {
	public static PsqlConnection db = new PsqlConnection();
	
	public static List<Pair<String,String>> getSimilarityBasedTranslations() throws SQLException{
		//get translations rules by similarity
		PreparedStatement ps = db.getConnection().prepareStatement("SELECT travis_parent, github_parent, MAX(refcount) from parent_mappings WHERE refcount>? GROUP BY travis_parent,github_parent ORDER BY refcount DESC");
		ps.setInt(1, config.Config.refCountForSimilarityTranslation);
		ps.execute();	
		ResultSet rs=ps.getResultSet();
		List<Pair<String,String>> temp = new ArrayList<Pair<String,String>>();
		while(rs.next()) {
			temp.add(new Pair<String,String>(rs.getString(1),rs.getString(2)));
		}
		return temp;
	}
	public static int getTravisStatementCount(String travisStatement) throws SQLException{
		PreparedStatement ps = db.getConnection().prepareStatement("SELECT travis_total from filtered_path_mappings WHERE travis=?");
		ps.setString(1, travisStatement);
		ps.execute();
		ResultSet rs=ps.getResultSet();
		if(rs.next()) {
			return rs.getInt(1);
		}else {
			return 0;
		}
	}
	public static List<String> getTravisStatements() throws SQLException{
		PreparedStatement ps = db.getConnection().prepareStatement("SELECT DISTINCT travis from filtered_path_mappings WHERE travis_total > ?");
		ps.setInt(1, config.Config.occurencesToIncludeInStatement);
		ps.execute();
		ResultSet rs=ps.getResultSet();
		List<String> travisStatements = new ArrayList<String>();
		while(rs.next()) {
			travisStatements.add(rs.getString(1));
		}
		return travisStatements;	
	}
	
	public static List<String> getBestTranslationsForStatementOneToOne(String travisStatement) throws SQLException{
		PreparedStatement ps = db.getConnection().prepareStatement("SELECT github from filtered_path_mappings WHERE travis = ? and cond_total>2 ORDER BY p_cond*p_cond_inv DESC LIMIT "+config.Config.oneToOneTranslationToFetch);
		ps.setString(1, travisStatement);
		ps.execute();
		List<String> translations = new ArrayList<String>();
		ResultSet rs=ps.getResultSet();
		while(rs.next()) {
			translations.add(rs.getString(1));
		}
		return translations;
	}
	
	public static List<String> getBestTranslationsForStatement(String travisStatement) throws SQLException{
		PreparedStatement ps = db.getConnection().prepareStatement("SELECT github from filtered_path_mappings WHERE travis = ? and cond_total>2 ORDER BY p_cond DESC LIMIT "+config.Config.manyToManyTranslationToFetch);
		ps.setString(1, travisStatement);
		ps.execute();
		List<String> translations = new ArrayList<String>();
		ResultSet rs=ps.getResultSet();
		while(rs.next()) {
			translations.add(rs.getString(1));
		}
		return translations;
	}

	
	public static void main(String[] args) throws IOException {
		 File file = new File("translations_for_gold_set_v3.csv");	
		 
	     FileWriter outputfile = new FileWriter(file);
	     
	     String[] header= {"travis","github","travisCount"};
	     CSVWriter writer = new CSVWriter(outputfile);
	     writer.writeNext(header);
		try {
			List<Pair<String,String>> simTranslations=getSimilarityBasedTranslations();
			List<String> simTranslatableTravis = new ArrayList<String>();
			simTranslations.forEach(s->simTranslatableTravis.add(s.first));
			List<String> travisStatements = getTravisStatements();
			simTranslatableTravis.forEach(System.out::println);
			for(String s:travisStatements) {
				String statementMinusLeaf = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "[");
				System.out.println(statementMinusLeaf);
				String travisCount=String.valueOf( getTravisStatementCount(s));
				List<String> bestTranslations=getBestTranslationsForStatement(s);
				for(String ss:bestTranslations) {
					String[] data = {s,ss,travisCount};
					writer.writeNext(data);
				}
				List<String> bestOneToOneTranslations = getBestTranslationsForStatementOneToOne(s);
				for(String ss:bestOneToOneTranslations) {
					String[] data = {s,ss,travisCount};
					writer.writeNext(data);
				}
				
			}
			for(Pair<String,String> pair:simTranslations) {
				String[] data = {pair.first,pair.second};
				writer.writeNext(data);
			}
			writer.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}

}

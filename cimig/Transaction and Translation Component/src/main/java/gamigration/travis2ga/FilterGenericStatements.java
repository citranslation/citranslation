package gamigration.travis2ga;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import alignement.PathMappingData;
import db.PsqlConnection;

public class FilterGenericStatements {
    //make db call to get all statements
	//fill in new db
	public static PsqlConnection db = new PsqlConnection();
	public static List<String> getGenericStatementsByQuery(String colName) throws SQLException{
		String sqlQuery;
		if("travis".equals(colName)) {
			sqlQuery="SELECT travis,AVG(p_cond_inv) as ave from path_mappings WHERE cond_total>"+config.Config.cond_total_for_generic_statement +" GROUP BY travis order by ave desc ";
		}else {
			sqlQuery="SELECT github,AVG(p_cond) as ave from path_mappings WHERE cond_total>"+ config.Config.cond_total_for_generic_statement +" GROUP BY github order by ave desc ";
		}
		PreparedStatement ps =
				db.getConnection().prepareStatement(sqlQuery);
		ps.execute();
		ResultSet rs=ps.getResultSet();
		List<String> statements=new ArrayList<String>();
		
		while(rs.next()) {
			String githubOrTravisStatement=rs.getString(1);
			double probability = rs.getDouble(2);
			if(probability>config.Config.genericCutOff)	
				statements.add(githubOrTravisStatement);
		}
		return statements;
	}
	
	public static List<String>  getTravisGenericStatements() throws SQLException {
		List<String> travisStatements = getAllTravisStatements();
		List<String> genericTravisStatements = new ArrayList<String>();
		for(String stmt:travisStatements) {
			if(isStatementGeneric("travis",stmt)) {
				genericTravisStatements.add(stmt);
			}
		}
		return genericTravisStatements;
    }
	public static List<String>  getGithubGenericStatements() throws SQLException {
		List<String> githubStatements = getAllGithubStatements();
		List<String> genericGithubStatements = new ArrayList<String>();
		for(String stmt:githubStatements) {
			if(isStatementGeneric("github",stmt)) {
				genericGithubStatements.add(stmt);
			}
		}
		return genericGithubStatements;
    }
	
    
	public static List<String> getAllTravisStatements() throws SQLException{

		return db.getDistinctStatements("travis");
	}
	public static List<String> getAllGithubStatements() throws SQLException{
		return db.getDistinctStatements("github");
	}
	public static List<PathMappingData> filterStatements(List<String> genericTravisStatements,List<String> genericGithubStatements,HashSet<PathMappingData> allRows) {
		  return allRows.stream().filter(
				  row->!genericTravisStatements.contains(row.getTravis().get(0))
				  &&!genericGithubStatements.contains(row.getGithub().get(0))).collect(Collectors.toList());
	}
	public static boolean isStatementGeneric(String statementOrigin,String statement) throws SQLException {
		Double d= db.getAverageProbability(statementOrigin, statement);
		if(d==null) return false;
		return d>config.Config.genericCutOff;
	}
	
	public static void main(String[] args) {
		try {

			List<String> genericTravisStatements = getTravisGenericStatements();
			List<String> genericGithubStatements = getGithubGenericStatements();
			System.out.println(isStatementGeneric("travis","language:java"));
			genericGithubStatements.forEach(System.out::println);
			HashSet<PathMappingData> allRows=db.fetchAllRowsFromDb();
			List<PathMappingData> pmd=filterStatements(genericTravisStatements,genericGithubStatements,allRows);
			db.insertPathMappingDataInBatchesInFilteredTable(pmd);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

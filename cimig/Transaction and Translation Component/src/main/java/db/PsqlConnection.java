package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import alignement.PathMappingData;
import util.ListUtils;

	
public class PsqlConnection {
	private static Connection c;
	public static HashSet<PathMappingData> pathMappingDataCache;
	public PsqlConnection() {
		if(c==null)
			try {
				Class.forName("org.postgresql.Driver");
				c=	DriverManager.getConnection("jdbc:postgresql://localhost:5432/travis2ga",
	                    "postgres", "123");
			} catch (ClassNotFoundException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
	}
	public Connection getConnection() {
		if(c==null)
			new PsqlConnection();
		return c;
	}
	public void insertParentEntry(String travis_parent,String github_parent) throws SQLException{
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO parent_mappings(travis_parent,github_parent,refcount) VALUES(?,?,1)");
		myStmt.setString(1, travis_parent);
		myStmt.setString(2, github_parent);
		myStmt.execute();
	}
	
	public void insertArgsEntry(String travis,String github,int parentId) throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO argument_mappings(travis,github,parents) VALUES(?,?,?)");
		myStmt.setString(1, travis);
		myStmt.setString(2, github);
		myStmt.setInt(3, parentId);
		myStmt.execute();
	}
	public int getParentId(String travis_parent,String github_parent) throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("SELECT * FROM parent_mappings WHERE travis_parent=? AND github_parent=?");
		myStmt.setString(1, travis_parent);
		myStmt.setString(2, github_parent);
		myStmt.execute();
		ResultSet rs = myStmt.getResultSet();
		if(rs.next()) {
			return rs.getInt("id");
		}else return -1;
	}
	
	public String getSimiliratyTranslation(String travisBranch) throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("SELECT * FROM parent_mappings WHERE travis_parent=? ORDER BY refcount DESC");
		myStmt.setString(1, travisBranch);
		myStmt.execute();
		ResultSet rs = myStmt.getResultSet();
		if(rs.next()) {
			System.out.println("worked for :"+travisBranch);
			return rs.getString(2);
		}else {
			return null;
		}
	}
	public void updateParentEntry(int id)throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("UPDATE parent_mappings SET refcount = refcount +1 WHERE id=?");
		myStmt.setInt(1, id);
		myStmt.execute();
	}
	public void updateIfExistElseInsert(String travis,String github,String travis_parent,String github_parent) throws SQLException {
		PreparedStatement myStmt;
		int id = this.getParentId(travis_parent, github_parent);
		if(id==-1) {
			insertParentEntry(travis_parent,github_parent);
			id = this.getParentId(travis_parent, github_parent);	
		} else {
			updateParentEntry(id);
		}
		insertArgsEntry(travis, github, id);
	}
	
	public void insertPathMappingData(String travis,String github,int travis_total,int github_total,int cond_total) throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO path_mappings(travis,github,travis_total,github_total,cond_total,p_cond,p_cond_inv) VALUES(?,?,?,?,?,?,?) ON CONFLICT (travis, github) DO NOTHING");
		myStmt.setString(1, travis);
		myStmt.setString(2, github);
		myStmt.setInt(3, travis_total);
		myStmt.setInt(4, github_total);
		myStmt.setInt(5, cond_total);
		myStmt.setDouble(6, 1.0*cond_total/travis_total*1.0);
		myStmt.setDouble(7, 1.0*cond_total/github_total*1.0);
		myStmt.execute();
	}
	public boolean pathExists(String path) {
		PreparedStatement myStmt;
		try {
			myStmt = c.prepareStatement("SELECT * from path_mappings where travis = ? OR github = ?");
			myStmt.setString(1, path);
			myStmt.setString(2, path);
			myStmt.execute();
			ResultSet rs = myStmt.getResultSet();
			if(rs.next()) {
				return true;
			}else return false;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public void insertIncorrectMapping(String travis,String github,int travis_total,int github_total,int cond_total) throws SQLException {
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO incorrect_path_mappings(travis,github,travis_total,github_total,cond_total,p_cond,p_cond_inv) VALUES(?,?,?,?,?,?,?)");
		myStmt.setString(1, travis);
		myStmt.setString(2, github);
		myStmt.setInt(3, travis_total);
		myStmt.setInt(4, github_total);
		myStmt.setInt(5, cond_total);
		myStmt.setDouble(6, 1.0*cond_total/travis_total*1.0);
		myStmt.setDouble(7, 1.0*cond_total/github_total*1.0);
		myStmt.execute();
		
	}
	//the mappings exists in the 1 to 1 mappings saved in cache (otherwise fetched from db)
	public boolean pathMappingExists(String travis,String github) {
		if(pathMappingDataCache != null) {
			return pathMappingDataCache.contains(new PathMappingData(travis,github));
		}else {
			fetchAllRowsFromDb();
			return pathMappingExists(travis,github);
		}
	
	}
	
	//fetch all rows made from 1 to 1 mappings
	public HashSet<PathMappingData> fetchAllRowsFromDb(){
		PreparedStatement myStmt;
		try {
			myStmt = c.prepareStatement("SELECT * from path_mappings");
			myStmt.execute();
			ResultSet rs=myStmt.getResultSet();
			HashSet<PathMappingData> returnHashSet = new HashSet<PathMappingData>();
			while(rs.next()) {
				returnHashSet.add(new PathMappingData(rs.getString("travis"),rs.getString("github"),rs.getInt("travis_total"),rs.getInt("github_total"),rs.getInt("cond_total")));
			}		
			pathMappingDataCache=returnHashSet;
			return returnHashSet;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public boolean pathMappingExistsInDb(String travis,String github) throws SQLException {
		PreparedStatement myStmt;
		myStmt = c.prepareStatement("SELECT * from path_mappings WHERE travis=? AND github=?");
		myStmt.setString(1, travis);
		myStmt.setString(2, github);
		myStmt.execute();
		ResultSet rs= myStmt.getResultSet();
		if(rs.next()) {
			return true;
		}else return false;
		
	}
	public void insertPathMappingDataInBatches(HashSet<PathMappingData> pathMappingData) throws SQLException {
		int batchSize=0;
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO path_mappings(travis,github,travis_total,github_total,cond_total,p_cond,p_cond_inv) VALUES(?,?,?,?,?,?,?) ");
		for(PathMappingData element:pathMappingData) {
			if(true) {
				myStmt.setString(1, ListUtils.fromListToString(element.getTravis()));
				myStmt.setString(2, ListUtils.fromListToString(element.getGithub()));
				myStmt.setInt(3, element.getTotalTravis());
				myStmt.setInt(4, element.getTotalGithub());
				myStmt.setInt(5, element.getBothExists());
				myStmt.setDouble(6, 1.0*element.getBothExists()/ element.getTotalTravis()*1.0);
				myStmt.setDouble(7, 1.0*element.getBothExists()/element.getTotalGithub()*1.0);
				myStmt.addBatch();
				batchSize++;
			}
		}
		if(batchSize>0)
			myStmt.executeBatch();
	}
	
	public void insertPathMappingDataInBatchesInFilteredTable(List<PathMappingData> pathMappingData) throws SQLException {
		int batchSize=0;
		PreparedStatement myStmt = c.prepareStatement("INSERT INTO filtered_path_mappings(travis,github,travis_total,github_total,cond_total,p_cond,p_cond_inv) VALUES(?,?,?,?,?,?,?) ");
		for(PathMappingData element:pathMappingData) {
			if(element.getTotalTravis() > 2 && element.getTotalGithub() >2 && element.getBothExists()>1) {
				myStmt.setString(1, ListUtils.fromListToString(element.getTravis()));
				myStmt.setString(2, ListUtils.fromListToString(element.getGithub()));
				myStmt.setInt(3, element.getTotalTravis());
				myStmt.setInt(4, element.getTotalGithub());
				myStmt.setInt(5, element.getBothExists());
				myStmt.setDouble(6, 1.0*element.getBothExists()/ element.getTotalTravis()*1.0);
				myStmt.setDouble(7, 1.0*element.getBothExists()/element.getTotalGithub()*1.0);
				myStmt.addBatch();
				batchSize++;
			}
		}
		if(batchSize>0)
			myStmt.executeBatch();
	}
	
	public List<String> getDistinctStatements(String column) throws SQLException{
		String stmt="SELECT DISTINCT "+column+" from path_mappings";
		System.out.println(stmt);
		PreparedStatement myStmt= c.prepareStatement(stmt);
		myStmt.execute();
		ResultSet rs= myStmt.getResultSet();
		List<String> statements=new ArrayList<String>();
		while(rs.next()) {
			statements.add(rs.getString(1));
		}
		return statements;
	}

	
	public Double getAverageProbability(String column,String travisOrGithubStatement) throws SQLException {
		String probabilityToSelect;
		if("travis".equals(column)) {
			probabilityToSelect="p_cond_inv";
		}else {
			probabilityToSelect="p_cond";
		}
		String stmt="SELECT AVG("+probabilityToSelect+") FROM path_mappings WHERE "+column+ "=? AND cond_total>2";
		PreparedStatement myStmt= c.prepareStatement(stmt);
		myStmt.setString(1, travisOrGithubStatement);
		myStmt.execute();
		ResultSet rs= myStmt.getResultSet();
		rs.next();
		return rs.getDouble(1);
		
		
	}
	public List<PathMappingData> getTranslationRule(String travisStatement,boolean oneToOne) throws SQLException{
		List<PathMappingData> array = new ArrayList<PathMappingData>();
		String sqlStatement;
		if(oneToOne==true) {
			sqlStatement="SELECT *,p_cond*p_cond_inv AS comb FROM filtered_path_mappings WHERE travis=? ORDER BY comb DESC LIMIT "+config.Config.manyToManyTranslationToFetch;
		}else {
			sqlStatement="SELECT *,p_cond AS comb FROM filtered_path_mappings WHERE travis=? ORDER BY comb DESC LIMIT "+config.Config.oneToOneTranslationToFetch;
		}
		PreparedStatement myStmt= c.prepareStatement(sqlStatement);
		myStmt.setString(1, travisStatement);
		myStmt.execute();
		ResultSet rs= myStmt.getResultSet();
		while(rs.next()) {
			array.add(new PathMappingData(rs.getString(1),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getInt(5)));
		}
		return array;	
	}
	
	public void deletePathMappings() throws SQLException {
		PreparedStatement myStmt= c.prepareStatement("DELETE FROM path_mappings");
		myStmt.execute();
		
	}
	
	

}

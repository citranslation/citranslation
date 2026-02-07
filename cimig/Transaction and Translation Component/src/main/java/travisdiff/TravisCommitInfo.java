package travisdiff;

import com.opencsv.bean.CsvBindByName;

public class TravisCommitInfo {
	@CsvBindByName(column = "repoUrl", required = true)	
	private String repoUrl;
	
	@CsvBindByName(column = "failCommit", required = true)	
	private String failCommit;
	
	@CsvBindByName(column = "passCommit", required = true)	
	private String passCommit;
	
	@CsvBindByName(column = "mainProblem")
	private String  mainProblem;	
	
	
	@CsvBindByName(column = "allProblems")
	private String  allProblems;
	
	@CsvBindByName(column = "problemBuildState")
	private String problemBuildState;
	
	
	public TravisCommitInfo(String repo,String failcmt, String passcmt)
	{
		this.repoUrl=repo;
		this.failCommit=failcmt;
		this.passCommit=passcmt;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public void setRepoUrl(String repoUrl) {
		this.repoUrl = repoUrl;
	}

	public String getFailCommit() {
		return failCommit;
	}

	public void setFailCommit(String failCommit) {
		this.failCommit = failCommit;
	}

	public String getPassCommit() {
		return passCommit;
	}

	public void setPassCommit(String passCommit) {
		this.passCommit = passCommit;
	}
	
	public String getMainProblem() {
		return mainProblem;
	}

	public void setMainProblem(String mainProblem) {
		this.mainProblem = mainProblem;
	}

	public String getAllProblems() {
		return allProblems;
	}

	public void setAllProblems(String allProblems) {
		this.allProblems = allProblems;
	}

	public String getProblemBuildState() {
		return problemBuildState;
	}

	public void setProblemBuildState(String problemBuildState) {
		this.problemBuildState = problemBuildState;
	}


}

package travisdiff;

import java.util.List;

import com.opencsv.bean.CsvBindByName;

public class FailFixMappingStatistics {
	@CsvBindByName(column = "repoUrl", required = true)	
	private String repourl;
	
	@CsvBindByName(column = "passCmt", required = true)	
	private String passCmt;
	
	
	@CsvBindByName(column = "mainProblem", required = true)	
	private String mainProblem;
	
	@CsvBindByName(column = "failType", required = true)	
	private String failType;
	
	@CsvBindByName(column = "fixLocation", required = true)	
	private String fixLocation;
	
	@CsvBindByName(column = "fixCmds", required = true)	
	private String fixCmds;
	
	private List<String> fixBlockList;	
	
	private List<String> firstLavelBlockList;
	

	private List<String> fixCmdList;
	
	private String changeLabel;
	
	private List<CmdStatement> cmdStmtList;
	
	private List<String> fixJsonList;
	

	public List<String> getFixJsonList() {
		return fixJsonList;
	}

	public void setFixJsonList(List<String> fixJsonList) {
		this.fixJsonList = fixJsonList;
	}

	public List<CmdStatement> getCmdStmtList() {
		return cmdStmtList;
	}

	public void setCmdStmtList(List<CmdStatement> cmdStmtList) {
		this.cmdStmtList = cmdStmtList;
	}
	
	public void addCmdStmtList(List<CmdStatement> cmdStmtList) {
		this.cmdStmtList.addAll(cmdStmtList);
	}

	public FailFixMappingStatistics(String url, String mainprb, String failtype, String fixloc,String fixcmds)
	{
		this.repourl=url;
		this.mainProblem=mainprb;
		this.failType=failtype;
		this.fixLocation=fixloc;
		this.fixCmds=fixcmds;
	}

	public String getRepourl() {
		return repourl;
	}

	public void setRepourl(String repourl) {
		this.repourl = repourl;
	}

	public String getMainProblem() {
		return mainProblem;
	}

	public void setMainProblem(String mainProblem) {
		this.mainProblem = mainProblem;
	}

	public String getFailType() {
		return failType;
	}

	public void setFailType(String failType) {
		this.failType = failType;
	}

	public String getFixLocation() {
		return fixLocation;
	}

	public void setFixLocation(String fixLocation) {
		this.fixLocation = fixLocation;
	}
	
	public String getFixCmds() {
		return fixCmds;
	}

	public void setFixCmds(String fixCmds) {
		this.fixCmds = fixCmds;
	}
	
	public String getPassCmt() {
		return passCmt;
	}

	public void setPassCmt(String passCmt) {
		this.passCmt = passCmt;
	}
	
	public List<String> getFixBlockList() {
		return fixBlockList;
	}

	public void setFixBlockList(List<String> fixBlockList) {
		this.fixBlockList = fixBlockList;
	}
	
	public List<String> getFirstLavelBlockList() {
		return firstLavelBlockList;
	}

	public void setFirstLavelBlockList(List<String> firstLavelBlockList) {
		this.firstLavelBlockList = firstLavelBlockList;
	}

	public List<String> getFixCmdList() {
		return fixCmdList;
	}

	public void setFixCmdList(List<String> fixCmdList) {
		this.fixCmdList = fixCmdList;
	}
	
	public String getChangeLabel() {
		return changeLabel;
	}

	public void setChangeLabel(String changeLabel) {
		this.changeLabel = changeLabel;
	}


}

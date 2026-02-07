package travisdiff;

import com.opencsv.bean.CsvBindByName;

public class FailFixMapping {
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
	
	
	

	public FailFixMapping(String url, String mainprb, String failtype, String fixloc,String fixcmds)
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


}

package travisdiff;

import java.util.ArrayList;
import java.util.List;

public class CmdStatement {
	List<String> cmdList;
	String strStmt;
	String action;
	
	
	public CmdStatement()
	{
		cmdList=new ArrayList<>();
	}
	
	public List<String> getCmdList() {
		return cmdList;
	}

	public void setCmdList(List<String> cmdList) {
		this.cmdList = cmdList;
	}
	
	public void addCmds(List<String> cmds)
	{
		cmdList.addAll(cmds);
	}

	public String getStrStmt() {
		return strStmt;
	}

	public void setStrStmt(String strStmt) {
		this.strStmt = strStmt;
	}
	
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

}

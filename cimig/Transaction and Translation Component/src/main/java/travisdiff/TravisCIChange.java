package travisdiff;

import java.util.ArrayList;
import java.util.List;

public class TravisCIChange {
	private String parentBlock;	
	private List<String> changeCmds;
	private List<String> changeLabels;
	
	public TravisCIChange()
	{
		this.changeCmds=new ArrayList<>();
		this.changeLabels=new ArrayList<>();
	}
	
	
	public String getParentBlock() {
		return parentBlock;
	}

	public void setParentBlock(String parentBlock) {
		this.parentBlock = parentBlock;
	}

	public List<String> getChangeCmds() {
		return changeCmds;
	}

	public void setChangeCmds(List<String> changeCmds) {
		this.changeCmds = changeCmds;
	}

	public List<String> getChangeLabels() {
		return changeLabels;
	}

	public void setChangeLabels(List<String> changeLabels) {
		this.changeLabels = changeLabels;
	}
	
	public void addToChangeCmds(String cmd)
	{
		changeCmds.add(cmd);
	}
	
	public void addToChangeLabels(String label)
	{
		changeLabels.add(label);
	}

}

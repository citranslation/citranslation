package alignement;

import java.util.ArrayList;
import java.util.List;

public class PathMappingData {
		List<String> travis;
		List<String> github;
		int totalTravis;
		public int getTotalTravis() {
			return totalTravis;
		}

		public int getTotalGithub() {
			return totalGithub;
		}

		public int getBothExists() {
			return bothExists;
		}
		int totalGithub;
		int bothExists;
		public PathMappingData(List<String> travis, List<String> github) {
			super();
			this.travis = travis;
			this.github = github;
			totalTravis=0;
			totalGithub=0;
			bothExists=0;
			
		}
		
		public List<String> getTravis() {
			return travis;
		}

		public List<String> getGithub() {
			return github;
		}

		public PathMappingData(String travis,String github) {
			ArrayList<String> travisList = new ArrayList<String>();
			travisList.add(travis);
			this.travis = travisList;
			ArrayList<String> githubList = new ArrayList<String>();
			githubList.add(github);
			this.github = githubList;
			totalTravis=0;
			totalGithub=0;
			bothExists=0;
		}
		
		public PathMappingData(String travis,String github,int totalTravis,int totalGithub,int bothExists) {
			super();
			ArrayList<String> travisList = new ArrayList<String>();
			travisList.add(travis);
			this.travis = travisList;
			ArrayList<String> githubList = new ArrayList<String>();
			githubList.add(github);
			this.github = githubList;
			this.totalTravis=totalTravis;
			this.totalGithub=totalGithub;
			this.bothExists=bothExists;
			
		}
		
		@Override
		public int hashCode() {
			return travis.hashCode()^github.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			 if (obj == null) {
		            return false;
		        }

		        if (obj.getClass() != this.getClass()) {
		            return false;
		        }
		        final PathMappingData other = (PathMappingData) obj;
		        if((this.travis== null || this.github == null) || (other.github == null || other.travis == null))
		        	return false;
		        
		        
		        if(this.travis.equals(other.travis) && this.github.equals(other.github) )
		        	return true;
		        
		        return false;
		}
		
		
}

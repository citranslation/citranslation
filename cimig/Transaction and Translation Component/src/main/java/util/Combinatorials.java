package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Combinatorials {
	public static void subsetsOf(List<String> list, int k, int index, List<String> tempSet, List<List<String>> finalSet) {
	    if (tempSet.size() == k) {
	        finalSet.add(new ArrayList<String>(tempSet));
	        return;
	    }

	    if (index == list.size())
	        return;


	    String human = list.get(index);

	    tempSet.add(human);
	    subsetsOf(list, k, index+1, tempSet, finalSet);

	    tempSet.remove(human);
	    subsetsOf(list, k, index+1, tempSet, finalSet);
	}

	public static List<List<String>> combinationIterative(List<String> list, int k) {
	    List<List<String>> result = new ArrayList<>();
	    subsetsOf(list, k, 0, new ArrayList<String>(), result);
	    return result;
	}
	public static List<List<String>> generateAllCombinationsSmallerThan(List<String> list,int k){
		List<List<String>> listToReturn = new ArrayList<List<String>>();
		for(int i=1;i<=k;i++) {
			listToReturn.addAll(combinationIterative(list,i));
		}
		return listToReturn;
	}

	public static List<List<String>> combineAdjecentElements(List<String> targetList,int size){
		 List<List<String>> combinations = new ArrayList<List<String>>();
		 System.out.println(targetList.size());
		 for(int i=0;i<=targetList.size()-size;i++) {
			 for(int k=1;k<=size;k++) {
				 for(String s:targetList.subList(i, i+k)) {
					 System.out.println(s);
				 }
			  combinations.add(targetList.subList(i, i+k));
			 }
		 }
		 
		 return combinations;
	}
}

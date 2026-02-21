package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListComparator {
	//return true if all elements in elementsToFind exist in sourceList in the same order that they occur in elementsToFind
	public static boolean containsAllAndOrderPreserved(List<String> sourceList,List<String> elementsToFind) {
		
		if(sourceList.containsAll(elementsToFind)) {
			if(elementsToFind.size()==1) return true;
			int nextElem=sourceList.indexOf(elementsToFind.get(0)); 
			int indexInSourceList;
			for(int i=1;i<elementsToFind.size();i++) {
				indexInSourceList=sourceList.indexOf(elementsToFind.get(i));
				if(indexInSourceList<nextElem) return false;
				else {
					nextElem=indexInSourceList;
				}
			}
			return true;
		}else {
			return false;
		}
		
	}
	//arraysAreEqualRegar([1,2,3],[1,2,3]) return false 
	//ezebi loopi 
	//3leh hethia me tekhdemch
	public static boolean arraysAreEqualRegardlessOfOrder(int[] array1,int[] array2) {
		int[] a1= array1.clone();
		int[] a2=array2.clone();
		Arrays.sort(a1);
		Arrays.sort(a2);
		return Arrays.equals(a1, a2);
		}
}

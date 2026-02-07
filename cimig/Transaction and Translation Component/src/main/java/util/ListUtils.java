package util;

import java.util.List;

public class ListUtils {

	public static String fromListToString(List<String> listOfStrings) {
		StringBuilder buffer = new StringBuilder();
		for(String sequence:listOfStrings) {
			buffer.append(sequence);
			buffer.append("\n");
		}

		buffer.setLength(buffer.length()-1); // gracefully trim last line break \n is two characters
		return buffer.toString();
	}

}

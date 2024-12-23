package org.lakehouse.cli.component;

import org.javatuples.Pair;
import org.springframework.stereotype.Component;

@Component
public class Parcer {

	
	public Pair<String, String> parceEntry(String s){
		if (s.length() < 2) new Pair<>("", "");
		try {
			StringBuffer sb = new StringBuffer(s.trim());
			int commandDelimiterPos = sb.indexOf(" ");
			if (commandDelimiterPos > 0) {
				String first = sb.substring(0, commandDelimiterPos).trim().toLowerCase();
				
				Pair<String, String> result = new Pair<>(
					first,
					sb.substring(commandDelimiterPos).trim());
				System.out.println(String.format("first %s second %s",result.getValue0(),result.getValue1()));
				return result;
			}
			else return new Pair<>(sb.toString(), "");
		}catch (Exception e) {
			e.printStackTrace();
			return new Pair<>("", "");
		}
	}
}

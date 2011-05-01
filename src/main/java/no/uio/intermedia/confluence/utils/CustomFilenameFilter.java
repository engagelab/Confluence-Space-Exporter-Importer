package no.uio.intermedia.confluence.utils;

import java.io.File;
import java.io.FilenameFilter;

public class CustomFilenameFilter implements FilenameFilter {

	private String pattern;
	
	public CustomFilenameFilter(String pattern) {
		this.pattern = pattern;
	}
	
	@Override
	public boolean accept(File dir, String fileName) {
		if( fileName != null && pattern !=null ) {
			if( fileName.equals(pattern) ) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

}

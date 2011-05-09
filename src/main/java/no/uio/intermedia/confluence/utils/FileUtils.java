package no.uio.intermedia.confluence.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;

/**
 * FileUtils
 * 
 * @author anthonjp
 *
 */
public class FileUtils {

	public static File[] findFile(String directoryPath, String fileName) {
		
		FilenameFilter filenameFilter = new CustomFilenameFilter(fileName);
		File dir = new File(directoryPath);
		File[] files = dir.listFiles(filenameFilter);
		return files;
		
	}
	
	public static void findAndReplace(String filePath, String pattern, String replace) throws IOException, FileNotFoundException {
		findAndReplace(new File(filePath), pattern, replace);
		
	}
	
	public static void findAndReplace(File file, String pattern, String replace) throws IOException, FileNotFoundException {
		
		FileInputStream fileInputStream = new FileInputStream(file);
		String content = IOUtils.toString(fileInputStream);
		content = content.replaceAll(pattern, replace);
		
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		IOUtils.write(content, fileOutputStream);
		
		fileInputStream.close();
		fileOutputStream.close();
	}
	
	public static void decompressFile(String zipPath, String destPath) throws IOException, ArchiveException {
		ArchiveCompression arch = new ArchiveCompression(zipPath, destPath);
		arch.decompressArchive();
	}
	
	public static void compressFile(String zipPath, String destPath) throws ArchiveException, IOException {
		ArchiveCompression arch = new ArchiveCompression(zipPath, destPath);
		arch.compressArchive();
	}
}

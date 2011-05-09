package no.uio.intermedia.confluence.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;

/**
 * Does the archiving and dearchiving of a path
 * 
 * @author anthonjp
 *
 */
public class ArchiveCompression {

	private String destPath;
	private String zipPath;

	public ArchiveCompression(String zipPath, String destPath) {
		this.zipPath = zipPath;
		this.destPath = destPath;

	}

	public File decompressArchive() throws IOException, ArchiveException {
		System.out.println("extracting file: " + zipPath);
		
		final InputStream is = new FileInputStream(zipPath);
		ArchiveInputStream in = new ArchiveStreamFactory()
				.createArchiveInputStream("zip", is);

		ZipArchiveEntry entry = (ZipArchiveEntry) in.getNextEntry();
		while (entry != null) {

			System.out.println("extracting: " + entry.getName());
			File file = new File(destPath, entry.getName());

			if (entry.isDirectory()) {
				file.mkdirs();
			} else {

				File dir = new File(file.getParent());
				dir.mkdirs();

				OutputStream out = new FileOutputStream(file);
				IOUtils.copy(in, out);
				out.flush();
			}

			entry = (ZipArchiveEntry) in.getNextEntry();
		}

		in.close();
		
		System.out.println("extracting finished!");
		
		return new File(destPath);
	}

	public void compressArchive() throws ArchiveException, IOException {
		System.out.println("stating compression.....for " + zipPath);
		//unix it!
        destPath = destPath.replaceAll("\\\\", "/");
		
        //remove the last bit
        if( StringUtils.endsWith(destPath, "/") ) {
        	destPath = StringUtils.remove(destPath, "/");
        }
		
		//1st make output stream of the zip
		final OutputStream out = new FileOutputStream(new File(zipPath));
		ArchiveOutputStream os = new ArchiveStreamFactory()
				.createArchiveOutputStream("zip", out);

		
		Iterator<File> iterateFiles = FileUtils.iterateFiles(new File(destPath), TrueFileFilter.TRUE, TrueFileFilter.INSTANCE);
		
		
		
		while (iterateFiles.hasNext()) {
			File nextFile = iterateFiles.next();
			
			String chomp = StringUtils.substring(nextFile.getPath(),destPath.length());
			ZipArchiveEntry zEntry = new ZipArchiveEntry(chomp);
			zEntry.setTime(nextFile.lastModified());
			os.putArchiveEntry(zEntry);
			IOUtils.copy(new FileInputStream(nextFile), os);
			os.closeArchiveEntry();
			os.flush();
		}
		os.finish();
//		out.close();
//		os.close();
		System.out.println("finished!");
	}

}

package no.uio.intermedia.confluence.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;
import no.uio.intermedia.confluence.ConfluenceConnector;
import no.uio.intermedia.confluence.utils.ArchiveCompression;
import no.uio.intermedia.confluence.utils.FileUtils;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.swizzle.confluence.ConfluenceException;
import org.codehaus.swizzle.confluence.SwizzleException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests some of the operations that are used in the client.
 * 
 * @author anthonjp
 *
 */
public class OperationsTests {
	
	private ConfluenceConnector confluence;
	String spaceKey = "cbets";
	String username = "";
	String password = "";
	String endpoint = "http://someurl/rpc/xmlrpc";
	String zipPath =  "/Users/bob/dev/workspaces/Confluence/Confluence-Blog-Exporter-Importer/cbets-110428111626+0200-xml.zip";
	String filePath = "/Users/bob/dev/workspaces/Confluence/Confluence-Blog-Exporter-Importer/cbets-110428111626+0200-xml";
	String fileToArchive = "/Users/bob/dev/workspaces/Confluence/Confluence-Blog-Exporter-Importer/cbets-110428111626+0200-xml.zip";

	String filePathForFindAndReplace = "/Users/bob/dev/workspaces/Confluence/Confluence-Blog-Exporter-Importer/entities.xml";
	
	private byte[] fileByte;
	
	@Ignore
	public void setUp() throws Exception {
		confluence = new ConfluenceConnector(endpoint);
		confluence.login(username, password);
		
		//get the bytes
		InputStream ios = new FileInputStream(zipPath);
		fileByte = IOUtils.toByteArray(ios);
	}

	@Ignore
	public void testImport() throws ConfluenceException, SwizzleException {
		confluence.importSpace(fileByte);
		System.out.println("import finished!");
	}
	
	@Ignore
	public void testUnzipping() throws IOException, ArchiveException {
		ArchiveCompression da = new ArchiveCompression(zipPath, filePath);
		da.decompressArchive();
	}
	
	
	@Ignore
	public void testFileSearch(){
	
		String fileName = "entities.xml";
		File[] files = FileUtils.findFile(filePath, fileName);
		
		Assert.assertTrue(files.length > 0);
		Assert.assertEquals(((File)files[0]).getName(), fileName);
		
	}
	
	
	@Ignore
	public void testFindAndReplace() throws FileNotFoundException, IOException {
		String pattern = "TESTER";
		String replace = "BOB";
		
		
		String fileName = "entities.xml";
		File[] files = FileUtils.findFile(filePath, fileName);
		
		FileUtils.findAndReplace(files[0], pattern, replace);
		
		FileInputStream fileInputStream = new FileInputStream(files[0]);
		String content = IOUtils.toString(fileInputStream);
		int countMatches = StringUtils.countMatches(content, replace);
		
		Assert.assertTrue(countMatches > 0);
		
		
	}

	@Ignore
	public void testZipping() throws Exception {
 		FileUtils.compressFile(fileToArchive, filePath);
	}

}

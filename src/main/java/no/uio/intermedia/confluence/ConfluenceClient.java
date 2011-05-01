package no.uio.intermedia.confluence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import no.uio.intermedia.confluence.utils.ArchiveCompression;
import no.uio.intermedia.confluence.utils.FileUtils;
import no.uio.intermedia.confluence.utils.HttpClientHelper;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.codehaus.swizzle.confluence.BlogEntry;
import org.codehaus.swizzle.confluence.BlogEntrySummary;
import org.codehaus.swizzle.confluence.ConfluenceException;
import org.codehaus.swizzle.confluence.SwizzleException;

/**
 * Command line client that exports blog posts a confluence instance
 * 
 * using swizzle:
 * http://swizzle.codehaus.org/swizzle-confluence/
 * 
 * @author anthonjp
 *
 */
public class ConfluenceClient {

	private static final String ENTITIES_XML = "entities.xml";
	private static final int _1000 = 1000;
	public boolean isDOWNLOADING = false;
	public int timeCountArchiveServer = 0;
	public int timeCountDownFromServer = 0;
	
	private static final String patternOption = "p";
	
	private static final String replaceOption = "r";
	
	private static final String fileOption = "f";
	
	private static final String helpOption = "help";

	private static final String hOption = "h";

	private static final String usernameOption = "u";

	private static final String passwordOption = "p";

	private static final String urlOption = "url";

	private static final String commandOption = "c";

	private static final String spaceKeyOption = "k";

	Logger  logger = Logger.getLogger(ConfluenceClient.class);
	
	char[] animationChars = new char[] { '|', '=', '/', '-', '\\' };
	
	private ConfluenceRedux confluence;

	private String username;
	private String password;
	private String spaceKey;
	private Date startExportDate;
	private Date endExportDate;
	 
	public ConfluenceClient() {
		
	}
	
	/**
	 * creates the connection
	 * 
	 * @param username
	 * @param password
	 * @param url
	 * @throws ConfluenceException
	 * @throws SwizzleException
	 * @throws MalformedURLException
	 */
	public ConfluenceClient(String username, String password, String url) throws ConfluenceException, SwizzleException, MalformedURLException {
		
		this.username = username;
		this.password = password;
		
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		String endpoint = url + "/rpc/xmlrpc";
		logger.info("url endpoint: " + endpoint);

		confluence = new ConfluenceRedux(endpoint);
		confluence.login(username, password);
	}

	/**
	 * imports a space into an instance from a zip
	 * 
	 * @param zipFileName
	 * @throws IOException
	 * @throws ConfluenceException
	 * @throws SwizzleException
	 */
	public void importSpace(String zipFileName) throws IOException, ConfluenceException, SwizzleException {
		
		System.out.println("doing import....");
		InputStream ios = new FileInputStream(zipFileName);
		byte[] fileByte = IOUtils.toByteArray(ios);
		confluence.importSpace(fileByte);
		System.out.println("import finished!");
		
	}
	
	/**
	 * replaces a file in an entities file
	 * 
	 * @param pattern
	 * @param replacement
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ArchiveException
	 */
	public void replaceStringInEntitiesFile(String pattern, String replacement, String fileName) throws FileNotFoundException, IOException, ArchiveException {
	
		
		if( fileName != null && fileName.endsWith(".zip")) {
			//zip the file
			
			System.out.println("extracting zip..." + fileName);
			
			ArchiveCompression da = new ArchiveCompression(fileName, StringUtils.remove(fileName, ".zip"));
			File decompressArchive = da.decompressArchive();
			
			System.out.println("replacing string " + pattern + " with " + replacement + " in " + ENTITIES_XML);
			
			File[] files = FileUtils.findFile(decompressArchive.getPath(), ENTITIES_XML);
			
			FileUtils.findAndReplace(files[0], pattern, replacement);
			
			System.out.println("done replacement.");
			
			System.out.println("backing up old zip....");
			
			org.apache.commons.io.FileUtils.copyFile(new File(fileName), new File(fileName.concat(".old")));
			
			System.out.println("zipping backup....");
			
			FileUtils.compressFile(fileName, StringUtils.remove(fileName, ".zip"));
			
			programExit();
			
			
		}
	
	}
	
	/**
	 * exports a space
	 * 
	 * @param spaceKey
	 * @throws ConfluenceException
	 * @throws SwizzleException
	 */
	private void exportSpace(final String spaceKey) throws ConfluenceException, SwizzleException {
		this.spaceKey = spaceKey;
		System.out.println("Starting space export....archiving (.zip) up on server..\n");
		
//		Thread downLoadThread = new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				isDOWNLOADING = true;
//				
//
//		  		System.out.print(animationChars[0]);
//		  		
//		  	   
////				while(isDOWNLOADING) {
////					try {
////						System.out.print(animationChars[1]);
////						timeCountArchiveServer = timeCountArchiveServer + _1000;
////						Thread.sleep(_1000);
////					} catch (InterruptedException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}
////					
////				}
//			
//				
//			}
//		});
//		
//		downLoadThread.start();
		
		
		
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				String downloadUrl = null;
				try {
					downloadUrl = confluence.exportSpace(spaceKey, "TYPE_XML");
				} catch (ConfluenceException e1) {
					e1.printStackTrace();
				} catch (SwizzleException e1) {
					e1.printStackTrace();
				} finally {
					isDOWNLOADING = false;
					downloadUrl = downloadUrl.concat("?os_username=" + username + "&os_password=" + password);
					System.out.print(animationChars[0]);
					System.out.println(" done file exported on server!");
					
				}
				
				try {
					
					
					downloadSpace(downloadUrl);
					
					isDOWNLOADING = false;
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		
		
	}
	
	/**
	 * downloads a zipped up space
	 * 
	 * @param downloadUrl
	 * @throws IOException
	 */
	private void downloadSpace(final String downloadUrl) throws IOException {
		
		new Thread(new Runnable() {
			  public void run() {
			    try {
			    	
			    	
			    	
			    	SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
			  		String format = df.format(new Date());
			  		String fileName = spaceKey + "-"+ format + "-xml.zip";
			    	
			    	
			    	System.out.println("\n\nDownloading zip from url: " + StringUtils.substring(downloadUrl,0, downloadUrl.indexOf("?")));	
			  		
			    	
			    	long currentTimeMillis1 = System.currentTimeMillis();
			    	
			    	org.apache.commons.io.FileUtils.copyURLToFile(new URL(downloadUrl), new File(fileName));
			  		//HttpClientHelper.getFile(downloadUrl, fileName);
			    	
			    	long currentTimeMillis2 = System.currentTimeMillis();

			  		long remainder = currentTimeMillis2 - currentTimeMillis2;
			  		
			    	System.out.print("done file downloaded.\n");
			  		System.out.print("\nTime: = "+ remainder/1000 + " sec");
			  		System.out.println("\nSpace export finished.\n");
			  		
					programExit();
			      }
			    
			    catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
			}).start();
		
		
		
		
	}
	
	/**
	 * exit
	 */
	public void programExit() {
		try {
			confluence.logout();
		} catch (ConfluenceException e) {
			e.printStackTrace();
		} catch (SwizzleException e) {
			e.printStackTrace();
		}
		System.out.println("Program exited successfully.");
	}

	private void getBlogPosts(String spaceKey) throws ConfluenceException, SwizzleException {
		
		
		List blogEntries = confluence.getBlogEntries(spaceKey);
		
		
		
		logger.info("list  " + blogEntries);
		
		for (Iterator iterator = blogEntries.iterator(); iterator.hasNext();) {
			BlogEntrySummary blogEntrySummary = (BlogEntrySummary) iterator.next();
			
			BlogEntry blogEntry = confluence.getBlogEntry(blogEntrySummary.getId());
			
			logger.info("blog entry: " + blogEntry.getContent());
			
			logger.info("comments: " + confluence.getComments(blogEntry.getId()));
			
			logger.info("attachments: " + confluence.getAttachments(blogEntry.getId()));
			
			
			
		}
	}
	
	/**
	 * takes command line args
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
        Options opt = new Options();

        opt.addOption(commandOption,true, "The command to perform: import | export.");
        opt.addOption(spaceKeyOption,true, "The confluence space key.");
        opt.addOption(usernameOption,true, "The username to use.");
        opt.addOption(passwordOption,true, "The password");
        opt.addOption(urlOption,true, "The url of the confluence instance. example http://www.intermedia.uio.no");
        opt.addOption(hOption,false, "Print the help.");
        opt.addOption(helpOption,false, "Print the help.");
        opt.addOption(patternOption,true, "String that needs replaced");
        opt.addOption(replaceOption,true, "Replacement String");
        opt.addOption(fileOption,true, "file");

        BasicParser parser = new BasicParser();
        CommandLine commandLine;
		try {
			commandLine = parser.parse(opt, args);
			HelpFormatter f;
			if( args.length == 0 || ( commandLine.hasOption(helpOption) || commandLine.hasOption(hOption)) ) {
				System.out.println("no arguments");
				f = new HelpFormatter();
				f.printHelp(" ", opt);
				String example = "\nExample export: ConfluenceClient -c export -k spacekey -u username -p password -url  http://confluenceInstanceUrl";
				System.out.println(example);
				example = "\nExample import: ConfluenceClient -c import -u username -p password -url http://confluenceInstanceUrl -f /full path to zip/some.zip";
				System.out.println(example);
				example = "\nExample replace: ConfluenceClient -c replace -p patternString -r replacementString -f /full path to zip/some.zip";
	        } else {
	        	
	        	String command = commandLine.getOptionValue(commandOption);
	        	
	    		String spaceKey;
	    		String userName;
	    		String password;
	    		String url;
	    		String pattern;
	    		String replacementString;
	    		String fileName;
	    		
	    		if(command !=  null && command.equals("import") ) {
	    			
	    			if( commandLine.getOptionValue(usernameOption) != null ) {
	    				userName = commandLine.getOptionValue(usernameOption);
	    			} else {
	    				System.out.println("Missing username -" + usernameOption +" example: bbarker");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(passwordOption) != null ) {
	    				password = commandLine.getOptionValue(passwordOption);
	    			} else {
	    				System.out.println("Missing password -" + passwordOption +" example: nodoggy");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(urlOption) != null ) {
	    				url = commandLine.getOptionValue(urlOption);
	    			} else {
	    				System.out.println("Missing url -" + urlOption +" http://<your confluence url>");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(fileOption) != null ) {
	    				fileName = commandLine.getOptionValue(fileOption);
	    			} else {
	    				System.out.println("Missing filename -" + fileOption );
	    				return;
	    			}
	    			
	    			
	    			
	    			ConfluenceClient confluenceClient = new ConfluenceClient(userName, password, url);
	    			
	    			confluenceClient.importSpace(fileName);
	    			
	    			
	    		} else if(command !=  null && command.equals("replace") ) {
	    			
	    			if( commandLine.getOptionValue(patternOption) != null ) {
	    				pattern = commandLine.getOptionValue(patternOption);
	    			} else {
	    				System.out.println("Missing pattern -" + patternOption );
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(replaceOption) != null ) {
	    				replacementString = commandLine.getOptionValue(replaceOption);
	    			} else {
	    				System.out.println("Missing replace string -" + replaceOption );
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(fileOption) != null ) {
	    				fileName = commandLine.getOptionValue(fileOption);
	    			} else {
	    				System.out.println("Missing filename -" + fileOption );
	    				return;
	    			}
	    			
	    			ConfluenceClient confluenceClient = new ConfluenceClient();
	    			
	    			confluenceClient.replaceStringInEntitiesFile(pattern, replacementString, fileName);
	    			
	    			
	    		} else if( command !=  null && command.equals("export") ) {
	    			
	    			if( commandLine.getOptionValue(spaceKeyOption) != null ) {
	    				spaceKey = commandLine.getOptionValue(spaceKeyOption);
	    			} else {
	    				System.out.println("Missing space key -" + spaceKeyOption +" example: cbets");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(usernameOption) != null ) {
	    				userName = commandLine.getOptionValue(usernameOption);
	    			} else {
	    				System.out.println("Missing username -" + usernameOption +" example: bbarker");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(passwordOption) != null ) {
	    				password = commandLine.getOptionValue(passwordOption);
	    			} else {
	    				System.out.println("Missing password -" + passwordOption +" example: nodoggy");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(urlOption) != null ) {
	    				url = commandLine.getOptionValue(urlOption);
	    			} else {
	    				System.out.println("Missing url -" + urlOption +" http://<your confluence url>");
	    				return;
	    			}
	    			
	    			ConfluenceClient confluenceClient = new ConfluenceClient(userName, password, url);
	    			
	    			confluenceClient.exportSpace(spaceKey);
	    			//confluenceClient.getBlogPosts(spaceKey);
	    			
	    		} else {
	    			System.out.println("Missing command -" + commandOption + " import or export");
	    			return;
	    		
	    		
	    		}
	        	
	        	
	        	
	        }
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ConfluenceException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SwizzleException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ArchiveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
        
		
		    
	     
		
	      

	}


}

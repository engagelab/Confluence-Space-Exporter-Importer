package no.uio.intermedia.confluence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.codehaus.swizzle.confluence.BlogEntry;
import org.codehaus.swizzle.confluence.BlogEntrySummary;
import org.codehaus.swizzle.confluence.Confluence;
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

	
	private static final String helpOption = "help";

	private static final String hOption = "h";

	private static final String usernameOption = "u";

	private static final String passwordOption = "p";

	private static final String urlOption = "url";

	private static final String commandOption = "c";

	private static final String spaceKeyOption = "k";

	Logger  logger = Logger.getLogger(ConfluenceClient.class);
	
	private Confluence confluence;

	private String username;
	private String password;
	private String spaceKey;
	 
	public ConfluenceClient() {
		
	}
	
	public ConfluenceClient(String username, String password, String url) throws ConfluenceException, SwizzleException, MalformedURLException {
		
		this.username = username;
		this.password = password;
		
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		String endpoint = url + "/rpc/xmlrpc";
		logger.info("url endpoint: " + endpoint);

		confluence = new Confluence(endpoint);
		confluence.login(username, password);
	}
	
	private void exportSpace(String spaceKey) throws ConfluenceException, SwizzleException {
		this.spaceKey = spaceKey;
		System.out.println("Starting space export....\n");
		
		String downloadUrl = confluence.exportSpace(spaceKey, "TYPE_XML");
		
		System.out.println("From url:  " + downloadUrl +"\n");
		
		downloadUrl = downloadUrl.concat("?os_username=" + username + "&os_password=" + password);
		
		
		try {
			this.downloadSpace(downloadUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Space export finished.");
		this.programExit();
		
	}
	
	private void downloadSpace(final String downloadUrl) throws IOException {
		
		new Thread(new Runnable() {
			  public void run() {
			    try {
			    
			    	URL u = new URL(downloadUrl);
			  		URLConnection uc = u.openConnection();
			  		String contentType = uc.getContentType();
			  		int contentLength = uc.getContentLength();
			  		// if (contentType.startsWith("text/") || contentLength == -1) {
			  		// test
			  		InputStream raw = uc.getInputStream();
			  		InputStream in = new BufferedInputStream(raw);
			  		byte[] data = new byte[contentLength];
			  		int bytesRead = 0;
			  		int offset = 0;

			  		char[] animationChars = new char[] { '|', '=', '/', '-', '\\' };

			  		System.out.print(animationChars[0]);
			  		while (offset < contentLength) {
			  			bytesRead = in.read(data, offset, data.length - offset);
			  			if (bytesRead == -1) {
			  				System.out.println("connection aborted!");
			  				break;
			  			}
			  			offset += bytesRead;
			  			System.out.print(animationChars[1]);
			  		}
			  		in.close();
			  		

			  		if (offset != contentLength) {
			  			throw new IOException("Only read " + offset + " bytes; Expected "
			  					+ contentLength + " bytes");
			  		}

			  		SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
			  		String format = df.format(new Date());
			  		String filename = spaceKey + "-"+ format + "-xml.zip";

			  		FileOutputStream out = new FileOutputStream(filename);
			  		out.write(data);
			  		out.flush();
			  		out.close();
			  		
			  		
			  		System.out.print(animationChars[0] + " done file downloaded!");
			      }
			    
			    catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
			}).start();
		
		
		
		
	}
	
	public void programExit() {
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
	 * @param args
	 */
	/**
	 * @param args
	 */
	/**
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
        
        

        BasicParser parser = new BasicParser();
        CommandLine commandLine;
		try {
			commandLine = parser.parse(opt, args);
			HelpFormatter f;
			if( args.length == 0 || ( commandLine.hasOption(helpOption) || commandLine.hasOption(hOption)) ) {
				System.out.println("no arguments");
				f = new HelpFormatter();
				f.printHelp(" ", opt);
				String example = "\nExample export: ConfluenceClient -c export -k cbests -u bbarker -p nodoggy -url http://<your confluence url>";
				System.out.println(example);
	        } else {
	        	
	        	String command = commandLine.getOptionValue(commandOption);
	        	
	    		String spaceKey;
	    		String userName;
	    		String password;
	    		String url;
	    		
	    		if(command !=  null && command.equals("import") ) {
	    			
	    			
	    		} else if( command !=  null && command.equals("export") ) {
	    			
	    			if( commandLine.getOptionValue(spaceKeyOption) != null ) {
	    				spaceKey = commandLine.getOptionValue(spaceKeyOption);
	    			} else {
	    				System.out.println("Missing space key -" + spaceKeyOption +" cbets");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(usernameOption) != null ) {
	    				userName = commandLine.getOptionValue(usernameOption);
	    			} else {
	    				System.out.println("Missing username -" + usernameOption +" bbarker");
	    				return;
	    			}
	    			
	    			if( commandLine.getOptionValue(passwordOption) != null ) {
	    				password = commandLine.getOptionValue(passwordOption);
	    			} else {
	    				System.out.println("Missing password -" + passwordOption +" nodoggy");
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
		}

        
        
		
		    
	     
		
	      

	}


}

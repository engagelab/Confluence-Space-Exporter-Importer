package no.uio.intermedia.confluence;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.jai.operator.URLDescriptor;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;


/**
 * BlogEntry class - represents a Confluence blog entry and a NarraHand POI.
 * It helps to be familiar with the Confluence remote api at http://confluence.atlassian.com/display/DOC/Remote+API+Specification
 * 
 * @author thomasd
 *
 */
public class BlogEntry {

	private static final Logger logger = Logger.getLogger(BlogEntry.class.getName());	
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final int MAX_CONTENT_TEASER_LENGTH = 35;


	private String confluencePageId;
	private String author;
	private String character;
	private String labels;
	private SentImage sentImage;
	private String title;
    private String content;
    private String contentTeaser;
	private String poiId;
	private long timeCreated; // time when the entry was created by a phone
	private long timeUpdated; // time when the entry was updated by a phone, not if there is a more recent confluence update!
	private	int x;
    private int y;
    private BlogEntry nextByCharacter;
    private BlogEntry previousByCharacter;
    private BlogEntry nextByAuthor;
    private BlogEntry previousByAuthor;
    private float distanceToCurrentPos;


    protected BlogEntry() {
	}
	
	
    /**
     * Create <code>BlogEntry</code> from a hashtable of request parameters
     * @param requestParams
     * @return a new BlogEntry
     */
    public static BlogEntry createBlogEntryFromHash(Hashtable<String, Serializable> requestParams) {
		BlogEntry blogEntry = new BlogEntry();

		blogEntry.poiId = (String) requestParams.get("id");
		if (blogEntry.poiId == null) {
			logger.error("poiId is null");
			return null;
		}
		blogEntry.author = ((String) requestParams.get("author")).trim();
		blogEntry.character = ((String) requestParams.get("character")).trim();
		blogEntry.title = stripHtml((requestParams.get("character")  + " (" + Phone2Wiki.getFormattedTime() + ")"));
		//blogEntry.title = stripHtml((String) requestParams.get("character"));
		blogEntry.sentImage = (SentImage) requestParams.get("sentImage");
        blogEntry.content = ((String) requestParams.get("content")).trim();;
        blogEntry.contentTeaser = stripHtml((String) requestParams.get("contentTeaser"));
        blogEntry.timeCreated = new Date().getTime();
        blogEntry.timeUpdated = 0;
        blogEntry.x = Integer.parseInt((String) requestParams.get("x"));
        blogEntry.y = Integer.parseInt((String) requestParams.get("y"));
		
		String lab = (String) requestParams.get("label");
		if (lab != null && lab.trim().length() > 0) {
			blogEntry.labels = lab.replaceAll(",", " ");
			blogEntry.labels = lab.replaceAll("  ", " ");
			blogEntry.labels = lab.trim().toLowerCase();
		} else {
			blogEntry.labels = null;
		}
		
		return blogEntry;
	}
	
	
	/**
	 * Create <code>BlogEntry</code> from a <code>org.dom4j.element</code>
	 * 
	 * @param element
	 * @return a new BlogEntry
	 */
//    protected static BlogEntry createBlogEntryFromElement(Element element) {
//		BlogEntry blogEntry = new BlogEntry();
//        blogEntry = new BlogEntry();
//        blogEntry.poiId = element.attribute("id").getValue();
//        blogEntry.confluencePageId = element.attribute("confluencePageId").getValue();
//        blogEntry.author = stripHtml(element.attribute("author").getValue().trim());
//        if (element.attribute("contentTeaser") != null) {
//            blogEntry.contentTeaser = stripHtml(element.attribute("contentTeaser").getValue());            
//        }
//		blogEntry.character = element.attribute("character").getValue().trim();
//        blogEntry.timeCreated = Long.valueOf(element.attribute("timeCreated").getValue()).longValue();
//        blogEntry.timeUpdated = Long.valueOf(element.attribute("timeUpdated").getValue()).longValue();
//        blogEntry.x = Integer.parseInt(element.attribute("x").getValue());
//        blogEntry.y = Integer.parseInt(element.attribute("y").getValue());
//		return blogEntry;
//	}
    
    protected static BlogEntry createBlogEntryFromElement(Element poiElement) {
		BlogEntry blogEntry = new BlogEntry();
        blogEntry = new BlogEntry();
        logger.info(poiElement.elements());
        blogEntry.poiId = poiElement.attribute("id").getValue();
        blogEntry.confluencePageId = (poiElement.element("confluencePageId")).getText();
        blogEntry.author = stripHtml((poiElement.element("author")).getText().trim());
        if ((poiElement.element("contentTeaser")).getText() != null) {
            blogEntry.contentTeaser = stripHtml((poiElement.element("contentTeaser")).getText());            
        }
		blogEntry.character = (poiElement.element("character")).getText().trim();
        blogEntry.timeCreated = Long.valueOf((poiElement.element("timeCreated")).getText()).longValue();
        blogEntry.timeUpdated = Long.valueOf((poiElement.element("timeUpdated")).getText()).longValue();
        blogEntry.x = Integer.parseInt((poiElement.element("x")).getText());
        blogEntry.y = Integer.parseInt((poiElement.element("y")).getText());
		return blogEntry;
	}
	
	
    /**
     * overwrites label and contents with Confluence as source
     */
	protected void refreshContentAndLabelsFromConfluence() {
        Hashtable<String, String> confluenceBlogHash = this.getConfluenceBlogEntry();
        if (confluenceBlogHash != null) {
            // getting content and labels from confluence to make sure we have up to date stuff         
            this.labels = this.getConfluenceLabelsAsString();
            this.labels = removeNorwegianCharacters(this.labels);
            this.content = confluenceBlogHash.get("content").trim();
            // update the xml, because this might be fresh stuff from confluence
            XMLOperations.updateXml(this, true);
        }    
	}
	    

	/**
	 * Get the content, matching Confluence-specific HTML markup
	 * 
	 * @return formatted content
	 */
	protected String getContentFormattedForWeb() {
		// get image string
		String imgHtml = this.generateHtmlImageString(true);	
		if (imgHtml != null && this.content != null) {
			return "{html}" + imgHtml + this.content + "{html}";
		} else if (this.content != null) {
			return "{html}" + this.content + "\n{html}";
		} else {
			return "";
		}
	}

	
	/**
	 * Create WML from content and other attributes, used when the phone client want's to view a single POI
	 * 
	 * @return complete WML representation of blogEntry (this)
	 */
	protected String getContentFormattedForWml() {
	    
	    // removing ALL html stuff from confluence!!!
        String formattedContent = stripHtmlExceptNarrahandPoiReference(this.getContent());

	    // image
		String imgHtml = this.generateHtmlImageString(false);
		if (imgHtml != null && imgHtml.contains("<img src")) {
			imgHtml = imgHtml.replaceAll("/download/attachments/", Phone2Wiki.HOST_URL + "/download/thumbnails/");
			formattedContent = imgHtml + "<p>" + formattedContent + "</p><br />";
		}
		
		// author
        //formattedContent = formattedContent + "<p><small>by " + this.getAuthor() + "@" + FORMAT.format(new Date(this.getTimeCreated())) + "</small></p>";
        
        // labels
		if (this.hasLabels()) {
			formattedContent = formattedContent + "<p>Labels: " + this.getLabels() + "</p>";
		}
		
		// "with charachter"-links
		if (this.previousByCharacter != null || this.nextByCharacter != null) {
		    formattedContent = formattedContent + "<p>";
		    if (this.previousByCharacter != null) {	            
	            formattedContent = formattedContent + "<a href=\"entry://id=" + this.previousByCharacter.getPoiId() + "\">&lt;&lt;</a>";		    
	        }
            formattedContent = formattedContent + " by " + this.character + " ";
            if (this.nextByCharacter != null) {             
                formattedContent = formattedContent + "<a href=\"entry://id=" + this.nextByCharacter.getPoiId() + "\">&gt;&gt;</a>";            
            }
            formattedContent = formattedContent + "</p>";
		}

        // "by author"-links commented out because of cluttered information display on the client
//        if (this.previousByAuthor != null || this.nextByAuthor != null) {
//            formattedContent = formattedContent + "<p>";
//            if (this.previousByAuthor != null) {             
//                formattedContent = formattedContent + "<a href=\"entry://id=" + this.previousByAuthor.getPoiId() + "\">&lt;&lt;</a>";            
//            }
//            formattedContent = formattedContent + " by " + this.author + " ";
//            if (this.nextByAuthor != null) {             
//                formattedContent = formattedContent + "<a href=\"entry://id=" + this.nextByAuthor.getPoiId() + "\">&gt;&gt;</a>";            
//            }
//            formattedContent = formattedContent + "</p>";
//        }	
		
        formattedContent = formattedContent + "<br />";
        
        //comment link
        //<a href=comment://pageId=40894554>Comment</a>
        formattedContent = formattedContent + "<p>" + "<a href=\"comment://pageId=" + this.confluencePageId  + Phone2Wiki.ADD_COMMENT_URL_SUFFIX + "\">Comment</a>" + "</p>";        
        
        // "view in map"-link
		formattedContent = formattedContent + "<p>" + "<a href=\"georef://x=" + this.getX() + "&y=" + this.getY() + "\">View in map</a>" + "</p>";

		//adding this <br /> as a hack around a bug in the FasterImaging wml-renderer
		formattedContent = formattedContent + "<br />";

		return GetWML.WML_HEADER + formattedContent + GetWML.WML_FOOTER;
	}
	
	protected String getContentFormattedForHtml() {
	    
	    // removing ALL html stuff from confluence!!!
        String formattedContent = stripHtmlExceptNarrahandPoiReference(this.getContent());

	    // image
		String imgHtml = this.generateSpecifiedHtmlImageString(false);
		if (imgHtml != null && imgHtml.contains("<img src")) {
			imgHtml = imgHtml.replaceAll("/download/attachments/", Phone2Wiki.HOST_URL + "/download/thumbnails/");
			formattedContent = imgHtml + "<div class=\"narra-poi-content\"><div>" + formattedContent + "</div><br />";
		}
		else {
			formattedContent = "<div class=\"narra-poi-content\"><div>" + formattedContent + "</div><br />";
		}
		
		// author
        //formattedContent = formattedContent + "<p><small>by " + this.getAuthor() + "@" + FORMAT.format(new Date(this.getTimeCreated())) + "</small></p>";
        
        // labels
		if (this.hasLabels()) {
			formattedContent = formattedContent + "<div><span class=\"narra-poi-lab\">Labels:</span><span class=\"narra-poi-labels\"> " + this.getLabels() + "</span></div>";
		}
		
		// "with charachter"-links
		if (this.previousByCharacter != null || this.nextByCharacter != null) {
		    formattedContent = formattedContent + "<div>";
		    if (this.previousByCharacter != null) {	            
	            formattedContent = formattedContent + "<a href=\"entry://id=" + this.previousByCharacter.getPoiId() + "\">&lt;&lt;</a>";		    
	        }
            formattedContent = formattedContent + " by " + this.character + " ";
            if (this.nextByCharacter != null) {             
                formattedContent = formattedContent + "<a href=\"entry://id=" + this.nextByCharacter.getPoiId() + "\">&gt;&gt;</a>";            
            }
            formattedContent = formattedContent + "</div>";
		}

        // "by author"-links commented out because of cluttered information display on the client
//        if (this.previousByAuthor != null || this.nextByAuthor != null) {
//            formattedContent = formattedContent + "<p>";
//            if (this.previousByAuthor != null) {             
//                formattedContent = formattedContent + "<a href=\"entry://id=" + this.previousByAuthor.getPoiId() + "\">&lt;&lt;</a>";            
//            }
//            formattedContent = formattedContent + " by " + this.author + " ";
//            if (this.nextByAuthor != null) {             
//                formattedContent = formattedContent + "<a href=\"entry://id=" + this.nextByAuthor.getPoiId() + "\">&gt;&gt;</a>";            
//            }
//            formattedContent = formattedContent + "</p>";
//        }	
		
        //comment link
        //<a href=comment://pageId=40894554>Comment</a>
        formattedContent = formattedContent + "<div>" + "<a href=\"comment://pageId=" + this.confluencePageId  + Phone2Wiki.ADD_COMMENT_URL_SUFFIX + "\">Comment</a>" + "</div>";        
        
        // "view in map"-link
		formattedContent = formattedContent + "<div>" + "<a href=\"georef://x=" + this.getX() + "&y=" + this.getY() + "\">View in map</a>" + "</div>";

		//adding this <br /> as a hack around a bug in the FasterImaging wml-renderer
		formattedContent = formattedContent + "</div><br />";

		return GetHTML.HTML_HEADER + formattedContent + GetHTML.HTML_FOOTER;
	}


	/**
	 * Creates an HTML img string which includes all the images/attachments for the blogEntry (this)
	 *  
	 * @param forWeb - boolean should be true if img elements are to be separated by a br
	 * @return string of HTML
	 */
	private String generateHtmlImageString(boolean forWeb) {
		String imgHtml = null;
		Vector<Hashtable<String, Serializable>> attachments = this.getConfluenceAttachments();
		if (attachments != null) {
			imgHtml= new String();
			Hashtable<String, Serializable> attachment;
			for (int i = 0 ; i < attachments.size() ; i++ ) {
				attachment = (Hashtable<String, Serializable>) attachments.elementAt(i);
                imgHtml = imgHtml + "<img src=\"" + Phone2Wiki.HOST_URL + "/download/thumbnails/" + this.confluencePageId + "/"
				+ (String) attachment.get("fileName") + "\">";
				if (forWeb) {
					imgHtml = imgHtml + "<br />\n";
				}
			}			
		}
		return imgHtml;
	}
	
	
	private String generateSpecifiedHtmlImageString(boolean forWeb) {
		String imgHtml = null;
		Vector<Hashtable<String, Serializable>> attachments = this.getConfluenceAttachments();
		if (attachments != null) {
			imgHtml= new String();
			Hashtable<String, Serializable> attachment;
			for (int i = 0 ; i < attachments.size() ; i++ ) {
				attachment = (Hashtable<String, Serializable>) attachments.elementAt(i);
                imgHtml = imgHtml + "<img src=\"" + Phone2Wiki.HOST_URL + "/download/thumbnails/" + this.confluencePageId + "/"
				//+ (String) attachment.get("fileName") + "\" width=\"100%\">";
                + (String) attachment.get("fileName") + "\">";
				if (forWeb) {
					imgHtml = imgHtml + "<br />\n";
				}
			}			
		}
		return imgHtml;
	}
	
	
	/**
	 * Creates a blog entry in Confluence for the blogEntry (this). Wrapper for <code>updateAndPostPageWithAttachement</code>.
	 * 
	 * @return confluencePageId for the new blog entry
	 */
	protected String postNewBlogEntryToConfluence() {
		Hashtable<String, String> newBlogEntryHash = null;

		// first create a new page, in order to get an id
		newBlogEntryHash = this.postBlogEntry();

		this.confluencePageId = newBlogEntryHash.get("id");
		logger.debug("confluencePageID: " + this.confluencePageId);
		
		// if an image was sent, post it as an attachment to the new page
		if (this.hasImage() && this.confluencePageId != null) {
			Hashtable<String, String> attachmentPage = this.addConfluenceAttachement();
			logger.debug("attachmentID: " + attachmentPage.get("id"));
			// update the last post..
			Hashtable<String, String> blogEntryWithAttachment = this.updateAndPostPageWithAttachement(newBlogEntryHash.get("version"));
			this.confluencePageId = blogEntryWithAttachment.get("id");
			// we have an attached image. update content for correct image src.
			this.content = this.getContentFormattedForWeb();
		}
		return this.confluencePageId;
	}
	
	
	/**
	 * Update an existing Confluence blog entry, based on blogEntry (this). Deletes old attachments first, if there are new ones. Wrapper for <code>updateAndPostPageWithAttachement</code>.
	 * 
	 * @param versionToUpdate - string to specify version of the Confluence page
     * @return confluencePageId for the updated blog entry
	 */
	protected String updateBlogEntryToConfluence(String versionToUpdate) {
		// check for attachment
		if (this.hasImage()) {
			// will only delete attached image(s) if there is an incoming image
			this.deleteConfluenceAttachments();
			Hashtable<String, String> success = this.addConfluenceAttachement();
			logger.debug("attachmentID" + success.get("id"));
		} 
		Hashtable<String, String> updatedPage = this.updateAndPostPageWithAttachement(incrementValueOfString(versionToUpdate));
		return updatedPage.get("id");
	}
	
	
    /**
     * Update or create a Confluence blog entry, based on blogEntry (this).
     * 
     * @param versionToUpdate - string to specify version of the Confluence page
     * @return Hashtable representing the Confluence blog entry
     */
	private Hashtable<String, String> updateAndPostPageWithAttachement(String versionToUpdate) {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		Hashtable<String, String> blogEntryHash = new Hashtable<String, String>();
		blogEntryHash = new Hashtable<String, String>();
		blogEntryHash.put("space", Phone2Wiki.SPACE_NAME);
		blogEntryHash.put("title", this.getTitle());
		blogEntryHash.put("content", this.getContentFormattedForWeb());
		blogEntryHash.put("parentId", Phone2Wiki.getHomePageIDForSpace());
		blogEntryHash.put("id", this.confluencePageId);
		blogEntryHash.put("version", versionToUpdate);
		params.add(blogEntryHash);
		return ConfluenceClient.executeXmlRpcCallCreateBlogEntry(params);
	}
	
	
	protected boolean hasImage() {
		return (this.sentImage != null && this.sentImage.getImageName() != null && this.sentImage.getImageName().trim().length() > 0);
	}
	
	
	protected boolean hasLabels() {
		return (this.labels != null && this.labels.trim().length() > 0);
	}

	/**
	 * Creates a new blog entry in Confluence based on blogEntry (this)
	 * 
     * @return Hashtable representing the Confluence blog entry
	 */
	private Hashtable<String, String> postBlogEntry() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		Hashtable<String, String> blogEntryHash = new Hashtable<String, String>();
		blogEntryHash.put("space", Phone2Wiki.SPACE_NAME);
		blogEntryHash.put("title", this.getTitle());
		blogEntryHash.put("content", this.getContentFormattedForWeb());
		//blogEntryHash.put("content", this.getContent());
		blogEntryHash.put("parentId", Phone2Wiki.getHomePageIDForSpace());
		params.add(blogEntryHash);
		return ConfluenceClient.executeXmlRpcCallCreateBlogEntry(params);
	}

    /**
     * Get a blog entry from Confluence
     * 
     * @return Hashtable representing the Confluence blog entry
     */
	private Hashtable<String, String> getConfluenceBlogEntry() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(this.getConfluencePageId());
		Hashtable<String, String> blogEntry = ConfluenceClient.executeXmlRpcCallGetBlogEntry(params);
		if (blogEntry == null) {
			logger.error("No BlogEntry in confluence with id = " + this.getConfluencePageId());			
		}
		return blogEntry;		
	}
	
	
	/**
	 * Get Labels from blogEntry (this)
	 * 
	 * @return string containing all the labels, space-delimited
	 */
	private String getConfluenceLabelsAsString() {
		Vector<Hashtable<String, Serializable>> labelsVector = this.getConfluenceLabels();
		if (labelsVector != null) {
			String labels = "";
			for (int i = 0 ; i < labelsVector.size() ; i++) {
				labels = labels + (String) labelsVector.get(i).get("name") + " ";
			}
			return labels.trim();			
		}
		return null;
	}
	
	 /**
     * Fetch labels from Confluence for the blogEntry (this)
     * 
     * @return vector containing labels
     */
	private Vector<Hashtable<String, Serializable>> getConfluenceLabels() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(this.getConfluencePageId());
		return ConfluenceClient.executeXmlRpcCallGetLabels(params);
	}
	
    /**
     * Fetch attachments from Confluence for the blogEntry (this)
     * 
     * @return vector containing attachments
     */
	protected Vector<Hashtable<String, Serializable>> getConfluenceAttachments() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(this.confluencePageId);
		return ConfluenceClient.executeXmlRpcCallGetAttatchments(params);
	}
	
	/**
	 * Get Confluence version of the blogEntry (this)
	 * 
	 * @return string with version
	 */
	protected String getConfluenceVersion() {
		Hashtable<String, String> blogEntry = this.getConfluenceBlogEntry();
		return (blogEntry == null ? null : blogEntry.get("version"));
	}
	
	
	/**
	 * Delete all labels in Confluence from blogEntry (this)
	 */
	protected void deleteConfluenceLabels() {
		Vector<Hashtable<String, Serializable>> labels = this.getConfluenceLabels();
		Hashtable<String, Serializable> label;
		Vector<Serializable> params;
		if (labels != null) {
			for (int i = 0 ; i < labels.size() ; i++ ) {
				label = (Hashtable<String, Serializable>) labels.elementAt(i);
				logger.debug("deleting label: " + label.get("name"));
				params = new Vector<Serializable>();
				params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
				params.add((String) label.get("id"));
				params.add(this.getConfluencePageId());
				ConfluenceClient.executeXmlRpcCallRemoveLabel(params);
			}			
		}
	}
	
	 /**
     * Delete all attachments in Confluence from blogEntry (this)
     */
	protected void deleteConfluenceAttachments() {
		Vector<Hashtable<String, Serializable>> attachments = this.getConfluenceAttachments();
		Hashtable<String, Serializable> attachment;
		Vector<Serializable> params;
		for (int i = 0 ; i < attachments.size() ; i++ ) {
			attachment = (Hashtable<String, Serializable>) attachments.elementAt(i);
			params = new Vector<Serializable>();
			params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
			params.add(attachment.get("contentId"));
			params.add(attachment.get("fileName"));
			ConfluenceClient.executeXmlRpcCallDeleteAttatchment(params);
		}
	}	


	/**
	 * Upload a belonging <code>SentImage</code> to Confluence as an attachment
	 * 
	 * @return hashtable representing the page containing the attachments
	 */
	protected Hashtable<String, String> addConfluenceAttachement() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(this.confluencePageId);
		Hashtable<String, String> newAttachment = new Hashtable<String, String>();
		if (this.getSentImage().getImageName().contains(".")) {
			newAttachment.put("fileName", this.getSentImage().getImageName());
		} else {
			newAttachment.put("fileName", this.getSentImage().getImageName() + "." + this.getSentImage().getImageExtension());
		}
		newAttachment.put("contentType", this.getSentImage().getContentType());
		newAttachment.put("comment", "content from mobile");
		params.add(newAttachment);
		params.add(this.getSentImage().getImageByteArray());
		return ConfluenceClient.executeXmlRpcCallAddAttachementToPage(params);
	}
	
	/**
	 * Add labels to blogEntry (this)'s corresponding blog entry in Confluence
	 * 
	 * @param labels - string of labels, space-delimited
	 * @return true if add was successful
	 */
	protected boolean addConfluenceLabels(String labels) {
		logger.debug("addLabels: " + labels);
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(labels);
		params.add(this.confluencePageId);
		return ConfluenceClient.executeXmlRpcCallAddLabelToPage(params);
	}
	
	 /**
     * Delete labels from blogEntry (this)'s corresponding blog entry in Confluence
     */
	protected void deleteConfluencePage() {
		Vector<Serializable> params = new Vector<Serializable>();
		params.add(ConfluenceClient.getLoginToken(Phone2Wiki.getConfluenceUserName(), Phone2Wiki.getConfluenceUserPassword()));
		params.add(this.confluencePageId);
		try {
			ConfluenceClient.executeXmlRpcCallRemovePage(params);
		} catch (NullPointerException e) {
			logger.error("deleteEntryFromConfluence: " + e);
		}
	}

	/**
	 * Remove Norwegian characters from a string of labels
	 * 
	 * @param labels - string containing labels, space-delimited
	 * @return labels - string without Noregian characters
	 */
	protected static String removeNorwegianCharacters(String labels) {
	    labels = labels.toLowerCase();
        labels = labels.replaceAll("æ", "ae");
        labels = labels.replaceAll("ø", "o");
        labels = labels.replaceAll("ö", "o");
        labels = labels.replaceAll("å", "a");
        return labels;
	}
	
	
	protected static String incrementValueOfString(String origin) {
		int i = Integer.parseInt(origin) + 1;
		return String.valueOf(i);
	}
	
	
	protected String getConfluencePageId() {
		return confluencePageId;
	}

	
	protected void setConfluencePageId(String confluencePageId) {
		this.confluencePageId = confluencePageId;
	}

	
	protected String getAuthor() {
		return author;
	}

	
	protected void setAuthor(String author) {
		this.author = author;
	}

	
	protected String getCharacter() {
		return character;
	}

	
	protected void setCharacter(String character) {
		this.character = character;
	}
	
	
	/**
	 * Content teaser should be a trimmed down and clean version of content, if there is content.
	 * 
	 * @return contentTeaser
	 */
	protected String getContentTeaser() {
	    if (content == null && contentTeaser == null) {
	        return null;
	    } else if (content == null && contentTeaser != null) {
	        return stripHtml(contentTeaser);
	    } else {
	        contentTeaser = stripHtml(content);
	        if (contentTeaser.length() > MAX_CONTENT_TEASER_LENGTH) {
	            contentTeaser = contentTeaser.substring(0, MAX_CONTENT_TEASER_LENGTH).trim() + "...";
	        }
	    }
	    return contentTeaser;
	}

    
    protected void setContentTeaser(String teaser) {
        this.contentTeaser = teaser;
    }

	
	protected String getLabels() {
		return labels;
	}

	
	protected void setLabels(String labels) {
		this.labels = labels;
	}

	
	protected String getTitle() {
		return title;
	}

	
	protected void setTitle(String title) {
		this.title = title;
	}

	
	protected String getContent() {
		return content;
	}

	
	protected void setContent(String content) {
		this.content = content;
	}

	
	protected String getPoiId() {
		return poiId;
	}

	
	protected void setPoiId(String poiId) {
		this.poiId = poiId;
	}


	protected SentImage getSentImage() {
		return sentImage;
	}

	
	protected void setSentImage(SentImage sentImage) {
		this.sentImage = sentImage;
	}


	
	protected long getTimeCreated() {
		return timeCreated;
	}


	
	protected void setTimeCreated(long timeCreated) {
		this.timeCreated = timeCreated;
	}


	
	protected long getTimeUpdated() {
		return timeUpdated;
	}

	
	protected void setTimeUpdated(long timeUpdated) {
		this.timeUpdated = timeUpdated;
	}
	
	   
    protected int getX() {
        return x;
    }

    
    protected void setX(int x) {
        this.x = x;
    }

    
    protected int getY() {
        return y;
    }


    protected void setY(int y) {
        this.y = y;
    }
    
    
    protected BlogEntry getNextByCharacter() {
        return nextByCharacter;
    }

    
    protected BlogEntry getPreviousByCharacter() {
        return previousByCharacter;
    }

    
    protected BlogEntry getNextByAuthor() {
        return nextByAuthor;
    }


    protected BlogEntry getPreviousByAuthor() {
        return previousByAuthor;
    }

    
    
    protected float getDistanceToCurrentPos() {
        return distanceToCurrentPos;
    }
    
    protected void setDistanceToCurrentPos(float distanceToCurrentPos) {
        this.distanceToCurrentPos = distanceToCurrentPos;
    }
    
    
    protected long getLatestEditTime() {
        long time = this.timeUpdated;
        if (time == 0) {
            time = this.getTimeCreated();
        }
        return time;
    }
    
    /**
     * Comparator class for comparing blogEntries on date (last created time)
     */
    public static Comparator dateCompare = new Comparator() {
    public int compare(Object o1, Object o2) {
        if ((o1 instanceof BlogEntry) && (o2 instanceof BlogEntry)) {
            BlogEntry b1 = (BlogEntry) o1;
            BlogEntry b2 = (BlogEntry) o2;
//            if (b2.getLatestEditTime() > b1.getLatestEditTime()) return 1;
//            else if (b2.getLatestEditTime() < b1.getLatestEditTime()) return -1;
            if (b2.timeCreated > b1.timeCreated) return 1;
            else if (b2.timeCreated < b1.timeCreated) return -1;
        }
        return 0;
    }};


    /**
     * Comparator class for comparing blogEntries on distance to a third position (current position)
     */
    public static Comparator distanceCompare = new Comparator() {
    public int compare(Object o1, Object o2) {
        if ((o1 instanceof BlogEntry) && (o2 instanceof BlogEntry)) {
            BlogEntry b1 = (BlogEntry) o1;
            BlogEntry b2 = (BlogEntry) o2;
            if (b2.getDistanceToCurrentPos() < b1.getDistanceToCurrentPos()) return 1;
            else if (b2.getDistanceToCurrentPos() > b1.getDistanceToCurrentPos()) return -1;
        }
        return 0;
    }};
    
    
    /**
     * Iterate thru all blog entries and build th relations to blogEntry (this). This includes next and previous in time, and next by author
     * @param blogEntries
     */
    protected void buildRelations(HashMap<String, BlogEntry> blogEntries) {
        BlogEntry otherBlogEntry;
        for (String key : blogEntries.keySet()) {
            otherBlogEntry = blogEntries.get(key);
            //check for next by character
            if (this.character.equals(otherBlogEntry.character)) {
                // same character
                if (this.timeCreated < otherBlogEntry.timeCreated) { 
                    // created later than otherBlogEntry
                    if (this.nextByCharacter == null || otherBlogEntry.timeCreated < this.nextByCharacter.timeCreated) { 
                        // created earlier than already set nextByChar
                        this.nextByCharacter = otherBlogEntry;
                    }
                } else if (this.timeCreated > otherBlogEntry.timeCreated){
                    // created earlier than otherBlogEntry
                    if (this.previousByCharacter == null || otherBlogEntry.timeCreated > this.previousByCharacter.timeCreated) { 
                        // created later than already set nextByChar
                        this.previousByCharacter = otherBlogEntry;
                    }
                }
            }
            //check for next by author
            if (this.author.equals(otherBlogEntry.author)) {
                // same author
                if (this.timeCreated < otherBlogEntry.timeCreated) {
                    // created later than otherBlogEntry
                    if (this.nextByAuthor == null || otherBlogEntry.timeCreated < this.nextByAuthor.timeCreated) { 
                        // created earlier than already set nextByAuthor
                        this.nextByAuthor = otherBlogEntry;
                    }
                } else if (this.timeCreated > otherBlogEntry.timeCreated) {
                    // created earlier than otherBlogEntry
                    if (this.previousByAuthor == null || otherBlogEntry.timeCreated > this.previousByAuthor.timeCreated) { 
                        // created later than already set nextByAuthor
                        this.previousByAuthor = otherBlogEntry;
                    }
                }
            }
        }
    }	
	
    
    /**
     * Remove some markup from a string. First translates troublesome web entities to proper characters.
     * @param stringWithHtml - string with markup
     * @return text - string without markup
     */
    protected static String stripHtml(String stringWithHtml) {
        if (stringWithHtml == null) {
            return null;
        }
        String text = stringWithHtml;
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("<br />", "\n");
        text = text.replaceAll("\\{html\\}", "");
        return text.replaceAll("\\<.*?\\>", "");
    }
    
    
    /**
     * Remove all markup from a string, translates troublesome web entities to proper characters, do not remove poi reference
     * @param stringWithMarkup - string with markup
     * @return text - string without markup
     */
    protected static String stripHtmlExceptNarrahandPoiReference(String stringWithMarkup) {
        if (stringWithMarkup == null) {
            return null;
        }
        String returnText = stringWithMarkup.replaceAll("&lt;", "<");
        returnText = returnText.replaceAll("&gt;", ">");
        returnText = returnText.replaceAll("<br />", "\n");
        returnText = returnText.replaceAll("\\{html\\}", "");
        
        String regexPattern ="((<a href=\"entry://id=\\d+\">.*?</a>)|<!\\[CDATA\\[.*?\\]\\]>|<!--.*?-->|<.*?>)";
        
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(returnText);
        return returnText.replaceAll(regexPattern, "$2");
    }
    
    
}

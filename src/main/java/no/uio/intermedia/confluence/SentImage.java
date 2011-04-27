package no.uio.intermedia.confluence;

import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.Serializable;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

/**
 * SentImage class - represents an image received from a phone client
 * 
 * @author thomasd
 */
public class SentImage implements Serializable {

	private static final long serialVersionUID = -8188307738226556970L;

	private final static Logger logger = Logger.getLogger(SentImage.class.getName());
	
	private int imageHeight = 0;
	private int imageWidth = 0;
	private byte[] imageByteArray;
	private String imageName;
	private String imageExtension;
	private String contentType;
	

	/**
	 * Constructor used when creating a BlogEntry from XML
	 */
	public SentImage(String imageName) {
		this.imageName = imageName;
	}

	
    /**
     * Constructor used when creating a BlogEntry from a client post
     */
	public SentImage(FileItem fileItem, String encoded) {
		imageByteArray = fileItem.get();
		Image image = null;
		
		if("true".equals(encoded)) {
			image = Toolkit.getDefaultToolkit().createImage(Base64.decode(new String(imageByteArray)));					
		}
		else {
			image = Toolkit.getDefaultToolkit().createImage(imageByteArray);
		}
		
		MediaTracker mediaTracker = new MediaTracker(new Container());
	    mediaTracker.addImage(image, 0);
	    
		try {
			mediaTracker.waitForID(0);
		} catch (InterruptedException e) {
			logger.error("problems with mediaTracker" + e);
		}
		
		imageWidth = image.getWidth(null);
		imageHeight = image.getHeight(null);		
		imageName = fileItem.getName();
		imageExtension = fileItem.getContentType().substring("image/".length(), fileItem.getContentType().length());
		contentType = fileItem.getContentType();
	}



	protected int getImageHeight() {
		return imageHeight;
	}


	protected int getImageWidth() {
		return imageWidth;
	}

	
	protected byte[] getImageByteArray() {
		return imageByteArray;
	}

	
	protected String getImageName() {
		return imageName;
	}

	
	protected void setImageName(String imageName) {
		this.imageName = imageName;
	}

	
	protected String getImageExtension() {
		return imageExtension;
	}

	
	protected void setImageExtension(String imageExtension) {
		this.imageExtension = imageExtension;
	}

	
	protected String getContentType() {
		return contentType;
	}

	
	protected void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
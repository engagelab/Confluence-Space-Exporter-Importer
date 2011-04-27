package no.uio.intermedia.confluence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;


public class CustomFileItem implements FileItem {

	private static final long serialVersionUID = 1L;
	private String fieldName = null;
	private String fieldContent = null;
	private boolean formFieldBool;


	public void delete() {
		// TODO Auto-generated method stub

	}


	public byte[] get() {
		// TODO Auto-generated method stub
		return null;
	}


	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}


	public String getFieldName() {
		return fieldName;
	}


	public InputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}


	public OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}


	public String getString() {
		return fieldContent;
	}


	public String getString(String arg0) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return null;
	}


	public boolean isFormField() {
		return formFieldBool;
	}


	public boolean isInMemory() {
		// TODO Auto-generated method stub
		return false;
	}


	public void setFieldName(String arg0) {
		fieldName = arg0;
	}


	public void setFormField(boolean arg0) {
		formFieldBool = arg0;
	}
	
	public void setFieldString(String arg0) {
		fieldContent = arg0;
	}


	public void write(File arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}

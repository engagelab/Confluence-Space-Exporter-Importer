package no.uio.intermedia.confluence.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

public class HttpClientHelper {
	
	static char[] animationChars = new char[] { '|', '=', '/', '-', '\\' };
	
	public static void getFile(String url,String fileName) throws ClientProtocolException, IOException {
		
		
		FileOutputStream out = new FileOutputStream(fileName);
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		  httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

              public void process(
                      final HttpRequest request,
                      final HttpContext context) throws HttpException, IOException {
                  if (!request.containsHeader("Accept-Encoding")) {
                      request.addHeader("Accept-Encoding", "gzip");
                  }
              }

          });

          httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

              public void process(
                      final HttpResponse response,
                      final HttpContext context) throws HttpException, IOException {
                  HttpEntity entity = response.getEntity();
                  Header ceheader = entity.getContentEncoding();
                  if (ceheader != null) {
                      HeaderElement[] codecs = ceheader.getElements();
                      for (int i = 0; i < codecs.length; i++) {
                          if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                              response.setEntity(
                                      new GzipDecompressingEntity(response.getEntity()));
                              return;
                          }
                      }
                  }
              }

          });
          
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		
		System.out.println("");
  		System.out.print(animationChars[0]);

		if (entity != null) {
			
			long currentTimeMillis1 = System.currentTimeMillis();
			
		    InputStream instream = entity.getContent();
		    
		    long length = entity.getContentLength();
		    
		    
		    int l;
		    byte[] tmp = new byte[2048];
		    while ((l = instream.read(tmp)) != -1) {
		    	IOUtils.write(tmp, out);
		    	System.out.print(animationChars[1]);
		    }
		    
		    instream.close();
		    out.close();
		    
			long currentTimeMillis2 = System.currentTimeMillis();

	  		long remainder = currentTimeMillis2 - currentTimeMillis2;

	  		System.out.print(animationChars[0] + " done file downloaded.\n");
	  		System.out.print("\nTime: = "+ remainder/1000 + " sec");
	  		System.out.println("\nSpace export finished.\n");

		}
		
		
	}
	

}

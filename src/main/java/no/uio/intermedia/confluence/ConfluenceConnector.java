package no.uio.intermedia.confluence;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcAhcTransportFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.codehaus.swizzle.confluence.ConfluenceException;
import org.codehaus.swizzle.confluence.MapObject;
import org.codehaus.swizzle.confluence.SwizzleException;

/**
 * This is a total hack of the swizzle-confluence client. A hack because the swizzle-confluence client doesnt have
 * the ability to import a space to an instance. This class only has the ability to import and export a space. For other functions
 * use the Confluence class in the swizzle libraries. 
 *
 */
public class ConfluenceConnector {
	private final XmlRpcClient client;
	private String token;
	protected boolean sendRawData;

	public ConfluenceConnector(String endpoint) throws MalformedURLException {

		if (endpoint.endsWith("/")) {
			endpoint = endpoint.substring(0, endpoint.length() - 1);
		}

		if (!endpoint.endsWith("/rpc/xmlrpc")) {
			endpoint += "/rpc/xmlrpc";
		}

		XmlRpcClientConfigImpl clientConfig = new XmlRpcClientConfigImpl();
		clientConfig.setServerURL(new URL(endpoint));
		clientConfig.setEnabledForExtensions(true);
		clientConfig.setConnectionTimeout(0);
		client = new XmlRpcClient();

		client.setTransportFactory(new XmlRpcAhcTransportFactory(client));
		client.setConfig(clientConfig);
	}

	public boolean willSendRawData() {
		return sendRawData;
	}

	public void sendRawData(boolean sendRawData) {
		this.sendRawData = sendRawData;
	}

	public void login(String username, String password)
			throws SwizzleException, ConfluenceException {
		token = (String) call("login", username, password);
	}

	/**
	 * remove this token from the list of logged in tokens. Returns true if the
	 * user was logged out, false if they were not logged in in the first place
	 * (we don't really need this return, but void seems to kill XML-RPC for me)
	 */
	public boolean logout() throws SwizzleException, ConfluenceException {
		Boolean value = (Boolean) call("logout");
		return value.booleanValue();
	}


	/**
	 * exports a space and returns a String holding the URL for the download.
	 * The export type argument indicates whether or not to export in XML, PDF,
	 * or HTML format - use "TYPE_XML", "TYPE_PDF", or "TYPE_HTML" respectively.
	 * Also, using "all" will select TYPE_XML.
	 */
	public String exportSpace(String spaceKey, String exportType)
			throws SwizzleException, ConfluenceException {
		return (String) call("exportSpace", spaceKey, exportType);
	}

	/**
	 * Imports a space with a byte array
	 * 
	 * @param zip
	 * @return
	 * @throws SwizzleException
	 * @throws ConfluenceException
	 */
	public Boolean importSpace(byte[] zip) throws SwizzleException,
			ConfluenceException {
		return (Boolean) call("importSpace", zip);
	}

	private Object call(String command) throws SwizzleException,
			ConfluenceException {
		Object[] args = {};
		return call(command, args);
	}

	private Object call(String command, Object arg1) throws SwizzleException,
			ConfluenceException {
		Object[] args = { arg1 };
		return call(command, args);
	}

	private Object call(String command, Object arg1, Object arg2)
			throws SwizzleException, ConfluenceException {
		Object[] args = { arg1, arg2 };
		return call(command, args);
	}

	private Object call(String command, Object arg1, Object arg2, Object arg3)
			throws SwizzleException, ConfluenceException {
		Object[] args = { arg1, arg2, arg3 };
		return call(command, args);
	}

	private Object call(String command, Object arg1, Object arg2, Object arg3,
			Object arg4) throws SwizzleException, ConfluenceException {
		Object[] args = { arg1, arg2, arg3, arg4 };
		return call(command, args);
	}

	private Object call(String command, Object[] args) throws SwizzleException {
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg instanceof MapObject) {
				MapObject map = (MapObject) arg;
				if (sendRawData) {
					args[i] = map.toRawMap();
				} else {
					args[i] = map.toMap();
				}
			}
		}
		Object[] vector;
		if (!command.equals("login")) {
			vector = new Object[args.length + 1];
			vector[0] = token;
			System.arraycopy(args, 0, vector, 1, args.length);
		} else {
			vector = args;
		}
		try {
			return client.execute("confluence1." + command, vector);
		} catch (XmlRpcClientException e) {
			throw new SwizzleException(e.getMessage(), e.linkedException);
		} catch (XmlRpcException e) {
			throw new ConfluenceException(e.getMessage(), e.linkedException);
		}
	}
}

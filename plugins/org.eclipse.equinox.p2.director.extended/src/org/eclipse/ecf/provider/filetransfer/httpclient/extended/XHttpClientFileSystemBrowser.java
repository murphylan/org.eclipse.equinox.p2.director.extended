package org.eclipse.ecf.provider.filetransfer.httpclient.extended;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.security.UnsupportedCallbackException;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.provider.filetransfer.httpclient.HttpClientFileSystemBrowser;

public class XHttpClientFileSystemBrowser extends HttpClientFileSystemBrowser {

	/**
	 * @param urlWithUserInfo
	 * @return The same url without user info or null if there was no userinfo in there.
	 */
	static URL getURLWithoutUserInfo(URL url) {
		String oriStr = url.toExternalForm();
		String userInfo = url.getUserInfo();
		if (userInfo == null || userInfo.length() == 0) {
			return null;
		}
		//make sure we can rebuild the url from the '@' at the end of the
		//user-info
		int at = oriStr.indexOf(userInfo+"@");
		if (at == -1) {
			return null;
		}
		StringBuffer newBuf = new StringBuffer();
		if (oriStr.startsWith("xhttp://")) {
			newBuf.append("http://");
		} else if (oriStr.startsWith("xhttps://")) {
			newBuf.append("https://");
		} else {
			newBuf.append(url.getProtocol()+"://");
		}
		newBuf.append(oriStr.substring(at+userInfo.length()+1));
		try {
			url = new URL(newBuf.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return url;
	}
	
	static IFileID createDerivedIFileID(IFileID original, URL newUrl) {
		return (IFileID) IDFactory.getDefault().createID(original.getNamespace(), new Object[] {newUrl});
	}
	
	private URL originalURL;
	
	
	public XHttpClientFileSystemBrowser(HttpClient httpClient,
			IFileID directoryOrFileID, IRemoteFileSystemListener listener,
			URL directoryOrFileURL, IConnectContext connectContext, Proxy proxy) {
		super(httpClient, directoryOrFileID, listener, directoryOrFileURL,
				connectContext, proxy);
		URL withoutUserInfo = getURLWithoutUserInfo(directoryOrFileURL);
		if (withoutUserInfo != null) {
			this.originalURL = directoryOrFileURL;
			this.directoryOrFile = withoutUserInfo;
			this.fileID = createDerivedIFileID(directoryOrFileID, withoutUserInfo);
		}
	}

	@Override
	protected Credentials getFileRequestCredentials()
			throws UnsupportedCallbackException, IOException {
		Credentials cred = super.getFileRequestCredentials();
		if (cred != null) {
			//the super class probably knows better.
			return cred;
		}
		if (originalURL != null) {
			String userInfo = originalURL.getUserInfo();
			UsernamePasswordCredentials userNamePass = new UsernamePasswordCredentials(userInfo);
			setPrivateUsername(userNamePass.getUserName());
			return userNamePass;
		}
		return cred;
	}
	
	
	private static Field FIELD_username;
	private void setPrivateUsername(String username) {
		try {
			if (FIELD_username == null) {
				FIELD_username = HttpClientFileSystemBrowser.class.getDeclaredField("username");
				FIELD_username.setAccessible(true);
			}
			FIELD_username.set(this, username);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}


}

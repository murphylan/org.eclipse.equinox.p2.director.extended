package org.eclipse.ecf.provider.filetransfer.httpclient.extended;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;

/**
 * Adds support for passing the user info in the url.
 * Typically used to pass username/password for HTTP Authentication.
 * 
 * @author hmalphettes
 *
 */
public class XHttpClientRetrieveFileTransferFactory implements
		IRetrieveFileTransferFactory {

	public IRetrieveFileTransfer newInstance() {
		return new XHttpClientRetrieveFileTransfer(new HttpClient(
				new MultiThreadedHttpConnectionManager()));
	}

}

/**
 * Copyright (c) 2010, Intalio Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intalio Inc. - initial API and implementation
 */
package org.eclipse.ecf.provider.filetransfer.httpclient.extended;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;

/**
 * Adds support for passing the user info in the url.
 * Typically used to pass username/password for HTTP Authentication.
 * For example http://joe:secret@example.com/repository
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

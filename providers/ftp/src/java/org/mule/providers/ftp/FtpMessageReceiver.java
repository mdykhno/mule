/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.ftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.resource.spi.work.Work;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FtpMessageReceiver extends PollingMessageReceiver {

	protected Set currentFiles = Collections.synchronizedSet(new HashSet());

	protected FtpConnector connector;
	
    public FtpMessageReceiver(UMOConnector connector,
            				  UMOComponent component,
            				  UMOEndpoint endpoint,
            				  Long frequency) throws InitialisationException {
		super(connector, component, endpoint, frequency);
		this.connector = (FtpConnector) connector;
    }
	
	public void poll() throws Exception {
		FTPFile[] files = listFiles();
		for (int i = 0; i < files.length; i++) {
			final FTPFile file = files[i];
			if (!currentFiles.contains(file.getName())) {
				getWorkManager().scheduleWork(new Work() {
					public void run() {
						try {
							processFile(file);
						} catch (Exception e) {
							connector.handleException(e);
						} finally {
							currentFiles.remove(file.getName());
						}
					}
					public void release() {
					}
				});
			}
		}
	}
	
	protected FTPFile[] listFiles() throws Exception {
		FTPClient client = null;
		UMOEndpointURI uri = endpoint.getEndpointURI();
		try {
			client = (FTPClient) connector.getFtp(uri);
			if (!client.changeWorkingDirectory(uri.getPath())) {
				throw new IOException("Ftp error: " + client.getReplyCode());
			}
			FTPFile[] files = client.listFiles();
			if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
				throw new IOException("Ftp error: " + client.getReplyCode());
			}
			UMOFilter filter = endpoint.getFilter();
			if (files == null || files.length > 0) {
				return files;
			}
			ArrayList v = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {
					if (filter == null || filter.accept(files[i].getName())) {
						v.add(files);
					}
				}
			}
			return (FTPFile[]) v.toArray(new FTPFile[v.size()]);
		} finally {
			connector.releaseFtp(uri, client);
		}
	}

	protected void processFile(FTPFile file) throws Exception {
		FTPClient client = null;
		UMOEndpointURI uri = endpoint.getEndpointURI();
		try {
			client = (FTPClient) connector.getFtp(uri);
			if (!client.changeWorkingDirectory(endpoint.getEndpointURI().getPath())) {
				throw new IOException("Ftp error: " + client.getReplyCode());
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (!client.retrieveFile(file.getName(), baos)) {
				throw new IOException("Ftp error: " + client.getReplyCode());
			}
			UMOMessage message = new MuleMessage(connector.getMessageAdapter(baos.toByteArray()), null);
			message.setProperty(FtpConnector.PROPERTY_FILENAME, file.getName());
			routeMessage(message);
			if (!client.deleteFile(file.getName())) {
				throw new IOException("Ftp error: " + client.getReplyCode());
			}
		} finally {
			connector.releaseFtp(uri, client);
		}
	}

}

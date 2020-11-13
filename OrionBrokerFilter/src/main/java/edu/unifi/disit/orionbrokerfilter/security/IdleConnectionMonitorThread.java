/* <NOME COMPONENTE>.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.orionbrokerfilter.security;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class IdleConnectionMonitorThread extends Thread {
	private final HttpClientConnectionManager connMgr;
	private volatile boolean shutdown;
	private final Integer timeout;

	public IdleConnectionMonitorThread(
			PoolingHttpClientConnectionManager connMgr, Integer timeout) {
		super();
		this.connMgr = connMgr;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		try {
			while (!shutdown) {
				synchronized (this) {
					wait(1000);
					connMgr.closeExpiredConnections();
					connMgr.closeIdleConnections(timeout, TimeUnit.SECONDS);
				}
			}
		} catch (InterruptedException ex) {
			shutdown();
		}
	}

	public void shutdown() {
		shutdown = true;
		synchronized (this) {
			notifyAll();
		}
	}
}
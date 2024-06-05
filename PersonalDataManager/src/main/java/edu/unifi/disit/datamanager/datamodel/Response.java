/* Data Manager (DM).
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
package edu.unifi.disit.datamanager.datamodel;

public class Response {

	Boolean result;
	String message;
	String kind;

	public Response() {
		super();
	}

	public Response(Boolean result, String message) {
		super();
		this.result = result;
		this.message = message;
		this.kind = "READ_ACCESS";
	}

	public Response(Boolean result, String message, String kind) {
		super();
		this.result = result;
		this.message = message;
		this.kind = ("READ_ACCESS".equals(kind) || "READ_WRITE".equals(kind) || 
                        "WRITE_ONLY".equals(kind) || "MODIFY".equals(kind)) ? kind : "READ_ACCESS";
	}

	public Boolean getResult() {
		return result;
	}

	public void setResult(Boolean result) {
		this.result = result;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "Response [result=" + result + ", message=" + message + ", kind=" + kind + "]";
	}
}

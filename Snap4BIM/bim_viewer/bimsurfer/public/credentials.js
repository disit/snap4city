/* Snap4BIM.
   Copyright (C) 2022 DISIT Lab http://www.disit.org - University of Florence

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

export class Credentials {
	constructor(bimServerApi) {
		this.bimServerApi = bimServerApi;

		this.viewer = document.getElementById("viewer");
		
		// main div
		this.div = document.createElement("div");
		this.div.id = "loginFormContainer";
		this.div.classList.add("container", "d-flex", "mt-5", "justify-content-center"); //"credentials",

		let loginForm = document.createElement("div");
		loginForm.id = "loginForm";
		loginForm.classList.add("row", "col-4", "p-2", "justify-content-center");
		this.div.appendChild(loginForm);
		
		// div that contains username
		let formDivUsername = document.createElement("div");
		formDivUsername.classList.add("form-group","row");
		loginForm.appendChild(formDivUsername);

		// div that contains password
		let formDivPassword = document.createElement("div");
		formDivPassword.classList.add("form-group","row");
		loginForm.appendChild(formDivPassword);

		// div used for erros
		this.error = document.createElement("div");
		this.error.classList.add("row", "alert", "alert-danger");
		this.error.style.display = "none";
		loginForm.appendChild(this.error);
		
		// user input fields
		this.usernameInput = document.createElement("input");
		this.usernameInput.type = "text";
		this.usernameInput.classList.add("col-3", "form-control");
		this.passwordInput = document.createElement("input");
		this.passwordInput.type = "password";
		this.passwordInput.classList.add("col-3", "form-control");

		let loginButton = document.createElement("button");
		loginButton.classList.add("btn", "btn-primary", "mb-2")
		loginButton.innerHTML = "Login";

		let usernameLabel = document.createElement("label");
		usernameLabel.classList.add("col-form-label");
		usernameLabel.innerHTML = "Username ";
		formDivUsername.appendChild(usernameLabel);
		usernameLabel.appendChild(this.usernameInput);

		let passwordLabel = document.createElement("label");
		passwordLabel.classList.add("col-form-label")
		passwordLabel.innerHTML = "Password ";
		formDivPassword.appendChild(passwordLabel)
		passwordLabel.appendChild(this.passwordInput);

		loginForm.appendChild(loginButton);
		
		let keypressListener = (event) => {
			if (event.keyCode == 13) {
				this.login();
			}
		};

		this.usernameInput.addEventListener("keypress", keypressListener);
		passwordLabel.addEventListener("keypress", keypressListener);
		
		loginButton.addEventListener("click", () => {
			this.login();
		});
	}
	
	login() {
		this.bimServerApi.login(this.usernameInput.value, this.passwordInput.value, () => {
			this.viewer.style.display = "";
			this.div.remove();
			localStorage.setItem("token", this.bimServerApi.token);
			this.resolve();
		}, (error) => {
			console.error(error);
			this.error.innerHTML = error.message;
			this.error.style.display = "";
			this.usernameInput.focus();
		});
	}
	
	getCredentials() {
		return new Promise((resolve, reject) => {
			let token = localStorage.getItem("token");
			if (token) {
				this.bimServerApi.setToken(token, () => {
					resolve();
				}, () => {
					document.body.appendChild(this.div);
					this.viewer.style.display = "none";
					this.usernameInput.focus();
					this.resolve = resolve;
					localStorage.removeItem("token")
				});
			} else {
				document.body.appendChild(this.div);
				this.usernameInput.focus();
				this.resolve = resolve;
			}
		});
	}
}
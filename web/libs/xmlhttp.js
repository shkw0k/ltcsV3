// xmlhttp.js
// Created by Shui Hung Kwok, Nov 8, 2005 (C)

// Include this file in main HTML code.
// <script type=text/javascript src="xmlhttp.js">
// </script>

// This class wraps XMLHttpRequest and has methods to handle XML.
function AjaxClass() {
	var self = this;
	
	self.timeOut = 30000;

	if (typeof XMLHttpRequest != 'undefined') {
		self.xmlHttp  = new XMLHttpRequest();
	} else {
		try {
			self.xmlHttp  = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (e1) {
			try {
				self.xmlHttp  = new ActiveXObject("Microsoft.XMLHTTP");
			} catch (e2) {
				self.xmlHttp  = false;
			}
		}
	}


	self.setTimeOut = function(tout) {
		self.timeOut = tout;
	};

	// The sendRequest (script, callback) method sends a GET request to the
	// web server and registers the callback function to be invoked when
	// a response is received. The callback function has one parameter:
	// the returned object.
	self.sendRequest = function(script, params, callback) {
		var xmlhttp = self.xmlHttp;
		self.end();

		var d = new Date();
		var rnd = d.getTime();
		params["rnd"] = rnd;
		var query = script + "?" + array2Query(params);
		xmlhttp.onreadystatechange = function() {
			if (xmlhttp.readyState == 4)
				callback(xmlhttp);
		};
		xmlhttp.open("GET", query, true); // true for asyncrhonuous
		xmlhttp.timeout = self.timeOut;
		xmlhttp.setRequestHeader("Content-Type", "text/xml");
		xmlhttp.send();
	}; // sendRequest

	self.postRequest = function(script, params, callback) {
		var xmlhttp = self.xmlHttp;
		self.end();

		var d = new Date();
		var rnd = d.getTime();
		params["rnd"] = rnd;
		var content = array2Query(params);
		xmlhttp.onreadystatechange = function() {
			if (xmlhttp.readyState == 4) {
				callback(xmlhttp);
				//xmlhttp.abort();				
			}
		};
		xmlhttp.open("POST", script, true); // true for asyncrhonuous
		xmlhttp.timeout = self.timeOut;
		xmlhttp.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		xmlhttp.send(content);
	}; // sendRequest

	self.end = function() {
		try {
			self.xmlHttp.abort();
		} catch (e) {
		}
	};
} // AjaxClass

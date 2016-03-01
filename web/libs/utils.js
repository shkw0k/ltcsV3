// Utility functions

// Escape special characters, &, <, >
// return the escaped string
function htmlEntities(str) {
	if (typeof str == "string")
		return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g,
				'&gt;');
	else
		return str;
}

// Returns the element by Id
function E(id) {
	return document.getElementById(id);
} // E

// Returns selected item
function C(id) {
	var elem = E(id);
	var sid = elem.selectedIndex;
	if (sid >= 0) {
		return elem.options[sid].value;
	}
	return "";
}

// Gets absolute position x/y in pixel coordinates of an element.
// Returns array [x, y]
function getAbsPosition(elem) {
	var prent = elem.offsetParent;
	var absLeft = elem.offsetLeft;
	var absTop = elem.offsetTop;
	while (prent) {
		absLeft += prent.offsetLeft;
		absTop += prent.offsetTop;
		prent = prent.offsetParent;
	}
	return {
		x : absLeft,
		y : absTop
	};
}

// Given a time stamp,
// returns a string showing how long ago it was set.
// Output format: 6 sec, 1 hour, ...
function howLongAgo(timeStr) {
	function parse(ts) {
		ts = unescape(ts);
		var t = ts.replace(/ /, ':').replace(/-/g, ':').split(':');
		var t2 = new Date();
		t2.setFullYear(t[0]);
		t2.setMonth(t[1] - 1);
		t2.setDate(t[2]);
		t2.setHours(t[3]);
		t2.setMinutes(t[4]);
		t2.setSeconds(t[5]);
		return t2.getTime();
	}

	function calcDiff() {
		var diff = Math.floor((now - tstamp) / 1000.0);
		if (diff < 0)
			diff = 0;
		if (diff < 61)
			return Math.floor(diff) + " sec";
		diff /= 60;
		if (diff < 61)
			return Math.floor(diff) + " min";
		diff /= 60;
		if (diff < 1.5)
			return "1 hour";
		if (diff < 25)
			return Math.floor(diff) + " hours";
		diff /= 24;
		if (diff < 2)
			return "1 day";
		if (diff < 365)
			return Math.floor(diff) + " days";
		diff = Math.floor(diff / 365.25);
		if (diff < 2)
			return "1 year";
		return diff + " years";
	}

	var now = new Date();
	now = now.getTime();
	var tstamp = parse(timeStr);
	return calcDiff() + " ago";
} // howLongAgo

function formattedTime(time0) {
	// Shows the current time in UT.
	// This shows the page is working.
	function pad0(n) {
		if (n > 9)
			return n;
		return '0' + n;
	}

	var dd = pad0(time0.getUTCDate());
	var mon = pad0(time0.getUTCMonth() + 1);
	var yy = pad0(time0.getUTCFullYear());
	var hh = pad0(time0.getUTCHours());
	var mm = pad0(time0.getUTCMinutes());
	var ss = pad0(time0.getUTCSeconds());
	return yy + "-" + mon + "-" + dd + " UT " + hh + ":" + mm + ":" + ss;
}

// Builds the URL query part, i.e. key/value pairs separated by &
function array2Query(arr) {
	var buf = new Array();
	for (idx in arr) {
		buf.push(idx + "=" + arr[idx]);
	}
	return buf.join("&");
}

// Returns the value for the cookie name
// Or "" if not defined
function getCookie(name) {
	// alert ("gettng " + document.cookie);
	if (document.cookie.length > 0) {
		var idx1 = document.cookie.indexOf(name, +"=");
		var idx2;
		if (idx1 != -1) {
			idx1 += name.length + 1;
			idx2 = document.cookie.indexOf(";", idx1);
			if (idx2 == -1)
				idx2 = document.cookie.length;
			return unescape(document.cookie.substring(idx1, idx2));
		}
	}
	return "";
}

function setCookie(name, value, path) {
	// alert ("saving " + name + "=" + value);
	var is_chrome = navigator.userAgent.toLowerCase().indexOf('chrome') > -1;
	if (!is_chrome && path)
		path = path + "/";
	document.cookie = name + "=" + value + ((path) ? ";path=" + path : "");
}

function trim(s) {
	return s.replace(/^[\s]+|\s+$/g, "");
}

function collapseSpaces(s) {
	return s.replace(/\s+/g, ' ');
}

function sexa2deg(angle) {
	var ang = angle.replace('h', ' ');
	ang = angle.replace('d', ' ');
	ang = ang.replace('m', ' ');
	ang = ang.replace('s', ' ');
	ang = ang.replace(/:/g, ' ') + ' 0 0 0 ';
	ang = ang.replace(/^ /, '');
	strs = ang.split(/\s+/);

	var dd = Number(strs[0]);
	var mm = Number(strs[1]);
	var ss = Number(strs[2]);
	var sign = 1.0;

	if (ang.charAt(0) == '-') {
		dd = -dd;
		sign = -1.0;
	}

	return sign * (dd + mm / 60.0 + ss / 3600.0);
} // sexa2deg

function deg2Sexagecimal(deg, ndigits) {
	function pad0(x) {
		return ((x < 10) ? "0" : "") + x;
	}

	var dd, mm, ss;
	var sign = ' ';
	if (deg < 0) {
		deg = -deg;
		sign = '-';
	}
	dd = Math.floor(deg);
	deg = (deg - dd) * 60.0;
	mm = Math.floor(deg);
	ss = (deg - mm) * 60.0;

	dd = pad0(dd);
	mm = pad0(mm);
	ss = pad0(ss.toFixed(ndigits));
	return sign + dd + ':' + mm + ':' + ss;
} // deg2Sexagecimal

function norm360(x) {
	while (x > 360)
		x -= 360;
	while (x < 0)
		x += 360;
	return x;
}

function norm180(x) {
	x = norm360(x);
	return (x > 180) ? x - 360 : x;
}

function splitArgs() {
	var out = {}
	var s = window.location.search.substr(1).split('&');
	if (!s)
		return out;
	for (e in s) {
		var parts = s[e].split('=', 2);
		out[parts[0]] = parts.length == 1 ? "" : decodeURIComponent(parts[1]
				.replace(/\+/g, " "));
	}
	return out;
} // splitArgs

// star utils

function julianDay(y, m, d) {
	var A, B;
	if (m <= 2) {
		m += 12;
		--y;
	}
	A = Math.floor(y / 100);
	B = 2 - A + Math.floor(A / 4);
	return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d
			+ B - 1524.5;
}

function siderealTime(y, m, d, hh, mm, ss) {
	var daydeg = (hh + mm / 60 + ss / 3600) * 15 * 1.00273790935;
	var T = (julianDay(y, m, d) - 2451545.0) / 36525;
	var st0 = 100.46061837 + T
			* (36000.770053608 + T * (0.0003879333 - T / 38710000));
	st0 += daydeg;
	return norm360(st0);
}

function getSiderealTime() {
	var time0 = new Date();
	var dd = time0.getUTCDate();
	var mon = time0.getUTCMonth() + 1;
	var yy = time0.getUTCFullYear();
	var hh = time0.getUTCHours();
	var mm = time0.getUTCMinutes();
	var ss = time0.getUTCSeconds() + time0.getUTCMilliseconds() / 1000.0;
	return siderealTime(yy, mon, dd, hh, mm, ss);
}

function raDec2AzEl(raDeg, decDeg, latitude, longitude) {
	// west longitudes are negative
	var toRadian = Math.PI / 180;
	var az, el;

	var stime0 = getSiderealTime();
	var ha = norm360(stime0 + longitude - raDeg);

	var latRad = toRadian * latitude;
	var decRad = toRadian * decDeg;
	var haRad = toRadian * ha;

	var sinLatitude = Math.sin(latRad);
	var cosLatitude = Math.cos(latRad);
	var sinDeclination = Math.sin(decRad);
	var cosDeclination = Math.cos(decRad);
	var sinHa = Math.sin(haRad);
	var cosHa = Math.cos(haRad);
	var tanDec = 0;

	if (cosDeclination != 0.0)
		tanDec = sinDeclination / cosDeclination;

	var az = Math.atan2(-sinHa, -(cosHa * sinLatitude - tanDec * cosLatitude))
			/ toRadian;
	az = norm360(az);
	var el = Math.asin(sinLatitude * sinDeclination + cosLatitude
			* cosDeclination * cosHa)
			/ toRadian;
	return [ az, el, stime0 + longitude ];
}

function genFooter() {
	var now = new Date();
	var dateString = now.toTimeString();
	var copyright = "W. M. Keck Observatory (C) " + now.getFullYear();
	return dateString + "<br>" + copyright;
}

function indexOf(ar, key) {
	for (var i in ar) {
		if (ar[i] == key) return i;
	}
	return -1;
}

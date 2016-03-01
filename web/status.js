function LTCSStatus() {
	var self = this;

	function setField(site, fieldName, value, className) {
		var elem = E(fieldName + site, value);
		if (!elem)
			return;
		elem.innerHTML = value;
		elem.className = className;
	}

	function impactedClass(impact) {
		var klass;
		switch (impact) {
		case "NO":
			klass = "greenClass";
			break;
		case "YES":
			klass = "redClass";
			break;
		default:
			klass = "downClass";
		}
		return klass;
	}

	function laserStatusClass(state) {
		var klass;
		switch (state) {
		case "OFF":
			klass = "greenClass";
			break;
		case "ON":
			klass = "redClass";
			break;
		default:
			klass = "downClass";
		}
		return klass;
	}

	function siteStatusClass(status) {
		var klassl
		switch (status) {
		case "UP":
			klass = "upClass";
			break;
		case "DOWN":
			klass = "downClass";
			break;
		default:
			klass = "neutralClass";
		}
		return klass;
	}

	function pntTime(t) {
		var tstr = self.getTimeStr(new Date(t));
		return howLongAgo(tstr);
	}

	self.whoLasing = function() {
		var url = "ltcsWS";
		var now = new Date();

		var day = now.getDate();
		var month = (now.getMonth() + 1);
		var year = now.getFullYear();

		var ajax = new AjaxClass();
		ajax.sendRequest(url, {
			Cmd : "wholasing",
			year : year,
			month : month,
			day : day
		}, self.updateWhoLasing);
	};

	self.updateWhoLasing = function(data) {
		// data contains a list of sites
		var rows = data.content;
		var i, k, s;
		var sites = data.info.slice(1);
		var elem;
		var site;

		// rows contains: data, wday, site, comment, sentby
		// sites is a list of possible sites
		var siteIdx = indexOf(data.fields, "Site");

		for (s in self.sites) {
			self.sites[s].flag = 0;
		}

		for (k in rows) {
			var lasingSite = rows[k][siteIdx];
			self.sites[lasingSite].flag = 1;
			elem = E("lasing" + lasingSite);
			if (elem == null)
				continue;
			elem.innerHTML = " YES <img src='laser.png'> ";
			elem.className = "upClass";
		}

		for (s in self.sites) {
			if (self.sites[s].flag)
				continue;
			var lasingSite = s;
			elem = E("lasing" + lasingSite);
			if (elem == null)
				continue;

			elem.innerHTML = " NO ";
			elem.className = "neutralClass";
		}

		self.lastLasingUpdated = new Date().getTime();
	};

	self.getTimeStr = function(time) {
		// Shows the current time in UT.
		// This shows the page is working.
		function pad0(n) {
			if (n > 9)
				return n;
			return '0' + n;
		}

		var dd = pad0(time.getDate());
		var mon = pad0(time.getMonth() + 1);
		var yy = pad0(time.getFullYear());
		var hh = pad0(time.getHours());
		var mm = pad0(time.getMinutes());
		var ss = pad0(time.getSeconds());

		return yy + "-" + mon + "-" + dd + " " + hh + ":" + mm + ":" + ss;
	};

	self.updateTime = function() {
		if (self.lastStatusUpdated < 0)
			return;

		E("statusTime").innerHTML = self.getTimeStr(new Date(
				self.lastStatusUpdated));
		// howLongAgo(self.getTimeStr(new Date(self.lastStatusUpdated)));

		var site;
		var nowTs = new Date().getTime();
		for (site in self.sites) {
			site = site.replace('.', '_');
			var ts = self.sites[site].tstamp;

			var pntInfo = pntTime(ts * 1000);
			var pntOK = (nowTs / 1000 - ts) < 15;

			setField(site, "pntInfo", pntInfo, pntOK ? "greenClass"
					: "downClass");
		}
	};

	self.updateAll = function() {
		// This is called every 60s to refresh the screen.
		// 1. Checks who is lasing today.
		// 2. For each possible site, update impact and laser flags.
		// 3. Updates and display current time.

		var now = new Date();
		var dtime = now.getTime() - self.lastLasingUpdated;
		var lasingUpdateRate = 3600000; // 1 hour
		var statusUpdateRate = 5000; // 5 sec

		if ((dtime > lasingUpdateRate) || (self.lastLasingUpdated < 0)) {
			self.whoLasing();
		}

		dtime = now.getTime() - self.lastStatusUpdated;
		if ((dtime > statusUpdateRate) || (self.lastStatusUpdated < 0)) {
			self.loadStatus();
		} else {
			self.updateTime();
		}

		if (self.timer != -99)
			clearInterval(self.timer);
		self.timer = setInterval(self.updateAll, 5000);
	};

	self.loadStatus = function() {
		function getOverride(info) {
			return info["OVRLASER_IMPACTED"] + "," + info["OVERLASER_STATE"]
					+ "," + info["OVERFOV"] + "," + info["OVERLOG_DATA"];
		}

		function callback(data) {
			if (data) {
				var site;
				var buf = [];

				for (site in data) {
					var info = data[site];
					site = site.replace('.', '_');
					var impacted = trim(info["LASER_IMPACTED"]);
					var siteStatus = trim(info["LTCSSRV_STATE"]);
					var laserState = trim(info["LASER_STATE"]);
					var override = trim(info["OVERRIDE_FIELDS"]);
					var pntTimeStamp = info["TIMESTAMP"]

					self.sites[site].tstamp = pntTimeStamp;
					setField(site, "siteStatus", siteStatus,
							siteStatusClass(siteStatus));
					setField(site, "impacted", impacted,
							impactedClass(impacted));
					setField(site, "laserState", laserState,
							laserStatusClass(laserState));
					setField(site, "override", override,
							override == "NO" ? "greenClass" : "upClass");
				}
				self.lastStatusUpdated = new Date().getTime();
			}
			self.updateTime();
		}
		var url = "getStatus";
		var ajax = new AjaxClass();
		ajax.sendRequest(url, {}, callback);
	};

	self.loadFirstStatus = function() {
		function callback(data) {
			var site, siteIdx;
			var url;
			var info;
			var href;
			var buf = [];
			var nowTs = new Date().getTime();
			buf
					.push("<table id='statusTable'><tr><th>Site<th>Lasing<br>Today<th>Site<br>Status<th>Laser<br>Impacted<th>Laser<br>State<th>Pnt Info<th>Override");
			for (site in data) {
				info = data[site];
				siteIdx = site.replace('.', '_');
				self.sites[siteIdx] = {
					name : site,
					tstamp : nowTs,
					flag : 0
				};
				url = info["LTCSURL"];
				href = site;
				if (url && (url.length > 0))
					href = "<a href='" + url + "'>" + site + "</a>";

				buf.push("<tr><td>" + href + "<td id='lasing" + siteIdx + "'>"
						+ "<td id='siteStatus" + siteIdx + "'>"
						+ "<td id='impacted" + siteIdx + "'>"
						+ "<td id='laserState" + siteIdx + "'>"
						+ "<td id='pntInfo" + siteIdx + "'>"
						+ "<td id='override" + siteIdx + "'>");
			}
			buf.push("</table>");
			E("statusTableDiv").innerHTML = buf.join("");
			self.lastUpdated = new Date().getTime();
			self.updateAll();
		}
		var url = "getStatus";
		var ajax = new AjaxClass();
		ajax.sendRequest(url, {}, callback);
	};

	self.timer = -99;
	self.sites = {};
	self.lastUpdated = self.lastLasingUpdated = self.lastStatusUpdated = -1;
	InitLTCS();

	self.loadFirstStatus();
	E("refresh").onclick = self.loadStatus;
}
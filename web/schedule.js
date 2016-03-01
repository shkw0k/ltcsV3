function gotoScene (yy, mm, dd) {
	window.location = "scene.html?year=" + yy + "&month=" + mm + "&day=" + dd;
}

function LTCSSchedule ()
{
	var self = this;
	
	self.gotoMonth = function (month, year) {
		if (month > 12) {
			month = 1;
			++year;
		}
		else if (month < 1) {
			month = 12;
			year -= 1;
		}
		window.location = "schedule.html?month="+month+"&year="+year;
	};
	
	self.prevMonth = function () {
		var month = 1 + Number(C("month"));
		var year = C("year");
		self.gotoMonth (month - 1, year);
	};
	
	self.nextMonth = function () {
		var month = 1 + Number(C("month"));
		var year = C("year");
		self.gotoMonth (month + 1, year);
	};	
	
	self.loadSchedule = function () {
		var month = 1 + Number(C("month"));
		var year = C("year");

		var callback = function(data) {
			self.updateSched(data);
		};

		var url = "ltcsWS";
		ajaxCall(url, {Cmd:'schedule', month:month, year:year}, callback);
	};
	
	self.collateByDate = function (data) 
	{
		// Merges rows of same day into one single row.
		var output = Array ();
		var lastDate = "";
		var site = "";
		var instr = "";
		var sep = ", ";
		var wday;
		
		for (i in data) 
		{
			var row = data[i];
			var date1 = row[0].replace(/-0/g, '-');
			var site1 = trim(row[2]);
			var instr1 = trim(row[3]);
			if (lastDate == date1) 
			{
				if (site)
					site += sep + site1;
				else 
					site = site1;
				if (instr)
					instr += sep + instr1;
				else
					instr = instr1;
			}
			else 
			{
				if (lastDate != "") {
					output.push ([lastDate, wday, site, instr]);
				}
				lastDate = date1;
				wday = row[1];
				site = site1;
				instr = instr1;
			}
		}
		if (lastDate != "") 
		{
			output.push ([lastDate, wday, site, instr]);
		}
		return output;
	};	
	
	self.updateSched = function(data)
	{
		function parseDate(dstr) {
			var parts = dstr.split('-');
			return new Date (parts[0], parts[1]-1, parts[2]);
		}
		
		function buildCallStr (tstamp)
		{			
			var t0 = new Date(tstamp);
			var y = t0.getUTCFullYear();
			var m = t0.getUTCMonth() + 1;
			var d = t0.getUTCDate();
			return "onclick='gotoScene(" + y + "," + m + "," + d + ");'";
		}
		
		var schedDiv = E("resultDiv");
		var content = data.content;
		var info = data.info;
		
		var rows = self.collateByDate(content);
		var len = rows.length;
		if (len == 0)
		{
			schedDiv.innerHTML = "No records found";
			return;
		}		
		
		var firstDate = parseDate(info[0]);
		var firstWDay = firstDate.getDay(); // 0-6, 0=Sunday
		var wdNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
		var monthNames = [ 		               	"January", "February", "March", 
		               	"April", "May", "June", 
		               	"July", "August", "September", 
		               	"October", "November", "December"];

		
		var out = [];
		var i, dnr;
		var didx, currDate;
		var dayClass;
		var tstamp;
		var msecInDay = 24 * 3600 * 1000;
		var todayTstamp = new Date().getTime() ;
		var monthLabel = monthNames[firstDate.getMonth()] + " " + firstDate.getFullYear();
		
		out.push ("<div class='monthLabelClass'>" + monthLabel + "</div>");
		
		out.push("<table id='ltcsSchedTable'><tr>");
		
		for (i = 0; i < 7; ++i) {
			out.push("<th>" + wdNames[i]);
		}
		
		out.push("<tr>");
		for (i = 0; i < firstWDay; ++i) {
			dayClass = i == 0 ? "weekendClass" : "workdayClass";	
			out.push("<td class='" + dayClass + "'>&nbsp;");
		}
		
		didx = 0; // index in rows
		tstamp = firstDate.getTime();
		currDate = parseDate(rows[didx][0]);
		for (dnr = 0; dnr < 32; ++dnr) {
			var nrTels = 0;
			var date1 = new Date(tstamp);
			var dayInMonth = date1.getDate();
			var dayStr = "<span class='dayClass'>" + dayInMonth + "</span>";
			var dtstamp = todayTstamp - tstamp; 
			
			dayClass = i == 0 ? "weekendClass" : "workdayClass";
			dayClass = (0 < dtstamp && dtstamp < msecInDay) ? "todayClass" : dayClass;
			if (dayInMonth < dnr) break;
			
			var callStr = "";
			var telescopes = "";
			
			if (tstamp == currDate.getTime()) {
				var val = decodeURI(rows[didx][2]);
				var parts = val.split(',');
				nrTels = parts.length;
				telescopes = parts.join("<br>") + "<br>";
				++didx;
				if (didx < rows.length) 
					currDate = parseDate(rows[didx][0]);
			}

			if (telescopes && tstamp <= todayTstamp)
			{
				callStr = buildCallStr(tstamp);
				out.push("<td class='" + dayClass + "'" + callStr + ">" + dayStr+"<br>");
				out.push("<a href=# " + callStr + ">" + telescopes + "</a>");
			}
			else 
			{
				out.push("<td class='" + dayClass + "'>" + dayStr+"<br>");
				out.push (telescopes);
			}
			
			for (var t = 0; t < 3-nrTels; ++t) {
				out.push("<br>");
			}
			tstamp += 24 * 3600 * 1000;
			++i;
			if (i > 6) {
				i = 0;
				out.push("<tr>");
			}
		}		
		
		out.push ("</table>");
		
		schedDiv.innerHTML = out.join("");
	};
	
	self.genSchedNavigation = function () {
		var params = splitArgs();
		var now;
		if (params['month'] && params['year']) {
			now = new Date (params['year'], params['month']-1, 1);
		}
		else {
			now = new Date();
		}
		var nowMonth = now.getMonth();
		var months = ["Jan", "Feb", "Mar",
        "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", 
        "Nov", "Dec"];
		var buf = [];
		buf.push ("<select id=month>");
		for (var m in months) {
			var sel = (m == nowMonth) ? "selected" : "";
			buf.push("<option value='" + m + "' " + sel + ">" + months[m]);
		}
		buf.push ("</select>");		
		E('monthDiv').innerHTML = buf.join('');
		
		buf = ["<select id='year'>"];
		var nowYear = now.getFullYear();
		var y;
		var endYear = nowYear; 
		if (nowMonth >=6) ++endYear;
		
		for (y = nowYear - 5; y <= endYear; ++y) {
			var sel = (y == nowYear) ? "selected" : "";
			buf.push("<option value='" + y + "' " + sel + ">" + y);
		}
		buf.push ("</select>");
		
		E('yearDiv').innerHTML = buf.join('');
	};
	
		
	InitLTCS();
	E("refresh").onclick = self.loadSchedule;
	E("prevMonth").onclick = self.prevMonth;
	E("nextMonth").onclick = self.nextMonth;
	self.genSchedNavigation();
	self.loadSchedule();
}

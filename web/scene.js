function LTCSScene ()
{
	var self = this;
	var refreshRateMs = 3 * 1000;
	self.timer = -99;
	self.failCnt = 0;

	self.getCurrTime = function() {
		return formattedTime(new Date());
	};

	self.callAgainIn = function(func, msecs) {
		if (self.timer != -99)
			clearTimeout(self.timer);
		self.timer = setTimeout(func, msecs);
	};

	self.checkServer = function(labelId, isOK) {
		if (isOK) {
			self.failCnt = 0;
			return;
		}
		self.failCnt += 1;
		if (self.failCnt < 2)
			return;
		var sl = E(labelId);
		if (!sl)
			sl = E("mainView");
		sl.innerHTML = "Fail to connect to server " + self.getCurrTime();
	};

	self.update3DScene = function() {
		var callback = function(data) {
			try {
				self.scenes.updateTelPositions(data);
				self.checkServer("lastUpdated", 1);
			} catch (e) {
				self.checkServer("lastUpdated", 0);
			}
			self.callAgainIn(self.update3DScene, refreshRateMs);
		};

		var url = "getStatus";
		var ajax;
		if (self.telPosAjax)
			ajax = self.telPosAjax;
		else {
			self.telPosAjax = new AjaxClass();
			ajax = self.telPosAjax;
		}

		ajax.sendRequest (url, {
			tel : 'all'
		}, callback);
	};

	self.loadView3D = function() {
		E("mainView").innerHTML = "<table><tr><td><div id='sceneContainer'></div>"
				+ "<td><div id='telTableDiv'></div></table>";
		self.scenes = new View3DScene("sceneContainer");
		self.update3DScene();
		self.scenes.render();
	};

	self.serverPerformance = function() {
		function callback(resp) {
			var cont = resp.responseText;
			var data;
			try {
				eval("data=" + cont + ";");
				E('rps').innerHTML = data['rps'] + ' req/s';
			} catch (e) {
				;
			}
		}
		function checkPerformance() {
			var ajax = new AjaxClass();
			ajax.sendRequest("getReqPerSec", {}, callback);
		}
		var sp = E("serverPerformance");
		if (!sp)
			return;
		sp.onmouseover = checkPerformance;
		sp.onmouseout = function() {
			E('rps').innerHTML = '';
		};
	};
	
	self.initViewerControl = function () {
		function handleViewer (data) {
			var az = Number(data['az']);
			var el = Number(data['el']);
			var dist = Number(data['dist']);
			self.scenes.setCameraPosition (dist, az, el);
			E('debugMsg').innerHTML = az + " " + el + " " + dist;
		}
		function checkViewer () {
			ajaxCall ("getViewerPosition", {}, handleViewer);
			timer = setTimeout (checkViewer, 1000);
		}
		function viewerControl () {
			oldGain =  self.scenes.cameraAnimGain;
			if (timer)
				clearTimeout(timer);
			if (vctrl.checked) {
				self.scenes.cameraAnimGain = 0.015;
				timer = setTimeout (checkViewer, 1000);
			} else {
				self.scenes.cameraAnimGain = oldGain;
			}
		}
		var oldGain = 0.2;
		var timer = 0;
		var vctrl = E('viewerCtrl');
		vctrl.onclick = viewerControl;
	};

	document.onunload = function () {
		if (self.timer != -99) {
			clearInterval(self.timer);
			self.timer = -99;
		}
		if (self.scenes) {
			self.scenes.stopRender();
			self.scenes = null;
		}
	};

	self.serverPerformance();
	self.initViewerControl();

	InitLTCS();
	self.loadView3D();
}

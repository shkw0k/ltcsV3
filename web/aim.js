function InitAim ()
{
	var self = this;

	self.gotoPageFunc = function (pname) {
		return function () { window.location = pname; }
	};

	function gotResults(data) {
		E("aimToAz").innerHTML = data.az;
		E("aimToEl").innerHTML = data.el;
	}

	self.calculate = function () {
		var targetTel = C("targetTel");
		var pointingTel = C("pointingTel");
		var distance = Number(E("distance").value);

		var ajax = new AjaxClass();
		var url = "findCollision";
		var parms = {pointingTel: pointingTel, targetTel: targetTel, distance:distance};
		ajax.sendRequest (url, parms, gotResults);
	};
	

	E('Schedule').onclick = self.gotoPageFunc("schedule.html");
	E('Status').onclick = self.gotoPageFunc("status.html");
	E('View3D').onclick = self.gotoPageFunc("scene.html");
	E('calcCollision').onclick = self.calculate;

}

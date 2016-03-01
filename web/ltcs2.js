function InitLTCS ()
{
	var self = this;

	self.gotoPageFunc = function (pname) {
		return function () { window.location = pname; }
	};
	

	E('Schedule').onclick = self.gotoPageFunc("schedule.html");
	E('Status').onclick = self.gotoPageFunc("status.html");
	E('View3D').onclick = self.gotoPageFunc("scene.html");
	E('footerDiv').innerHTML = genFooter();

}

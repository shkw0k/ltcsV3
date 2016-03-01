/**
 * Graphics with SVG. A class to create and manipulate SVG elements.
 * 
 * Elements are not created directly, rather a SVG tag is generated and returned
 * as string.
 * 
 * Date: 2014-02-09 Author: Shui Hung Kwok Initial version.
 * 
 */

function Axis() {
	var self = this;
	self.min = 0;
	self.max = 100;
	self.low = 0; // scale low
	self.high = 100; // scale high

	function linearScale(x, xlo, xhi, tolo, tohi) {
		var denom = xhi - xlo;
		if (denom == 0)
			return x;
		var m = (tohi - tolo) / denom;
		var b = -m * xlo + tolo;
		return m * x + b;
	}

	self.setRange = function(mn, mx) {
		self.min = mn;
		self.max = mx;
	};

	self.setLimits = function(lo, hi) {
		self.low = lo;
		self.high = hi;
	};

	self.scale = function(x) {
		return linearScale(x, self.min, self.max, self.low, self.high);
	};
}

function SVG(container, clsName) {
	var self = this;
	self.clsName = clsName;
	self.xAxis = new Axis();
	self.yAxis = new Axis();

	function E(n) {
		return document.getElementById(n);
	}

	function genElem(tag, attrs) {
		var i;
		var buf = [ '<' + tag + ' class="' + clsName + '" ' ];
		for (i in attrs) {
			buf.push(i + '="' + attrs[i] + '" ');
		}
		// buf.push ('></' + tag + '>');
		buf.push(" />");
		return buf.join('');
	}

	function rect(id, x, y, sz) {
		x = x | 0;
		y = y | 0;
		return genElem('rect', {
			'id' : clsName + id,
			'x' : x,
			'y' : y,
			'width' : sz,
			'height' : sz
		});
	}

	function circle(id, cx, cy, dm) {
		var rd = (dm | 1) / 2;
		return genElem('circle', {
			'id' : clsName + id,
			'cx' : cx,
			'cy' : cy,
			'r' : rd
		});
	}

	self.apply = function (data, fn) {
		var i;
		for (i in data) {
			fn(i, data[i]);
		}
	}

	self.move = function (dx, dy) {
		function fn(i, node) {
			var ts = "translate("+dx+","+dy+")";
			node.setAttribute ("transform", ts);
		}
		self.apply(self.nodes, fn);
	}

	self.setData = function(data) {
		// for now, assume each row is (x,y);
		self.data = data;
		return this;
	};

	self.setXLimits = function(lo, hi) {
		self.xAxis.setLimits(lo, hi);
		return this;
	};

	self.setYLimits = function(lo, hi) {
		self.yAxis.setLimits(lo, hi);
		return this;
	};

	self.setXRange = function(xmin, xmax) {
		self.xAxis.setRange(xmin, xmax);
		return this;
	};

	self.setYRange = function(ymin, ymax) {
		self.yAxis.setRange(ymin, ymax);
		return this;
	};

	self.rememberNodes = function(data) {
		function fn(idx, row) {
			nodes.push(E(clsName + idx));
		}
		var nodes = [];
		self.apply(data, fn);
		self.nodes = nodes;
	};

	self.genChart = function(marker, msize) {
		// Create nodes with id= clsName + nr
		var i;
		var buf1 = [];
		var data = self.data;
		if (!data)
			return this;
		buf1.push("<svg class='svg" + clsName + "' >");
		var fx = self.xAxis.scale;
		var fy = self.yAxis.scale;
		for (i in data) {
			var row = data[i];
			buf1.push(eval(marker + '(i, fx(row[0]), fy(row[1]), msize);'));
		}
		buf1.push("</svg>");
		self.container.innerHTML = buf1.join('');
		self.rememberNodes(data);
		return this;
	};

	self.container = E(container);
	return this;
}
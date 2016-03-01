/**
 * BigIsland 3D scene
 */

function TelObject(shape, telView, marker) {
	var self = this;

	self.trackingMode = "SIDEREAL";
	self.shape = shape;
	self.telView = telView;
	self.colMarker = marker; // collision marker
	self.colDist = 0; // collision distance from base

	// Reported ra and dec in degree
	self.raDeg = 0;
	self.decDeg = 0;

	// desired tel az and el
	self.desiredAz = 0;
	self.desiredEl = 0;

	// Calcuated az and el for display
	self.targAz = 0;
	self.targEl = 0;

	// current az and el for animation
	self.currAz = 0;
	self.currEl = -1;
}


// This function defines the 3D scene and the camera.
function View3DScene(container) {
	var self = this;
	var camDefaultAz = -90;
	var camDefaultEl = 60;
	var viewDefaultDist = 20;

	// terrain data is in 10 m resolution.
	// 512 size of projection grid
	// 13798 size of terrain matrix columns (width)
	var gridSize = 512.0;
	var terrainSize = 13798.0;
	var sceneScale = gridSize / terrainSize / 10.0;

	// height of the view target.
	var viewHeight = 4200;	

	self.reqId = 0;

	self.viewDist = viewDefaultDist;
	self.camAz = camDefaultAz;
	self.camEl = camDefaultEl;
	self.viewDestDist = viewDefaultDist;
	self.camDestAz = camDefaultAz;
	self.camDestEl = camDefaultEl;
	self.showLabelsFlag = false;
	self.useTexture = false;
	self.compass = false;
	self.cameraAnimGain = 0.2;
	self.telescopes = {};

	// Sets up the initial the lights, camera position and the rendere size
	self.initScenes = function() {
		var width = 500;
		var height = 500;
		self.width = width;
		self.height = height;

		self.scene = new THREE.Scene();

		var myLight = new THREE.DirectionalLight(0xffffff, 1);
		myLight.position.set(0.0, 0.2, 0.4);
		self.scene.add(myLight);
		var myLight2 = new THREE.DirectionalLight(0xffffff, 1);
		myLight2.position.set(-0.2, -1, 0.5);
		self.myLight = myLight2;

		self.scene.add(myLight2);

		try {
			if (window.WebGLRenderingContext) {
				self.renderer = new THREE.WebGLRenderer({
					antialias : true
				});
				self.useTexture = true;
			} else {
				self.renderer = new THREE.CanvasRenderer({
					antialias : false
				});
			}
		} catch (e) {
			self.renderer = new THREE.CanvasRenderer({
				antialias : false
			});
		}
		self.renderer.setSize(width, height);

		self.camera = new THREE.PerspectiveCamera(10, width / height, 0.1, 2000);
		self.camera.position.z = 100;
		self.camera.position.y = 0;
		self.camera.position.x = -100;
		self.camera.up = new THREE.Vector3(0, 0, 1);
		self.updateCameraPosition(viewDefaultDist, camDefaultAz, camDefaultEl);

		var container = E(containerName);
		self.canvas = self.renderer.domElement;
		container.appendChild(self.canvas);
		self.msg = E("fpsMsg");
		self.lastCnt = 0;
		self.cnt = 0;
		self.lastTime = new Date().getTime();
		self.tick = self.lastTime;
	};

	// Creates an Array of Vector3 for representing the doms
	self.createPtArray = function(arr) {
		// Creates an array for LatheGeometry (geometry by revolution)
		// arr contains X,Z coordinates, Y=0
		var i;
		var points = new Array();
		for (i in arr) {
			var pt = arr[i];
			points.push(new THREE.Vector3(pt[0], 0, pt[1]));
		}
		return points;
	};

	// Creates one dom geometry model
	self.createDom = function() {
		// The dome shape, X,Z coordinates, Y=0
		var pTable = [ [ 0, 0.1 ], [ 0.02, 0.098 ], [ 0.04, 0.09 ],
				[ 0.06, 0.08 ], [ 0.08, 0.06 ], [ 0.1, 0 ], [ 0.08, -0.02 ],
				[ 0.08, -0.2 ], [ 0.0, -0.2 ] ];

		var points = self.createPtArray(pTable);
		var shape;
		if (self.useTexture) {
			shape = new THREE.Mesh(new THREE.LatheGeometry(points, 12),
					new THREE.MeshLambertMaterial({
						ambient : 0xffffff,
						color : 0x55ff55,
						side : THREE.BackSide,
						shading : THREE.SmoothShading
					}));
		} else {
			shape = new THREE.Mesh(new THREE.LatheGeometry(points, 6),
					new THREE.MeshBasicMaterial({
						color : 0x55ff55
					}));
		}
		return shape;
	};

	// Creates the ray representing the the viewing beam of a telescope
	self.createViewRay = function() {
		var viewpts;
		var telView;
		if (self.useTexture) {
			var p1 = [ [ 0, 0 ], [ 0.01, 0 ], [ 1.4, 200 ] ];
			viewpts = self.createPtArray(p1);
			telView = new THREE.Mesh(new THREE.LatheGeometry(viewpts, 12),
					new THREE.MeshLambertMaterial({
						ambient : 0xffffff,
						color : 0xffffff,
						side : THREE.BackSide,
						shading : THREE.SmoothShading,
						transparent : true,
						opacity : 0.7
					}));
		} else {
			var p2 = [ [ 0, 0 ], [ 0.01, 0 ], [ 0.1, 10 ], [ 0, 10 ] ];
			viewpts = self.createPtArray(p2);
			telView = new THREE.Mesh(new THREE.LatheGeometry(viewpts, 6),
					new THREE.MeshBasicMaterial({
						color : 0xffffff
					}));
		}
		return telView;
	};

	// Creates a color maker for showing where is a collision
	self.createColMarker = function() {
		var p1 = [ [ 0, 0.05 ], [ 0.05, 0 ], [ 0, -0.05 ] ];
		var points = self.createPtArray(p1);
		var marker = new THREE.Mesh(new THREE.LatheGeometry(points, 4),
				new THREE.MeshLambertMaterial({
					ambient : 0xffffff,
					color : 0xff0000,
					side : THREE.BackSide,
					shading : THREE.FlatShading
				}));
		return marker;
	};

	// Creates a telescope model consisting of a view beam, a dom and a marker.
	self.addOneScope = function(name, x, y, z) {
		var shape = self.createDom();
		shape.position.set(x, y, z);
		self.scene.add(shape);

		var telView = self.createViewRay();
		telView.rotation.set(0, 0, 0);
		telView.position.set(x, y, z);
		// Don't add telView to scene until there is an update from server.

		var marker = self.createColMarker();
		marker.position.set(x, y, z + 0.1);
		self.scene.add(marker);

		var telObj = new TelObject(shape, telView, marker);
		self.telescopes[name] = telObj;

		var nameLabel = document.createElement('div');
		nameLabel.style.position = 'absolute';
		nameLabel.innerHTML = name;
		nameLabel.className = 'labelClass';
		telObj.nameLabel = nameLabel;
		document.body.appendChild(nameLabel);
	};

	// Creates a plane simulating the ocean
	self.addWater = function() {
		// add elevation data for ocean and texture.
		var geoPlane = new THREE.PlaneGeometry(11200, 10120, 4, 4);
		var verts = geoPlane.vertices;
		var i, l = verts.length;
		for (i = 0; i < l; ++i) {
			verts[i].z = -0.1;
		}
		THREE.GeometryUtils.triangulateQuads(geoPlane);
		// self.geoPlane = geoPlane;
		var waterURL = "scenes/water.png";
		var waterTexture = THREE.ImageUtils.loadTexture(waterURL);
		self.plane = new THREE.Mesh(geoPlane, new THREE.MeshLambertMaterial({
			ambient : 0x00aaff,
			color : 0x8fafff,
			shading : THREE.SmoothShading,
			map : waterTexture
		}));

		self.scene.add(self.plane);
	};	

	// Creates a compass plane (kind of ugly)
	self.addCompass = function() {
		var geoPlane = new THREE.PlaneGeometry(1, 1, 3, 3);
		var verts = geoPlane.vertices;
		var i, l = verts.length;
		for (i = 0; i < l; ++i) {
			verts[i].z = viewHeight * sceneScale;
		}
		THREE.GeometryUtils.triangulateQuads(geoPlane);
		var compassURL = "scenes/compass.png";
		var compassTexture = THREE.ImageUtils.loadTexture(compassURL);
		var compass = new THREE.Mesh(geoPlane, new THREE.MeshLambertMaterial({
			ambient : 0xffffff,
			color : 0xffffff,
			shading : THREE.FlatShading,
			map : compassTexture,
			transparent: true,
			opacity: 0.5
		}));
		
        compass.visible = false;
		self.compass = compass;
		self.scene.add(compass);
	};

	// Creates the geometry for the Big Island as plane geometry with elevation vertices and texture
	self.addBigIsland = function() {
		// Add Big Island elevation data and texture.
		var width = BigIsland.width;
		var height = BigIsland.height;
		var xoff = BigIsland.xoff;
		var yoff = BigIsland.yoff;
		var vertices = BigIsland.vertices;

		var scale = gridSize / width;
		var aspect = height / width;

		var islandPlane = new THREE.PlaneGeometry(gridSize, gridSize * aspect,
				width - 1, height - 1);
		var gverts = islandPlane.vertices;
		var len = width * height;
		var i = 0;
		for (i = 0; i < len; ++i) {
			gverts[i].z = vertices[i];
		}
		THREE.GeometryUtils.triangulateQuads(islandPlane);

		var island;
		if (self.useTexture) { // with graphic acceleration
			var textureURL = "scenes/BItexture.jpg";
			var texture = THREE.ImageUtils.loadTexture(textureURL);
			texture.magFilter = THREE.LinearFilter;
			// texture.minFilter = THREE.NearestMipMapNearestFilter;
			island = new THREE.Mesh(islandPlane, new THREE.MeshLambertMaterial(
					{
						ambient : 0xafafaf,
						color : 0xbfbfbf,
						map : texture,
						shading : THREE.FlatShading
					}));
		} else { // slow, with canvas
			island = new THREE.Mesh(islandPlane, new THREE.MeshNormalMaterial({
				ambient : 0xafafaf,
				color : 0xbfbfbf,
				shading : THREE.FlatShading
			}));
		}

		island.position.set(-scale * (xoff - width / 2.0), scale
				* (yoff - height / 2.0), 0);
		self.scene.add(island);
	};

	// Defines the summit regions
	// Consists of a textured plane geometry and high resolution vertices
	self.addSummit = function() {
		// add summit region with finer elevation data and texture.
		var width = Summit.width;
		var height = Summit.height;
		var vertices = Summit.vertices;

		var nWidth = gridSize * width / terrainSize;
		var aspect = height / width;

		var islandPlane = new THREE.PlaneGeometry(nWidth, nWidth * aspect,
				width - 1, height - 1);

		var gverts = islandPlane.vertices;
		var len = width * height;
		var i = 0;
		for (i = 0; i < len; ++i) {
			gverts[i].z = vertices[i];
		}
		// THREE.GeometryUtils.triangulateQuads(islandPlane);

		var textureURL = "scenes/SummitTexture.jpg";
		var texture = THREE.ImageUtils.loadTexture(textureURL);
		texture.magFilter = THREE.NearestFilter;

		// texture.minFilter = THREE.LinearMipMapLinearFilter;
		var summit = new THREE.Mesh(islandPlane, new THREE.MeshLambertMaterial(
				{
					ambient : 0xffffff,
					color : 0xcfcfcf,
					map : texture,
					shading : THREE.SmoothShading
				}));

		self.scene.add(summit);
	};

	// List of telescopes to be displayed and their coordinates.
	self.TelInfos = {
		// name: [x, y, z ] position of telescope in meters; lat, long in deg
		'CFHT' : [ 561.73, -144.97, 44.480, 19.8252518, -155.46887571666667 ],
		'GEMINI' : [ 543.80, -305.63, 53.820, 19.823801447222223,
				-155.46904675277779 ],
		'IRTF' : [ 234.33, -37.91, 8.470, 19.826218316666665,
				-155.4719987888889 ],
		'KECK1' : [ -50.78, -68.01, 0.010, 19.825946547222223,
				-155.4747185138889 ],
		'KECK2' : [ 0.00, 0.00, 0.000, 19.826560522222223, -155.47423407777777 ],
		'SUBARU' : [ -187.08, -117.04, 3.440, 19.825503958333332,
				-155.47601866388888 ],
		'UH2.2M' : [ 503.26, -395.40, 53.950, 19.822991066666667,
				-155.46943353611113 ],
	// 'UKIRT': [0, 0, 0, 19.822431483333332,-155.47032675]
	};

	// Create all the telescopes
	self.addTelInfos = function() {
		var xoff = -18.000000; // relative to Keck 2 in terrain coords
		var yoff = 34.000000;
		var zoff = 415;
		var xyscale = 0.099333; // meters to terrain coords
		var zscale = 0.100000;

		var name;
		var toModel = gridSize / terrainSize;
		for (name in self.TelInfos) {
			var row = self.TelInfos[name];
			var x = toModel * (row[0] * xyscale + xoff);
			var y = toModel * (row[1] * xyscale + yoff);
			var z = toModel * (row[2] * zscale + zoff);
			self.addOneScope(name, x, y, z);
		}
	};
	
	// Adds all the objects: telescopes, water, island, compass.
	self.addObjects = function() {
		if (self.useTexture) {
			self.addWater();
			self.addBigIsland();
			self.addSummit();
			self.addCompass();
		}

		self.addTelInfos();
	};

	// The render function. 
	// This is called periodically. 
	self.render = function() {
		self.animateTelescopes();
		self.checkPerformance();
		self.renderer.render(self.scene, self.camera);

		if (self.useTexture) {
			// if using webgl, full speed.
			self.reqId = requestAnimationFrame(self.render);
		} else {
			// If using canvas, slow down the rendering.
			setTimeout(function() {
				self.reqId = requestAnimationFrame(self.render);
			}, 100);
		}
	};

	// Stops renderer, ie before leaving the page
	self.stopRender = function() {
		if (self.reqId) {
			window.cancelRequestAnimationFrame(self.reqId);
			self.reqId = 0;
		}
	};

	// Calculates and shows frames per second
	self.checkPerformance = function() {
		self.cnt += 1;
		var now = new Date().getTime();
		if (self.cnt % 120 == 0) {
			var dt = now - self.lastTime;
			var dcnt = self.cnt - self.lastCnt;

			self.lastCnt = self.cnt;
			self.lastTime = now;
			var fps = Math.floor(dcnt / dt * 1000);

			self.msg.innerHTML = fps + " fps";
		}
		self.tick = now;
	};

	// Returns cross product of a and b as Vector3
	function cross(a, b) {
		var x = a.y * b.z - a.z * b.y;
		var y = a.z * b.x - a.x * b.z;
		var z = a.x * b.y - a.y * b.x;
		var res = new THREE.Vector3(x, y, z);
		return res.normalize();
	}

	self.setCameraPosition = function(dist, az, el) {
		// Sets camera position destination.
		// The animation will moves the camera to the destination position.
		self.camDestAz = norm360(az);
		self.camDestEl = el;
		self.viewDestDist = dist;
	};

	// The camera position is given in AZ/EL and distance to view destination.
	// Calculates the necessary vector to move the camera
	self.updateCameraPosition = function(destDist, az, el) {
		// Updates internal camera position
		self.camAz = norm360(az);
		self.camEl = Math.min(90, Math.max(5, el));
		// E("debugMsg").innerHTML = "AZ=" + self.camAz + " EL=" + self.camEl;

		self.viewDist = Math.max(20, Math.min(550, destDist));
		var dist = self.viewDist;
		var toRads = Math.PI / 180.0;
		var azRad = self.camAz * toRads;
		var elRad = self.camEl * toRads;
		var cosAz = Math.cos(azRad);
		var sinAz = Math.sin(azRad);
		var cosEl = Math.cos(elRad);
		var sinEl = Math.sin(elRad);
		// camera vector
		var camx = cosEl * cosAz;
		var camy = cosEl * sinAz;
		var camz = sinEl;
		// vector normal to camera vector
		var upx = -sinEl * cosAz;
		var upy = -sinEl * sinAz;
		var upz = cosEl;

		// height of the view center
		var z = viewHeight * sceneScale; // 420.0 * 512 / 13798;
		// 420 = 4200 meters of Mauna Kea divided by 10 (ten meter resolution)
		// 512 size of projected grid
		// 13798 width of terrain file

		var camv = new THREE.Vector3(-camx, -camy, -camz);
		var upV = new THREE.Vector3(upx, upy, upz);
		var right = cross(camv, upV);

		self.camera.position.set(camx * dist, camy * dist, camz * dist + z);
		self.camera.up = cross(right, camv);
		self.camera.lookAt(new THREE.Vector3(0, 0, z));
		// self.myLight.position.set (camx, camy, camz+0.6);
	};

	// Relative move of the camera position.
	self.moveCameraRel = function(dist, daz, delv) {
		self.setCameraPosition(dist, self.camAz + daz, self.camEl + delv);

		self.renderer.render(self.scene, self.camera);
	};

	
	// Mouse handlers
	self.anchorx = 0;
	self.anchory = 0;
	self.dragging = 0;

	self.mouseDown = function(evt) {
		self.anchorx = evt.clientX;
		self.anchory = evt.clientY;
		self.dragging = 1;
		return false;
	};

	self.mouseUp = function(evt) {
		self.dragging = 0;
		self.anchorx = evt.clientX;
		self.anchory = evt.clientY;
		return false;
	};

	self.mouseMoved = function(evt) {
		var mx = evt.clientX;
		var my = evt.clientY;
		var bt = evt.button;
		if (self.dragging == 0) {
			return;
		}

		var dx = mx - self.anchorx;
		var dy = my - self.anchory;

		evt = evt || window.event;
		var bt; // 0 left, 1 mid, 2 right
		if (evt.buttons) {
			bt = evt.buttons < 2 ? 0 : (evt.buttons == 4 ? 1 : 2);
		} else if (evt.which) {
			bt = evt.which < 2 ? 0 : (evt.which == 2 ? 1 : 2);
		} else {
			bt = evt.button < 2 ? 0 : (evt.button == 4 ? 1 : 2);
		}

		var viewDist = self.viewDist;

		if (bt == 1 || evt.shiftKey) {
			viewDist = (dy > 0 ? 1.3 : 0.8)* viewDist ;
			dx = dy = 0;
		}
		self.anchorx = mx;
		self.anchory = my;
		self.moveCameraRel(viewDist, -dx, dy);
	};

	// Changes the color of the beam if lasing.
	self.setLasing = function(tel, isLasing) {
		var col = 0xeeeeee;
		if (isLasing == "ON-SKY")
			col = 0xffa500;
		else if (isLasing == "ON")
			col = 0xeeee00;
		var tcol = new THREE.Color(col);
		tel.telView.material.color = tcol;
	};

	// Changes the color of the dom if laser_impacted.
	self.setImpacted = function(tel, impact, override, stale) {
		var col = 0x55ff55;
		if (impact == "YES")
			col = 0xffeef44;
		else if (stale && !override)
			col = 0xaaaaaa;
		else if (override)
			col = 0xaaaaff;

		var tcol = new THREE.Color(col);
		tel.shape.material.color = tcol;
	};

	// Set the view beam AZ/EL angles.
	self.slewTelToAzEl = function(tel, az, el) {
		tel.targAz = norm360(az);
		tel.targEl = Math.min(90, Math.max(-1, el));
		if (self.ready)
			return;
		tel.currAz = tel.targAz;
		tel.currEl = tel.targEl;
	};

	self.setTelRaDec = function(tel, raDeg, decDeg) {
		tel.raDeg = raDeg;
		tel.decDeg = decDeg;
	};

	self.setDesiredAzEl = function (tel, az, el) {
		tel.desiredAz = az;
		tel.desiredEl = el;
	};

	// Calcuates the positions for the telescope labels.
	self.showLabels = function(show) {
		var name;
		var vector = new THREE.Vector3();
		var projector = new THREE.Projector();
		var ppos = getAbsPosition(self.canvas);
		var cnvx = ppos.x;
		var cnvy = ppos.y;
		var w2 = self.width / 2;
		var h2 = self.height / 2;
		var x, y;
		for (name in self.telescopes) {
			var telObj = self.telescopes[name];
			var shape = telObj.shape;
			projector.projectVector(vector
					.getPositionFromMatrix(shape.matrixWorld), self.camera);
			x = (vector.x * w2) + w2 + cnvx;
			y = h2 - (vector.y * h2) + cnvy;
			var nl = telObj.nameLabel;
			with (nl.style) {
				top = y + "px";
				left = x + "px";
				if (show)
					display = "block";
				else
					display = "none";
			}
		}
	}

	// Slowly moves the telescope view beams to their destination coordinates.
	self.animateTelescopes = function() {
		function slew2(telObj, az, el) {
			var telView = telObj.telView;
			var colMarker = telObj.colMarker;
			var colDist = telObj.colDist;

			var x = telView.position.x;
			var y = telView.position.y;
			var z = telView.position.z;
			var toRadians = Math.PI / 180;
			var elevRad = toRadians * el;
			var zDistRad = toRadians * (90 - el);
			var azRad = toRadians * (90 - az);

			telView.position.set(0, 0, 0);
			telView.rotation.set(0, zDistRad, azRad, "ZXY");
			telView.position.set(x, y, z);

			telView.visible = el > 5;
			if (telView.visible) {
				if (colDist > 0) {
					colDist *= sceneScale;
					var sinEl = Math.sin(elevRad) * colDist;
					var cosEl = Math.cos(elevRad) * colDist;
					var u = cosEl * Math.cos(azRad) + x;
					var v = cosEl * Math.sin(azRad) + y;
					var w = sinEl + z;
					colMarker.position.set(u, v, w);
				} else {
					colMarker.position.set(0, 0, 0);
				}
			}
			else {
				colMarker.position.set(0,0,0);
			}

			E(name + 'AZ').innerHTML = az.toFixed(3);
			E(name + 'EL').innerHTML = el.toFixed(3);
		}

		function animateCamera() {
			var gain = self.cameraAnimGain;
			var az = self.camAz;
			var el = self.camEl;
			var dist = self.viewDist;

			var destAz = norm360(self.camDestAz);
			var destEl = self.camDestEl;
			var destDist = self.viewDestDist;

			var daz = shortestPath(destAz - az, 10);
			var delv = destEl - el;
			var ddist = destDist - dist;
			var newAz = az + daz * gain;
			var newEl = el + delv * gain;
			var newDist = dist + ddist * gain;

			if (Math.abs(daz) < 0.1 && Math.abs(delv) < 0.1
					&& Math.abs(ddist) < 0.1) {
				if (daz == 0 && delv == 0 && ddist == 0) {
					self.showLabels(self.showLabelsFlag);
					return;
				}
				self.updateCameraPosition(destDist, destAz, destEl);
				self.showLabels(self.showLabelsFlag);
			} else {
				self.updateCameraPosition(newDist, newAz, newEl);
				self.showLabels(false);
			}
		}

		function shortestPath(daz, mx) {
			var res = daz;
			if (daz > 180)
				res = daz - 360;
			else if (daz < -180)
				res = 360 + daz;
			if (res < -mx)
				return -mx;
			else if (res > mx)
				return mx;
			return res
		}

		function animateOne(n) {
			var gain = 0.05;
			var tel = self.telescopes[n];
			var az, el;
			var mx = 10;

			var rrr = (self.tick / 1000) % 2;
			if (rrr < 1) {
				if (tel.trackingMode == "SIDEREAL") {
					var tInfo = self.TelInfos[n];
					var res = raDec2AzEl(tel.raDeg, tel.decDeg, tInfo[3], tInfo[4]);
					self.slewTelToAzEl(tel, res[0], res[1]);
				}
				else if (tel.trackingMode == "AZ_EL") {
					tel.currAz = tel.desiredAz;
					tel.currEl = tel.desiredEl;
				}
			}

			az = tel.currAz;
			el = tel.currEl;

			var daz = shortestPath(tel.targAz - az, mx);
			var delev = tel.targEl - el;

			if (delev < -mx)
				delev = -mx;
			else if (delev > mx)
				delev = mx;

			az = daz * gain + az;
			el = delev * gain + el;

			az = norm180(az);
			el = Math.min(90, Math.max(-1, el));

			tel.currAz = az;
			tel.currEl = el;
			slew2(tel, az, el);
		}

		if (!self.ready)
			return;
		var name;
		for (name in self.telescopes) {
			animateOne(name);
		}

		animateCamera();
	};

	// Updates telescope positions.
	self.updateTelPositions = function(telPos) {
		var telName;
		var idx = 0;
		for (telName in self.telescopes) {
			var tp = telPos[telName];
			idx += 1;
			var tInfo = self.TelInfos[telName];
			if (!tp)
				continue;
			var ovr = tp['OVERRIDE_FIELDS'] == "YES" ? "OVR" : "";
			// collision distance from telescope base in view direction.
			var colList = tp['COLLISIONS'];

			var laser_state = tp[ovr + 'LASER_STATE'];
			var isStale = tp['isStale'];
			var impacted = tp[ovr + 'LASER_IMPACTED'];

			var telObj = self.telescopes[telName];
			var col;
			var cnt = 0;
			for (col in colList) {
				var colDist = colList[col];
				if (colDist > 0) {
					telObj.colDist = colDist;
				} else {
					telObj.colDist = 0;
				}
				cnt += 1;
				break;
			}
			if (cnt == 0) {
				telObj.colDist = 0;
			}

			var trackingMode = tp['TRACKING_MODE'];
			if (trackingMode)
				telObj.trackingMode = trackingMode;
			else
				trackingMode = telObj.trackingMode;	


			if (trackingMode == 'SIDEREAL') {
				var raDeg, decDeg;
				raDeg = norm360(Number(tp['RA']) * 15);
				decDeg = Number(tp['DEC']);
				self.setTelRaDec(telObj, raDeg, decDeg);
			}
			else if (trackingMode == "AZ_EL") {
				var azDeg, elDeg;
				azDeg = norm360(Number(tp['AZ']));
				elDeg = Number(tp['EL']);
				self.setDesiredAzEl(azDeg, elDeg);
			}

			self.setImpacted(telObj, impacted, ovr, isStale);
			self.setLasing(telObj, laser_state);
			if (!self.ready) {
				self.scene.add(telObj.telView);
			}

			var rowClass = idx % 2 == 0 ? "upRowEven" : "upRowOdd";

			if (isStale)
				rowClass = "redCell";

			var more = "";// "RA=" + (raDeg/15).toFixed(3) + " DEC=" +
			// decDeg.toFixed(3);
			E(telName + 'Row').className = rowClass;
			E(telName + 'Info').innerHTML = ovr
					+ " "
					+ ((laser_state == "ON" || laser_state == "ON-SKY") ? laser_state
							: "") + (impacted == 'YES' ? " Impacted " : "")
					+ more;
		}
		self.ready = 1;
		E("lastUpdated").innerHTML = "Last updated: "
				+ formattedTime(new Date());
	};

	// Sets up a table for showing telescope pointing information.
	self.initTable = function() {
		var buf = Array();
		buf.push("<table id='telTable'>");
		buf.push("<tr><th>Telescope<th>AZ [deg]<th>EL [deg]<th>Laser");
		var t;
		var idx = 0;
		for (t in self.TelInfos) {
			var rowClass = idx % 2 == 0 ? "upRowEven" : "upRowOdd";
			buf.push("<tr id='" + t + "Row' class='" + rowClass + "'>");
			buf.push("<td>" + t);
			buf.push("<td align='right' id='" + t + "AZ' >");
			buf.push("<td align='right' id='" + t + "EL' >");
			buf.push("<td id='" + t + "Info' >");
			idx += 1;
		}

		buf.push("</table>");
		var telTable = E("telTableDiv");
		telTable.innerHTML = "<div id='lastUpdated'></div>" + buf.join("");
	};

	// Creates nagivation buttons N,E,W,S and reset
	// Also checkboxes for showing labels and compass
	self.createNavButtons = function(divName) {
		function goAz(az) {
			self.setCameraPosition(self.viewDist, az, self.camEl);
		}
		function north() {
			goAz(270);
		}
		function west() {
			goAz(0);
		}
		function east() {
			goAz(180);
		}
		function south() {
			goAz(90);
		}
		function reset() {
			self.setCameraPosition(viewDefaultDist, camDefaultAz, camDefaultEl);
		}
		function setShowLabels() {
			self.showLabelsFlag = E("showLabels").checked;
		}
		function setShowCompass() {
            self.compass.visible = !self.compass.visible;
		}
		var n;
		var buf = Array();
		var nlist = {
			'W' : west,
			'N' : north,
			'E' : east,
			'S' : south,
			'Reset' : reset
		};
		for (n in nlist) {
			buf.push("<input class='navButtons' type='button' id='" + n
					+ "NavBt' value='" + n + "'>");
		}
		buf.push("<input type='checkbox' id='showLabels'>Labels</input>");
		buf.push("<input type='checkbox' id='showCompass'>Compass</input>");
		E(divName).innerHTML = buf.join("");
		for (n in nlist) {
			E(n + 'NavBt').onclick = nlist[n];
		}
		E("showLabels").onclick = setShowLabels;
		E("showCompass").onclick = setShowCompass;
	};

	E(container).innerHTML = "<div id='topRow'></div><div id='sceneCanvas'></div><div id='fpsMsg'></div>";
	self.createNavButtons("topRow");

	var containerName = "sceneCanvas";
	self.initScenes();
	self.canvas.onmouseover = self.mouseUp;
	self.canvas.onmouseup = self.mouseUp;
	self.canvas.onmouseout = self.mouseUp;
	self.canvas.onmousedown = self.mouseDown;
	self.canvas.onmousemove = self.mouseMoved;
	self.addObjects();
	self.initTable();
	window.onbeforeUnload = self.stopRender();
	return this;
}

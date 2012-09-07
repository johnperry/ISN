window.onload = load;

function load() {
	var sentStudiesTable = document.getElementById("SentStudiesTable");
	if (sentStudiesTable) startIntervalTimer();
}

var ajaxCount = 0;
function updateSentStudiesTable() {
	var sentStudiesTable = document.getElementById("SentStudiesTable");
	if (sentStudiesTable) {
		var req = new AJAX();
		var url = "/" + context;
		var qs = "update=SentStudiesTable" + "&" + req.timeStamp();
		req.GET(url, qs, null);
		ajaxCount++;
		if (req.success()) {
			var xml = req.responseXML();
			var root = xml.firstChild;
			if (root.tagName.toLowerCase() == "table") {
				replaceTable(sentStudiesTable, root);
			}
		}
	}
}

var interval = 4000;
var intervalId = null;

function startIntervalTimer() {
	stopIntervalTimer();
	intervalId = window.setInterval(updateSentStudiesTable, interval);
}

function stopIntervalTimer() {
	if (intervalId) {
		window.clearInterval(intervalId);
		intervalId = null;
	}
}

function replaceTable(oldTable, newTable) {
	var newRows = newTable.getElementsByTagName("tr");
	var oldRows = oldTable.getElementsByTagName("TR");
	var titleRow = oldRows[0];
	var parent = titleRow.parentNode;
	while (titleRow.nextSibling) parent.removeChild(titleRow.nextSibling);
	for (var i=1; i<newRows.length; i++) {
		insertNode(parent, newRows[i]);
	}
}

function insertNode(targetNode, sourceNode) {
	if (sourceNode != null) {
		var type = sourceNode.nodeType;
		if (type == 1) {
			var name = sourceNode.tagName.toUpperCase();
			var node = document.createElement(name);
			targetNode.appendChild(node);

			var attrs = sourceNode.attributes;
			for (var k=0; k<attrs.length; k++) {
				var attrName = attrs[k].name;
				if (attrName == "class") attrName = "className";
				node.setAttribute(attrName, attrs[k].value);
			}

			var child = sourceNode.firstChild;
			while (child != null) {
				insertNode(node, child);
				child = child.nextSibling;
			}
		}
		else if (type == 3) {
			var node = document.createTextNode(sourceNode.nodeValue);
			targetNode.appendChild(node);
		}
	}
}
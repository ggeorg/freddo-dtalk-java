(function() {

	var DTalk2 = {
			
		_ws: null,
		
		_connect: function(url) {
			DTalk2._ws = new WebSocket(url);
		},
		
		connect: function(url, connectCallback, errorCallback) {
			
			if (window.XDTalk2) {
				
			} else if (window.AndroidDTalk2) {
				
			} else {
				
				window.WebSocket || (window.WebSocket = window.MozWebSocket);
				
				if (window.WebSocket) {
					DTalk2._connect(url);
					DTalk2._ws.onopen = function() {
						console.log("Web Socket Opened...");
						// transmit client info / authenticate
					}
					DTalk2._ws.onmessage = function(evt) {
						DTalk2._log(evt.data);
					}
					DTalk2._ws.onclose = function() {
						var msg = "Whoops! Lost connection to " + url;
						
						DTalk2._log(msg);
						
						if (errorCallback) {
							errorCallback(msg);
						}
					}
					DTalk2._send = function(msg) {
						DTalk2._log(">>> _send: " + msg);
						DTalk2._ws.send(msg);
					}
					DTalk2.disconnect = function(disconnectCallback) {
						DTalk2._log(">>> disconnect")
						DTalk2._send(JSON.stringify({
							dtalk: 2,
							action: "disconnect"
						}));
						DTalk2._ws.close();
						if (disconnectCallback) {
							disconnectCallback();
						}
					}
				}
				
			}
		},
		
		_log: function(msg) {
			console.log(msg);
		},
		
		_warn: function(msg) {
			console.warn(msg);
		},
		
		_error: function(msg) {
			console.error(msg);
		}
			
	};
	
	window.DTalk2 = DTalk2;
	
})(window);
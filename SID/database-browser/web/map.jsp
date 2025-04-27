<%@ page import="edu.stanford.sid.database.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<!-- This uses the Google Maps API to provide a map of all avaliable SID monitors. -->

<%!

//DataFileIndex index;

public void jspInit()
{
	//index = DataFileIndex.getInstance(this.getServletContext());
}

%>

<%

DataFileIndex index = DataFileIndex.getInstance(this.getServletContext());


%>

<CENTER>
<A HREF="http://solar-center.stanford.edu/SID">
<IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0></a>
</CENTER>

<HTML xmlns="http://www.w3.org/1999/xhtml">

</head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
  <title>Google Maps JavaScript API Example</title>
  <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAAOcerV9uJpl9FEMrNBj2eHRTuofQd0Bmvbyy2KgjnE62uYUiAlxSKJ8MAyGc3t2DydiWwrUybk8DNjw"
          type="text/javascript"></script>
  <script type="text/javascript">
  //<![CDATA[
  
      var icon = new GIcon();
      icon.image = "http://labs.google.com/ridefinder/images/mm_20_green.png";
      icon.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
      icon.iconSize = new GSize(12, 20);
      icon.shadowSize = new GSize(22, 20);
      icon.iconAnchor = new GPoint(6, 20);
      icon.infoWindowAnchor = new GPoint(5, 1);
  
  function createMarker(point, html) {
    var marker = new GMarker(point);
    GEvent.addListener(marker, "click", function() {
      marker.openInfoWindowHtml(html);
    });
    return marker;
  }
  
  function createSmallMarker(point, html) {
    var marker = new GMarker(point, icon);
    GEvent.addListener(marker, "click", function() {
      marker.openInfoWindowHtml(html);
    });
    return marker;
  }
  
  function load() {
    if (GBrowserIsCompatible()) {
      var map = new GMap2(document.getElementById("map"));
      //map.setCenter(new GLatLng(37.4419, -122.1419), 13);
      

      
      
      map.setCenter(new GLatLng(0, 0), 2);
      
      map.addControl(new GSmallMapControl());
	  map.addControl(new GMapTypeControl());
	  
	  GDownloadUrl("stations.xml", function(data, responseCode)
	  {
        var xml = GXml.parse(data);
        var markers = xml.documentElement.getElementsByTagName("station");
        for (var i = 0; i < markers.length; i++)
        {
          var longitude = 0;
          var latitude = 0;
          var ID = "";
          var attributes = markers[i].childNodes;
          for(var j = 0;j < attributes.length;j++)
          {
          	var node = attributes[j];
            if(node.nodeName == "longitude")
            {
              longitude = parseFloat(node.firstChild.nodeValue);
            }else if(node.nodeName == "latitude")
            {
              latitude = parseFloat(node.firstChild.nodeValue);
            }else if(node.nodeName == "ID")
            {
              ID = node.firstChild.nodeValue;
            }
          }
          
          map.addOverlay(createSmallMarker(new GLatLng(latitude, longitude), ID));
          //alert(latitude + " " + longitude + " " + ID);
        }
      });
	  <%for(MonitorInfo monitor : index.getMonitors(edu.stanford.sid.MonitorComparators.STATION))
	  	{
	  		if(monitor.hasLatitudeLongidute())
	  		{%>map.addOverlay(createMarker(new GLatLng(<%=monitor.getLatitude()%>, <%=monitor.getLongitude()%>), "<%=monitor.getSite()%> <%=monitor.getMonitor()%>"));
                <%
	  		}else
	  		{
	  			%>//Monitor <%=monitor.getMonitor()%> has no latitude or longitude.
                <%
	  		}
	  	}
	  %>
    }
  }
  
  //]]>
  </script>
</head>

<BODY onload="load()" onunload="GUnload()">
<center>
  <div id="map" style="width: 1000px; height: 700px"></div>
</center>
</BODY>
</HTML>

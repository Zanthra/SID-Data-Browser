<HTML>
<head>
<title>SID Data Access</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">

<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-19047793-3']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>
</head>

<body bgcolor="#FFFFFF" text="#000000">
<CENTER>

<A HREF="http://solar-center.stanford.edu/SID"><IMG SRC="http://solar-center.stanford.edu/images/swtopsmall.jpg" border=0/></a><br/>

</CENTER>

<H2>Linking to the browser.</H2><p/>

If you want to link to a specific page on the browser, at the bottom of the page is a link that can be copied into
your own website, while maintaining the monitor selection.<p/>

The parameters to such a link are:<br/>
date - This is the date you want the graph to start on.  If this is not present, the program will calculate the latest data
for the monitors provided.<br/>
monitor - Any number of these parameters may be present.  They can either be full identifiers or just the monitor ID of the
monitor you want the link to show. eg. S-0031-FB-0031 or TinySID-0001$Jaap$DHO<br/>
display - vertical or superimposed .  This determines whether all the monitors are displayed on the same graph or separate graphs.<br/>
time - The ID of the time zone you want the graph label to display.<br/>
size - Width and Height of the graph.  (width)x(height)<br/>
timeRange - The number of minutes in length the graph should display.<br/>
mss - If true, this will disable the display of the monitor sunrise and sunset indicators.<br/>
sss - If true, this will disable the display of the station sunrise and sunset indicators.<br/>
goes - If true, this will disable the display of the GOES flare indicators.<br/>
goesFlareStrength - This is a string identifying the minimum strength of a flare in order to be displayed on the graph.  This
must have an A B C M or X followed by a number with one digit on either side of the decimal point (note that GOES does not record
flares smaller than B1.0 in the database the graphs use).
<p/>
<H2>Linking to the latest plot</H2><p/>
There is a page called "latest.jsp" with many of the same parameters as browse.jsp, but it requires a monitor parameter, and
does not listen to a date parameter.  It provides a PNG plot of the latest data from that monitor.  This can be directly placed
in an IMG tag on a website to show latest data. 

</BODY>
</HTML>
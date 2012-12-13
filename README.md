Google Play Crawler JAVA API
===================

google-play-crawler is simply for searching android applications on GooglePlay, and also downloading them.

It also has checkin ability to generate ANDROID-ID for you. 

While doing checkin with your account, it uses Galaxy S3 properties. So only Galaxy S3 compatible applications will be retrieved.
If you try to download incompatible application for Galaxy S3, you will get an exception like this : 
```java
"The item you were attempting to purchase could not be found."
```

Motivated users can add other device properties from here : http://www.glbenchmark.com/phonedetails.jsp?benchmark=glpro25&D=Samsung+GT-I9300+Galaxy+S+III&testgroup=system


This project is available thanks to this project : https://github.com/egirault/googleplay-api. 

All the protobuf research in Google Play API belongs to them.


Take a look at code, it is not that sophisticated..


TODO
----
Add other device properties to use with checkin.. (Tablet,.. etc.)

Preapare an interface... at least CLI!


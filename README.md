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

## Usage

### General

    java -jar googleplay.jar --help
    usage: googleplay [-h] [-f [CONF]] [-i [ANDROIDID]] [-e [EMAIL]]
                      [-p [PASSWORD]] [-a [HOST]] [-l [PORT]]
                      {download,checkin,list,categories,search,permissions,reviews}
                      ...

    Play with Google Play API :)
    
    optional arguments:
      -h, --help             show this help message and exit
      -f [CONF], --conf [CONF]
                             Configuration file to used for login! If any of
                             androidid, email and password is supplied, it
                             will be ignored!
      -i [ANDROIDID], --androidid [ANDROIDID]
                             ANDROID-ID to be used! You can use "Checkin"
                             mechanism, if you don't have one!
      -e [EMAIL], --email [EMAIL]
                             Email address to be used for login.
      -p [PASSWORD], --password [PASSWORD]
                             Password to be used for login.
      -a [HOST], --host [HOST]
                             Proxy host
      -l [PORT], --port [PORT]
                             Proxy port
    
    subcommands:
      Command to be executed.
    
      {download,checkin,list,categories,search,permissions,reviews}

### Sub-commands

You can get usage of sub-commands like this :

    java -jar googleplay.jar list --help
    usage: googleplay list [-h] [-s SUBCATEGORY] [-o OFFSET] [-n NUMBER]
                      category
    
    Lists sub-categories and applications within them!
    
    positional arguments:
      category               defines category
    
    optional arguments:
      -h, --help             show this help message and exit
      -s SUBCATEGORY, --subcategory SUBCATEGORY
                             defines sub-category
      -o OFFSET, --offset OFFSET
                             offset to define where list begins
      -n NUMBER, --number NUMBER
                             how many app will be listed

### About Login & Proxy Arguments

Login & Proxy arguments (androidid, email, password, host, port) can be defined in a configuration file not to pass them every time in command line. 
If you don't want to use proxy, just comment proxy host and port!

    # Login Information
    email = xxxxxxxxx@gmail.com
    password = xxxxxxxxx
    androidid = xxxxxxxxxxxxxxxx
    
    # Proxy Information
    host=localhost
    port=8888
    
You can use this file like this:

    java -jar googleplay.jar --conf crawler.conf ...

TODO
----
Add other device properties to use with checkin.. (Tablet,.. etc.)


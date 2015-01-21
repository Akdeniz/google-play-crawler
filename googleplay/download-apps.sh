#!/bin/bash
SCRIPTNAME=`basename $0`
SCRIPTDIR=`dirname $0`
CRAWLER_DOWNLOAD="$SCRIPTDIR/googleplay-crawler  download"

if [ "$#" -eq 0 ]; then
    echo "download-apps.sh <app-list> "
    exit -1
fi

FILELIST=$1
if [ ! -f $FILELIST ]; then
    echo "File $FILELIST not existed "
    exit -1
fi
    
APPS=`cat $FILELIST`
for APP in $APPS; do
   $CRAWLER_DOWNLOAD $APP
   sleep 0.5  
done

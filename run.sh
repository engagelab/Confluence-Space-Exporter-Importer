#! /bin/bash
FILE="README"
cat $FILE

echo -n "Enter your Commands:"
read commands
 java -jar -Dcom.ning.http.client.AsyncHttpClientConfig.defaultRequestTimeoutInMS=900000 -Xms512m -Xmx2048m Confluence-Blog-Exporter-Importer-1.0.jar $commands

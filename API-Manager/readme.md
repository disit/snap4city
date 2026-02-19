# Instructions for Setup
On the host where kong is running, open localhost:8002 (kong web ui)

On the menu on the right, go to Plugins

At the end of the webpage, select the (only) custom plugin

Select Advanced Parameters

Replace the given url with the url where the storing python server enforces rules (eg. http://192.168.1.18:50000/checkme, where /checkme exposes the api for enforcing rules)

Remember to set the protocols to at least http and https

For activatting the post-request logging go again into Plugins

Select the Plugin named HTTP Log, under category Logging

Insert, in the http endpoint, the url where the storing python server receives logs (eg. http://192.168.1.18:50000/save, where /save exposes the api for receiving post-request logs)
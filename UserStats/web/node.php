<html>
<script>
function getPage(url) {
 var req = new XMLHttpRequest();
 req.open('GET', url, false);
 req.send(null);

 if(req.status == 200) {
    return req.responseText;
 }
}
node = getPage("https://www.snap4city.org/userstats/suggestNode.php");
if(node != "" && typeof(node) != "undefined") {
 window.location.href = "../drupal/node/" + node;
}
</script>
</html>

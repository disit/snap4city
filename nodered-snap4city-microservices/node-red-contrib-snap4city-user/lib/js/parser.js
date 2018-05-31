/* determine type of value and extract the contained attributes according to the following formats 
- format A (comma separated pairs):
  attribute:value, ...., attribute:value  
- format B (comma separated basic values):
  value, ..., value (in this case the attribute name would be csv_"pos" where pos is the position) 
- format C (JSON values):
  {attribute:value, ..., attribute:value}
- format D (XML values):
  <record><attribute>value</attribute>...<attribute>value</attribute></record>  
*/
function snap4cityParser(sensorID, protocol, value)
{
  if (protocol=="ngsi") 
   { value["source"]="ngsi"; 
     return value;
   } 	 
  var attributes = [];
  var f = ""; // identified format
  console.log("valore processato " + value);
  value=value.trim();
  var first = value.charAt(0);
  switch (first)
  {
    case "{": // Format C
        attributes = parseJSON(value);
		f="JSON";
	    break;
    case "<": // Format D
        attributes = parseXML(value);
		f="XML";
	    break;
    default: // Format A or B
       attributes = parseCSV(value);
	   f="CSV";
  }
  return {"id": sensorID, "source": protocol, attributes};
}

Number.prototype.padLeft = function(base,chr){
   var  len = (String(base || 10).length - String(this).length)+1;
   return len > 0? new Array(len).join(chr || '0')+this : this;
}


function parseCSV(value)
{
  /*
  parse comma separated values 
  each value can assume the following formats:
  format A (comma separated pair): attribute:value  
  format B (comma separated basic value):  value
  */
  var delims = ","; 
  var properties = value.split(delims);
  var attributes = [];
  var format = "A";
  var cv;
  var pos=1;
  for (i = 0; i < properties.length; i++)
  {
    var prop = properties[i].trim();
    if (prop.indexOf(":")==-1 || (prop.indexOf(":")>0 && prop.indexOf(":")!=prop.lastIndexOf(":")))
    { // condition is that ":" do not occur or
      // ":"  occurs once in any prop and not in the first position
        id ="csv_" + pos;
        pos++;
        val =properties[i].trim();
        if (determineType(val)=="date")
          {
            var d = new Date(val);
            val =[ (d.getMonth()+1).padLeft(),
                    d.getDate().padLeft(),
                    d.getFullYear()].join('/')+
                    ' ' +
                  [ d.getHours().padLeft(),
                    d.getMinutes().padLeft(),
                    d.getSeconds().padLeft()].join(':');

          }
          attributes.push({[id]:{"type": determineType(val),"value" : val}});
    }
    else
    {
	  cv= properties[i].split(":");
	  myid=cv[0].trim();
          val=cv[1].trim();
          var attr={};
          if (determineType(val)=="date") 
          {
            var d = new Date(val);
            val =[ (d.getMonth()+1).padLeft(),
                    d.getDate().padLeft(),
                    d.getFullYear()].join('/')+
                    ' ' +
                  [ d.getHours().padLeft(),
                    d.getMinutes().padLeft(),
                    d.getSeconds().padLeft()].join(':');

          }
          attr[myid]= {"type": determineType(val), "value":val};
          attributes.push(attr);
    }
  }
  console.log(attributes);
  return attributes;
}

/*  this function should be completed */
function parseXML(value)
{
  var attributes = [];
  var pos= 1;
  id ="xml_" + pos;
  val =prop.trim();
  attributes.push({id:{ "type": determineType(val), "value":val}});
}

/*  this function should be completed */
function parseJSON(value)
{
  var attributes = [];
  var pos= 1;
  id ="json_" + pos;
  val =prop.trim();
  attributes.push({id:{ "type": determineType(val), "value":val}});
}


function validateFloat(strFloat) {
    if (/^(\-|\+)?([0-9]+(\.[0-9]+)?|Infinity)$/
      .test(strFloat))
      return true;
  return false;
}

function validateInt(strInt) {
    if (/^(\-|\+)?([0-9]+)$/
      .test(strInt))
      return true;
  return false;
}

// YYYY/MM/DD or MM/DD/YYYY
function validateDate(strDate) {
//  var t = /^(?=.+([\/.-])..\1)(?=.{10}$)(?:(\d{4}).|)(\d\d).(\d\d)(?:.(\d{4})|)$/;
//  strDate.replace(t, function($, _, y, m, d, y2) {
//    $ = new Date(y = y || y2, m, d);
//    t = $.getFullYear() != y || $.getMonth() != m || $.getDate() != d;
//  });
//  return !t;
   var mydate= Date.parse(strDate);
   if (isNaN(mydate)) return false;
   else return true; 
}

function determineType(value)
{
  var selectorType= "";
  if (validateInt(value)) {selectorType = "integer";}
  else if (value.match("[A-Za-z][A-Za-z0-9]*")) {selectorType = "string";}
  else if (validateFloat(value)) {selectorType = "float";}
  else if (validateDate(value)) {selectorType = "date";
  }else {selectorType = "object";}
  return selectorType;
}


function testA(){
  return "aaa"
}


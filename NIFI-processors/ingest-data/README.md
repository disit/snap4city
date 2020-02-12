# NIFI-processors / ingestion-ngsi

## Build:

Processors must be compiled using maven.  
To compile a processor:
* `cd` into the processor's top level folder `/ingest-ngsi`.

* Then `mvn clean package -DskipTests`  
* **Note**: `-DskipTests` skip the unit tests execution

The nar archives are placed in the subfolder:
```
nifi-ingest-ngsi-nar/target
```

## Installation:

To install the processor, copy the nar archive in the
`lib` subfolder of your NiFi installation root (Eg: `/srv/nifi/lib` ), then restart the NiFi instance with:

```
/srv/nifi/nifi.sh restart
```
**Note**: alternatively, from `Nifi-1.9` a custom processor can be installed by copying the nar archive in the `extension` subfolder of the Nifi installation root ( Eg: `/srv/nifi/extendsions` ), **WITHOUT RESTARTING THE NIFI INSTANCE**.

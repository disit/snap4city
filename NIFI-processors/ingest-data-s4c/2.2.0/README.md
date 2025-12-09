# NIFI-processors / ingest-data

## Build:

Nifi target version: `2.2.0`

Requirements:

* Java `21+`

* Maven `3.9.6+`  



To compile and package the  processor:

* `cd` into the processor's top level folder `/ingest-data`.

* Then `mvn clean package -DskipTests`  

* **Note**: `-DskipTests` skip the unit tests execution

The nar archives are placed in the subfolder:

```
nifi-ingest-data-nar/target
```

## Installation:

To install the processor, copy the nar archive in the
`nar_extensions` subfolder of your NiFi installation root (Eg: `/srv/nifi/nar_extensions` ).

This won't require a Nifi restart if it's the **first time installing the processor**.



**NOTE**: in the case of a processor **UPDATE** the Nifi instance(s) **ALWAYS NEED TO BE RESTARTED** in order to reload the `.nar` archives.

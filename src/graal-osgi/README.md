# graal-osgi

A wrapper to make [graal-js](https://github.com/graalvm/graaljs) available as Javascript script engine to OSGi/OGEMA

## Build

- download the GraalVM community edition for Linux (irrespectively of your OS): https://github.com/oracle/graal/releases and decompress it
- edit this project's pom.xml: change the property value <graalvm.path> to the absolute path of the graalvm base dir
- execute 'mvn clean install' in the base folder of this project

# Run
Install the built jar in an OSGi container. Maven coordinates: org.smartrplace.tools/graal-osgi/0.0.1-SNAPSHOT. Note that the bundle requires 
the imports `sun.misc` and `com.sun.management`; add them to the framework property `org.osgi.framework.system.packages.extra`.

When used with OGEMA you can set the active script engine to graaljs by executing `ogs:init js` 
(requires the [OGEMA console scripting bundle](https://github.com/ogema/ogema/tree/public/src/tools/ogema-console-scripting)).
To list all available script engines: `ogs:listEngines -f`.

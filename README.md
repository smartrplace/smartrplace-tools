# smartrplace-tools

---
## Overview
This repository contains tools for [OGEMA](http://www.ogema.org). Content:

- schedule-management: a GUI for managing time series (create, copy, import/export data, plot) from various sources (schedules [OGEMA time series resources], log data, CSV files, etc.).
- ogema-timeseries-rest: a generic REST interface for accessing time series data in OGEMA. See the [Wiki page](https://github.com/smartrplace/smartrplace-tools/wiki/OGEMA-Timeseries-REST).

It requires Java 8 or higher.   

## Build
Prerequisites: git, Java and Maven installed, [OGEMA widgets](https://github.com/ogema/ogema-widgets) binaries available. 

1. Clone this repository
2. In a shell, navigate to the src folder and execute `mvn clean install`.


 

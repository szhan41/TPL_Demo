# TPL_Demo
├── demo_geomap // Demo for showing user trajectory 
│   ├── image
│   │   └── emory_logo.png
│   ├── index.html
│   └── js
│       ├── leaflet-animated-marker.js
│       └── leaflet-ant-path.js
├── demo_usr_bpl_fpl_tpl // Demo for showing user TPL/BPL/FPL  
│   ├── index.html
│   ├── js
│   │   ├── echarts.js
│   │   └── echarts.min.js
│   └── theme
│       ├── dark.js
│       ├── infographic.js
│       ├── macarons.js
│       ├── roma.js
│       ├── shine.js
│       └── vintage.js
├── demo_usr_counts_poi_time // Demo for showing the number of users changing during a period time. 
│   ├── index.html
│   ├── js
│   │   ├── echarts.js
│   │   └── echarts.min.js
│   └── theme
│       ├── dark.js
│       ├── infographic.js
│       ├── macarons.js
│       ├── roma.js
│       ├── shine.js
│       └── vintage.js
├── demo_usrs_tpl_histogram // Demo for Users TPL Histogram
│   ├── index.html
│   ├── js
│   │   ├── echarts.js
│   │   └── echarts.min.js
│   └── theme
│       ├── dark.js
│       ├── infographic.js
│       ├── macarons.js
│       ├── roma.js
│       ├── shine.js
│       └── vintage.js
├── lib // Libraries for running TPL Demo Java Program
│   ├── commons-logging-1.2.jar
│   ├── commons-math3-3.6.jar
│   ├── javaoctave-0.6.4.jar
│   ├── javaoctave-0.6.4-javadoc.jar
│   ├── javatuples-1.2.jar
│   ├── log4j-api-2.3.jar
│   ├── log4j-core-2.3.jar
│   ├── opencsv-3.8.jar
│   ├── sqlite-jdbc-3.8.7.jar
│   └── ujmp-complete-0.3.0.jar
├── README.md
└── src
    ├── log4j2.xml
    └── tpl_demo
        ├── MarkovChain.java // a class for representing MarkovChain
        ├── TPL_Demo.java // the main class for execution
        └── TrajectoryAnalyse.java // a class for analysing Trajectory Data

First, run the java program tpl_demo, after the execution, there are outputs in the csv folder.
Then, run each demos, the demo needs to read csvdata files in order to generating the graph.

Related paper(s): 
Yang Cao, Masatoshi Yoshikawa, Yonghui Xiao and Li Xiong, "Quantifying Differential Privacy under Temporal Correlations," The 33rd IEEE International Conference on Data Engineering (ICDE2017), April, 2017.
—————————

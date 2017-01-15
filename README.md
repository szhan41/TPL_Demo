# TPL_Demo
├── demo_geomap // Demo for showing user trajectory
│   ├── image
│   └── js
├── demo_usr_bpl_fpl_tpl // Demo for showing user TPL/BPL/FPL
│   ├── js
│   └── theme
├── demo_usr_counts_poi_time // Demo for showing the number of users changing during a period time
│   ├── js
│   └── theme
├── demo_usrs_tpl_histogram // Demo for Users TPL Histogram
│   ├── js
│   └── theme
├── lib // Libraries for running TPL Demo Java Program
└── src
    └── tpl_demo
        ├── MarkovChain.java // a class for representing MarkovChain
        ├── TPL_Demo.java // the main class for execution
        └── TrajectoryAnalyse.java // a class for analysing Trajectory Data

First, run the java program tpl_demo, after the execution, there are outputs in the csv folder.
Then, run each demos, the demo needs to read csvdata files in order to generating the graph.

Related paper(s): 
Yang Cao, Masatoshi Yoshikawa, Yonghui Xiao and Li Xiong, "Quantifying Differential Privacy under Temporal Correlations," The 33rd IEEE International Conference on Data Engineering (ICDE2017), April, 2017.
—————————

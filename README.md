# Move Method Generator

This tool can be used to generate a dataset of potentionally movable functions which can introduce a Feature Envy smell when moved. Dataset format described below. Dataset can be used for learning and evaluation.

Tool requires any Java project as an input.
Usage:
```
./generate-dataset <path to project> <path to output folder>
```
where \<path to project\> is a system path to project to analyze. Found anti-refactorings will be stored in \<path to output folder\>/\<project name\> folder. Note that these two paths must be **relative**.

If you are getting an error: 
```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
```
run the following command to fix the issue:
```
gradle wrapper
```

If command successfully completes the following output will be printed:
```
BUILD SUCCESSFUL
```
with other information about execution. 

Generated data can be found in \<path to output folder\> splitted in 3 files:
> method.csv - movable methods which can introduce Feature Envy code smell

> classes.csv - table of classes, which are referenced in method.csv

> log - log file of execution

## classes.csv
This table consists of four columns: 
1. class id
2. qualified class name (with packages)
3. path to file, where class is defined (relative to project path)
4. offset from the file start to the start of class definition in the file

## methods.csv
This table consists of six columns:
1. method id
2. qualified method name (with source class and packages)
3. path to file, where method is defined (relative to project path)
4. offset from the file start to the start of method definition in the file
5. class id, where method is defined (source class id)
6. list of class ids (space is delimiter), where this method can be moved

The listed methods can be moved all at once or one by one to create code smells in project for further analysis. Projects which are going to be used for modification should have mature design and proper architecture (ie. should have almost zero code smells).

Then this csv output can be passed to methods-mover with the following command:
```
./methods-mover <path to project> <path to csv files folder>
```
This command will try to move physically given methods and as an output it will generate moved-methods.csv which describes methods the tool actually has moved.

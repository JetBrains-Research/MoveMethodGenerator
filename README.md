# Move Method Generator

This is utility to generate dataset of move method refactorings. It can be used for learning and evaluation purposes.

To generate refactorings you need to have Java project where refactorings will be searched. 
Then to start the utility run the following in console:
```
./generate-dataset <path to project> <path to output folder>
```
where \<path to project\> is a system path to project where you want to find possible refactorings, \<path to output folder\> - path where found refactorings will be stored. Note that these two paths must be **relative**.

If you are getting an error: 
```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
```
then run the following before starting our bash script:
```
gradle wrapper
```

After success you will get successful gradle output:
```
BUILD SUCCESSFUL
```
with other information data. 

Then generated data can be found in \<path to output folder\> and there will three files:
> method.csv - таблица методов
> classes.csv - таблица классов, которые используются в method.csv
> log - log file of execution

## classes.csv
This table consists of four columns: 
1. class id
2. qualified class name (with packages)
3. path to file, where class is defined (relative to output folder path)
4. offset from the file start to the start of class definition in the file

## methods.csv
This table consists of six columns:
1. method id
2. qualified method name (with source class and packages)
3. path to file, where method is defined (relative to output folder path)
4. offset from the file start to the start of method definition in the file
5. class id, where method is defined (source class id)
6. list of class ids (space is delimiter), where this method can be moved

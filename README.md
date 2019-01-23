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

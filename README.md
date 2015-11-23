# mujmail
(c) Pavel Machek, Petr Spatka, Martin Stefan, Nguyen Son Tung, Nodir Yuldashev, David Hauzar, David T. Nguyen, Martin Šuška, Pavel Jancik, April 2009

Website: http://mujmail.org/

NOTE: In this revision MessageHeader format have been modified (added message flag indicator).
      Please CLEAR MESSAGE DATABASES before using this revision (choose 'sync w\ Servers' for
      synchronization with servers).

How to create the project (MIDlet) in NETBEANS?



A. Download NETBEANS with mobility pack

B. Create new project 

    1. File → New project → Mobility → MIDP application → Next 

    2. Tick off Create Hello MIDlet → Next 

    3. Device configuration: cldc 1.1, Device Profile: midp 1.0 → Finish

C. Copy all source codes to the directory src of the new project.

D. Set up the project

    1. Right click to the project → Properties

    2. Platform: Optional Packages

    tick off all except Mobile Media API and File Connection and PIM Optional Packages

    3. Application Descriptor → Attributes: General Attributes for JAD and Jar Manifest:

    midlet-name: mujMail
    midlet-Vendor: Students of www.mff.cuni.cz
    version: 2.xx

    4. Application Descriptor → MIDlets → Add:

    midlet-name: MujMail
    midlet class: MujMail
    midlet icon: logo.png

    5. Build → Compiling:

    tick Compile with Optimalization
    Encoding :Cp1250

    6. Build → Libraries and Resources:

    Add Jar/Zip → Add the library(ies) from ./src/lib 

    7. Build → Obfuscating:

    Obfuscation Level: High

    8. Build → Creating JAR:

    tick Compress Jar

    9. Press OK

E. Build → Build main project





How to set up the language of the project (MIDlet) in NETBEANS?

Untill now, there are 7 languages available: English, German, Italian, Polish, Czech, Hungarian, (Brazilian)Portuguese. Find the appropriate language class in mujmail.org/lang.zip and replace it with the Lang.java file in the source directory where you have the mujMail source code. Do not forget to rename the class (for instace, from LangEN to Lang).

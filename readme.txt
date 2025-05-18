Shimeji-ee: Shimeji English Enhanced

Shimeji-ee is a desktop mascot for Windows, macOS, and Linux that freely wanders and plays around the screen. The mascot
is very configurable; its actions are defined through XML and its animations/images can be (painstakingly) customized.
Shimeji was originally created by Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/). This branch of the
original Shimeji project not only translates the program/source to English, but adds additional enhancements to Shimeji
by Kilkakon and other members of the community.

==== Contents ====

1. Links
2. Requirements
3. How to Start
4. Basic Configuration
5. Advanced Configuration
6. How to Quit
7. How to Uninstall
8. Source
9. Trouble Shooting

==== Links ====

* Kilkakon's Shimeji homepage: https://kilkakon.com/shimeji/
* Shimeji-ee homepage: https://code.google.com/archive/p/shimeji-ee/
* Shimeji homepage (archive): https://web.archive.org/web/20160901003054/http://www.group-finity.com/Shimeji/
* Shimeji mirror download: https://www.vector.co.jp/soft/winnt/amuse/se476479.html

==== Requirements ====

* Windows Vista or newer / macOS / Linux
* Java 11 or newer

==== How to Start ====

1. Open the Shimeji-ee JAR file (Shimeji-ee.jar). On Windows, opening Shimeji-ee.exe will also work.
2. Right-click the tray icon for general options.
3. Right-click a Shimeji for options relating to it.

For a tutorial on how to get Shimeji running, watch this video: https://www.youtube.com/watch?v=S7fPCGh5xxo

You can also watch the FAQ if you encounter problems: https://www.youtube.com/watch?v=A1y9C1Vbn6Q

You can also join Kilkakon's Discord server: https://discord.gg/dcJGAn3

==== Basic Configuration ====

If you want multiple Shimeji types, you must have multiple image sets. Basically, you put different folders with the
correct Shimeji images under the img directory.

For example, if you want to add, say, a new Batman Shimeji:

1. Create an img/Batman folder.
2. You must have an image set that mimics the contents of img/Shimeji. Create and put new versions of shime1.png -
   shime46.png (with Batman images, of course) in the img/Batman folder. The filenames must be the same as the
   img/Shimeji files. Refer to img/Shimeji for the proper character positions.
3. Start Shimeji-ee. Now Shimeji and Batman will drop. Right-click Batman to perform Batman specific options. Pressing
   "Call Shimeji" in the tray icon will randomly create and add either Shimeji or Batman.

When Shimeji-ee starts, one Shimeji for every image set in the img folder will be created. If you have too many image
sets, a lot of your computer's memory will be used... so be careful.

Shimeji-ee will ignore all the image sets that are in the img/unused folder, so you can hide image sets in there. There
is also a tool, Image Set Chooser, that will let you select image sets at run time. It remembers previous options via
the conf/settings.properties file. Don't choose too many at once.

For more information, read through the configuration files in conf/. Most options are somewhat complicated, but it's
not too hard to limit the total number of Shimeji or to turn off certain behaviors (hint: set frequency to 0).

==== Advanced Configuration ====

All configuration files are located in the conf folders. In general, none of these should need to be touched.

The logging.properties file defines how logging errors is done.

The actions.xml file specifies the different actions Shimeji can do. When listing images, only include the file name.
More detail on this file will hopefully be added later.

The behaviors.xml file specifies when Shimeji performs each action. More detail on this file will hopefully be added
later.

The settings.properties file details which Shimeji are active as well as the windows with which they can interact. These
settings can be changed using the program itself.

Each type of Shimeji is configured through:

1. An image set. This is located in img/[NAME]. The image set must contain all image files specified in the actions
   file.
2. An actions file. Unless img/[NAME]/conf/actions.xml or conf/[NAME]/actions.xml exists, conf/actions.xml will be used.
3. A behaviors file. Unless img/[NAME]/conf/behaviors.xml or conf/[NAME]/behaviors.xml exists, conf/behaviors.xml will
   be used.

When Shimeji-ee starts, one Shimeji for every image set in the img folder will be created. If you have too many image
sets, a lot of your computer's memory will be used... so be careful.

Shimeji-ee will ignore all the image sets that are in the img/unused folder, so you can hide image sets in there. There
is also a tool, Image Set Chooser, that will let you select image sets at run time. It remembers previous options via
the conf/settings.properties file. Don't choose too many at once.

The Image Set Chooser looks for the shime1.png image. If it's not found, no image set preview will be shown. Even if
you're not using an image named shime1.png in your image set, you should include one for the Image Set Chooser's sake.

Editing an existing configuration is fairly straightforward, but writing a brand-new configuration file is very
time-consuming and requires a lot of trial and error. Hopefully someone will write a guide for it someday, but until
then, you'll have to look at the existing conf files to figure it out. Basically, for every behavior, there must be a
corresponding action. Actions and behaviors can be a sequence of other actions or behaviors.

The following actions must be present for the actions.xml to be valid:

* ChaseMouse
* Fall
* Dragged
* Thrown

The following behaviors must be present for the behaviors.xml to be valid:

* ChaseMouse
* Fall
* Dragged
* Thrown

The icon used for the system tray is img/icon.png.

==== How to Quit ====

Right-click the tray icon of Shimeji-ee, and select "Dismiss All".

==== How to Uninstall ====

Delete the unzipped folder.

==== Source ====

Programmers may feel free to use the source. The Shimeji-ee source is under the New BSD license.

Shimeji by Yuki Yamada is licensed under the zlib/libpng license.

==== Trouble Shooting ====

For a tutorial on how to get Shimeji running, watch this video: https://www.youtube.com/watch?v=S7fPCGh5xxo

You can also watch the FAQ if you encounter problems: https://www.youtube.com/watch?v=A1y9C1Vbn6Q

You can also join Kilkakon's Discord server: https://discord.gg/dcJGAn3

Shimeji-ee takes a LOT of time to start if you have a lot of image sets, so give it some time. Try moving all but one
image set from the img folder to the img/unused folder to see if you have a memory problem. If Shimeji-ee is running out
of memory, try editing Shimeji-ee.bat and changing -Xmx1000m to a larger number.

If the Shimeji-ee icon appears, but no Shimeji appear:

1. Make sure you have the newest version of Shimeji-ee.
2. Make sure you only have image set folders in your img directory.
3. Make sure you have Java 11 or newer on your system.
4. If you're somewhat computer savvy, you can try running Shimeji-ee from the command line. Navigate to the Shimeji-ee
   directory and run this command: "C:\Program Files\Java\jdk-11\bin\java" -jar Shimeji-ee.jar
5. Try checking the log (ShimejieeLogX.log) for errors. If you find a bug (which is very likely), report it on
   Kilkakon's Discord server.

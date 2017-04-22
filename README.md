# Yugioh Database Downloader
Downloads information about each TCG Yugioh card from the Yugioh wiki and packages it into a Json file.

## Usage

Using the `run` batch file will generate a complete list of cards with all details.

* Use `-basic` or `-b` to limit the list to just the page id, card title and wiki url extension (much faster).
* Use `-min` or `-m` to generate a minified json file (smaller file size).
* Use `-o <filename>` to specify the output file.
* Use `-img` to download card images (around 4.5gb as of April 2017).

## Card Details

The following details are available for each card:

* Title
* Wiki URL Extension
* Image URL
* Description/Effect
* Archetype(s)
* Action(s)
* Type(s)
* Level
* ATK
* DEF
* Number
* Fusion Materials
* Effect Types
* Pendulum Effect Types
* Pendulum Effect
* Tips
* Status
* Sets (English/NA)
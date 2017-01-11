# Yugioh Database Downloader
Downloads information about each TCG Yugioh card from the Yugioh wiki and packages it into a Json file.

## Usage

Using the `run` batch file will generate a complete list of cards with all details.
The option `-min` will limit the list to just the page id, card title and wiki url extension (Much faster).
`-o <filename>` can be used to specify the output file.

## Card Details

The following details are available for each card:

* Title
* Wiki URL Extension
* Image URL
* Description/Effect
* Archetype
* Action
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
* Sets (English)
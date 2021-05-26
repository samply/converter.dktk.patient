# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [2.1.0 - 2020-11-09]
### Changed
- Apache POI 5.0.0 (SXSSFWorkbook)

## [2.0.0 - 2020-11-09]
- Github release
### Changed
- Samply parent 11.1.0
## Added
- Github Actions
- Google Code Style

## [1.1.0 - 2018-05-29]
-Updated parent pom to 10.1
### Changed
- Upgrade apache poi to 3.17
## [1.0.7 - 2017-10-26]
### Changed
- Upgrade apache poi to 3.17

## [1.0.6 - 2017-09-26]
### Added
- Method to find the first value for a given mdr key in a container tree

## [1.0.5 - 2017-08-31]
### Added
- Add autofilter option to all sheets. This is basically the excel "format as table" functionality without fancy colors
- An mdr id blacklist can be handed to the Converter to keep them out of the excel export

### Changed
- Sort all sheets by MDS-ID. Put columns without MDS-IDs at the end
- When validating values, always set null or empty as true, since fields my simply not be available

### Fixed
- Cell height on info sheet was too small.
- When checking for DKTK-IDs in slots, trim the slot keys first. (There is at least one dataelement with surplus whitespace)

## [1.0.4 - 2017-08-25]
### Fixed
- Swap Cell Styles when sorting. Otherwise the background colors for validation errors are messed up

## [1.0.3 - 2017-08-25]
### Added
- Allow to define how invalid attributes shall be treated when exporting. Do not validate, remove invalid
or keep invalid (and mark as such)

### Changed
- By default, validate all entries against the MDR and remove invalid attributes

### Fixed
- The hyperlinks in the header row are now correctly switched with their anchors when sorting the columns

## [1.0.2 - 2017-08-11]
### Fixed
- When sorting by dktk mds id, temporarily add a high number for entries without this number


## [1.0.1 - 2017-07-25]
### Fixed
- Check if sheet is available before trying to sort/filter on it


## [1.0.0 - 2017-07-05]
### Added
- Initial release

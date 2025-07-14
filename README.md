# ext-mp-scenery

A visualizer extension for musicplayer to show gently scrolling background scenery with tour guides who offer
customizable commentary on the current track, current artist, and/or current scenery.

General idea:

1. Have a very wide landscape image that we can slowly scroll back and forth.
2. Have configurable "companions" (animated characters) that can pop up at configurable intervals to display information.
3. Use text animation to display messages from these companions.
4. Companion text messages could be purely informational ("current track is XYZ by the artist ABC" or whatever), OR could be configurable based on the scenery image.

TODO: Screenshots! Lots of screenshots!

TODO: example json is provided in jar resources, but it should have an overview here

User config:

- specify a directory where custom scenery images and their json metadata live
- specify a directory where companions live
- on activate, preload all companions (these should be relatively small and not memory intensive)
- we can't preload all scenery images as there are potentially a lot of them
- so, store all companions globally, but load scenery images on demand (or based on some criteria?)
  - Note: there's currently no list-select form field in swing-extras (issue 60 just added for this)
    - https://github.com/scorbo2/swing-extras/issues/60
  - so until that's addressed in swing-extras, the user might be limited to choosing a single scenery tag
  - or I guess I could write a custom component here using PanelField...
- companions can specify their own font style and size properties, with fallback defaults
  - there should be an option to prevent companions from overriding the defaults and force their use

## Requirements

MusicPlayer 2.8 or higher.

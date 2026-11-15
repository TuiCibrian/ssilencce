#!/system/bin/sh
content insert --uri content://media/external/file \
  --bind _data:s:/sdcard/Music/QA/Formats/qa.opus \
  --bind _display_name:s:qa.opus \
  --bind mime_type:s:audio/ogg \
  --bind media_type:i:2 \
  --bind title:s:QA_OPUS \
  --bind "artist:s:QA Formats" \
  --bind "album:s:QA Format Album" \
  --bind duration:i:20000
content insert --uri content://media/external/file \
  --bind _data:s:/sdcard/Music/QA/Formats/qa.wma \
  --bind _display_name:s:qa.wma \
  --bind mime_type:s:audio/x-ms-wma \
  --bind media_type:i:2 \
  --bind title:s:QA_WMA \
  --bind "artist:s:QA Formats" \
  --bind "album:s:QA Format Album" \
  --bind duration:i:20000
echo insert-finished

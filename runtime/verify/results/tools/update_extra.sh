#!/system/bin/sh
content update --uri content://media/external/audio/media \
  --bind album_id:i:61 \
  --bind artist_id:i:57 \
  --bind "album:s:QA Format Album" \
  --bind "artist:s:QA Formats" \
  --where "_id=1755"
content insert --uri content://media/external/file \
  --bind _data:s:/sdcard/Music/QA/Formats/qa.wma \
  --bind _display_name:s:qa.wma \
  --bind mime_type:s:audio/x-ms-wma \
  --bind media_type:i:2 \
  --bind title:s:QA_WMA \
  --bind album_id:i:61 \
  --bind artist_id:i:57 \
  --bind "artist:s:QA Formats" \
  --bind "album:s:QA Format Album" \
  --bind duration:i:20000
echo update-finished

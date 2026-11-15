#!/system/bin/sh
content update --uri content://media/external/file \
  --bind media_type:i:2 \
  --bind album_id:i:71 \
  --bind artist_id:i:57 \
  --bind title:s:QA_WMA \
  --bind "artist:s:QA Formats" \
  --bind "album:s:QA Format Album" \
  --where "_id=1756"
echo fix-finished

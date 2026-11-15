#!/system/bin/sh
content update --uri content://media/external/audio/media --bind is_music:i:1 --where "_id=1755"
content update --uri content://media/external/audio/media --bind is_music:i:1 --where "_id=1756"
echo flags-finished

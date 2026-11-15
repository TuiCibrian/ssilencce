#!/system/bin/sh
find /sdcard/Music/QA -type f | while IFS= read -r f; do
  u=$(printf '%s' "$f" | sed 's/ /%20/g')
  am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$u" >/dev/null
done
echo scan-finished

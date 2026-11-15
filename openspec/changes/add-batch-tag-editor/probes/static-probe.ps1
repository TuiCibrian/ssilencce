#Requires -Version 5.1
<#
.SYNOPSIS
    Static source probe for the AA-07 batch tag editor change.
.DESCRIPTION
    Verifies the source contract described in openspec/changes/add-batch-tag-editor.
    This is a static probe only; it does not compile, build, or run the app.
#>
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")).Path
Set-Location $root

$script:pass = 0
$script:fail = 0
$script:failures = @()

function Check {
    param([string]$Name, [bool]$Condition)
    if ($Condition) {
        $script:pass++
    } else {
        $script:fail++
        $script:failures += $Name
    }
}

function Test-Contains {
    param([string]$Path, [string]$Text)
    if (-not (Test-Path $Path)) { return $false }
    return [bool](Select-String -Path $Path -Pattern $Text -SimpleMatch -Quiet)
}

function Test-Regex {
    param([string]$Path, [string]$Pattern)
    if (-not (Test-Path $Path)) { return $false }
    return [bool](Select-String -Path $Path -Pattern $Pattern -Quiet)
}

function Test-NotContains {
    param([string]$Path, [string]$Text)
    if (-not (Test-Path $Path)) { return $false }
    return -not [bool](Select-String -Path $Path -Pattern $Text -SimpleMatch -Quiet)
}

function Test-NotRegex {
    param([string]$Path, [string]$Pattern)
    if (-not (Test-Path $Path)) { return $false }
    return -not [bool](Select-String -Path $Path -Pattern $Pattern -Quiet)
}

function Count-Regex {
    param([string]$Path, [string]$Pattern)
    if (-not (Test-Path $Path)) { return 0 }
    $matches = Select-String -Path $Path -Pattern $Pattern -AllMatches
    if ($null -eq $matches) { return 0 }
    return ($matches | ForEach-Object { $_.Matches.Count } | Measure-Object -Sum).Sum
}

function Get-Block {
    param([string]$Path, [string]$Start, [string]$End)
    if (-not (Test-Path $Path)) { return "" }
    $lines = Get-Content -Path $Path
    $startMatch = $lines | Select-String -Pattern $Start | Select-Object -First 1
    if ($null -eq $startMatch) { return "" }
    $startIndex = $startMatch.LineNumber
    $endMatch = $lines | Select-String -Pattern $End | Where-Object { $_.LineNumber -gt $startIndex } | Select-Object -First 1
    if ($null -eq $endMatch) { return "" }
    return ($lines[($startIndex - 1)..($endMatch.LineNumber - 2)] -join "`n")
}

$field = "app/src/main/java/com/ssilencce/core/model/tag/TagEditField.kt"
$plan = "app/src/main/java/com/ssilencce/core/model/tag/TagEditPlan.kt"
$report = "app/src/main/java/com/ssilencce/core/model/tag/TagEditReport.kt"
$verifier = "app/src/main/java/com/ssilencce/data/local/TagWriteVerifier.kt"
$writer = "app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt"
$vm = "app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorViewModel.kt"
$screen = "app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorScreen.kt"
$activity = "app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorActivity.kt"
$menu = "app/src/main/java/com/ssilencce/ui/component/menu/MenuItemClickExt.kt"
$menuRes = "app/src/main/res/menu/menu_media_selection.xml"
$manifest = "app/src/main/AndroidManifest.xml"
$mainModule = "app/src/main/java/com/ssilencce/MainModule.kt"
$strings = "app/src/main/res/values/strings.xml"

# F: field model and plan
Check "F1 TagEditField exists" (Test-Path $field)
Check "F1 exactly 17 enum entries" ((Count-Regex $field '^\s{4}[A-Z_]+\(') -eq 17)
Check "F2 CONDUCTOR uses MetadataReader.PRODUCER" (Test-Contains $field "CONDUCTOR(MetadataReader.PRODUCER")
Check "F2 PUBLISHER uses MetadataReader.COPYRIGHT" (Test-Contains $field "PUBLISHER(MetadataReader.COPYRIGHT")
Check "F2 YEAR uses MetadataReader.YEAR" (Test-Contains $field "YEAR(MetadataReader.YEAR")
Check "F2 track total uses MetadataReader.TRACK_TOTAL" (Test-Contains $field "TRACK_TOTAL(MetadataReader.TRACK_TOTAL")
Check "F2 disc total uses MetadataReader.DISC_TOTAL" (Test-Contains $field "DISC_TOTAL(MetadataReader.DISC_TOTAL")
Check "F2 no cover field" (Test-NotRegex $field '^\s{4}(COVER|ARTWORK)\(')
Check "F3 label resources present" (Test-Contains $field "R.string.title" -and (Test-Contains $field "R.string.comment"))
Check "F4 FieldOperation Keep/Set/Clear" ((Test-Contains $plan "data object Keep") -and (Test-Contains $plan "data class Set") -and (Test-Contains $plan "data object Clear"))
Check "F5 toPropertyOverrides exists" (Test-Contains $plan "fun toPropertyOverrides")
Check "F5 Keep omitted from overrides" (Test-Contains $plan "FieldOperation.Keep ->")
Check "F5 Set uses trimmed value" (Test-Contains $plan "operation.value.trim()")
Check "F5 Clear maps to null" (Test-Contains $plan "put(field.propertyKey, null)")
Check "F6 blank Set invalid" ((Test-Contains $plan "isValid") -and (Test-Contains $plan "FieldOperation.Set && it.value.isBlank()"))

# W: writer and verifier
Check "W1 writePropertyMap accepts overrides" (Test-Contains $writer "overrides: Map<String, String?>")
Check "W2 writeBatch exists" (Test-Contains $writer "suspend fun writeBatch")
Check "W2 writeBatch calls writePropertyMap(fd, overrides)" (Test-Contains $writer "writePropertyMap(fd, overrides)")
Check "W2 batch loops contents" (Test-Contains $writer "contents.forEachIndexed")
$batchBlock = Get-Block $writer "suspend fun writeBatch" "private fun createPicture"
Check "W3 batch block exists" ($batchBlock.Length -gt 0)
Check "W3 no picture write in batch block" (-not ($batchBlock -match "picture\(|pictureDeleted\(|createPicture|createPictureThumbFile"))
Check "W4 legacy write remains" (Test-Contains $writer "suspend fun write(")
Check "W4 legacy write uses properties map" (Test-Contains $writer "writePropertyMap(fd, properties)")
Check "W5 current map merge retained" (Test-Contains $writer "currentProperties.forEach")
Check "W5 ReplayGain canonicalization retained" (Test-Contains $writer "ReplayGainTags.canonicalProperties")
Check "W5 TagLib savePropertyMap retained" (Test-Contains $writer "TagLib.savePropertyMap")
Check "W6 read-back metadata read" (Test-Contains $writer "fun readPropertyMap" -and (Test-Contains $writer "TagLib.getMetadata"))
Check "W6 verifier called" (Test-Contains $writer "TagWriteVerifier.verify")
Check "W7 verifier exists" (Test-Path $verifier)
Check "W7 statuses exist" ((Test-Contains $report "FieldWriteStatus") -and (Test-Contains $report "KEPT_CHANGED") -and (Test-Contains $report "UNSUPPORTED"))
Check "W8 Set absent -> Unsupported" ((Test-Contains $verifier "afterValue == null -> FieldWriteStatus.UNSUPPORTED") -or (Test-Contains $verifier "FieldWriteStatus.UNSUPPORTED"))
Check "W8 Set mismatch" (Test-Contains $verifier "FieldWriteStatus.MISMATCH")
Check "W8 Clear mismatch" (Test-Contains $verifier "FieldWriteStatus.CLEARED")
Check "W8 Keep changed" (Test-Contains $verifier "FieldWriteStatus.KEPT_CHANGED")
Check "W9 file statuses" ((Test-Contains $report "SUCCEEDED") -and (Test-Contains $report "PARTIAL") -and (Test-Contains $report "FAILED") -and (Test-Contains $report "SKIPPED"))
Check "W9 file status derives from fields" (Test-Contains $verifier "fun fileStatus")

# V: ViewModel
Check "V1 BatchTagEditorViewModel exists" (Test-Path $vm)
Check "V1 accepts List<Long>" (Test-Contains $vm "songIds: List<Long>")
Check "V1 no first-file shortcut" ((Test-NotContains $vm "target.first") -and (Test-NotContains $vm "contents.first()"))
Check "V3 resolves through Repository.songById" (Test-Contains $vm "repository.songById")
Check "V4 reads every readable song" (Test-Contains $vm "MetadataReader(song.uri, readPictures = false)")
Check "V5 Common/Varies/Absent" ((Test-Contains $vm "data class Common") -and (Test-Contains $vm "data object Varies") -and (Test-Contains $vm "data object Absent"))
Check "V5 mixed absence becomes varies" ((Test-Contains $vm "first != value") -and (Test-Contains $vm "varies = true"))
Check "V6 limit enforced" ((Test-Contains $vm "MAX_BATCH_SONGS") -and (Test-Contains $vm "ids.size > MAX_BATCH_SONGS"))
Check "V7 EditTarget.Content built" (Test-Contains $vm "EditTarget.Content(song.id, song.uri, song.data)")
Check "V7 no new EditTarget type" (Test-NotContains $vm "Type.MultipleSongs")

# U: UI
Check "U1 screen exists" (Test-Path $screen)
Check "U1 one row per field" ((Test-Contains $screen "items(state.fields") -and (Test-Contains $screen "BatchFieldEditor"))
Check "U1 Keep default control" ((Test-Contains $screen "FieldOperation.Keep") -and (Test-Contains $screen "batch_tag_editor_keep"))
Check "U1 Set/Clear controls" ((Test-Contains $screen "FieldOperation.Set") -and (Test-Contains $screen "FieldOperation.Clear"))
Check "U2 common value / varies / absent" ((Test-Contains $screen "FieldAggregate.Common") -and (Test-Contains $screen "FieldAggregate.Varies") -and (Test-Contains $screen "FieldAggregate.Absent"))
Check "U2 Set prefill common or blank" ((Test-Contains $screen "state.commonValue.orEmpty()") -and (Test-Contains $screen "FieldOperation.Set(value)"))
Check "U3 blank Set blocks save" ((Test-Contains $screen "state.hasBlankSet") -and (Test-Contains $screen "state.canSave"))
Check "U3 Clear separate from blank" (Test-Contains $screen "batch_tag_editor_clear")
Check "U4 save summary and progress" ((Test-Contains $screen "batch_tag_editor_save_summary") -and (Test-Contains $screen "LinearProgressIndicator"))
Check "U5 per-file report" ((Test-Contains $screen "FileReportCard") -and (Test-Contains $screen "FileWriteReport"))
Check "U5 field statuses shown" (Test-Contains $screen "fieldStatusLabel")
Check "U5 no blanket success string" (Test-NotContains $screen "changes_saved_successfully")
Check "U6 no cover/musicbrainz controls" ((Test-NotContains $screen "MusicBrainz") -and (Test-NotContains $screen "picture") -and (Test-NotContains $screen "cover"))
Check "U7 activity extends AbsBaseActivity" ((Test-Contains $activity "class BatchTagEditorActivity : AbsBaseActivity()") -or (Test-Contains $activity "class BatchTagEditorActivity : AbsBaseActivity"))
Check "U7 write request on save" (Test-Contains $activity "MediaStore.createWriteRequest")
Check "U8 back callback while writing" ((Test-Contains $activity "OnBackPressedCallback") -and (Test-Contains $activity "BatchTagEditorUiState.Writing"))

# E: entry and wiring
Check "E1 menu action" (Test-Contains $menuRes '@+id/action_tag_editor')
Check "E2 multi-select branch" (Test-Contains $menu "R.id.action_tag_editor")
Check "E3 activity intent extra" ((Test-Contains $activity "EXTRA_SONG_IDS") -and (Test-Contains $activity "LongArray"))
Check "E4 dedupe/limit before launch" ((Test-Contains $menu "distinct()") -and (Test-Contains $menu "MAX_BATCH_SONGS"))
Check "E5 manifest registration" (Test-Contains $manifest "BatchTagEditorActivity")
Check "E6 Koin registration" (Test-Contains $mainModule "BatchTagEditorViewModel")
Check "E7 legacy TagEditorViewModel registration remains" (Test-Contains $mainModule "TagEditorViewModel(")
Check "E8 string resources exist" ((Test-Contains $strings "batch_tag_editor_title") -and (Test-Contains $strings "batch_tag_editor_set_blank"))

# S: scope / legacy guard
Check "S1 no Gradle dependency change" (-not (git diff --name-only -- gradle/libs.versions.toml app/build.gradle.kts))
Check "S2 no version change" (-not (git diff -U0 app/build.gradle.kts | Select-String -Pattern "versionCode|versionName" -Quiet))
Check "S3 no Room database version change" (-not (git diff -U0 -- app/src/main/java/com/ssilencce/core/SsilencceDatabase.kt | Select-String -Pattern "version\s*=" -Quiet))
Check "S3 no new Room files" (-not (git status --porcelain -- app/src/main/java/com/ssilencce/data/local/room | Select-String -Pattern "^\?\?" -Quiet))
Check "S4 no translated strings change" (-not (git diff --name-only -- "app/src/main/res/values-*"))
Check "S5 no new manifest permission" (-not (git diff -U0 app/src/main/AndroidManifest.xml | Select-String -Pattern "uses-permission" -Quiet))
$aggregator = "app/src/main/java/com/ssilencce/ui/screen/tageditor/LegacyTagAggregator.kt"
$albumEditor = "app/src/main/java/com/ssilencce/ui/screen/tageditor/AlbumTagEditorActivity.kt"
$artistEditor = "app/src/main/java/com/ssilencce/ui/screen/tageditor/ArtistTagEditorActivity.kt"

Check "S6 legacy aggregator reads every selected file" ((Test-Path $aggregator) -and (Test-Contains $aggregator "MetadataReader(uri, readPictures = false)") -and (Test-Contains $aggregator "uris.forEach") -and (Test-Contains $aggregator "if (!reader.hasMetadata)") -and (Test-Contains $aggregator "totalCount = uris.size"))
Check "S6 album aggregates every selected file" (Test-Contains $albumEditor "aggregateTagValues(viewModel.uris, ALBUM_FIELDS)")
Check "S6 artist aggregates every selected file" (Test-Contains $artistEditor "aggregateTagValues(viewModel.uris, ARTIST_FIELDS)")
Check "S6 album writes only dirty fields" ((Test-Contains $albumEditor "get() = dirtyPropertyMap()") -and (Test-Contains $albumEditor "addIfChanged(TagEditField.ALBUM"))
Check "S6 artist writes only dirty fields" ((Test-Contains $artistEditor "get() = dirtyPropertyMap()") -and (Test-Contains $artistEditor "addIfChanged(TagEditField.ARTIST"))
Check "S6 legacy editors migrated to TagEditField keys" ((Test-NotContains $albumEditor "MetadataReader.") -and (Test-NotContains $artistEditor "MetadataReader.") -and (Test-Contains $aggregator "field.propertyKey"))
Check "S6 no first-file read in legacy editors" ((Test-NotContains $albumEditor "target.first") -and (Test-NotContains $artistEditor "target.first") -and (Test-Contains $albumEditor "viewModel.uris") -and (Test-Contains $artistEditor "viewModel.uris"))
Check "S6 album shows varies" ((Test-Contains $albumEditor "tag_editor_varies") -and (Test-Contains $albumEditor "setVariesHint"))
Check "S6 artist shows varies" ((Test-Contains $artistEditor "tag_editor_varies") -and (Test-Contains $artistEditor "setVariesHint"))
Check "S6 album destructive fields confirmation-gated" ((Test-Contains $albumEditor "tag_editor_destructive_title") -and (Test-Contains $albumEditor "DESTRUCTIVE_FIELDS") -and (Test-Contains $albumEditor "TagEditField.TRACK_TOTAL") -and (Test-Contains $albumEditor "TagEditField.DISC_NUMBER") -and (Test-Contains $albumEditor "TagEditField.DISC_TOTAL"))
Check "S6 artist destructive field confirmation-gated" ((Test-Contains $artistEditor "tag_editor_destructive_title") -and (Test-Contains $artistEditor "DESTRUCTIVE_FIELDS = setOf(TagEditField.DISC_TOTAL)"))
Check "S6 artist conditional ARTIST clear removed" ((Test-NotContains $artistEditor "put(MetadataReader.ARTIST, albumArtist)") -and (Test-NotContains $artistEditor "isSingleSong"))
Check "S6 artist explicit ARTIST rename retained" ((Test-Contains $artistEditor "addIfChanged(TagEditField.ARTIST") -and (Test-Contains "app/src/main/java/com/ssilencce/core/model/tag/TagEditField.kt" "ARTIST(MetadataReader.ARTIST"))
Check "S6 single-entity handlers still launch legacy guards" ((Test-Contains $menu "AlbumTagEditorActivity") -and (Test-Contains $menu "ArtistTagEditorActivity"))
Check "S6 multi-select still launches the batch editor" ((Test-Contains $menu "BatchTagEditorActivity") -and (Test-Contains $activity "EXTRA_SONG_IDS") -and (Test-Contains $vm "repository.songById"))
Check "S7 MISSING_FEATURES updated" ((Test-Contains "docs/MISSING_FEATURES.md" "multi-select text-field") -and (Test-Contains "docs/MISSING_FEATURES.md" "batch covers"))
Check "S8 FEATURES/CODE_MAP updated" ((Test-Contains "docs/FEATURES.md" "Multi-song batch tag editor") -and (Test-Contains "docs/CODE_MAP.md" "BatchTagEditorActivity.kt"))


# F: AA-07 follow-up (2026-11-08): per-file plan and cooperative cancellation
Check "F1 BatchTagEditPlan exists" (Test-Contains $plan "data class BatchTagEditPlan")
Check "F2 per-song override map" ((Test-Contains $plan "perSong") -and (Test-Contains $plan "fun planFor(songId: Long)"))
Check "F3 per-song Keep overrides common Set" ((Test-Contains $plan "if (operation is FieldOperation.Keep)") -and (Test-Contains $plan "effective.remove(field)"))
Check "F4 changed-field union" ((Test-Contains $plan "val changedFields: Set<TagEditField>") -and (Test-Contains $plan "perSong.values.forEach"))
Check "F5 batch plan validity checks per-song blanks" ((Test-Contains $plan "perSong.values.none") -and (Test-Contains $plan "it.value.isBlank()"))
Check "F6 writer accepts BatchTagEditPlan" (Test-Contains $writer "plan: BatchTagEditPlan")
Check "F7 writer uses effective plan per file" ((Test-Contains $writer "val filePlan = plan.planFor(content.id)") -and (Test-Contains $writer "TagWriteVerifier.verify(filePlan"))
Check "F8 writer reports UNCHANGED files" ((Test-Contains $writer "FileWriteStatus.UNCHANGED") -and (Test-Contains $writer "no_changes"))
Check "F9 writer cooperative cancellation" ((Test-Contains $writer "currentCoroutineContext().ensureActive()") -and (Test-Contains $writer "shouldContinue") -and (Test-Contains $writer "FileWriteStatus.CANCELLED"))
Check "F10 writer cleanup on cancellation" ((Test-Contains $writer "NonCancellable") -and (Test-Contains $writer "runCatching { scanFiles"))
Check "F11 report has cancellation/unchanged counts" ((Test-Contains $report "unchangedFiles") -and (Test-Contains $report "cancelledFiles") -and (Test-Contains $report "UNCHANGED") -and (Test-Contains $report "CANCELLED"))
Check "F12 empty requested set is UNCHANGED" (Test-Contains $verifier "requested.isEmpty() -> FileWriteStatus.UNCHANGED")
Check "F13 ViewModel prepare/write jobs" ((Test-Contains $vm "prepareJob") -and (Test-Contains $vm "writeJob") -and (Test-Contains $vm "fun cancelPrepare") -and (Test-Contains $vm "fun stopWriting"))
Check "F14 ViewModel cooperative reads" ((Test-Contains $vm "currentCoroutineContext().ensureActive()") -and (Test-Contains $vm "withContext(Dispatchers.IO)"))
Check "F15 ViewModel skipped-file evidence" ((Test-Contains $vm "BatchSkippedFile") -and (Test-Contains $vm "BatchSkipReason.MISSING") -and (Test-Contains $vm "BatchSkipReason.UNREADABLE") -and (Test-Contains $vm "skippedFiles"))
Check "F16 ViewModel per-file operations" ((Test-Contains $vm "fun setPerFileOperation") -and (Test-Contains $vm "fun openPerFile") -and (Test-Contains $vm "fun closePerFile") -and (Test-Contains $vm "perFileField"))
Check "F17 ViewModel passes BatchTagEditPlan and shouldContinue" ((Test-Contains $vm "BatchTagEditPlan(") -and (Test-Contains $vm "shouldContinue = { !stopRequested }"))
Check "F18 Screen has per-file editor" ((Test-Contains $screen "PerFileFieldDialog") -and (Test-Contains $screen "onOpenPerFile") -and (Test-Contains $screen "onPerFileOperation"))
Check "F19 Screen has stop action" ((Test-Contains $screen "onStop") -and (Test-Contains $screen "batch_tag_editor_stop"))
Check "F20 Screen has pre-save skipped list" ((Test-Contains $screen "batch_tag_editor_show_skipped") -and (Test-Contains $screen "batch_tag_editor_skipped_line"))
Check "F21 Screen caps long aggregate preview" ((Test-Contains $screen "maxLines = 4") -and (Test-Contains $screen "TextOverflow.Ellipsis"))
Check "F22 Screen maps unknown errors" ((Test-Contains $screen "batch_tag_editor_error_unexpected") -and (Test-NotContains $screen "state.detail.orEmpty()"))
Check "F23 Activity wires per-file, stop and prepare cancel" ((Test-Contains $activity "onPerFileOperation") -and (Test-Contains $activity "onStop") -and (Test-Contains $activity "cancelPrepare"))
Check "F24 Activity resets write permission flag" ((Test-Contains $activity "writePermissionRequested = false"))
Check "F25 Legacy album/artist editors still present" ((Test-Path $albumEditor) -and (Test-Path $artistEditor))
Check "F26 Legacy guards still launch from single-entity actions" ((Test-Contains $menu "AlbumTagEditorActivity") -and (Test-Contains $menu "ArtistTagEditorActivity"))
Check "F27 Artist layout has explicit artist field" ((Test-Contains "app/src/main/res/layout/tag_editor_artist_field.xml" "@+id/artist") -and (Test-Contains "app/src/main/res/layout/tag_editor_artist_field.xml" "@string/album_artist"))
Check "F28 Destructive strings exist" ((Test-Contains $strings "tag_editor_destructive_title") -and (Test-Contains $strings "tag_editor_destructive_message"))

Write-Output "AA-07 static probe: PASS $script:pass / FAIL $script:fail"
if ($script:fail -gt 0) {
    Write-Output "Failures:"
    $script:failures | ForEach-Object { Write-Output " - $_" }
    exit 1
}
exit 0

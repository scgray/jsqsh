## Variable

  `multiline_editing` - Controls ability to perform multi-line line editing

## Description

  When JLine is in use (during an interactive jsqsh session), the `${multiline_editing}`
  variable controls whether or not multi-line editing is in use. When enabled, the jsqsh
  line editor treats an entire SQL statement as a single local line and allows editing
  of the entire statement via the line editor.  When disabled the behavior falls back
  to that of jsqsh prior to 3.0, which allows line editing of only the current line of
  the statement.

  Under most circumstances it is expected that you would want this feature enabled.

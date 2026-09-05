Set fso = CreateObject("Scripting.FileSystemObject")
strScriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = strScriptDir
WshShell.Run Chr(34) & strScriptDir & "\start_server.bat" & Chr(34), 0, False
Set WshShell = Nothing
Set fso = Nothing


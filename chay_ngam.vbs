Set WshShell = CreateObject("WScript.Shell")
strCurrentDir = WshShell.CurrentDirectory
WshShell.Run Chr(34) & strCurrentDir & "\start_server.bat" & Chr(34), 0
Set WshShell = Nothing

#define MyAppName "Pharmacy POS Pilot"
#define MyAppVersion "0.1.0"
#define MyAppPublisher "Pharmacy POS"
#define MyAppExeName "PharmacyPOS-Pilot-Setup.exe"

[Setup]
AppId={{4A363356-E057-4EA6-955D-D260C82DD89A}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\Pharmacy POS Pilot
DefaultGroupName=Pharmacy POS Pilot
DisableProgramGroupPage=yes
OutputDir=..\outputs\installer
OutputBaseFilename=PharmacyPOS-Pilot-Setup
SetupIconFile=..\pharmacy-frontend\src\app\favicon.ico
UninstallDisplayIcon={app}\pharmacy-frontend\src\app\favicon.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=no
RestartApplications=no

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: checkedonce

[Files]
Source: "..\docker-compose.pilot.yml"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\Dockerfile"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\pom.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\mvnw"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\mvnw.cmd"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\.mvn\*"; DestDir: "{app}\.mvn"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\src\*"; DestDir: "{app}\src"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\connectors\*"; DestDir: "{app}\connectors"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\README.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\API.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\pharmacy-frontend\*"; DestDir: "{app}\pharmacy-frontend"; Excludes: "node_modules\*,.next\*,coverage\*,out\*,.env.local,*.log,tsconfig.tsbuildinfo"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "scripts\*"; DestDir: "{app}\installer\scripts"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "README.md"; DestDir: "{app}\installer"; Flags: ignoreversion
Source: "pilot-release.json"; DestDir: "{app}\installer"; Flags: ignoreversion

[Icons]
Name: "{group}\Open Pharmacy POS"; Filename: "{app}\installer\scripts\open-pos.cmd"; WorkingDir: "{app}"; IconFilename: "{app}\pharmacy-frontend\src\app\favicon.ico"
Name: "{group}\Start Pharmacy POS"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\installer\scripts\start-pilot.ps1"" -InstallDir ""{app}"""; WorkingDir: "{app}"; IconFilename: "{app}\pharmacy-frontend\src\app\favicon.ico"
Name: "{group}\Stop Pharmacy POS"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\installer\scripts\stop-pilot.ps1"" -InstallDir ""{app}"""; WorkingDir: "{app}"
Name: "{group}\Check Pharmacy POS"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -NoExit -File ""{app}\installer\scripts\status-pilot.ps1"" -InstallDir ""{app}"""; WorkingDir: "{app}"
Name: "{group}\Back up Pharmacy POS"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -NoExit -File ""{app}\installer\scripts\backup-pilot.ps1"" -InstallDir ""{app}"""; WorkingDir: "{app}"
Name: "{group}\Restore Pharmacy POS"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -NoExit -File ""{app}\installer\scripts\restore-pilot.ps1"" -InstallDir ""{app}"""; WorkingDir: "{app}"
Name: "{group}\Check for Pharmacy POS updates"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -NoExit -File ""{app}\installer\scripts\check-updates.ps1"" -CurrentVersion ""{#MyAppVersion}"" -Repository ""Mark-Gachau/pos"""; WorkingDir: "{app}"
Name: "{autodesktop}\Pharmacy POS"; Filename: "{app}\installer\scripts\open-pos.cmd"; WorkingDir: "{app}"; IconFilename: "{app}\pharmacy-frontend\src\app\favicon.ico"; Tasks: desktopicon

[Run]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\installer\scripts\install-pilot.ps1"" -InstallDir ""{app}"" -NoOpen"; StatusMsg: "Building and starting Pharmacy POS. This can take several minutes..."; Flags: runhidden waituntilterminated
Filename: "{app}\installer\scripts\open-pos.cmd"; Description: "Open Pharmacy POS"; Flags: postinstall nowait skipifsilent

[UninstallRun]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\installer\scripts\stop-pilot.ps1"" -InstallDir ""{app}"""; Flags: runhidden waituntilterminated; RunOnceId: "StopPharmacyPOS"

[Code]
function InitializeSetup(): Boolean;
var
  DockerDesktopPath: String;
begin
  DockerDesktopPath := ExpandConstant('{pf}\Docker\Docker\Docker Desktop.exe');
  Result := FileExists(DockerDesktopPath);
  if not Result then
    MsgBox(
      'Pharmacy POS Pilot currently requires Docker Desktop.' + #13#10 + #13#10 +
      'Install Docker Desktop, start it once, and then run this setup again.',
      mbError,
      MB_OK
    );
end;

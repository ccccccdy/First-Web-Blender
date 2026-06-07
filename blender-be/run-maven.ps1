$Maven = 'C:\maven\apache-maven-3.6.1\bin\mvn.cmd'
$Settings = Join-Path $PSScriptRoot 'maven-settings.xml'

if (-not (Test-Path $Maven)) {
    Write-Error "找不到 Maven：$Maven"
    exit 1
}

if (-not (Test-Path $Settings)) {
    Write-Error "找不到项目 Maven 配置：$Settings"
    exit 1
}

& $Maven -s $Settings @args

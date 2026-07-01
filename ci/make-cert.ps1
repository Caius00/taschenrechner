# Erzeugt ein selbstsigniertes Code-Signing-Zertifikat KOMPLETT im Arbeitsspeicher.
# Umgeht den Windows-Zertifikatspeicher (kein Profil/Adminrechte noetig) und damit
# den Fehler NTE_PERM ("Access denied"), der auf dem GitLab-Windows-Runner auftritt.
#
# Ergebnis: devcert.pfx (zum Signieren) + devcert.cer (zum Vertrauen/Installieren)

param(
    [string]$Subject  = "CN=Taschenrechner",
    [string]$PfxPath  = "devcert.pfx",
    [string]$CerPath  = "devcert.cer",
    [string]$Password = "Taschenrechner!2026"
)

$ErrorActionPreference = "Stop"

$rsa = [System.Security.Cryptography.RSA]::Create(2048)
try {
    $dn = New-Object System.Security.Cryptography.X509Certificates.X500DistinguishedName $Subject
    $req = New-Object System.Security.Cryptography.X509Certificates.CertificateRequest(
        $dn, $rsa,
        [System.Security.Cryptography.HashAlgorithmName]::SHA256,
        [System.Security.Cryptography.RSASignaturePadding]::Pkcs1)

    # Key Usage: Digitale Signatur
    $req.CertificateExtensions.Add(
        (New-Object System.Security.Cryptography.X509Certificates.X509KeyUsageExtension(
            [System.Security.Cryptography.X509Certificates.X509KeyUsageFlags]::DigitalSignature, $true)))

    # Basic Constraints: keine CA
    $req.CertificateExtensions.Add(
        (New-Object System.Security.Cryptography.X509Certificates.X509BasicConstraintsExtension(
            $false, $false, 0, $true)))

    # Enhanced Key Usage: Code Signing (1.3.6.1.5.5.7.3.3)
    $oids = New-Object System.Security.Cryptography.OidCollection
    [void]$oids.Add((New-Object System.Security.Cryptography.Oid("1.3.6.1.5.5.7.3.3")))
    $req.CertificateExtensions.Add(
        (New-Object System.Security.Cryptography.X509Certificates.X509EnhancedKeyUsageExtension($oids, $true)))

    $notBefore = [System.DateTimeOffset]::UtcNow.AddDays(-1)
    $notAfter  = [System.DateTimeOffset]::UtcNow.AddYears(5)
    $cert = $req.CreateSelfSigned($notBefore, $notAfter)

    $pfxBytes = $cert.Export(
        [System.Security.Cryptography.X509Certificates.X509ContentType]::Pfx, $Password)
    [System.IO.File]::WriteAllBytes((Join-Path (Get-Location) $PfxPath), $pfxBytes)

    $cerBytes = $cert.Export(
        [System.Security.Cryptography.X509Certificates.X509ContentType]::Cert)
    [System.IO.File]::WriteAllBytes((Join-Path (Get-Location) $CerPath), $cerBytes)

    Write-Host "Zertifikat erzeugt: $PfxPath und $CerPath (Subject: $Subject)"
}
finally {
    $rsa.Dispose()
}

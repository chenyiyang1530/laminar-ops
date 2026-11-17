# 备份文件 AES-256-CBC + HMAC-SHA256 加解密。
# 格式：LAMENC1 + salt(16) + iv(16) + ciphertext + hmac(32)

function Get-BackupKeyMaterial([string]$Password, [byte[]]$Salt) {
  $derive = [System.Security.Cryptography.Rfc2898DeriveBytes]::new(
    $Password,
    $Salt,
    100000,
    [System.Security.Cryptography.HashAlgorithmName]::SHA256)
  try {
    return $derive.GetBytes(64)
  } finally {
    $derive.Dispose()
  }
}

function Protect-BackupFile([string]$SourcePath, [string]$Password) {
  if ([string]::IsNullOrWhiteSpace($Password)) { throw "未设置 BACKUP_ENCRYPTION_PASSWORD" }
  $plain = [System.IO.File]::ReadAllBytes($SourcePath)
  $salt = [byte[]]::new(16)
  $iv = [byte[]]::new(16)
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($salt)
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($iv)
  $material = Get-BackupKeyMaterial $Password $salt
  $aesKey = $material[0..31]
  $macKey = $material[32..63]
  $aes = [System.Security.Cryptography.Aes]::Create()
  try {
    $aes.Key = $aesKey
    $aes.IV = $iv
    $aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
    $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
    $encryptor = $aes.CreateEncryptor()
    $cipher = $encryptor.TransformFinalBlock($plain, 0, $plain.Length)
    $magic = [Text.Encoding]::ASCII.GetBytes("LAMENC1")
    $header = [byte[]]::new(7 + 16 + 16)
    [Array]::Copy($magic, 0, $header, 0, 7)
    [Array]::Copy($salt, 0, $header, 7, 16)
    [Array]::Copy($iv, 0, $header, 23, 16)
    $authenticated = [byte[]]::new($header.Length + $cipher.Length)
    [Array]::Copy($header, 0, $authenticated, 0, $header.Length)
    [Array]::Copy($cipher, 0, $authenticated, $header.Length, $cipher.Length)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($macKey)
    try { $tag = $hmac.ComputeHash($authenticated) } finally { $hmac.Dispose() }
    $output = [byte[]]::new($authenticated.Length + $tag.Length)
    [Array]::Copy($authenticated, 0, $output, 0, $authenticated.Length)
    [Array]::Copy($tag, 0, $output, $authenticated.Length, $tag.Length)
    $target = "$SourcePath.enc"
    [System.IO.File]::WriteAllBytes($target, $output)
    return $target
  } finally {
    $aes.Dispose()
  }
}

function Unprotect-BackupFile([string]$SourcePath, [string]$OutputPath, [string]$Password) {
  if ([string]::IsNullOrWhiteSpace($Password)) { throw "未设置 BACKUP_ENCRYPTION_PASSWORD" }
  $data = [System.IO.File]::ReadAllBytes($SourcePath)
  if ($data.Length -lt 7 + 16 + 16 + 32) { throw "备份密文长度不正确" }
  $magic = [Text.Encoding]::ASCII.GetString($data, 0, 7)
  if ($magic -ne "LAMENC1") { throw "不是 laminar 加密备份" }
  $salt = $data[7..22]
  $iv = $data[23..38]
  $cipherLength = $data.Length - 7 - 16 - 16 - 32
  $cipher = $data[39..(38 + $cipherLength)]
  $tag = $data[($data.Length - 32)..($data.Length - 1)]
  $header = $data[0..38]
  $authenticated = [byte[]]::new($header.Length + $cipher.Length)
  [Array]::Copy($header, 0, $authenticated, 0, $header.Length)
  [Array]::Copy($cipher, 0, $authenticated, $header.Length, $cipher.Length)
  $material = Get-BackupKeyMaterial $Password $salt
  $aesKey = $material[0..31]
  $macKey = $material[32..63]
  $hmac = [System.Security.Cryptography.HMACSHA256]::new($macKey)
  try {
    $expected = $hmac.ComputeHash($authenticated)
    $diff = 0
    for ($i = 0; $i -lt $expected.Length; $i++) {
      $diff = $diff -bor ($expected[$i] -bxor $tag[$i])
    }
    if ($diff -ne 0) { throw "备份密码错误或文件被篡改" }
  } finally { $hmac.Dispose() }
  $aes = [System.Security.Cryptography.Aes]::Create()
  try {
    $aes.Key = $aesKey
    $aes.IV = $iv
    $aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
    $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
    $decryptor = $aes.CreateDecryptor()
    $plain = $decryptor.TransformFinalBlock($cipher, 0, $cipher.Length)
    [System.IO.File]::WriteAllBytes($OutputPath, $plain)
    return $OutputPath
  } finally { $aes.Dispose() }
}

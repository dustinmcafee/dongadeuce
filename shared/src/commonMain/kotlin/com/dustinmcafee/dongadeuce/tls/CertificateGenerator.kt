package com.dustinmcafee.dongadeuce.tls

expect fun generateOrLoadCertificate(
    keystorePath: String,
    keystorePassword: String = "dongadeuce",
    keyAlias: String = "dongadeuce",
    privateKeyPassword: String = "dongadeuce"
): CertificateInfo

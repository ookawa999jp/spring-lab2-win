package com.example.springlab.security;

public final class SecurityPaths {

    // 認証不要で公開するエンドポイント。
    //
    // 【一時対応】元の NestJS 版は認証なしで /items を叩ける仕様のため、それに合わせて
    // "/items/**" を公開している。既存の認証機構（SecurityConfig の oauth2 リソースサーバ、
    // AuthController 等）はそのまま残してある。
    // 本番相当に戻す場合は、下の "/items/**" の行を削除すれば /items は認証必須に戻る。
    public static final String[] PUBLIC_ENDPOINTS = {"/auth/login", "/items/**"};

    private SecurityPaths() {
    }
}

package com.healthlog.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.healthlog.demo.entity.AuthToken;
import com.healthlog.demo.entity.AuthToken.TokenType;

import jakarta.persistence.LockModeType;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

	boolean existsByToken(String token);

	// idx_auth_tokens_token (unique) — トークン単体での照合
	Optional<AuthToken> findByToken(String token);

	// verifyEmail() / confirmPasswordReset() — トークン + 種別での照合
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuthToken> findByTokenAndTokenType(String token, TokenType tokenType);

	// resendVerification() のクールダウン判定用 — ユーザー・種別ごとの最新トークン発行日時を取得
	Optional<AuthToken> findFirstByUser_IdAndTokenTypeOrderByCreatedAtDesc(Long userId, TokenType tokenType);

	// idx_auth_tokens_user_type — ユーザーごとのトークン種別検索
	List<AuthToken> findByUser_IdAndTokenType(Long userId, TokenType tokenType);

	// ユーザーごとの未使用トークンのみ取得（再送信・再発行前の確認用）
	List<AuthToken> findByUser_IdAndTokenTypeAndUsedFlgFalse(Long userId, TokenType tokenTypeOtp);

	// トークン文字列で削除（使わない場合は残しておいても害なし）
	void deleteByToken(String token);

	// idx_auth_tokens_expire — 期限切れトークンの一括削除（バッチ処理用）
	@Modifying
	@Query("DELETE FROM AuthToken t WHERE t.expiresAt < :now")
	int deleteAllExpiredTokens(@Param("now") LocalDateTime now);

	// resendVerification() / requestPasswordReset() — 既存の未使用トークンを無効化
	@Modifying
	@Query("UPDATE AuthToken t SET t.usedFlg = true WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.usedFlg = false")
	int invalidateActiveTokens(@Param("userId") Long userId, @Param("tokenType") String tokenType);
}
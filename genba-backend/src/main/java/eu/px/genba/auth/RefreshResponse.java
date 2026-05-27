package eu.px.genba.auth;

import lombok.Builder;

@Builder
public record RefreshResponse(
        String accessToken,
        long accessTokenExpiresInMs) {
}

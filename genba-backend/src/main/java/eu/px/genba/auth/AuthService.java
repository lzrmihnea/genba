package eu.px.genba.auth;

import eu.px.genba.common.exception.InvalidCredentialsException;
import eu.px.genba.common.exception.TokenInvalidException;
import eu.px.genba.common.exception.UserInactiveException;
import eu.px.genba.organization.UserOrganization;
import eu.px.genba.organization.UserOrganizationMapper;
import eu.px.genba.organization.UserOrganizationRepository;
import eu.px.genba.security.CurrentUser;
import eu.px.genba.security.GenbaUserPrincipal;
import eu.px.genba.security.JwtTokenProvider;
import eu.px.genba.security.JwtTokenType;
import eu.px.genba.user.User;
import eu.px.genba.user.UserMapper;
import eu.px.genba.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserMapper userMapper;
    private final UserOrganizationMapper userOrganizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findActiveByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isActive()) {
            throw new UserInactiveException();
        }

        return buildLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public RefreshResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = tokenProvider.parseClaims(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidException(e);
        }

        if (tokenProvider.extractType(claims) != JwtTokenType.REFRESH) {
            throw new TokenInvalidException();
        }

        UUID userId = tokenProvider.extractUserId(claims);
        User user = userRepository.findActiveById(userId)
                .orElseThrow(TokenInvalidException::new);
        if (!user.isActive()) {
            throw new UserInactiveException();
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        return RefreshResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresInMs(tokenProvider.getAccessTokenExpirationMs())
                .build();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        GenbaUserPrincipal principal = currentUser.get()
                .orElseThrow(TokenInvalidException::new);
        User user = userRepository.findActiveById(principal.userId())
                .orElseThrow(TokenInvalidException::new);
        List<UserOrganizationDtoEntry> memberships = loadMemberships(user.getId());
        return CurrentUserResponse.builder()
                .user(userMapper.toDto(user))
                .organizations(memberships.stream().map(e -> e.dto).toList())
                .build();
    }

    /**
     * No-op for Layer 0. Layer 1+ will blacklist the JWT via a Redis-backed
     * revocation list keyed by the token's {@code jti} claim. The endpoint
     * is exposed today so the frontend can wire its discard-tokens action
     * against a stable URL.
     */
    public void logout() {
        // Intentional no-op for L0.
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        List<UserOrganizationDtoEntry> memberships = loadMemberships(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresInMs(tokenProvider.getAccessTokenExpirationMs())
                .user(userMapper.toDto(user))
                .organizations(memberships.stream().map(e -> e.dto).toList())
                .build();
    }

    private List<UserOrganizationDtoEntry> loadMemberships(UUID userId) {
        List<UserOrganization> rows = userOrganizationRepository.findActiveByUserId(userId);
        return rows.stream()
                .map(uo -> new UserOrganizationDtoEntry(userOrganizationMapper.toDto(uo)))
                .toList();
    }

    /** Wrapper so the stream pipeline stays readable; one-shot internal type. */
    private record UserOrganizationDtoEntry(eu.px.genba.organization.UserOrganizationDto dto) {
    }
}
